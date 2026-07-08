# -*- coding: utf-8 -*-
"""
【模块作用】合并 Pydantic Settings 与可选项目根 config.yaml，为 v2/MCP 提供 RuntimeLimits 只读快照。

【缓存】get_runtime_limits 使用 lru_cache(1)，进程内配置变更需重启才生效。
"""
from __future__ import annotations  # 延迟注解

from dataclasses import dataclass  # 不可变数据类存各超时与行数上限
from functools import lru_cache  # 单例缓存避免重复读盘
from pathlib import Path  # 定位 config.yaml
from typing import Any  # YAML 嵌套 dict 取值

try:
    import yaml  # type: ignore  # 可选依赖：无则仅用环境变量/Settings
except Exception:
    yaml = None  # type: ignore  # 导入失败时 _load_yaml_overrides 返回 {}

# 本文件所在 src/，parent 为 agent2 项目根
_PROJECT_ROOT = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class RuntimeLimits:
    """运行期数值快照：查询行上限、各阶段超时秒数。"""

    hard_max_rows: int  # 单次查询硬上限，防 OOM
    default_limit: int  # 未传 limit 时的默认 TOP N
    default_recent_years: int  # 缺少年份时「最近 N 年」窗口（与 common.pick_year_range 等配合）
    db_query_timeout_sec: int  # 预留：直连 DB 时的语句超时（当前 SQLite 同步可能未全接上）
    llm_planner_timeout_sec: int  # 规划大模型 HTTP 超时
    llm_report_timeout_sec: int  # 报告/分析大模型超时
    server_per_tool_timeout_sec: int  # MCP 或线程池单工具执行上限


def _deep_get(d: dict[str, Any], *keys: str, default: Any = None) -> Any:
    """按路径 keys 逐级下钻 dict；任一层非 dict 或缺键则返回 default。"""
    cur: Any = d  # 当前节点
    for k in keys:
        if not isinstance(cur, dict) or k not in cur:
            return default
        cur = cur[k]
    return cur


def _load_yaml_overrides() -> dict[str, Any]:
    """读取项目根 config.yaml；不存在、非 dict、解析失败、无 PyYAML 时返回 {}。"""
    p = _PROJECT_ROOT / "config.yaml"
    if not p.is_file() or yaml is None:
        return {}
    try:
        data = yaml.safe_load(p.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}


@lru_cache(maxsize=1)
def get_runtime_limits() -> RuntimeLimits:
    """
    合并顺序：先 get_settings()（环境变量/.env），再用 YAML 同路径键覆盖。
    各字段均 max(1, ...) 防止配置写成 0 导致除零或无意义超时。
    """
    from src.config import get_settings  # 延迟导入避免循环引用

    s = get_settings()  # Pydantic Settings 单例
    y = _load_yaml_overrides()  # 可选 YAML 扁平/嵌套覆盖
    hard = int(_deep_get(y, "query", "hard_max_rows", default=s.query_hard_max_rows))
    dlim = int(_deep_get(y, "query", "default_limit", default=s.query_default_limit))
    ry = int(_deep_get(y, "query", "default_recent_years", default=s.query_default_recent_years))
    db_t = int(_deep_get(y, "llm", "db_query_timeout_sec", default=s.db_query_timeout_sec))
    pl_t = int(_deep_get(y, "llm", "planner_timeout_sec", default=s.llm_planner_timeout_sec))
    rp_t = int(_deep_get(y, "llm", "report_timeout_sec", default=s.llm_report_timeout_sec))
    sv_t = int(_deep_get(y, "server", "per_tool_timeout_sec", default=s.server_per_tool_timeout_sec))
    return RuntimeLimits(
        hard_max_rows=max(1, hard),
        default_limit=max(1, dlim),
        default_recent_years=max(1, ry),
        db_query_timeout_sec=max(1, db_t),
        llm_planner_timeout_sec=max(1, pl_t),
        llm_report_timeout_sec=max(1, rp_t),
        server_per_tool_timeout_sec=max(1, sv_t),
    )
