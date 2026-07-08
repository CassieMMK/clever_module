# -*- coding: utf-8 -*-
"""
FastAPI v2 侧 MCP Client：通过 Streamable HTTP 与独立 MCP 进程通信。
使用 ClientSession.call_tool 并行执行规划给出的 tools；记录结构化日志；按 Settings 控制超时与并发。
"""
from __future__ import annotations  # 延迟注解

import asyncio  # gather / Semaphore 并行与限流
import json  # 日志里序列化 params；解析工具返回 JSON 信封
import logging  # 记录每次 callTool 的耗时与结果 ok
import time  # monotonic 计算预算；perf_counter 计算单次耗时
from datetime import timedelta  # MCP SDK call_tool 的 read_timeout_seconds 类型
from typing import Any  # trace / params 宽松类型

import httpx  # MCP transport 底层 HTTP 客户端
import mcp.types as mcp_types  # CallToolResult / TextContent 等协议类型
from mcp.client.session import ClientSession  # MCP 客户端会话：initialize + call_tool
from mcp.client.streamable_http import streamable_http_client  # 官方 Streamable HTTP 传输上下文

from src.config import get_settings  # URL、token、超时、连接池、并行度

LOG = logging.getLogger("mcp_client_v2")  # 与 uvicorn root logger 配置配合


def mcp_tool_endpoint_url() -> str:
    """
    拼接 MCP Streamable HTTP 的完整 URL。
    若配置了 mcp_client_base_url 则优先使用（便于 Docker/反代）；否则默认 127.0.0.1 + mcp_http_port + /mcp。
    """
    s = get_settings()
    base = (s.mcp_client_base_url or "").strip()
    if base:
        b = base.rstrip("/")
        return b if b.endswith("/mcp") else b + "/mcp"
    return f"http://127.0.0.1:{int(s.mcp_http_port)}/mcp"


def _redact_params(params: dict[str, Any] | None) -> dict[str, Any]:
    """
    日志脱敏：屏蔽疑似密钥字段，截断超长字符串，避免日志泄露或膨胀。
    """
    if not params:
        return {}
    out: dict[str, Any] = {}
    for k, v in params.items():
        lk = str(k).lower()
        if any(x in lk for x in ("password", "token", "secret", "api_key", "authorization")):
            out[k] = "***"
        elif isinstance(v, str) and len(v) > 200:
            out[k] = v[:200] + "…"
        else:
            out[k] = v
    return out


def _call_tool_result_to_envelope(name: str, result: mcp_types.CallToolResult) -> dict[str, Any]:
    """
    将 MCP 协议的 CallToolResult 转回业务侧信封 dict（与 registry.run_named_tool 返回形状一致）。
    成功路径：解析 TextContent 中的 JSON 字符串；失败路径：拼接 isError 文本为 error。
    """
    if result.isError:
        parts: list[str] = []
        for block in result.content:
            if isinstance(block, mcp_types.TextContent):
                parts.append(block.text)
            elif isinstance(block, dict) and block.get("type") == "text":
                parts.append(str(block.get("text") or ""))
        msg = "; ".join(parts).strip() or "MCP tool error"
        return {
            "tool": name,
            "ok": False,
            "data": {"rows": [], "count": 0},
            "error": msg,
        }
    texts: list[str] = []
    for block in result.content:
        if isinstance(block, mcp_types.TextContent):
            texts.append(block.text)
        elif isinstance(block, dict) and block.get("type") == "text":
            texts.append(str(block.get("text") or ""))
    raw = "".join(texts).strip()
    try:
        data = json.loads(raw)
        if isinstance(data, dict) and "tool" in data and "ok" in data:
            return data
    except json.JSONDecodeError:
        pass
    return {
        "tool": name,
        "ok": False,
        "data": {"rows": [], "count": 0},
        "error": f"无法解析 MCP 返回 JSON: {raw[:300]}",
    }


def _per_tool_timeout_sec(tool: str) -> float:
    """按工具类型取 call_tool 的读超时上界（秒）；web_search 通常更慢。"""
    s = get_settings()
    if tool == "web_search":
        return float(s.mcp_calltool_web_search_timeout_sec)
    return float(s.mcp_calltool_timeout_sec)


async def execute_tools_via_mcp(
    tools: list[tuple[str, dict[str, Any]]],
    *,
    session_id: str,
) -> list[dict[str, Any]]:
    """
    对 (name, params) 列表并行 callTool；返回 tool_trace 元素列表，每项含 id/tool/result。
    """
    s = get_settings()
    url = mcp_tool_endpoint_url()
    max_parallel = max(1, int(s.mcp_max_parallel_tools))
    total_budget = float(s.mcp_v2_request_timeout_sec)
    conn_limit = max(1, int(s.mcp_httpx_max_connections))

    headers: dict[str, str] = {}
    token = (s.mcp_bearer_token or "").strip()
    if token:
        headers["Authorization"] = f"Bearer {token}"

    _handshake_slack = 90.0
    _overall = max(total_budget + _handshake_slack, 180.0)
    _read = max(300.0, total_budget + _handshake_slack)
    timeout = httpx.Timeout(_overall, connect=30.0, read=_read)
    limits = httpx.Limits(max_connections=conn_limit, max_keepalive_connections=conn_limit)

    sem = asyncio.Semaphore(max_parallel)

    async def run_batch() -> list[dict[str, Any]]:
        async with httpx.AsyncClient(headers=headers, timeout=timeout, limits=limits) as http_client:
            async with streamable_http_client(url, http_client=http_client) as (read, write, _get_sid):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    budget_end = time.monotonic() + max(total_budget, 30.0)

                    async def one(
                        session: ClientSession, name: str, params: dict[str, Any], idx: int
                    ) -> dict[str, Any]:
                        async with sem:
                            left = budget_end - time.monotonic()
                            if left <= 0:
                                env = {
                                    "tool": name,
                                    "ok": False,
                                    "data": {"rows": [], "count": 0},
                                    "error": (
                                        f"工具阶段已超过合计预算 {total_budget:g}s（自 initialize 完成起算），跳过"
                                    ),
                                }
                                return {"id": f"m{idx}", "tool": name, "result": env}
                            t0 = time.perf_counter()
                            try:
                                cap = _per_tool_timeout_sec(name)
                                to = min(cap, max(left, 0.1))
                                result = await session.call_tool(
                                    name,
                                    arguments=params or {},
                                    read_timeout_seconds=timedelta(seconds=to),
                                )
                                env = _call_tool_result_to_envelope(name, result)
                            except Exception as e:
                                env = {
                                    "tool": name,
                                    "ok": False,
                                    "data": {"rows": [], "count": 0},
                                    "error": str(e),
                                }
                            elapsed_ms = int((time.perf_counter() - t0) * 1000)
                            LOG.info(
                                "mcp_call_tool session_id=%s idx=%s tool=%s ms=%s params=%s ok=%s",
                                session_id,
                                idx,
                                name,
                                elapsed_ms,
                                json.dumps(_redact_params(params), ensure_ascii=False),
                                env.get("ok"),
                            )
                            return {"id": f"m{idx}", "tool": name, "result": env}

                    return await asyncio.gather(
                        *[one(session, name, dict(params or {}), i) for i, (name, params) in enumerate(tools)]
                    )

    return await run_batch()
