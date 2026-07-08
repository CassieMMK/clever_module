# -*- coding: utf-8 -*-
"""
【模块作用】全局配置：从项目根下 key.env / .env 加载；含数据库路径、API/MCP 端口、模型名与各类超时。

【MySQL 迁移】可在此增加连接串字段；SQLite 路径字段可逐步废弃。
"""
from __future__ import annotations  # PEP 563

import os  # 读取环境变量兜底 API Key
from functools import lru_cache  # get_settings 单例缓存，避免重复解析 .env
from pathlib import Path  # 类型安全的路径运算

from pydantic_settings import BaseSettings, SettingsConfigDict  # pydantic v2 设置模型

_PROJECT_ROOT = Path(__file__).resolve().parent.parent  # 本文件在 src/，parent.parent 即 agent2 根


def _env_file_paths() -> tuple[str, ...] | None:
    """
    返回存在的 env 文件路径元组；都不存在则返回 None，让 pydantic 仅用环境变量。
    """
    names = (".env", "key.env")  # 优先常见命名；二者可并存由 pydantic 合并加载顺序
    found = tuple(str(_PROJECT_ROOT / n) for n in names if (_PROJECT_ROOT / n).is_file())  # 只保留磁盘上存在的
    return found if found else None  # 空元组也视为 falsy，用 None 更语义化


class Settings(BaseSettings):
    """所有可调配置项；字段名大写形式对应环境变量（如 LLM_PLANNER_TIMEOUT_SEC）。"""

    model_config = SettingsConfigDict(
        env_file=_env_file_paths(),  # 从找到的文件加载
        env_file_encoding="utf-8",  # Windows 上避免默认编码误判
        extra="ignore",  # .env 里多出来的键不报错，便于与别的项目共用文件
    )

    aliyun_api_key: str = ""  # 也可仅通过环境变量 ALIYUN_API_KEY 注入
    agent_data_dir: Path = _PROJECT_ROOT / "import_xlsx"  # Excel 导入目录（若有离线导入流程）
    agent_sqlite_path: Path = _PROJECT_ROOT / "data" / "skldata.db"  # 业务 SQLite 文件路径

    agent_api_host: str = "0.0.0.0"  # FastAPI 监听地址；run_api.py 当前写死 0.0.0.0，可与此处统一
    agent_api_port: int = 5001  # HTTP API 端口

    mcp_http_host: str = "127.0.0.1"  # MCP 进程 bind；默认本机回环
    mcp_http_port: int = 5000  # MCP Streamable HTTP 端口
    mcp_client_base_url: str = ""  # 非空则覆盖默认 http://127.0.0.1:port/mcp 的基址（不含尾 /mcp 时自动补）
    mcp_bearer_token: str = ""  # 非空则 run_mcp 包 Bearer 校验；API 侧 mcp_client 同步带 Authorization
    mcp_httpx_max_connections: int = 10  # MCP Client 侧 httpx 连接池上限
    mcp_max_parallel_tools: int = 5  # asyncio 并行 callTool 上限
    mcp_v2_request_timeout_sec: float = 120.0  # initialize 之后整批工具阶段的预算（秒），用于 one() 内 left 计算
    mcp_calltool_timeout_sec: float = 90.0  # 单次 callTool（非 web）读超时上界
    mcp_calltool_web_search_timeout_sec: float = 90.0  # web_search 走大模型联网，单独更长

    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"  # 百炼 OpenAI 兼容根
    chat_model: str = "deepseek-v3.2"  # 默认对话/规划/报告回落模型 id
    sql_max_rows: int = 500  # 部分旧查询引擎上限（若仍被引用）

    query_hard_max_rows: int = 100  # 单工具返回行数硬顶
    query_default_limit: int = 20  # 未传 limit 时默认条数
    query_default_year_start: int = 2000  # 未传 year_start 时宽松下界
    query_default_year_end: int = 2030  # 未传 year_end 时宽松上界
    query_default_recent_years: int = 3  # 文档/其它模块「近 N 年」语义保留

    db_query_timeout_sec: int = 5  # 单条 SQL 执行超时（若 connection 层实现使用）
    llm_planner_timeout_sec: int = 180  # 规划 LLM HTTP 读超时（秒）
    llm_report_timeout_sec: int = 120  # 报告/分析 LLM 读超时
    server_per_tool_timeout_sec: int = 120  # 旧 executor 路径单工具超时（若仍引用）
    planner_model: str = ""  # 空则 planner_chat_complete 用 chat_model
    report_model: str = ""  # 空则 report_chat_complete 用 chat_model


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """
    进程内单例 Settings；首次调用时读 env，之后返回缓存实例。
    单元测试若需换配置需清缓存或重启进程。
    """
    return Settings()  # pydantic 会合并 env_file 与环境变量


def ensure_api_key() -> str:
    """
    校验阿里云 API Key 存在；优先 Settings.aliyun_api_key，否则读 ALIYUN_API_KEY。
    缺失则抛 RuntimeError，避免请求发到一半才 401。
    """
    s = get_settings()  # 取配置中的 key 字段
    key = (s.aliyun_api_key or os.getenv("ALIYUN_API_KEY") or "").strip()  # 环境变量兜底常见 CI/部署方式
    if not key:  # 仍为空则无法调用百炼
        raise RuntimeError(
            "ALIYUN_API_KEY missing: add ALIYUN_API_KEY=... to key.env or .env in agent2 root."
        )  # 明确提示文件位置
    return key  # 非空 key 字符串供 OpenAI 客户端使用
