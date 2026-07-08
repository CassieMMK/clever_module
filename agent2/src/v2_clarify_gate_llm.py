# -*- coding: utf-8 -*-
"""
【模块作用】v2 入口「接待大模型」：与用户多轮对话，判断信息是否足以进入下游「规划 + 查库」。

- 不足：返回 action=clarify，由 assistant_message 直接回复用户（不调规划、不调 MCP）。
- 已够：返回 action=proceed，给出 planner_user_message 供 plan_with_llm 使用。

【关闭】context.llm_intake 为 False 时跳过本模块，直接使用用户原句规划。

【轮次】同一 session 内追问 assistant 最多 5 次，超出则强制合并用户侧发言进入规划，避免死循环。
"""
from __future__ import annotations

import json
import logging
from typing import Any

from src.config import get_settings
from src.llm_client import chat_complete
from src.planner.planner_llm import extract_json_object
from src.sessions import SessionState

LOG = logging.getLogger("v2_clarify_gate")

_SLOT_MSGS = "_v2_intake_thread"
_MAX_ASSISTANT_CLARIFIES = 5

INTAKE_SYSTEM = """你是「社科智能分析助手」的前台接待，只负责和用户用**自然中文**对话，判断何时可以进入后台查库。

【后台能力简述】（你**不要**在用户面前列 JSON 或工具名，心里知道即可）
下游会根据用户意图调用：本地机构/人员/课题/团队/奖项查询，以及可选联网检索，最后生成分析与报告。

【你的任务】
1) 若用户目标、时间范围、机构/对象、或关心的维度（课题/团队/奖项/人员/机构）仍**不清楚**，先和用户**简短追问**（一两句即可，像真人同事）。
2) 若信息**已足够**让后台去规划查询，则不要再追问，准备进入查库。

【输出格式——必须严格遵守】
只输出一个 JSON 对象，不要 Markdown，不要其它解释：
- 需要继续问用户时：
  {"action":"clarify","assistant_message":"（给用户的追问正文，纯中文，语气友好简洁）"}
- 可以进入查库时：
  {"action":"proceed","planner_user_message":"（一段话：把用户多轮表述里的机构、时间、维度、是否要联网/要报告等**合并成**给规划模型用的完整意图描述；可含【】小标题便于规划）"}

【原则】
- clarify 时不要写长报告、不要假装已经查了数据库。
- proceed 时 planner_user_message 要自包含，不要写「同上」等指代不清的话。
- 用户只是在打招呼、闲聊、与数据无关时：用 clarify 礼貌引导到「想查什么数据」。
"""


def _merge_user_lines(msgs: list[dict[str, str]]) -> str:
    """把对话里所有 user 轮次拼成一段，供强制进入规划时用。"""
    parts = [m.get("content", "").strip() for m in msgs if m.get("role") == "user" and m.get("content", "").strip()]
    return "\n".join(parts) if parts else ""


def run_intake_gate(sess: SessionState, display_user_message: str, ctx: dict[str, Any]) -> dict[str, Any]:
    """
    返回 dict，必有键 action ∈ {"clarify","proceed"}。
    clarify 时必有 assistant_message；proceed 时必有 planner_user_message（字符串，可为空则调用方回退原句）。
    """
    if ctx.get("llm_intake") is False:
        return {"action": "proceed", "planner_user_message": display_user_message}

    pending = list(sess.slots.get(_SLOT_MSGS) or [])
    thread = pending + [{"role": "user", "content": display_user_message.strip()}]

    n_clarify = sum(1 for m in pending if m.get("role") == "assistant")
    if n_clarify >= _MAX_ASSISTANT_CLARIFIES:
        merged = _merge_user_lines(thread)
        sess.slots.pop(_SLOT_MSGS, None)
        return {"action": "proceed", "planner_user_message": merged or display_user_message}

    extra = ""
    if ctx:
        try:
            extra = "\n\n【前端 context JSON】\n" + json.dumps(ctx, ensure_ascii=False)[:4000]
        except Exception:
            pass

    messages = [{"role": "system", "content": INTAKE_SYSTEM + extra}] + thread
    s = get_settings()
    timeout_sec = min(120.0, float(s.llm_planner_timeout_sec))

    try:
        raw = chat_complete(
            messages,
            enable_search=False,
            temperature=0.2,
            timeout_sec=timeout_sec,
        )
        data = extract_json_object(raw)
    except Exception as e:
        LOG.warning("intake_gate_parse_failed: %s", e)
        sess.slots.pop(_SLOT_MSGS, None)
        return {"action": "proceed", "planner_user_message": display_user_message}

    action = str(data.get("action") or "").strip().lower()
    if action == "clarify":
        am = str(data.get("assistant_message") or "").strip()
        if not am:
            am = "为便于准确查询，请补充：要查的机构或单位关键词、时间范围（若有），以及更关心课题、团队、奖项还是人员？"
        sess.slots[_SLOT_MSGS] = thread + [{"role": "assistant", "content": am}]
        return {"action": "clarify", "assistant_message": am}

    if action == "proceed":
        pu = str(data.get("planner_user_message") or "").strip()
        sess.slots.pop(_SLOT_MSGS, None)
        return {"action": "proceed", "planner_user_message": pu or display_user_message}

    LOG.warning("intake_gate_unknown_action: %s", action)
    sess.slots.pop(_SLOT_MSGS, None)
    return {"action": "proceed", "planner_user_message": display_user_message}
