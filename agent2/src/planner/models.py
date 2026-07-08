# -*- coding: utf-8 -*-
"""
【模块作用】旧版规划 JSON 的 Pydantic 模型：PlanStep 列表 + 并行/顺序执行模式。

【v2 主路径】当前线上 v2 多用 validate_tools_plan_dict 的简化 tools 数组；本模型仍保留供 executor/测试。
"""
from __future__ import annotations  # 延迟注解

from typing import Any, Literal  # arguments 为任意 JSON；execution_mode 枚举

from pydantic import BaseModel, Field  # 数据类 + 字段约束


class PlanStep(BaseModel):
    """单步：稳定 id、工具名、参数字典、可选人类可读 reason。"""

    id: str = Field(min_length=1)  # sequential 时占位符 {{id:path}} 引用此 id
    tool: str  # 必须在 TOOL_NAMES 内（由 validate 再验）
    arguments: dict[str, Any] = Field(default_factory=dict)  # 传给 run_named_tool 的参数
    reason: str | None = None  # 规划器解释（可选）


class AgentPlan(BaseModel):
    """整包计划：版本号、意图、步骤列表、执行模式。"""

    schema_version: str = "1"  # 预留迁移
    intent: str = ""  # 一句话意图（展示或日志）
    reason: str | None = None  # 整包说明（可选）
    execution_mode: Literal["parallel", "sequential"] = "parallel"  # 默认并行
    steps: list[PlanStep] = Field(default_factory=list)  # 至少一步由 validate 保证
