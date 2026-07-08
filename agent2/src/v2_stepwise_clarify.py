# -*- coding: utf-8 -*-
"""
【已废弃】启发式分步澄清。产品现改用「接待大模型」`v2_clarify_gate_llm.run_intake_gate`，本文件保留仅供参考、不再被 v2_service 引用。

历史行为：context.stepwise_clarify 等；请勿在新需求中依赖。
"""
from __future__ import annotations

import re
from typing import Any

from src.sessions import SessionState

_SLOT_GOAL = "v2_stepwise_goal"
_SLOT_PHASE = "v2_stepwise_phase"

# 出现若干个字即认为用户已给出可规划的业务线索（与长度组合判断）
_TOPIC_HINTS = (
    "课题",
    "项目",
    "团队",
    "奖项",
    "获奖",
    "人员",
    "人才",
    "机构",
    "单位",
    "组织",
    "社科",
    "学院",
    "大学",
    "研究院",
    "科学院",
    "出版",
    "出版社",
    "统计",
    "多少",
    "几个",
    "列出",
    "查询",
    "分析",
    "报告",
    "数据",
)


def _reset_slots(sess: SessionState) -> None:
    sess.slots.pop(_SLOT_GOAL, None)
    sess.slots.pop(_SLOT_PHASE, None)


def _message_suggest_need_detail(msg: str) -> bool:
    """
    启发式：若用户首句已含年份或足够长、或命中足够业务词，则不再追问，直接规划。
    宁可对「略短但信息够」少追问，也不要对已写清的长问句误触发追问。
    """
    t = (msg or "").strip()
    if not t:
        return True
    if len(t) <= 4:
        return True
    if re.search(r"20\d{2}", t):
        return False
    hits = sum(1 for k in _TOPIC_HINTS if k in t)
    if len(t) >= 44:
        return False
    if hits >= 2 and len(t) >= 6:
        return False
    if hits >= 1 and len(t) >= 30:
        return False
    if len(t) >= 22 and hits >= 1:
        return False
    return True


def process_stepwise_clarify(
    sess: SessionState,
    sid: str,
    message: str,
    ctx: dict[str, Any],
) -> tuple[str, Any]:
    """
    返回 (mode, payload)：
    - ("normal", message)：不追问，继续主流程。
    - ("early", dict)：need_clarify=True，直接作为 JSON 返回。
    - ("merged", str)：用合并后的长文本替换 message 再走规划。
    """
    if ctx.get("reset_stepwise"):
        _reset_slots(sess)
    # 显式 False 才关闭；未传则默认开启
    if ctx.get("stepwise_clarify") is False:
        return "normal", message

    phase = sess.slots.get(_SLOT_PHASE)
    t = message.strip()

    if phase == "need_detail":
        # 用户未按上一轮追问回答，但直接发了「已足够具体」的新问题：放弃澄清轮次
        if not _message_suggest_need_detail(t):
            _reset_slots(sess)
            return "normal", message
        goal = str(sess.slots.get(_SLOT_GOAL) or "").strip()
        detail = t
        merged = "【分步澄清后的查询意图】\n"
        if goal:
            merged += f"原始描述：{goal}\n"
        merged += f"补充与确认：{detail}"
        _reset_slots(sess)
        return "merged", merged

    if not phase:
        if not _message_suggest_need_detail(t):
            return "normal", message
        sess.slots[_SLOT_GOAL] = t
        sess.slots[_SLOT_PHASE] = "need_detail"
        return (
            "early",
            _response(
                sid,
                "为准确查库与生成报告，请先补充下面信息（**下一轮一条消息里逐条写即可**，不必单独点按钮）：\n"
                "1）**机构全称或关键词**（如：四川省社会科学院）\n"
                "2）**关注维度**：机构信息 / 立项课题 / 科研团队 / 获奖成果 / 科研人员（可多选）\n"
                "3）**时间范围**（如：不限、2020–2024 年）\n"
                "4）是否需要**联网公开信息补充**（是/否）\n"
                "5）是否需要**正式 Markdown 报告**（是/否）\n\n"
                "若你本条已经写得很完整，也可以直接再发一条把上列信息一次说完。",
                "need_detail",
            ),
        )

    return "normal", message


def _response(sid: str, assistant: str, phase: str) -> dict[str, Any]:
    return {
        "version": "v2",
        "session_id": sid,
        "need_clarify": True,
        "assistant_message": assistant,
        "plan": None,
        "tool_trace": [],
        "result": {
            "stepwise_clarify": True,
            "clarify_phase": phase,
        },
        "error": None,
    }
