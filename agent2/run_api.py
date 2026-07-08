# -*- coding: utf-8 -*-
"""
【作用】在本机启动 Agent 的 HTTP API（FastAPI 应用定义于 src.api.main:app）。

【注意】须在 agent2 项目根目录执行（python run_api.py），保证 `import src` 能找到包。
"""
import socket  # 检测端口是否已被占用

import requests  # 访问健康检查接口，判断占用端口的是否本服务
import uvicorn  # 标准 ASGI 服务器，用于开发/单机部署


def _is_port_listening(host: str, port: int) -> bool:
    """
    检测目标端口是否已在监听。

    说明：
    - 仅用于启动前的快速判断，避免重复启动时报 WinError 10048。
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.8)
        return sock.connect_ex((host, port)) == 0


def _is_agent_healthy(port: int) -> bool:
    """
    检查当前 5001 端口是否已经是本项目服务。

    说明：
    - 若健康检查返回 200 且包含 ok=true，则认为服务已启动，无需重复拉起。
    - 失败时返回 False，继续按原流程启动，让 uvicorn 给出明确报错。
    """
    try:
        resp = requests.get(f"http://127.0.0.1:{port}/api/agent/health", timeout=1.5)
        if resp.status_code != 200:
            return False
        data = resp.json() if resp.content else {}
        return bool(data.get("ok"))
    except Exception:
        return False


if __name__ == "__main__":  # 仅脚本直接运行时进入，避免被 import 时误启动服务
    host = "0.0.0.0"
    port = 5001

    # 端口占用时先判定是否“已是本服务在运行”，避免每次手动清 PID。
    if _is_port_listening("127.0.0.1", port) and _is_agent_healthy(port):
        print(f"Agent API already running on http://127.0.0.1:{port}, skip duplicate start.")
    else:
        uvicorn.run(  # 阻塞运行直到进程结束
            "src.api.main:app",  # 应用对象路径：模块 main 中的 app 实例
            host=host,  # 监听所有网卡，便于局域网调试；生产可改为 127.0.0.1
            port=port,  # 与前端 vue 代理、文档示例一致；改端口需同步前端配置
            reload=False,  # 关闭热重载：生产或稳定演示；开发可改为 True
        )
