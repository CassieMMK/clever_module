# -*- coding: utf-8 -*-
"""
【模块作用】POST /api/agent/chat/v2 的业务实现：规划 → 校验 → MCP 调工具 → 证据 →
「大模型两次调用」：先只做证据分析与结论文本，再单独生成 Markdown 报告，降低单次输出混杂导致的格式/事实错误。

【MCP】工具只在独立 MCP 进程执行；本模块通过 execute_tools_via_mcp 远程调用，不在 API 进程内直连 run_named_tool。

【追问】默认由接待大模型（v2_clarify_gate_llm）与用户对话后决定；context.llm_intake=false 时跳过，直接用用户原句规划。
"""
from __future__ import annotations  # 延迟注解

import json  # 序列化 context、evidence 给大模型
import logging  # MCP 失败等异常记录
from typing import Any  # body/返回体宽松 dict

from src.sessions import get_session  # 会话级 history
from src.planner.planner_llm import plan_with_llm  # 产出 tools JSON
from src.planner.validate_tools_plan import validate_tools_plan_dict  # 白名单校验（模块名 validate_tools_plan）
from src.mcp_client_v2 import execute_tools_via_mcp  # 并行 MCP callTool
from src.llm_client import report_chat_complete  # 分析、报告两次均走报告模型与报告超时
from src.v2_clarify_gate_llm import run_intake_gate  # 接待大模型：追问或进入规划

LOG = logging.getLogger("v2_service")

_VAGUE = frozenset({"查", "查询", "搜", "帮我查"})

_MARK_A = "<<<ANALYSIS>>>"  # 若第二步模型仍输出旧格式，用于切分
_MARK_R = "<<<MARKDOWN_REPORT>>>"  # 同上

# 以下长字符串注入大模型：与 tool_trace 对齐、防假链；0 行工具不写进报告；联网须逐条交代证据内条目以防幻觉（两步提示共用）
_TOOL_TRACE_RULES = (
    "【数据优先级——须严格遵守】\n"
    "1) 核心结论、定量统计、表格、占比、机构/课题/团队/奖项事实：凡本地库工具在 tool_trace 中**实际返回行数大于 0** 的，必须以对应 data.rows / count 为唯一依据；"
    "不得用联网内容覆盖或否定这些记录。若当前**没有任何**行数>0 的本地命中，则不作基于本地表的量化表述，可一句概括未检索到本地相关记录，再按需引用 web_search（遵守第 2、3 条）。\n"
    "2) web_search 为「公开信息补充」：凡在分析或报告中出现的联网观点、事实、数字，**必须能追溯到** tool_trace 里 web_search 的 data.results 的**具体条目**（按 results 数组顺序从 1 编号），"
    "每条须写清：**第几条**、**title**、以及从 **snippet** 中摘出的**原文短语**（十余字即可）作为你引用该条的依据；**禁止**编造 evidence 里不存在的网站名、文件、统计口径。"
    "若无 web_search 调用或 results 为空，不得写仿佛已联网检索到的具体细节。\n"
    "3) 链接、无链接原因与反幻觉：Markdown 中**禁止**写入 evidence JSON 里未出现、或未出现在该条 result.url 中的任何 http(s) 地址；"
    "仅当 results[i].url 为非空字符串时，才允许 `- [标题](该 url 原样)`。"
    "【为何会出现 url 为空】常见原因：① 云端检索可见片段里**没有**完整可复制的 http(s)；② 模型填的 URL **未出现在**工具原始输出 raw 中，后端会清空该 url，避免假链/死链。"
    "这表示「本条在工具返回里无可点开地址」，**不是**允许你在正文里凭想象补网址或虚构来源。\n"
    "若某条 url 为空：仍须写清来源第几条、标题、snippet 依据短语，并用**一句中性事实**说明该条未返回可点开链接即可。\n"
    "若 results 非空，正式报告中须有「## 联网补充参考」：**逐条**对应 results[1..n] 写出（有链则 Markdown 链；无链则写清标题+snippet 依据+无链接原因如上）。\n"
    "【读者可见用语】交付给用户的分析、报告、数据来源、联网补充等正文里，**禁止**出现对用户说教、警告式的链接提示（让读者觉得在被命令不要去补网址之类）。"
    "不编造链接等纪律**仅内化遵守**，用客观陈述即可，**不要**把内部写作纪律原文复述给用户看。\n"
    "4)「数据来源说明」：**只列**实际返回行数>0 的本地 query_*（工具名、大致行数或表名）；**不得**列出返回 0 行的工具，不得写「某某工具 0 条」之类。"
    "另起一段写 web_search：是否调用、共几条 results；**逐条**写第 i 条的 title、snippet 依据、url 或「该条无有效 http 链接（工具侧未返回）」。\n"
    "\n"
    "【工具结果书写纪律】对照 evidence.tool_trace 时请遵守：\n"
    "A) 某 query_* 若 result.ok 为 true 且 data.rows 为空或 count 为 0：**第 1 步分析与第 2 步报告均不得提及该工具**"
    "（不要写该工具名、不要写「0 条」「无匹配」「未查到」等针对该次查询的表述），数据来源说明中也**不要列出**该工具；仅依据**行数>0**的工具组织正文。\n"
    "B) 某 query_* 若 ok 为 true 且行数大于 0：报告正文、统计与表格必须与 data.rows / count 一致，禁止写「未采纳」「未使用」该工具结果。\n"
    "C) 用户问「趋势」而证据中课题/奖项年份集中在单一年度时，须在摘要或局限性中点明：当前为「该年度横截面」归纳，"
    "非多年度对比的动态趋势，除非证据中已出现多个年份可对比。\n"
    "D) 若 result.ok 为 false 且 result.error 中出现「超时」「Timeout」「time out」等字样，须表述为「本次工具执行超时，未能返回结果」；"
    "禁止把超时当成「已有数据但为 0 条」或编造「Excel 未导入」；**不要**将超时与「某工具 0 条」混为一谈（0 行工具本就不写入报告）。\n"
)


