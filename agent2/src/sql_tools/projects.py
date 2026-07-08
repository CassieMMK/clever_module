# -*- coding: utf-8 -*-
"""
【模块作用】query_projects：省课题 province_topic_info 多表连接查询。

topic_type 对应 Excel 导入的 province_topic_category（列 category / type），
不是用户口语「省课题」等表级泛称；泛称传入时本模块会忽略 topic_type 条件以免误过滤为空。

【MySQL】表名、列名与线上一致时可复用 SQL 骨架。
"""
from __future__ import annotations

from typing import Any

from src.db.connection import run_query
from src.sql_tools.common import clamp_limit, like_param, pick_year_range

# 规划器/用户常把「省课题」理解成查 province_topic_info，但 category 列往往不含该字面串
_IGNORE_TOPIC_TYPE_META = frozenset(
    {
        "省课题",
        "省级课题",
        "省项目",
        "省级项目",
        "省级",
        "省课题情况",
        "课题情况",
    }
)


def _topic_type_like_for_category(args: dict[str, Any]) -> str | None:
    """
    若 topic_type 为泛称或空，返回 None（不加 category/type 条件）。
    否则返回带 % 的 LIKE 参数串。
    """
    raw = args.get("topic_type")
    if raw is None:
        return None
    s = str(raw).strip()
    if not s:
        return None
    if s in _IGNORE_TOPIC_TYPE_META:
        return None
    return like_param(s)


def query_projects(args: dict[str, Any]) -> dict[str, Any]:
    """
    必选时间窗：由 pick_year_range 从 year_start/year_end 或默认近年推算，再用于 create_time BETWEEN。
    可选：机构、学科、课题负责人、类别双列模糊。
    """
    lim = clamp_limit(args.get("limit"))
    ys, ye = pick_year_range(args)  # 闭区间 [ys, ye]
    org_like = like_param(args.get("org_name"))
    disc_like = like_param(args.get("discipline"))
    topic_type_like = _topic_type_like_for_category(args)
    header_like = like_param(args.get("header_name"))
    conds = ["1=1"]
    params: list[Any] = []
    conds.append("p.create_time BETWEEN ? AND ?")  # create_time 存年份整数
    params.extend([ys, ye])
    if org_like is not None:
        conds.append("o.unit_name LIKE ?")
        params.append(org_like)
    if disc_like is not None:
        conds.append("di.disci_name LIKE ?")
        params.append(disc_like)
    if topic_type_like is not None:
        conds.append("(ptc.category LIKE ? OR ptc.type LIKE ?)")  # 两列 OR 提高命中
        params.extend([topic_type_like, topic_type_like])
    if header_like is not None:
        conds.append("hp.name LIKE ?")
        params.append(header_like)
    where_sql = " AND ".join(conds)
    sql = (
        "SELECT p.id, p.name, p.create_time AS year, o.unit_name, o.unit_address, "
        "di.disci_name, hp.name AS header_name "
        "FROM province_topic_info p "
        "LEFT JOIN organization_info o ON p.org_id = o.id "
        "LEFT JOIN discipline_info di ON p.discipline_id = di.id "
        "LEFT JOIN person_info hp ON p.header_id = hp.id "
        "LEFT JOIN province_topic_category ptc ON p.topic_id = ptc.id "
        "WHERE " + where_sql + " ORDER BY p.create_time DESC LIMIT ?"
    )
    rows = run_query(sql, tuple(params + [lim]))
    return {"table": "province_topic_info", "rows": [dict(r) for r in rows], "count": len(rows)}
