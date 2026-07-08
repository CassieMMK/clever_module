# -*- coding: utf-8 -*-
"""
【模块作用】对话编排核心：槽位合并 → 缺参追问 → 否则依次调用
db_search → web_search → merge_evidence → analysis →（可选）report。

【MySQL 迁移】
- 本文件不直接写 SQL；若调整工具顺序或增加「只查库不联网」分支，在此修改即可。
- 机构关键词缺省时补默认年份区间（2000–2030）的逻辑可随产品口径调整，与 MySQL 无关。
"""
from __future__ import annotations

import time  # _step 内 perf_counter 式耗时用 time.time 差值毫秒
from typing import Any  # body/ctx 为宽松 dict

from src.evidence import merge_evidence  # 合成 evidence 包
from src.sessions import get_session  # 按 session_id 取会话
from src.slots import extract_slots, missing_for_search  # 抽槽、判断缺参
from src.tools.analysis import tool_analysis  # 大模型分析证据
from src.tools.db_search import tool_db_search  # 本地 SQL 检索
from src.tools.report import tool_report  # Markdown 报告
from src.tools.web_search import tool_web_search  # 联网检索


def _wants_report(message: str, ctx: dict[str, Any]) -> bool:
    """前端传 context.output_type=report，或用户消息含报告/导出/下载 时生成报告。"""
    ot = (ctx.get("output_type") or "").lower()
    if ot == "report":
        return True
    return any(x in message for x in ("报告", "导出", "下载"))


def _step(name: str, fn):
    """
    包装同步调用：记录步骤名、耗时、是否成功。
    fn 一般为 lambda: 已算好的结果（避免重复执行工具时此处仅用于计时占位）。
    """
    t0 = time.time()
    try:
        out = fn()
        ok = True if not isinstance(out, dict) else out.get("ok", True)
        return {"name": name, "status": "success" if ok else "failed", "duration_ms": int((time.time() - t0) * 1000)}
    except Exception as e:
        return {"name": name, "status": "failed", "duration_ms": int((time.time() - t0) * 1000), "error": str(e)}


def _merge_prev(sl, prev: dict[str, Any]) -> None:
    """
    把会话里上一轮已保存的槽位合并进当前 sl（原地修改）。
    用于多轮：用户第二轮只发「2024」时仍能继承第一轮「乡村振兴」。
    """
    if prev.get("region") and not sl.region:
        sl.region = prev.get("region")
    if prev.get("org_keyword") and not sl.org_keyword:
        sl.org_keyword = prev.get("org_keyword")
    if prev.get("topic") and not sl.topic:
        sl.topic = prev.get("topic")
    if prev.get("start_year") is not None and prev.get("end_year") is not None:
        if sl.start_year is None or sl.end_year is None:
            sl.start_year = int(prev["start_year"])
            sl.end_year = int(prev["end_year"])
            sl.raw_time = prev.get("raw_time")


def run_chat_turn(body: dict[str, Any]) -> dict[str, Any]:
    """
    POST /api/agent/chat 的唯一业务入口。
    body：session_id、message、可选 context（region/time_range/topic/output_type）。
    """
    # 会话 id 缺省为 default，与前端 demo 对齐
    sid = str(body.get("session_id") or "default")
    message = str(body.get("message") or "")
    ctx = body.get("context") or {}
    sess = get_session(sid)

    # 仅从用户文本规则抽槽；不含大模型
    base_slots = extract_slots(message)
    if ctx.get("region"):
        base_slots.region = ctx["region"]
    if ctx.get("time_range"):
        from src.slots import extract_years

        sy, ey, raw = extract_years(str(ctx["time_range"]))
        if sy and ey:
            base_slots.start_year, base_slots.end_year, base_slots.raw_time = sy, ey, raw
    if ctx.get("topic"):
        base_slots.topic = ctx["topic"]

    # 与历史 sess.slots 合并，支持多轮补全
    _merge_prev(base_slots, sess.slots)

    # 仅有机构、未提年份：给宽松默认区间，避免卡在追问时间
    if base_slots.org_keyword and base_slots.start_year is None:
        base_slots.start_year, base_slots.end_year = 2000, 2030
        base_slots.raw_time = "2000-2030"

    # dict.fromkeys 保持 miss 顺序且去重
    miss = list(dict.fromkeys(missing_for_search(base_slots)))

    if miss:
        sess.slots.update({k: v for k, v in base_slots.to_dict().items() if v is not None})
        if "region_or_org" in miss and "time_range" in miss:
            msg = "请补充**地区或机构关键词**以及**年份或时间区间**。"
        elif "region_or_org" in miss:
            msg = "请补充**地区**（地址关键词）或**机构名称关键词**（如社科院）。"
        else:
            msg = "请补充**年份或区间**（如 2024 或 2020-2024）。"
        sess.history.append({"role": "user", "content": message})
        sess.history.append({"role": "assistant", "content": msg})
        return {
            "status": "clarify",
            "assistant_message": msg,
            "missing_slots": miss,
            "filled_slots": base_slots.to_dict(),
            "steps": [],
            "result": None,
            "error": None,
        }

    steps = []
    sess.slots = base_slots.to_dict()

    # 本地两表检索（省课题 + 国奖）
    db_res = tool_db_search(base_slots)
    steps.append(_step("db_search", lambda: db_res))

    # 拼接联网查询串：地区/主题/机构/原句非空片段
    web_q = " ".join(x for x in [base_slots.region, base_slots.topic, base_slots.org_keyword, message] if x)
    web_res = tool_web_search(web_q)
    steps.append(_step("web_search", lambda: web_res))

    bundle = merge_evidence(db_res, web_res, {"user_query": message, "slots": base_slots.to_dict()})
    an = tool_analysis(bundle, message)
    steps.append(_step("analysis", lambda: an))

    report_md = None
    if _wants_report(message, ctx):
        rep = tool_report(bundle, an["data"]["text"], message)
        steps.append(_step("report", lambda: rep))
        report_md = rep["data"]["markdown"]

    sess.history.append({"role": "user", "content": message})
    out_msg = report_md or an["data"]["text"]
    sess.history.append({"role": "assistant", "content": out_msg})

    return {
        "status": "done",
        "assistant_message": out_msg,
        "missing_slots": [],
        "filled_slots": base_slots.to_dict(),
        "steps": steps,
        "result": {
            "summary": an["data"]["text"],
            "markdown_report": report_md,
            "evidence": bundle,
        },
        "error": None,
    }
