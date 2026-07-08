# -*- coding: utf-8 -*-
"""
v2 专用 MCP Server：仅注册六个 query_* 与 web_search。
每个工具内部调用 registry.run_named_tool，返回与 registry 一致的 JSON 字符串（供 MCP TextContent 传输）。
由 run_mcp.py 独立进程启动；FastAPI 通过 streamable_http_client 调本服务。
"""
from __future__ import annotations  # 延迟注解

import json  # 将 dict 信封序列化为 str 返回给 MCP 客户端
from typing import Any  # kwargs 类型

from mcp.server.fastmcp import FastMCP  # 官方高层封装，自动生成 tools/list 与 streamable HTTP

from src.config import get_settings  # 读取 MCP bind host/port
from src.db.sqlite_loader import ensure_database  # MCP 进程启动时确保 SQLite 文件存在且可加载
from src.sql_tools.registry import run_named_tool  # 唯一业务执行入口（含 org 扩展与 web_search）


def _dump(tool: str, payload: dict[str, Any]) -> str:
    """把 run_named_tool 返回的信封 dict 转成 UTF-8 JSON 字符串（ensure_ascii=False 保留中文）。"""
    return json.dumps(payload, ensure_ascii=False)


def _pick(kwargs: dict[str, Any]) -> dict[str, Any]:
    """过滤值为 None 的键，使下游 SQL 使用「未传参」分支（如默认年份）。"""
    return {k: v for k, v in kwargs.items() if v is not None}


def build_mcp_v2_server() -> FastMCP:
    """
    构造 FastMCP 实例并注册全部 v2 工具；不在此调用 run()，由 run_mcp 用 uvicorn 启动 ASGI。
    """
    s = get_settings()  # 取 host/port 等（FastMCP 构造里也会存一份 settings）
    ensure_database()  # 若 data 目录或库缺失，尝试初始化/提示（具体逻辑见 sqlite_loader）

    mcp = FastMCP(  # 实例名出现在 MCP 握手信息中
        "skl-agent-v2-tools",  # 服务标识
        host=s.mcp_http_host,  # 与 uvicorn 可一致；部分版本仅作元数据
        port=s.mcp_http_port,
        streamable_http_path="/mcp",  # 与客户端 URL 路径一致
    )

    @mcp.tool()  # 注册为 MCP tool；函数名即 tool 名（须与规划白名单一致）
    def query_organization(
        org_name: str | None = None,
        address_keyword: str | None = None,
        limit: int | None = None,
    ) -> str:
        """机构表模糊查询；参数签名影响 MCP 自动生成的 JSON Schema。"""
        args = _pick({"org_name": org_name, "address_keyword": address_keyword, "limit": limit})  # 组装 args dict
        return _dump("query_organization", run_named_tool("query_organization", args))  # 同步执行并序列化

    @mcp.tool()
    def query_person(
        person_name: str | None = None,
        org_name: str | None = None,
        limit: int | None = None,
    ) -> str:
        """人员 + 可选机构条件。"""
        args = _pick({"person_name": person_name, "org_name": org_name, "limit": limit})
        return _dump("query_person", run_named_tool("query_person", args))

    @mcp.tool()
    def query_projects(
        org_name: str | None = None,
        discipline: str | None = None,
        topic_type: str | None = None,
        header_name: str | None = None,
        year_start: int | None = None,
        year_end: int | None = None,
        limit: int | None = None,
    ) -> str:
        """省课题主表；topic_type 误用会导致 0 行，提示在 docstring 给规划模型看。"""
        args = _pick(
            {
                "org_name": org_name,
                "discipline": discipline,
                "topic_type": topic_type,
                "header_name": header_name,
                "year_start": year_start,
                "year_end": year_end,
                "limit": limit,
            }
        )
        return _dump("query_projects", run_named_tool("query_projects", args))

    @mcp.tool()
    def query_teams(
        org_name: str | None = None,
        discipline: str | None = None,
        group_type: str | None = None,
        year_start: int | None = None,
        year_end: int | None = None,
        limit: int | None = None,
    ) -> str:
        """社科团队；年份与起止时间列重叠判断在 teams.py 内实现。"""
        args = _pick(
            {
                "org_name": org_name,
                "discipline": discipline,
                "group_type": group_type,
                "year_start": year_start,
                "year_end": year_end,
                "limit": limit,
            }
        )
        return _dump("query_teams", run_named_tool("query_teams", args))

    @mcp.tool()
    def query_awards(
        org_name: str | None = None,
        person_name: str | None = None,
        discipline: str | None = None,
        award_type: str | None = None,
        year_start: int | None = None,
        year_end: int | None = None,
        limit: int | None = None,
    ) -> str:
        """国奖/省奖；award_type 含国/省字样时分流。省奖带出版单位字段，国奖带类别 categorie 名称。"""
        args = _pick(
            {
                "org_name": org_name,
                "person_name": person_name,
                "discipline": discipline,
                "award_type": award_type,
                "year_start": year_start,
                "year_end": year_end,
                "limit": limit,
            }
        )
        return _dump("query_awards", run_named_tool("query_awards", args))

    @mcp.tool()
    def web_search(query: str) -> str:
        """联网检索；唯一必填 query；内部走 DashScope 联网 + 结构化 JSON。"""
        q = (query or "").strip()  # 去空白
        return _dump("web_search", run_named_tool("web_search", {"query": q}))  # 与 SQL 工具统一走 registry

    return mcp  # 返回给 run_mcp：streamable_http_app() / 或历史 build_mcp_server 兼容