def _wants_report(message: str, ctx: dict[str, Any]) -> bool:
    """根据用户措辞与 context.output_type 决定是否要求更完整的 Markdown 报告段。"""
    ot = (ctx.get("output_type") or "").lower()
    if ot == "report":
        return True
    return any(x in message for x in ("报告", "导出", "下载"))


def _local_db_summary(trace: list[dict[str, Any]]) -> dict[str, Any]:
    """统计各本地 query_* 成功返回的行数合计，供报告模型判断「是否完全未命中本地」。"""
    by_tool: dict[str, int] = {}
    total = 0
    for step in trace:
        res = step.get("result") or {}
        tool = str(res.get("tool") or step.get("tool") or "")
        if tool == "web_search":
            continue
        if not res.get("ok"):
            by_tool[tool] = by_tool.get(tool, 0)
            continue
        data = res.get("data") or {}
        c = data.get("count")
        if c is None and isinstance(data.get("rows"), list):
            c = len(data["rows"])
        c = int(c or 0)
        by_tool[tool] = by_tool.get(tool, 0) + c
        total += c
    return {"local_total_rows": total, "by_tool": by_tool}


def _build_evidence(trace: list[dict[str, Any]], message: str) -> dict[str, Any]:
    """打包 evidence JSON：用户原话 + 本地汇总 + 完整 tool_trace。"""
    summ = _local_db_summary(trace)
    return {
        "version": "v2",
        "user_query": message,
        "local_db_summary": summ,
        "tool_trace": trace,
    }


def _split_analysis_report(raw: str) -> tuple[str, str | None]:
    """按固定标记切分输出为（分析段, 报告段）；第二步若模型误用旧模板，用此函数兜底取 Markdown。"""
    s = (raw or "").strip()
    if _MARK_R in s:
        a, b = s.split(_MARK_R, 1)
        a = a.replace(_MARK_A, "").strip()
        return a, b.strip() or None
    if _MARK_A in s:
        a = s.replace(_MARK_A, "").strip()
        return a, None
    return s, None


