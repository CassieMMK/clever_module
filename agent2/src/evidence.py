# -*- coding: utf-8 -*-
"""
【模块作用】将本地库检索结果与联网检索结果合并为统一 evidence，供分析与报告工具消费。

【MySQL 迁移】无需修改（仅数据结构打包，不涉及 SQL）。
"""
from __future__ import annotations  # 延迟求值注解

from typing import Any  # db/web/meta 均为 JSON 可序列化的 dict


def merge_evidence(db: dict[str, Any], web: dict[str, Any], meta: dict[str, Any]) -> dict[str, Any]:
    """
    组装标准证据包，键名固定便于下游 prompt 引用。

    db：tool_db_search 返回的整包（含 ok、tool_name、data 等）。
    web：tool_web_search 返回的整包。
    meta：本轮用户问题、槽位等元信息，便于模型在结论中引用「用户问了什么」。
    """
    return {
        "meta": meta,  # 用户 query、filled_slots 等
        "local_db": db,  # 省课题 + 国奖等本地结构化结果
        "web": web,  # 联网摘要/链接列表
    }
