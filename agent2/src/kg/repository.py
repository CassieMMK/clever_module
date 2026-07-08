# -*- coding: utf-8 -*-
"""
知识图谱数据库访问层（Repository/DAO）。

设计目标：
1. 业务层只调用 Repository，不直接写 SQL，便于 SQLite -> MySQL 切换。
2. 所有与数据库方言相关的位置，显式标记“ MySQL 切换改造点 ”。
"""
from __future__ import annotations  # 延迟求值类型注解，避免循环引用问题

from abc import ABC, abstractmethod  # 抽象基类与抽象方法
from typing import Any  # 通用字典值类型

from src.db.connection import get_connection  # 复用现有 SQLite 连接上下文


class BaseKgRepository(ABC):
    """知识图谱数据访问抽象接口。"""

    @abstractmethod
    def fetch_organizations(self) -> list[dict[str, Any]]:
        """读取机构实体基础数据。"""

    @abstractmethod
    def fetch_persons(self) -> list[dict[str, Any]]:
        """读取人员实体基础数据。"""

    @abstractmethod
    def fetch_topics(self) -> list[dict[str, Any]]:
        """读取省课题实体基础数据。"""

    @abstractmethod
    def fetch_province_awards(self) -> list[dict[str, Any]]:
        """读取省社科奖实体基础数据。"""

    @abstractmethod
    def fetch_national_awards(self) -> list[dict[str, Any]]:
        """读取国社科奖实体基础数据。"""


class SQLiteKgRepository(BaseKgRepository):
    """
    SQLite 版本 Repository。

    MySQL 切换改造点：
    - 当前 SQL 占位符与语法按 SQLite 习惯编写；
    - 切换时建议新增 MySQLKgRepository，保持同名方法返回相同字段。
    """

    def _fetch_all(self, sql: str) -> list[dict[str, Any]]:
        """
        执行查询并转为字典列表。

        MySQL 切换改造点：
        - SQLite 使用 `conn.execute` + Row；
        - MySQL 通常改为 cursor.execute + DictCursor。
        """
        with get_connection() as conn:  # 打开连接并自动关闭，避免连接泄露
            rows = conn.execute(sql).fetchall()  # 执行只读查询并获取全部结果
        return [dict(r) for r in rows]  # 统一转为普通 dict，业务层无需感知 Row 类型

    def fetch_organizations(self) -> list[dict[str, Any]]:
        """读取机构实体：V1 启用实体之一。"""
        return self._fetch_all(
            """
            SELECT
                id,
                unit_name,
                unit_address
            FROM organization_info
            """
        )

    def fetch_persons(self) -> list[dict[str, Any]]:
        """读取人员实体：携带所属机构名称用于摘要。"""
        return self._fetch_all(
            """
            SELECT
                p.id,
                p.org_id,
                p.name,
                o.unit_name AS org_name
            FROM person_info p
            LEFT JOIN organization_info o ON o.id = p.org_id
            """
        )

    def fetch_topics(self) -> list[dict[str, Any]]:
        """读取省课题实体：携带负责人、机构、学科，便于摘要构造。"""
        return self._fetch_all(
            """
            SELECT
                t.id,
                t.name,
                t.header_id,
                t.org_id,
                t.discipline_id,
                t.topic_id,
                COALESCE(t.create_time, 0) AS year,
                p.name AS header_name,
                o.unit_name AS org_name,
                d.disci_name AS discipline_name,
                tc.type AS topic_type,
                tc.category AS topic_category
            FROM province_topic_info t
            LEFT JOIN person_info p ON p.id = t.header_id
            LEFT JOIN organization_info o ON o.id = t.org_id
            LEFT JOIN discipline_info d ON d.id = t.discipline_id
            LEFT JOIN province_topic_category tc ON tc.id = t.topic_id
            """
        )

    def fetch_province_awards(self) -> list[dict[str, Any]]:
        """读取省社科奖实体：携带获奖人、机构、学科。"""
        return self._fetch_all(
            """
            SELECT
                a.id,
                a.obj_name,
                a.person_id,
                a.org_id,
                a.discipline_id,
                a.level_id,
                COALESCE(a.year, 0) AS year,
                p.name AS person_name,
                o.unit_name AS org_name,
                d.disci_name AS discipline_name,
                l.level AS level_name
            FROM social_science_aware_info a
            LEFT JOIN person_info p ON p.id = a.person_id
            LEFT JOIN organization_info o ON o.id = a.org_id
            LEFT JOIN discipline_info d ON d.id = a.discipline_id
            LEFT JOIN social_science_aware_level l ON l.id = a.level_id
            """
        )

    def fetch_national_awards(self) -> list[dict[str, Any]]:
        """读取国社科奖实体：携带获奖人、机构、学科。"""
        return self._fetch_all(
            """
            SELECT
                a.id,
                a.obj_name,
                a.person_id,
                a.org_id,
                a.discipline_id,
                a.type_id,
                a.categorie_id,
                COALESCE(a.year, 0) AS year,
                p.name AS person_name,
                o.unit_name AS org_name,
                d.disci_name AS discipline_name,
                t.type_name AS award_type_name,
                c.categorie_name AS award_category_name
            FROM national_social_science_aware_info a
            LEFT JOIN person_info p ON p.id = a.person_id
            LEFT JOIN organization_info o ON o.id = a.org_id
            LEFT JOIN discipline_info d ON d.id = a.discipline_id
            LEFT JOIN nat_social_science_group_type t ON t.id = a.type_id
            LEFT JOIN nat_social_science_group_categorie c ON c.id = a.categorie_id
            """
        )

