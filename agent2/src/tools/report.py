# -*- coding: utf-8 -*-
"""
【模块作用】v1 Agent「报告」工具：在已有 analysis 摘要基础上，按固定 Markdown 骨架调用大模型生成长文。

【MySQL】不涉及数据库。
"""
from __future__ import annotations

import json  # evidence 序列化进 prompt
from datetime import date  # 当天日期写入提示，减少模型编造日期
from typing import Any

from src.llm_client import chat_complete  # 通用 chat，此处关闭联网

# 与需求文档「报告格式.md」对齐的章节提纲，约束模型输出结构
REPORT_HINT = """
按以下 Markdown 结构输出报告（无数据的小节可省略）：
# 【报告标题】
**生成时间**：YYYY-MM-DD
**报告类型**：项目统计 / 热点分析 / 趋势报告
## 一、摘要
## 二、数据概览（含表格）
## 三、分析与结论（含子节）
## 四、建议与展望
## 五、数据来源说明（说明本地库表与联网摘要）
"""


def tool_report(evidence: dict[str, Any], analysis_text: str, user_message: str) -> dict[str, Any]:
    """
    evidence：merge_evidence 输出；截断到 8000 字符防超长。
    analysis_text：上一轮 tool_analysis 结论文本，减轻模型重复读全量 JSON。
    user_message：用户原始需求，用于报告口径对齐。
    """
    ev = json.dumps(evidence, ensure_ascii=False)[:8000]
    prompt = (
        REPORT_HINT
        + f"\n证据JSON片段：\n{ev}\n分析摘要：\n{analysis_text}\n用户原始需求：\n{user_message}\n"
        + f"生成时间请用：{date.today().isoformat()}"
    )
    md = chat_complete([{"role": "user", "content": prompt}], enable_search=False)
    return {"ok": True, "tool_name": "report", "data": {"markdown": md}}
