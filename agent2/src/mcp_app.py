# -*- coding: utf-8 -*-
"""
【模块作用】兼容旧入口文件名：历史上在此注册 v1 风格 MCP 工具（local_db_search 等）。

【现状】旧工具已废弃；此处仅 re-export v2 的 FastMCP 工厂，供仍引用 mcp_app 的脚本使用。
"""
from __future__ import annotations  # 统一启用延迟注解（与本项目其它模块一致）

from src.mcp_v2_server import build_mcp_v2_server  # 实际实现：六类 query_* + web_search


def build_mcp_server():
    """
    与历史 run_mcp 导入名对齐：返回已注册全部 v2 工具的 FastMCP 实例。
    无参数：端口/host 从 Settings 读取，在 run_mcp.py 里 uvicorn 启动时生效。
    """
    return build_mcp_v2_server()  # 委托给 mcp_v2_server，避免两处维护工具列表
