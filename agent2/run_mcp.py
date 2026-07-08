# -*- coding: utf-8 -*-
"""
【作用】启动 v2 MCP 工具进程：Streamable HTTP，默认路径 /mcp。

【进程模型】与 FastAPI 分离；API 通过 MCP Client 调本进程。可选 Bearer 鉴权（Settings.mcp_bearer_token）。
"""
from __future__ import annotations  # 延迟注解

import uvicorn  # 承载 Starlette/FastMCP 生成的 ASGI 应用
from starlette.responses import JSONResponse  # 鉴权失败时返回 JSON 401

from src.config import get_settings  # 读 host/port/token
from src.mcp_v2_server import build_mcp_v2_server  # 构造带工具注册的 FastMCP


class _BearerAsgiWrapper:
    """
    最外层 ASGI 壳：在 HTTP 请求进入内层 MCP 应用前校验 Authorization。
    lifespan 事件原样转发，避免破坏 Streamable HTTP 会话管理。
    """

    def __init__(self, inner, token: str) -> None:
        self.inner = inner  # FastMCP.streamable_http_app() 返回的 Starlette 应用
        self.token = token  # 非空时才在 http 分支做比对

    async def __call__(self, scope, receive, send):  # ASGI 3 可调用对象入口
        if scope["type"] == "lifespan":  # 生命周期：启动/关闭钩子必须透传
            await self.inner(scope, receive, send)  # 交给内层处理 MCP session manager
            return  # lifespan 处理完毕，不再走 HTTP 鉴权
        if scope["type"] == "http" and self.token:  # 仅普通 HTTP 且配置了 token 才校验
            auth_val = b""  # 默认无 Authorization 头
            for k, v in scope.get("headers") or []:  # ASGI headers 为 List[[bytes, bytes]]
                if k.lower() == b"authorization":  # 大小写不敏感匹配头名
                    auth_val = v  # 取头值字节串
                    break  # 只取第一个 Authorization
            try:
                auth_s = auth_val.decode("latin-1")  # HTTP 头常用 latin-1 解码，避免非 UTF-8 崩
            except Exception:
                auth_s = ""  # 解码失败视为无合法头
            if auth_s != f"Bearer {self.token}":  # 与客户端 MCP 侧 Authorization 一致
                await JSONResponse({"detail": "Unauthorized"}, status_code=401)(scope, receive, send)  # 短响应体
                return  # 不再进入内层
        await self.inner(scope, receive, send)  # 通过鉴权或非 http：交给 MCP 应用


if __name__ == "__main__":  # 脚本入口
    s = get_settings()  # 一次读取配置（含 mcp_http_host、mcp_http_port、mcp_bearer_token）
    mcp = build_mcp_v2_server()  # 注册工具并绑定 host/port/path（部分在 FastMCP 内部使用）
    inner = mcp.streamable_http_app()  # 生成带 /mcp 路由的 Starlette ASGI 应用
    token = (s.mcp_bearer_token or "").strip()  # 空串表示本机不启用 Bearer
    app = _BearerAsgiWrapper(inner, token) if token else inner  # 有 token 则包一层，否则直接用 inner
    uvicorn.run(app, host=s.mcp_http_host, port=int(s.mcp_http_port), log_level="info")  # 阻塞服务 MCP