def _analysis_only_prompt(*, message: str, ev_text: str, zero_hint: str) -> str:
    """
    第一步：只要求模型读证据、定调、写分析结论；禁止写正式报告章节，减少与第二步抢上下文。
    """
    return (
        "你是社科数据分析助手。【当前为第 1 步：仅做证据解读与分析】\n"
        "任务：阅读证据 JSON（tool_trace），输出连贯中文分析段落。\n"
        "禁止：输出 Markdown 报告结构（不要用 # 章节标题写长报告）、不要使用 "
        + _MARK_A
        + " 或 "
        + _MARK_R
        + " 标记。\n"
        + _TOOL_TRACE_RULES
        + "\n"
        + zero_hint
        + "证据：\n"
        + ev_text
        + "\n用户问题：\n"
        + message
        + "\n\n请按条目组织（可用短句编号，但总篇幅仍是一段式分析即可）：\n"
        "1）**仅**写返回行数>0 的 query_*：工具名与要点；对 ok 且 0 行的工具**一句也不要写**；\n"
        "2）与用户问题直接相关的结论要点（数字须与 evidence 中行数>0 的工具一致）；\n"
        "3）web_search 若存在：**逐条**写清第几条、title、snippet 中依据短语；有 url 可写；无 url 说明「工具返回该条无可验证 http」；禁止无 evidence 的网址与事实；\n"
        "4）局限性：只写有数据或超时等**已写入上文**的情况；不要逐条复述 0 行工具。\n"
    )


def _report_from_analysis_prompt(
    *,
    message: str,
    ev_text: str,
    zero_hint: str,
    want_full_report: bool,
    analysis_text: str,
) -> str:
    """
    第二步：在第一步「分析要点」已锁定的前提下写 Markdown；仍附带证据片段便于核对表格与数字，避免仅凭记忆编造。
    """
    rep_hint = (
        "报告须含：摘要、数据概览（仅基于行数>0 的本地结果）、结论、数据来源说明（不列 0 行工具）；"
        "若有 web_search 则须含「## 联网补充参考」：**与 evidence 中 results 逐条对齐**（第几条、标题、snippet 依据短语、有 url 则链，无 url 则说明工具侧未返回可验证链接），"
        "使读者能核对每条联网内容来自工具返回而非模型臆造；读者可见处只用中性事实句，不出现对用户说教式链接提示（纪律内化，不必在正文重复内部规则）。"
        if want_full_report
        else "报告可精简：本地只写有行的工具；联网须逐条交代 title+snippet 依据+url 或无链接原因，与 evidence 对齐，避免幻觉。"
    )
    analysis_block = (analysis_text or "").strip()[:8000]  # 防止第一步过长撑爆上下文；保留主要定调内容
    return (
        "你是社科数据分析助手。【当前为第 2 步：仅写 Markdown 正式报告】\n"
        "下列「分析要点」为上一步已定结论，报告正文必须与之一致，不得出现与之矛盾的数量或事实。\n"
        + _TOOL_TRACE_RULES
        + "\n"
        + zero_hint
        + "【分析要点（第 1 步输出，须严格遵循）】\n"
        + analysis_block
        + "\n\n证据（供核对数字与表格；正文表述仍以上述分析要点与 evidence 为准）：\n"
        + ev_text
        + "\n用户问题：\n"
        + message
        + "\n\n"
        + rep_hint
        + "\n只输出 Markdown 报告正文（可用 # / ## / 表格等）。不要输出 "
        + _MARK_A
        + " / "
        + _MARK_R
        + " 标记，不要重复整段「分析要点」标题以外的第 1 步全文。\n"
    )


