# -*- coding: utf-8 -*-
"""
【模块作用】将规划 JSON 中的 tool 名称映射到 Python 实现，并包装为统一返回信封。

【返回形状】
{"tool": str, "ok": bool, "data": {...}, "error": str|None}
data：SQL 类含 rows/count；web_search 含 results（title/snippet/url）。

【MySQL 迁移】
- 各 query_* 内部 SQL 在对应文件中改为 MySQL 方言；本文件只做分发。
"""
from __future__ import annotations  # 延迟注解

from typing import Any, Callable  # 标注 SQL 工具为 Callable[[dict], dict]

# 以下为各表查询实现：SQL 均在各自模块内白名单拼接，禁止把用户原文拼进 SQL
from src.sql_tools.organization import query_organization  # 机构表
from src.sql_tools.person import query_person  # 人员 + 机构 join
from src.sql_tools.projects import query_projects  # 省课题
from src.sql_tools.teams import query_teams  # 社科团队
from src.sql_tools.awards import query_awards  # 国奖/省奖合并查询
from src.sql_tools.common import expand_org_name_for_search  # 机构简称规范化
from src.tools.web_search import tool_web_search_structured  # 结构化联网（内部再调 report_chat_complete）

# 规划器与 MCP 注册允许出现的工具名；frozenset 防止运行时被误改
TOOL_NAMES = frozenset(
    {
        "query_organization",
        "query_person",
        "query_projects",
        "query_teams",
        "query_awards",
        "web_search",
    }
)

# SQL 类工具：统一签名为 (args: dict) -> dict，返回 data 部分由本文件包信封
_SQL_FUNCS: dict[str, Callable[[dict[str, Any]], dict[str, Any]]] = {
    "query_organization": query_organization,
    "query_person": query_person,
    "query_projects": query_projects,
    "query_teams": query_teams,
    "query_awards": query_awards,
}


def run_named_tool(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    """
    按工具名分发执行；供 MCP Server 进程内同步调用。
    name：已通过 validate_tools_plan_dict 白名单校验。
    arguments：规划产出的 params 对象；web_search 仅使用 query 键。
    """
    if name == "web_search":  # 与 SQL 工具分支不同：走百炼联网
        q = str(arguments.get("query") or "").strip()  # 必填检索句；strip 避免全空白
        if not q:  # 空查询无意义，直接失败信封
            return _fail("web_search", "query 为空")
        r = tool_web_search_structured(q)  # 返回 dict 含 ok/tool_name/data/source；此处取 data
        return _ok("web_search", r["data"])  # 只把 data 塞进统一信封
    fn = _SQL_FUNCS.get(name)  # 其余名称必须在 SQL 映射表里
    if not fn:  # 防御：理论上不会走到（校验已拦）
        return _fail(name, "未知工具")
    try:
        args = dict(arguments or {})  # 浅拷贝，避免修改调用方传入的 dict
        if args.get("org_name") is not None:  # 仅当有 org_name 键时尝试扩展简称
            exp = expand_org_name_for_search(str(args.get("org_name") or ""))  # 空串扩展结果为 None
            if exp is not None:  # 有规范化结果则写回
                args["org_name"] = exp
        data = fn(args)  # 执行具体 SQL 工具，返回 {table, rows, count} 等
        return _ok(name, data)  # 成功路径
    except Exception as e:  # SQL 异常、类型错误等统一转失败信封，避免 MCP 层未捕获 500
        return _fail(name, str(e))  # error 字符串给前端与报告模型阅读


def _ok(tool: str, data: dict[str, Any]) -> dict[str, Any]:
    """构造成功信封：与 v2 tool_trace 中 result 形状一致。"""
    return {"tool": tool, "ok": True, "data": data, "error": None}  # error 显式 None


def _fail(tool: str, err: str) -> dict[str, Any]:
    """构造失败信封：data 给空表结构，避免下游假设 rows 一定存在时报 KeyError。"""
    return {"tool": tool, "ok": False, "data": {"rows": [], "count": 0}, "error": err}  # count 与 rows 对齐
