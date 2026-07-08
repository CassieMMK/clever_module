# -*- coding: utf-8 -*-
"""
【模块作用】query_person：人员表 person_info 左连 organization_info，按人名与所属单位名过滤。

【MySQL】JOIN 语法一致；注意字符集排序规则影响 LIKE。
"""
from __future__ import annotations

from typing import Any

from src.db.connection import run_query
from src.sql_tools.common import clamp_limit, like_param


def query_person(args: dict[str, Any]) -> dict[str, Any]:
    """person_name、org_name 均可选；至少建议传其一以缩小结果集。"""
    lim = clamp_limit(args.get("limit"))
    name_like = like_param(args.get("person_name"))
    org_like = like_param(args.get("org_name"))
    conds = ["1=1"]
    params: list[Any] = []
    if name_like is not None:
        conds.append("p.name LIKE ?")
        params.append(name_like)
    if org_like is not None:
        conds.append("o.unit_name LIKE ?")
        params.append(org_like)
    where_sql = " AND ".join(conds)
    sql = (
        "SELECT p.id, p.name, o.unit_name AS org_name FROM person_info p "
        "LEFT JOIN organization_info o ON p.org_id = o.id WHERE "
        + where_sql
        + " ORDER BY p.id DESC LIMIT ?"
    )
    rows = run_query(sql, tuple(params + [lim]))
    return {"table": "person_info", "rows": [dict(r) for r in rows], "count": len(rows)}
