# -*- coding: utf-8 -*-
"""
【模块作用】多轮对话会话状态：按 session_id 存槽位与历史；进程内字典，重启清空。

【MySQL 迁移】无需修改；若需持久化会话，可换 Redis/DB，但与本项目业务库分离。
"""
from __future__ import annotations  # 延迟求值注解，便于类型前向引用

from dataclasses import dataclass, field  # dataclass 简化会话结构；field 提供可变默认值工厂
from typing import Any  # slots 内可存任意 JSON 兼容值（澄清状态、临时标记等）


@dataclass
class SessionState:
    """单会话在内存中的全部状态：澄清流程用 slots，审计/上下文用 history。"""

    slots: dict[str, Any] = field(default_factory=dict)  # 每会话独立字典；默认空，避免跨请求共享引用
    history: list[dict[str, str]] = field(default_factory=list)  # 每项建议为 {"role","content"}；用于对话连贯


# 模块级全局表：进程内唯一；多 worker / 多机部署时彼此不共享，仅适合单机演示或单进程 API
_STORE: dict[str, SessionState] = {}


def get_session(sid: str) -> SessionState:
    """
    按 session_id 取或建会话。
    首次出现的 sid 会 new SessionState() 并放入 _STORE；之后同一 sid 拿到同一对象引用。
    """
    if sid not in _STORE:  # 新会话分支
        _STORE[sid] = SessionState()  # 初始化空 slots 与空 history
    return _STORE[sid]  # 已存在则返回已有状态，便于多轮写入 slots/history
