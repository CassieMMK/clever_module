# -*- coding: utf-8 -*-
"""
【模块作用】v1「本地库检索」工具：并行语义上封装省课题 + 国家社科奖两类 query_engine 查询。

【MySQL】改 query_engine / connection 即可；本文件通常只改返回字段说明。
"""
from __future__ import annotations

from typing import Any

from src.db.query_engine import run_national_award_search, run_province_topic_search
from src.slots import Slots


def tool_db_search(slots: Slots) -> dict[str, Any]:
    """
    slots 已由编排层补全地区/机构/年份等；此处不做缺参判断。
    返回 data 内两个键：province_topics、national_awards，供 merge_evidence 使用。
    """
    prov = run_province_topic_search(slots)
    nat = run_national_award_search(slots)
    return {"ok": True, "tool_name": "db_search", "data": {"province_topics": prov, "national_awards": nat}}
