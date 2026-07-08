# -*- coding: utf-8 -*-
"""
【模块作用】数据库连接与只读查询。当前为 SQLite 单文件。

【MySQL 迁移 — 必改】
1. 将 sqlite3 换为 pymysql / SQLAlchemy 等，使用连接池。
2. run_query 内部改为对 MySQL 执行；占位符由「?」改为「%s」等（与驱动一致）。
3. get_connection 可改为从池取连接；注意线程安全与 asyncio 场景。
"""
# 注解延迟求值
from __future__ import annotations

# Python 自带 SQLite3 驱动；MySQL 时整文件替换为对应驱动
import sqlite3
# 上下文管理器：保证连接关闭与事务边界
from contextlib import contextmanager
# 仅 _db_path 用 Path 类型
from pathlib import Path
# 查询参数用元组传递，避免 SQL 注入
from typing import Any, Generator, Tuple

# 读取 agent_sqlite_path 配置
from src.config import get_settings


def _db_path() -> Path:
    """
    解析 SQLite 数据库文件绝对路径，并确保父目录（如 data/）已创建。
    【MySQL】删除本函数，改为从连接池取连接。
    """
    # 从配置取 Path 对象
    p = get_settings().agent_sqlite_path
    # mkdir parents=True：递归创建 data 等上级目录；exist_ok 已存在不报错
    p.parent.mkdir(parents=True, exist_ok=True)
    return p


@contextmanager
def get_connection() -> Generator[sqlite3.Connection, None, None]:
    """
    同步上下文：进入时打开连接，正常退出时 commit，异常时 rollback，最终 close。
    【MySQL】改为从池 checkout / checkin，或 async with。
    """
    # check_same_thread=False：允许 FastAPI 多线程访问同一连接对象（若后续改为每请求新建连接可改回 True）
    conn = sqlite3.connect(str(_db_path()), check_same_thread=False)
    # Row 工厂：结果行可用列名索引，便于转 dict
    conn.row_factory = sqlite3.Row
    try:
        # SQLite 外键默认不强制，显式打开以便与 MySQL 行为更接近（演示用）
        conn.execute("PRAGMA foreign_keys = ON")
        # 把连接交给 with 块内代码使用
        yield conn
        # with 块正常结束：提交事务
        conn.commit()
    except Exception:
        # 任意异常：回滚，避免脏写
        conn.rollback()
        # 继续向上抛，让调用方处理
        raise
    finally:
        # 无论成功失败都关闭连接，释放文件句柄
        conn.close()


def run_query(sql: str, params: Tuple[Any, ...] = ()) -> list[sqlite3.Row]:
    """
    执行一条只读 SQL，返回所有行。
    sql 必须是代码内白名单模板，params 为用户槽位转出的值，禁止拼接原始用户字符串进 sql。
    【MySQL】conn.execute 改为 cursor.execute，占位符按驱动调整。
    """
    # with 触发 get_connection 的 commit/close
    with get_connection() as conn:
        # 参数化执行：? 由 params 按顺序替换
        cur = conn.execute(sql, params)
        # fetchall 读出全部结果行（已受 query_engine 中 LIMIT 限制）
        return list(cur.fetchall())
