# -*- coding: utf-8 -*-
"""
知识图谱（简版）数据库访问层。

实现目标：
1. V1 只读 SQLite，围绕“作者-论文（课题名）”关系提供查询。
2. 业务层只依赖抽象接口，不直接写死 SQLite 方言。
3. 与数据库方言相关位置显式标注“MySQL 切换改造点”。
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from src.db.connection import get_connection


class BaseSimpleKgRepository(ABC):
    """简版知识图谱仓储抽象。"""

    @abstractmethod
    def fetch_author_paper_rows(self) -> list[dict[str, Any]]:
        """读取作者-论文原始关系行。"""

    @abstractmethod
    def fetch_author_related_info(self) -> dict[int, dict[str, Any]]:
        """读取作者关联的扩展信息（机构/奖项/团队等）。"""


class SQLiteSimpleKgRepository(BaseSimpleKgRepository):
    """
    SQLite 实现。

    MySQL 切换改造点：
    - 当前 SQL 使用 SQLite 执行方式和占位符习惯。
    - 切换时建议新增 MySQLSimpleKgRepository，并保持返回字段结构不变。
    """

    def _fetch_all(self, sql: str) -> list[dict[str, Any]]:
        """
        执行只读查询并返回字典列表。

        MySQL 切换改造点：
        - SQLite 用 conn.execute + Row。
        - MySQL 常见为 cursor.execute + DictCursor。
        """
        with get_connection() as conn:
            rows = conn.execute(sql).fetchall()
        return [dict(row) for row in rows]

    def fetch_author_paper_rows(self) -> list[dict[str, Any]]:
        """
        读取“作者-论文”关系数据。

        约定：
        - 作者来自 person_info.name。
        - 论文来源做“多源合并”，避免只取单表导致论文数偏少：
          1) province_topic_info.name（课题名视作论文名）
          2) social_science_aware_info.obj_name（省奖成果名）
          3) national_social_science_aware_info.obj_name（国奖成果名）
        - 三类来源统一映射为 author -> paper 行，后续由引擎做去重与建图。
        """
        return self._fetch_all(
            """
            WITH merged AS (
                -- 来源1：省级课题表（负责人 -> 课题）
                SELECT
                    p.id AS author_id,
                    p.name AS author_name,
                    ('province_' || CAST(t.id AS TEXT)) AS paper_uid,
                    t.name AS paper_name,
                    COALESCE(t.create_time, 0) AS year,
                    COALESCE(t.discipline_id, 0) AS discipline_id,
                    'province_topic' AS paper_source,
                    COALESCE(o.id, 0) AS org_id,
                    COALESCE(o.unit_name, '') AS org_name
                FROM province_topic_info t
                INNER JOIN person_info p ON p.id = t.header_id
                LEFT JOIN organization_info o ON o.id = t.org_id

                UNION ALL

                -- 来源2：省奖成果名（人员 -> 成果）
                SELECT
                    p.id AS author_id,
                    p.name AS author_name,
                    ('province_award_' || CAST(s.id AS TEXT)) AS paper_uid,
                    s.obj_name AS paper_name,
                    COALESCE(s.year, 0) AS year,
                    COALESCE(s.discipline_id, 0) AS discipline_id,
                    'province_award' AS paper_source,
                    COALESCE(o.id, 0) AS org_id,
                    COALESCE(o.unit_name, '') AS org_name
                FROM social_science_aware_info s
                INNER JOIN person_info p ON p.id = s.person_id
                LEFT JOIN organization_info o ON o.id = p.org_id

                UNION ALL

                -- 来源3：国奖成果名（人员 -> 成果）
                SELECT
                    p.id AS author_id,
                    p.name AS author_name,
                    ('national_award_' || CAST(n.id AS TEXT)) AS paper_uid,
                    n.obj_name AS paper_name,
                    COALESCE(n.year, 0) AS year,
                    COALESCE(n.discipline_id, 0) AS discipline_id,
                    'national_award' AS paper_source,
                    COALESCE(o.id, 0) AS org_id,
                    COALESCE(o.unit_name, '') AS org_name
                FROM national_social_science_aware_info n
                INNER JOIN person_info p ON p.id = n.person_id
                LEFT JOIN organization_info o ON o.id = p.org_id
            )
            SELECT
                author_id,
                author_name,
                paper_uid,
                paper_name,
                year,
                discipline_id,
                paper_source,
                org_id,
                org_name
            FROM merged
            WHERE COALESCE(TRIM(author_name), '') <> ''
              AND COALESCE(TRIM(paper_name), '') <> ''
            """
        )

    def fetch_author_related_info(self) -> dict[int, dict[str, Any]]:
        """
        聚合作者的扩展信息。

        返回结构（按 author_id）：
        - org_id/org_name：作者所属机构
        - province_awards：省社科奖列表
        - national_awards：国家社科奖列表
        - groups：社科团队列表
        """
        result: dict[int, dict[str, Any]] = {}

        # 作者基础信息 + 机构。
        person_rows = self._fetch_all(
            """
            SELECT
                p.id AS author_id,
                p.name AS author_name,
                COALESCE(o.id, 0) AS org_id,
                COALESCE(o.unit_name, '') AS org_name
            FROM person_info p
            LEFT JOIN organization_info o ON o.id = p.org_id
            WHERE COALESCE(TRIM(p.name), '') <> ''
            """
        )
        for row in person_rows:
            author_id = int(row.get("author_id") or 0)
            if not author_id:
                continue
            result[author_id] = {
                "author_id": author_id,
                "author_name": str(row.get("author_name") or ""),
                "org_id": int(row.get("org_id") or 0),
                "org_name": str(row.get("org_name") or ""),
                "province_awards": [],
                "national_awards": [],
                "groups": [],
            }

        # 省社科奖（人员维度）。
        province_rows = self._fetch_all(
            """
            SELECT
                s.person_id AS author_id,
                COALESCE(s.obj_name, '') AS award_name,
                COALESCE(l.level, '') AS award_level,
                COALESCE(s.year, 0) AS award_year
            FROM social_science_aware_info s
            LEFT JOIN social_science_aware_level l ON l.id = s.level_id
            WHERE s.person_id IS NOT NULL
            """
        )
        for row in province_rows:
            author_id = int(row.get("author_id") or 0)
            if not author_id:
                continue
            item = {
                "name": str(row.get("award_name") or ""),
                "level": str(row.get("award_level") or ""),
                "year": int(row.get("award_year") or 0),
                "type": "province",
            }
            if author_id not in result:
                result[author_id] = {
                    "author_id": author_id,
                    "author_name": "",
                    "org_id": 0,
                    "org_name": "",
                    "province_awards": [],
                    "national_awards": [],
                    "groups": [],
                }
            result[author_id]["province_awards"].append(item)

        # 国家社科奖（人员维度）。
        national_rows = self._fetch_all(
            """
            SELECT
                n.person_id AS author_id,
                COALESCE(n.obj_name, '') AS award_name,
                COALESCE(t.type_name, '') AS award_type_name,
                COALESCE(c.categorie_name, '') AS award_category_name,
                COALESCE(n.year, 0) AS award_year
            FROM national_social_science_aware_info n
            LEFT JOIN nat_social_science_group_type t ON t.id = n.type_id
            LEFT JOIN nat_social_science_group_categorie c ON c.id = n.categorie_id
            WHERE n.person_id IS NOT NULL
            """
        )
        for row in national_rows:
            author_id = int(row.get("author_id") or 0)
            if not author_id:
                continue
            item = {
                "name": str(row.get("award_name") or ""),
                "type_name": str(row.get("award_type_name") or ""),
                "category_name": str(row.get("award_category_name") or ""),
                "year": int(row.get("award_year") or 0),
                "type": "national",
            }
            if author_id not in result:
                result[author_id] = {
                    "author_id": author_id,
                    "author_name": "",
                    "org_id": 0,
                    "org_name": "",
                    "province_awards": [],
                    "national_awards": [],
                    "groups": [],
                }
            result[author_id]["national_awards"].append(item)

        # 社科团队（负责人维度）。
        group_rows = self._fetch_all(
            """
            SELECT
                g.person_id AS author_id,
                COALESCE(g.group_name, '') AS group_name,
                COALESCE(gt.type_name, '') AS group_type_name,
                COALESCE(g.start_time, 0) AS start_year,
                COALESCE(g.end_time, 0) AS end_year
            FROM social_science_group_info g
            LEFT JOIN social_science_group_type gt ON gt.id = g.type_id
            WHERE g.person_id IS NOT NULL
            """
        )
        for row in group_rows:
            author_id = int(row.get("author_id") or 0)
            if not author_id:
                continue
            item = {
                "name": str(row.get("group_name") or ""),
                "type_name": str(row.get("group_type_name") or ""),
                "start_year": int(row.get("start_year") or 0),
                "end_year": int(row.get("end_year") or 0),
            }
            if author_id not in result:
                result[author_id] = {
                    "author_id": author_id,
                    "author_name": "",
                    "org_id": 0,
                    "org_name": "",
                    "province_awards": [],
                    "national_awards": [],
                    "groups": [],
                }
            result[author_id]["groups"].append(item)

        return result

