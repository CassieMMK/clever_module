# -*- coding: utf-8 -*-
"""
【模块作用】白名单 SQL：根据槽位（地区、机构关键词、年份、主题）查询省课题与国家社科奖相关表。

【MySQL 迁移】
1. 表名、字段名与线上一致时，SQL 主体可复用；注意反引号包裹保留字。
2. LIMIT 语法一致；若用分页，可改为 LIMIT offset, count。
3. LIKE 在 MySQL 下默认不区分大小写（取决于 collation），与 SQLite 行为可能不同。
4. 若连接池为异步，需将本模块改为 async 或在线程池中执行 run_query。
"""
from __future__ import annotations

from typing import Any  # 动态 dict 结构

import sqlite3  # Row 类型用于 _rows_to_dicts

from src.config import get_settings  # sql_max_rows 上限
from src.db.connection import run_query  # 统一参数化查询入口
from src.slots import Slots  # 槽位：region/org_keyword/topic/年份


def _rows_to_dicts(rows: list[sqlite3.Row]) -> list[dict[str, Any]]:
    """把 sqlite3.Row 转成普通 dict，便于 json 序列化进 evidence。"""
    return [dict(r) for r in rows]


def run_province_topic_search(sl: Slots, limit: int | None = None) -> dict[str, Any]:
    """
    查询省课题表 province_topic_info，左连 organization_info 取单位名称与地址。
    条件全部 AND：同时有地区与机构关键词时，需同一行 org 同时满足 address 与 name 匹配。
    """
    # 未传 limit 则用配置中的默认上限
    lim = limit or get_settings().sql_max_rows
    # 恒真占位，便于后面统一用 AND 拼接
    conds = ["1=1"]
    # 与 conds 中 ? 占位符顺序严格一致
    params: list[Any] = []
    # 地区：匹配单位地址字段（不是 unit_name）
    if sl.region:
        conds.append("o.unit_address LIKE ?")
        params.append(f"%{sl.region}%")
    # 机构关键词：匹配单位名称
    if sl.org_keyword:
        conds.append("o.unit_name LIKE ?")
        params.append(f"%{sl.org_keyword}%")
    # 年份：本地库 create_time 存年度整数，用 BETWEEN 闭区间
    if sl.start_year is not None and sl.end_year is not None:
        conds.append("p.create_time BETWEEN ? AND ?")
        params.extend([sl.start_year, sl.end_year])
    # 主题：课题名称模糊匹配
    if sl.topic:
        conds.append("p.name LIKE ?")
        params.append(f"%{sl.topic}%")
    where_sql = " AND ".join(conds)
    # 先 COUNT，便于前端展示「共几条」而不拉全表
    sql_count = f"""
      SELECT COUNT(*) AS c
      FROM province_topic_info p
      LEFT JOIN organization_info o ON p.org_id = o.id
      WHERE {where_sql}
    """
    rows_c = run_query(sql_count, tuple(params))
    total = int(rows_c[0]["c"]) if rows_c else 0
    # 列表查询：多一列 LIMIT，参数在 params 后追加 lim
    sql_list = f"""
      SELECT p.id, p.name, p.create_time AS year, o.unit_name, o.unit_address
      FROM province_topic_info p
      LEFT JOIN organization_info o ON p.org_id = o.id
      WHERE {where_sql}
      ORDER BY p.create_time DESC
      LIMIT ?
    """
    params2 = tuple(list(params) + [lim])
    rows = run_query(sql_list, params2)
    return {"table": "province_topic_info", "total": total, "rows": _rows_to_dicts(rows)}


def run_national_award_search(sl: Slots, limit: int | None = None) -> dict[str, Any]:
    """
    查询国家社科奖信息表 national_social_science_aware_info，左连 organization_info。
    与省课题查询独立，便于并行封装进 tool_db_search。
    """
    lim = limit or get_settings().sql_max_rows
    conds = ["1=1"]
    params: list[Any] = []
    if sl.org_keyword:
        conds.append("o.unit_name LIKE ?")
        params.append(f"%{sl.org_keyword}%")
    if sl.start_year and sl.end_year:
        conds.append("n.year BETWEEN ? AND ?")
        params.extend([sl.start_year, sl.end_year])
    if sl.topic:
        conds.append("n.obj_name LIKE ?")
        params.append(f"%{sl.topic}%")
    where_sql = " AND ".join(conds)
    sql = f"""
      SELECT n.id, n.obj_name, n.year, o.unit_name
      FROM national_social_science_aware_info n
      LEFT JOIN organization_info o ON n.org_id = o.id
      WHERE {where_sql}
      ORDER BY n.year DESC
      LIMIT ?
    """
    rows = run_query(sql, tuple(list(params) + [lim]))
    return {"table": "national_social_science_aware_info", "rows": _rows_to_dicts(rows), "count": len(rows)}
