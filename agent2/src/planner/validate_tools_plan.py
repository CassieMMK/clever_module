# -*- coding: utf-8 -*-
"""校验规划 LLM 产出的 {tools: [{name, params}]} 结构，供 v2 在调用 MCP 前使用。"""
from __future__ import annotations  # 延迟注解

from typing import Any, Tuple  # 返回 (bool, err, specs)

from src.sql_tools.registry import TOOL_NAMES  # 与 MCP 注册、registry 白名单一致


def validate_tools_plan_dict(
    d: dict[str, Any],
) -> Tuple[bool, str | None, list[tuple[str, dict[str, Any]]] | None]:
    """
    校验 tools 数组；成功时返回 (True, None, [(name, params), ...])。
    params 缺省按空对象处理；name 必须在 TOOL_NAMES 中。
    """
    raw = d.get("tools")  # 规划 JSON 的核心键
    if not isinstance(raw, list):  # 必须是 JSON 数组
        return False, "invalid_plan: tools 必须是数组", None
    if not raw:  # 空数组无法执行任何工具
        return False, "invalid_plan: tools 为空", None
    out: list[tuple[str, dict[str, Any]]] = []  # 规范化后的顺序列表，供 MCP 并行保持顺序
    for i, item in enumerate(raw):  # 带下标便于错误信息定位
        if not isinstance(item, dict):  # 每项必须是对象
            return False, f"invalid_plan: tools[{i}] 不是对象", None
        name = item.get("name")  # 工具名字符串
        if not isinstance(name, str) or not name.strip():  # 非 str 或全空白
            return False, f"invalid_plan: tools[{i}].name 无效", None
        name = name.strip()  # 去掉首尾空白，避免 MCP 侧大小写或空格问题
        if name not in TOOL_NAMES:  # 不在白名单则拒绝，防止任意函数调用
            return False, f"invalid_plan: 未知工具 {name}", None
        params = item.get("params")  # 可为缺省
        if params is None:  # 规划省略 params 时视为空对象
            params = {}
        elif not isinstance(params, dict):  # 禁止数组或字符串当 params
            return False, f"invalid_plan: tools[{i}].params 必须是对象", None
        out.append((name, dict(params)))  # 拷贝 dict，避免后续修改污染原始 plan
    return True, None, out  # 校验通过
