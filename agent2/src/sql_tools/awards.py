# -*- coding: utf-8 -*-
"""
【模块作用】query_awards：按 award_type 关键词分流省奖表、国奖表或两者各取一半再合并。

省奖行附带出版单位（publish_unit_info，经 a.pub_unit_id）；国奖行附带类别字典（nat_social_science_group_categorie，经 n.categorie_id）。

【MySQL】字段别名已与前端/模型习惯对齐；换库时检查 JOIN 与 year 列名。
"""
from __future__ import annotations

from typing import Any

from src.db.connection import run_query
from src.sql_tools.common import clamp_limit, like_param, pick_year_range


def _scope(args: dict[str, Any]) -> str:
    """
    根据 award_type 字符串粗分三类：含「国」→仅国奖；含「省」→仅省奖；否则 both 各查一半上限。
    """
    at = str(args.get("award_type") or "").strip()
    if "国" in at:
        return "national"
    if "省" in at:
        return "provincial"
    return "both"


def query_awards(args: dict[str, Any]) -> dict[str, Any]:
    """
    公共过滤：年份闭区间、机构/人员/学科 LIKE。
    返回 table 标记为 awards_merged，rows 中 scope 字段区分 provincial / national。
    """
    lim = clamp_limit(args.get("limit"))
    ys, ye = pick_year_range(args)
    org_like = like_param(args.get("org_name"))
    person_like = like_param(args.get("person_name"))
    disc_like = like_param(args.get("discipline"))
    scope = _scope(args)
    rows_out: list[dict[str, Any]] = []

    def prov_sql(extra_limit: int):
        """构造省奖 SQL 与参数元组；extra_limit 传入 LIMIT 占位。"""
        conds = ["1=1"]
        params: list[Any] = []
        conds.append("a.year BETWEEN ? AND ?")
        params.extend([ys, ye])
        if org_like is not None:
            conds.append("o.unit_name LIKE ?")
            params.append(org_like)
        if person_like is not None:
            conds.append("pp.name LIKE ?")
            params.append(person_like)
        if disc_like is not None:
            conds.append("di.disci_name LIKE ?")
            params.append(disc_like)
        wsql = " AND ".join(conds)
        sql = (
            "SELECT 'provincial' AS scope, a.id, a.obj_name, a.year, o.unit_name AS org_name, "
            "l.level AS award_level, di.disci_name, pp.name AS person_name, "
            "pu.pub_unit_name AS publish_unit_name, pu.pub_unit_address AS publish_unit_address "
            "FROM social_science_aware_info a "
            "LEFT JOIN organization_info o ON a.org_id = o.id "
            "LEFT JOIN social_science_aware_level l ON a.level_id = l.id "
            "LEFT JOIN discipline_info di ON a.discipline_id = di.id "
            "LEFT JOIN person_info pp ON a.person_id = pp.id "
            "LEFT JOIN publish_unit_info pu ON a.pub_unit_id = pu.id "
            "WHERE " + wsql + " ORDER BY a.year DESC LIMIT ?"
        )
        return sql, tuple(params + [extra_limit])

    def nat_sql(extra_limit: int):
        """构造国奖 SQL 与参数元组。"""
        conds = ["1=1"]
        params: list[Any] = []
        conds.append("n.year BETWEEN ? AND ?")
        params.extend([ys, ye])
        if org_like is not None:
            conds.append("o.unit_name LIKE ?")
            params.append(org_like)
        if person_like is not None:
            conds.append("pp.name LIKE ?")
            params.append(person_like)
        if disc_like is not None:
            conds.append("di.disci_name LIKE ?")
            params.append(disc_like)
        wsql = " AND ".join(conds)
        sql = (
            "SELECT 'national' AS scope, n.id, n.obj_name, n.year, o.unit_name AS org_name, "
            "ty.type_name AS award_type, cat.categorie_name AS award_categorie_name, "
            "di.disci_name, pp.name AS person_name "
            "FROM national_social_science_aware_info n "
            "LEFT JOIN organization_info o ON n.org_id = o.id "
            "LEFT JOIN nat_social_science_group_type ty ON n.type_id = ty.id "
            "LEFT JOIN nat_social_science_group_categorie cat ON n.categorie_id = cat.id "
            "LEFT JOIN discipline_info di ON n.discipline_id = di.id "
            "LEFT JOIN person_info pp ON n.person_id = pp.id "
            "WHERE " + wsql + " ORDER BY n.year DESC LIMIT ?"
        )
        return sql, tuple(params + [extra_limit])

    if scope == "provincial":
        sql, par = prov_sql(lim)
        rows_out = [dict(r) for r in run_query(sql, par)]
    elif scope == "national":
        sql, par = nat_sql(lim)
        rows_out = [dict(r) for r in run_query(sql, par)]
    else:
        half = max(1, lim // 2)  # 双源各最多一半，再截断到 lim
        rows_out = [dict(r) for r in run_query(*prov_sql(half))]
        rows_out.extend([dict(r) for r in run_query(*nat_sql(half))])
        rows_out = rows_out[:lim]

    return {"table": "awards_merged", "rows": rows_out, "count": len(rows_out)}
