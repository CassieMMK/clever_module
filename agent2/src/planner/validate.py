# -*- coding: utf-8 -*-
"""
【模块作用】校验「旧版」AgentPlan：含 steps、execution_mode 的 Pydantic 模型。

【并行约束】execution_mode 为 parallel 时，任何参数里不得出现 from_context（无法定序）。
"""
from __future__ import annotations  # 延迟注解

from typing import Any, Tuple  # validate 返回 (ok, err, plan)

from src.sql_tools.registry import TOOL_NAMES  # 允许的工具名集合
from src.planner.models import AgentPlan  # Pydantic 计划模型
from src.planner.bindings import has_from_context  # 递归检测 from_context 键


def validate_plan_dict(d: dict[str, Any]) -> Tuple[bool, str | None, AgentPlan | None]:
    """
    入参：通常为历史格式或测试用的计划 dict。
    返回：(是否通过, 错误文案, 解析后的 AgentPlan 或 None)。
    """
    try:
        plan = AgentPlan.model_validate(d)  # Pydantic v2：严格类型与必填项
    except Exception as e:
        return False, "invalid_plan: " + str(e), None
    if not plan.steps:
        return False, "invalid_plan: empty steps", None
    for st in plan.steps:
        if st.tool not in TOOL_NAMES:
            return False, "invalid_plan: unknown tool " + st.tool, None
    if plan.execution_mode == "parallel":
        for st in plan.steps:
            if has_from_context(st.arguments):
                return False, "invalid_plan: parallel forbids from_context", None
    ids = [s.id for s in plan.steps]
    if len(set(ids)) != len(ids):
        return False, "invalid_plan: duplicate step id", None
    return True, None, plan
