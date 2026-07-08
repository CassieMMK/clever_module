# -*- coding: utf-8 -*-
"""
【模块作用】query_teams：社科团队 social_science_group_info。

时间过滤用区间重叠语义：团队 [start,end] 与查询 [ys,ye] 有交集即命中，避免 end_time NULL 被简单 BETWEEN 误杀。

【MySQL】NULL 语义与 SQLite 类似，注意年份类型为 INT 还是 DATE。
"""
from __future__ import annotations

from typing import Any

from src.db.connection import run_query
from src.sql_tools.common import clamp_limit, like_param, pick_year_range


def query_teams(args: dict[str, Any]) -> dict[str, Any]:
    """
    重叠条件：start_time 为空视为未知起点，仅用 end 与 ys 关系；end 为空视为仍在进行。
    可选：单位名、学科名、团队类型 type_name 模糊。
    """
    lim = clamp_limit(args.get("limit"))
    ys, ye = pick_year_range(args)
    org_like = like_param(args.get("org_name"))
    disc_like = like_param(args.get("discipline"))
    gtype_like = like_param(args.get("group_type"))
    conds = ["1=1"]
    params: list[Any] = []
    conds.append("(g.start_time IS NULL OR g.start_time <= ?)")  # 团队起点不晚于查询窗口终点
    params.append(ye)
    conds.append("(g.end_time IS NULL OR g.end_time >= ?)")  # 团队终点不早于查询窗口起点（NULL= ongoing）
    params.append(ys)
    if org_like is not None:
        conds.append("o.unit_name LIKE ?")
        params.append(org_like)
    if disc_like is not None:
        conds.append("di.disci_name LIKE ?")
        params.append(disc_like)
    if gtype_like is not None:
        conds.append("t.type_name LIKE ?")
        params.append(gtype_like)
    where_sql = " AND ".join(conds)
    sql = (
        "SELECT g.id, g.group_name, g.start_time, g.end_time, o.unit_name, di.disci_name, t.type_name "
        "FROM social_science_group_info g "
        "LEFT JOIN organization_info o ON g.org_id = o.id "
        "LEFT JOIN discipline_info di ON g.discipline_id = di.id "
        "LEFT JOIN social_science_group_type t ON g.type_id = t.id "
        "WHERE " + where_sql + " ORDER BY COALESCE(g.end_time, g.start_time, 0) DESC LIMIT ?"
    )
    rows = run_query(sql, tuple(params + [lim]))
    return {"table": "social_science_group_info", "rows": [dict(r) for r in rows], "count": len(rows)}