async def run_chat_turn_v2_async(body: dict[str, Any]) -> dict[str, Any]:
    """v2 主入口：async 供 FastAPI 路由 await。"""
    sid = str(body.get("session_id") or "default")
    display_user_message = str(body.get("message") or "").strip()
    ctx = body.get("context") or {}
    sess = get_session(sid)
    message = display_user_message

    if message in _VAGUE:
        msg = "请说明要查的机构、人员、课题、团队、奖项或需要联网检索的主题。"
        sess.history.append({"role": "user", "content": message})
        sess.history.append({"role": "assistant", "content": msg})
        return {
            "version": "v2",
            "session_id": sid,
            "need_clarify": True,
            "assistant_message": msg,
            "plan": None,
            "tool_trace": [],
            "result": None,
            "error": None,
        }

    try:
        gate = run_intake_gate(sess, display_user_message, ctx)
    except Exception as e:
        LOG.exception("intake_gate_failed session_id=%s", sid)
        gate = {"action": "proceed", "planner_user_message": display_user_message}

    if gate.get("action") == "clarify":
        am = str(gate.get("assistant_message") or "").strip() or "请再具体说明要查询的内容。"
        sess.history.append({"role": "user", "content": display_user_message})
        sess.history.append({"role": "assistant", "content": am})
        return {
            "version": "v2",
            "session_id": sid,
            "need_clarify": True,
            "assistant_message": am,
            "plan": None,
            "tool_trace": [],
            "result": {"llm_intake": True, "clarify_phase": "gate"},
            "error": None,
        }

    message = str(gate.get("planner_user_message") or display_user_message).strip()

    extra = json.dumps(ctx, ensure_ascii=False) if ctx else None
    try:
        plan_dict = plan_with_llm(message, extra)
    except Exception as e:
        return {
            "version": "v2",
            "session_id": sid,
            "need_clarify": False,
            "assistant_message": "",
            "plan": None,
            "tool_trace": [],
            "result": None,
            "error": {"code": "planner_failed", "message": str(e)},
        }

    ok, err, tool_specs = validate_tools_plan_dict(plan_dict)
    if not ok or tool_specs is None:
        return {
            "version": "v2",
            "session_id": sid,
            "need_clarify": False,
            "assistant_message": "",
            "plan": plan_dict,
            "tool_trace": [],
            "result": None,
            "error": {"code": "invalid_plan", "message": err or "invalid"},
        }

    try:
        trace = await execute_tools_via_mcp(tool_specs, session_id=sid)
    except Exception as e:
        LOG.exception("mcp_execute_failed session_id=%s", sid)
        return {
            "version": "v2",
            "session_id": sid,
            "need_clarify": False,
            "assistant_message": "",
            "plan": plan_dict,
            "tool_trace": [],
            "result": None,
            "error": {"code": "mcp_failed", "message": str(e)},
        }

    evidence = _build_evidence(trace, message)
    ev_text = json.dumps(evidence, ensure_ascii=False)[:12000]
    summ = evidence.get("local_db_summary") or {}
    zero_local = int(summ.get("local_total_rows") or 0) == 0
    zero_hint = ""
    if zero_local:
        zero_hint = (
            "【说明】当前本地 query_* 合计返回行数为 0。"
            "分析与报告中：**不要**逐工具写「某某工具 0 条」；允许用**一句**概括「未从本地库检索到与问题相关的记录」。"
            "禁止编造本地表中的具体行；不要展开冗长排障清单。\n\n"
        )

    want_report = _wants_report(message, ctx)

    # ---------- 第 1 次 LLM：只分析证据，不定稿长报告 ----------
    analysis_prompt = _analysis_only_prompt(message=message, ev_text=ev_text, zero_hint=zero_hint)
    analysis_text = report_chat_complete(
        [{"role": "user", "content": analysis_prompt}],
        enable_search=False,
        temperature=0.2,
    ).strip()

    # ---------- 第 2 次 LLM：基于分析要点写 Markdown；失败则回退为仅展示分析 ----------
    report_md: str | None = None
    try:
        report_prompt = _report_from_analysis_prompt(
            message=message,
            ev_text=ev_text,
            zero_hint=zero_hint,
            want_full_report=want_report,
            analysis_text=analysis_text,
        )
        report_raw = report_chat_complete(
            [{"role": "user", "content": report_prompt}],
            enable_search=False,
            temperature=0.25,
        )
        # 若模型仍输出旧版双段标记，从原始文本中尽量抽出报告段；否则整段视为 Markdown
        _, maybe_md = _split_analysis_report(report_raw)
        report_md = (maybe_md or (report_raw or "").strip()) or None
    except Exception as e:
        LOG.exception("report_llm_failed session_id=%s", sid)
        report_md = None

    out_msg = (report_md or analysis_text or "").strip()
    sess.history.append({"role": "user", "content": display_user_message})
    sess.history.append({"role": "assistant", "content": out_msg})

    return {
        "version": "v2",
        "session_id": sid,
        "need_clarify": False,
        "assistant_message": out_msg,
        "plan": plan_dict,
        "tool_trace": trace,
        "result": {
            "analysis": analysis_text,
            "markdown_report": report_md,
            "evidence": evidence,
        },
        "error": None,
    }
