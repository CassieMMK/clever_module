# -*- coding: utf-8 -*-
"""
【模块作用】启动时若 SQLite 库为空，则按 IMPORT_ORDER 从 Excel 批量导入各业务表。

【MySQL 迁移 — 通常删除或停用本模块】
1. 生产环境数据由 MySQL 维护时，不再从 Excel 导入；删除 ensure_database 中的导入调用，或整文件废弃。
2. 若仍需一次性迁移脚本，可改为生成 INSERT 语句或 mysqldump，勿与线上一键导入混用。
3. schema_sqlite.py 中的 DDL 仅作本地演示；线上以 MySQL 建表脚本为准。
"""
from __future__ import annotations  # 推迟注解求值

import logging  # 记录导入跳过、失败行、成功行数
import sqlite3  # 类型标注用 Connection；实际连接来自 connection.get_connection
from pathlib import Path  # 拼接 xlsx 绝对路径

import pandas as pd  # 读 Excel 为 DataFrame

from src.config import get_settings  # 取 agent_sqlite_path、agent_data_dir
from src.db.schema_sqlite import SCHEMA_SQL  # 建表 DDL 长字符串
from src.db.connection import get_connection  # 统一连接/提交/关闭

# 本模块 logger，便于在控制台筛选 sqlite_loader 日志
LOG = logging.getLogger("sqlite_loader")

# 元组列表：(磁盘上的 Excel 文件名, 导入目标表名)
# 顺序：先父表后子表（person 依赖 organization；课题依赖多表）；与需求文档一致
IMPORT_ORDER = [
    ("organization_info.xlsx", "organization_info"),
    ("discipline_info.xlsx", "discipline_info"),
    ("province_topic_category.xlsx", "province_topic_category"),
    ("publish_unit_info.xlsx", "publish_unit_info"),
    ("social_science_aware_level.xlsx", "social_science_aware_level"),
    ("social_science_group_type.xlsx", "social_science_group_type"),
    ("nat_social_science_group_type.xlsx", "nat_social_science_group_type"),
    ("nat_social_science_group_categorie.xlsx", "nat_social_science_group_categorie"),
    ("person_info.xlsx", "person_info"),
    ("province_topic_info.xlsx", "province_topic_info"),
    ("national_social_science_aware_info.xlsx", "national_social_science_aware_info"),
    ("social_science_aware_info.xlsx", "social_science_aware_info"),
    ("social_science_group_info.xlsx", "social_science_group_info"),
]


def _db_has_data(conn: sqlite3.Connection) -> bool:
    """
    判断库里是否已有业务数据：用 organization_info 是否有行作为「已导入」信号。
    返回 True 则跳过后续整批 Excel 导入。
    """
    # sqlite_master 查询表是否已创建
    cur = conn.execute(
        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='organization_info'"
    )
    # 表不存在则视为未初始化
    if cur.fetchone()[0] == 0:
        return False
    # 表存在则看是否有至少一行单位数据
    n = conn.execute("SELECT COUNT(*) FROM organization_info").fetchone()[0]
    return int(n) > 0


def _init_schema(conn: sqlite3.Connection) -> None:
    """执行 schema_sqlite.SCHEMA_SQL 中所有 CREATE TABLE IF NOT EXISTS。"""
    # executescript 可一次执行多条语句
    conn.executescript(SCHEMA_SQL)


def _import_one(conn: sqlite3.Connection, xlsx: Path, table: str) -> None:
    """
    导入单个 Excel 到指定表名：列名与表字段一致，INSERT OR REPLACE 按主键 id 覆盖。
    单行失败仅 warning，不中断整表。
    """
    # 文件不存在（用户未拷贝该 xlsx）则跳过
    if not xlsx.exists():
        LOG.warning("skip missing file: %s", xlsx)
        return
    try:
        # 默认读第一个 sheet
        df = pd.read_excel(xlsx)
    except Exception as e:
        # 文件损坏或格式不对
        LOG.error("read excel failed %s: %s", xlsx, e)
        return
    # 列名去空格，避免 Excel 列名带不可见字符
    df.columns = [str(c).strip() for c in df.columns]
    cols = list(df.columns)
    # 构建与列数相同的 ? 占位符
    placeholders = ",".join(["?"] * len(cols))
    # 表名来自白名单 IMPORT_ORDER，非用户输入；cols 来自 Excel 表头
    sql = f"INSERT OR REPLACE INTO {table} ({','.join(cols)}) VALUES ({placeholders})"
    rows_ok = 0
    # 按行迭代；数据量大时可改为 to_sql 批量（当前便于逐行容错）
    for _, row in df.iterrows():
        try:
            # NaN 转 None，SQLite 存为 NULL
            vals = tuple((None if pd.isna(row[c]) else row[c]) for c in cols)
            conn.execute(sql, vals)
            rows_ok += 1
        except Exception as e:
            # 单行违反约束等：记录并跳过
            LOG.warning("skip row in %s: %s", table, e)
    LOG.info("imported %s rows into %s from %s", rows_ok, table, xlsx.name)


def ensure_database() -> None:
    """
    FastAPI lifespan 启动时调用：保证库文件存在、表结构存在；若库为空则尝试从 Excel 导入。
    【MySQL】改为 ping MySQL 或省略；勿再执行 SQLite 建表与导入。
    """
    settings = get_settings()
    # 确保 skldata.db 所在目录存在
    settings.agent_sqlite_path.parent.mkdir(parents=True, exist_ok=True)
    # 第一段 with：只负责建表 + 判断是否已有数据
    with get_connection() as conn:
        _init_schema(conn)
        if _db_has_data(conn):
            LOG.info("database already populated, skip import")
            return
    # 数据目录：默认 import_xlsx 或环境变量覆盖路径
    data_dir = settings.agent_data_dir
    if not data_dir.exists():
        LOG.warning("data dir missing: %s (set AGENT_DATA_DIR)", data_dir)
        return
    # 第二段 with：执行导入（与上一段分开，因上面可能 early return）
    with get_connection() as conn:
        for fname, table in IMPORT_ORDER:
            _import_one(conn, data_dir / fname, table)
        LOG.info("import finished")
