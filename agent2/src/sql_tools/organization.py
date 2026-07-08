# -*- coding: utf-8 -*-
"""
【模块作用】MCP/本地工具 query_organization：仅查 organization_info，支持单位名与地址关键词模糊。

【MySQL】表 organization_info；占位符改为 %s 即可。
"""
from __future__ import annotations  # 延迟注解

from typing import Any  # args 为宽松 dict

from src.db.connection import run_query  # 参数化只读查询
from src.sql_tools.common import clamp_limit, like_param  # limit 封顶与 LIKE 参数


def query_organization(args: dict[str, Any]) -> dict[str, Any]:
    """
    org_name、address_keyword 均可选；全无则 where 仅为 1=1（返回前 lim 条，慎用）。
    返回 rows + count，与 registry 统一信封格式一致。
    """
    lim = clamp_limit(args.get("limit"))  # 解析 limit 并夹在 [1, hard_max]
    org_like = like_param(args.get("org_name"))  # None 表示不加该条件
    addr_like = like_param(args.get("address_keyword"))
    conds = ["1=1"]  # 恒真，便于 AND 拼接
    params: list[Any] = []  # 与 conds 中 ? 顺序一致
    if org_like is not None:
        conds.append("unit_name LIKE ?")
        params.append(org_like)
    if addr_like is not None:
        conds.append("unit_address LIKE ?")
        params.append(addr_like)
    where_sql = " AND ".join(conds)
    sql = (
        "SELECT id, unit_name, unit_address FROM organization_info WHERE "
        + where_sql
        + " ORDER BY id DESC LIMIT ?"
    )
    rows = run_query(sql, tuple(params + [lim]))  # LIMIT 占位防拉全表
    return {"table": "organization_info", "rows": [dict(r) for r in rows], "count": len(rows)}
