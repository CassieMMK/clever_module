# -*- coding: utf-8 -*-
"""
【模块作用】按 AgentPlan.execution_mode 执行步骤：parallel 用线程池；sequential 逐步解析占位符。

【超时】
- 本地 SQL 通常远小于 30s；web_search 走百炼+联网，需与 llm_report_timeout_sec 同量级。
- 超时时返回 ok=False 的信封，不抛异常，避免整段 v2 500。

【MySQL 迁移】
- run_named_tool 内部最终会走 connection.run_query；换库时改 connection 即可。
"""
from __future__ import annotations  # 延迟注解

import concurrent.futures  # ThreadPoolExecutor + Future.result(timeout=)
from typing import Any  # trace/ctx 中 result 为宽松 dict

from src.config import get_settings  # 读取 llm_report_timeout_sec 等
from src.runtime_limits import get_runtime_limits  # 统一 per_tool 超时配置
from src.sql_tools.registry import run_named_tool  # 实际执行 query_* / web_search
from src.planner.models import AgentPlan  # 已校验的计划模型
from src.planner.bindings import resolve_value  # sequential 参数中的占位符展开


def _tool_timeout_sec(tool: str, base_sec: float) -> float:
    """
    单步允许的最长秒数。web_search 与报告/分析共用百炼，默认至少取 llm_report_timeout_sec。
    """
    s = get_settings()  # 环境变量/.env 中的 Settings
    if tool == "web_search":
        return max(float(base_sec), float(s.llm_report_timeout_sec))  # 联网更慢，抬升下限
    return float(base_sec)  # 其它工具用 base_timeout（来自 RuntimeLimits）


def _run_tool(tool: str, arguments: dict[str, Any], base_timeout: float) -> dict[str, Any]:
    """
    在线程里跑 run_named_tool（阻塞 IO/SQL）；超时则返回失败信封，不向外抛 TimeoutError。
    单步单线程池：避免与外层 parallel 池嵌套死锁的简单折中。
    """
    timeout = _tool_timeout_sec(tool, base_timeout)  # web_search 可能大于 base
    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            fut = pool.submit(run_named_tool, tool, arguments)  # 异步提交
            return fut.result(timeout=timeout)  # 阻塞等待，超时抛 TimeoutError
    except concurrent.futures.TimeoutError:
        return {
            "tool": tool,
            "ok": False,
            "data": {"rows": [], "count": 0},
            "error": (
                f"工具执行超时（{int(timeout)}s）。"
                "联网检索较慢：可在环境变量中增大 SERVER_PER_TOOL_TIMEOUT_SEC 或 LLM_REPORT_TIMEOUT_SEC。"
            ),
        }
    except Exception as e:
        return {
            "tool": tool,
            "ok": False,
            "data": {"rows": [], "count": 0},
            "error": str(e),
        }


def execute_plan(plan: AgentPlan):
    """
    返回 (trace, ctx)。
    trace：列表，元素含 id/tool/result，供 API 与证据。
    ctx：step_id → 上一步归一化结果，用于 sequential 依赖解析。
    """
    lim = get_runtime_limits()  # 进程内缓存的运行期上限
    base_timeout = float(lim.server_per_tool_timeout_sec)  # 默认单工具秒数
    ctx: dict[str, dict[str, Any]] = {}  # step_id → result 信封
    trace: list[dict[str, Any]] = []  # 有序步骤轨迹

    if plan.execution_mode == "parallel":
        with concurrent.futures.ThreadPoolExecutor(max_workers=max(4, len(plan.steps))) as pool:
            futs: dict[str, concurrent.futures.Future] = {}  # step_id → Future
            for st in plan.steps:
                futs[st.id] = pool.submit(_run_tool, st.tool, dict(st.arguments), base_timeout)
            for st in plan.steps:
                res = futs[st.id].result()  # 无超时：Future 已在 _run_tool 内限时
                ctx[st.id] = res
                trace.append({"id": st.id, "tool": st.tool, "result": res})
        return trace, ctx

    for st in plan.steps:
        args = resolve_value(dict(st.arguments), ctx)  # 展开 from_context 与 {{id:path}}
        res = _run_tool(st.tool, args, base_timeout)
        ctx[st.id] = res
        trace.append({"id": st.id, "tool": st.tool, "result": res})
    return trace, ctx
