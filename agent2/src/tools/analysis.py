# -*- coding: utf-8 -*-
"""
【模块作用】v1 Agent「分析」工具：把 merge_evidence 后的 JSON 与用户问题发给大模型，生成中文短结论。

【MySQL】不涉及；仅可调整 evidence 截断长度。
"""
from __future__ import annotations

import json  # dumps 整包证据
from typing import Any

from src.llm_client import chat_complete  # enable_search=False：严禁联网编造


def tool_analysis(evidence: dict[str, Any], user_message: str) -> dict[str, Any]:
    """
    payload 截断 12000 字符：与报告阶段分工，分析可略长。
    返回统一 ok/tool_name/data 结构供 orchestrator 写入 steps。
    """
    payload = json.dumps(evidence, ensure_ascii=False)[:12000]
    prompt = (
        "你是社科数据分析助手。根据证据JSON，输出简洁结论（中文），"
        "并列出关键数量指标。证据如下：\n"
        f"{payload}\n用户问题：{user_message}"
    )
    text = chat_complete([{"role": "user", "content": prompt}], enable_search=False)
    return {"ok": True, "tool_name": "analysis", "data": {"text": text}}
