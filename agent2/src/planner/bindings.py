# -*- coding: utf-8 -*-
"""
【模块作用】顺序执行时解析参数中的依赖：from_context 与 {{stepId:json_pointer}} 占位符。

【JSON Pointer】简化实现：仅支持 / 分隔的 dict 键与 list 整数下标。
"""
from __future__ import annotations  # 延迟注解

import re  # 占位符正则
from typing import Any  # 递归解析任意 JSON 子树

# 匹配 {{step_1:/data/rows/0/name}} 形式；两段分别为 stepId 与 pointer
_PLACE = re.compile(r"\{\{([^:{}]+):([^}]+)\}\}")


def get_json_pointer(root: Any, pointer: str) -> Any:
    """
    从 root（常为某步 result）按 pointer 取值。
    pointer 以 / 开头为标准 JSON Pointer；空或 "/" 返回 root 本身。
    """
    p = pointer.strip()
    if not p or p == "/":
        return root
    if not p.startswith("/"):
        p = "/" + p  # 统一成绝对 pointer
    cur: Any = root
    for seg in p.lstrip("/").split("/"):
        if seg == "":
            continue
        if isinstance(cur, list):
            cur = cur[int(seg)]  # 段必须是非负整数索引
        elif isinstance(cur, dict):
            cur = cur[seg]  # dict 键
        else:
            raise KeyError(pointer)  # 叶子非容器无法继续下钻
    return cur


def resolve_from_context_token(token: str, ctx: dict[str, dict[str, Any]]) -> Any:
    """
    token 形如 "step_a:/data/count"：从 ctx[step_a] 取上一步结果，再按 pointer 取值。
    若该步不存在或 ok 为假，抛 ValueError（调用方应捕获或预先校验计划）。
    """
    if ":" not in token:
        raise ValueError("bad from_context")
    sid, path = token.split(":", 1)  # 只分割第一个冒号（path 内可有冒号则需注意，此处简化）
    base = ctx.get(sid)
    if not base or not base.get("ok"):
        raise ValueError("missing step " + sid)
    data = base.get("data") or {}
    ptr = path if path.startswith("/") else "/" + path
    return get_json_pointer(data, ptr)


def resolve_value(val: Any, ctx: dict[str, dict[str, Any]]) -> Any:
    """
    递归遍历 val：dict 含 from_context 则替换为解析值；
    其它 dict/list 继续递归；str 内 {{...}} 替换为 str(解析值)。
    """
    if isinstance(val, dict):
        if "from_context" in val:
            return resolve_from_context_token(str(val["from_context"]), ctx)
        return {k: resolve_value(v, ctx) for k, v in val.items()}
    if isinstance(val, list):
        return [resolve_value(x, ctx) for x in val]
    if isinstance(val, str):

        def repl(m: re.Match[str]) -> str:
            sid = m.group(1).strip()
            path = m.group(2).strip()
            v = resolve_from_context_token(sid + ":" + path, ctx)
            return str(v)

        return _PLACE.sub(repl, val)
    return val  # 数字、布尔、None 等原样返回


def has_from_context(obj: Any) -> bool:
    """深度优先：任意嵌套处出现键 from_context 则返回 True。"""
    if isinstance(obj, dict):
        if "from_context" in obj:
            return True
        return any(has_from_context(v) for v in obj.values())
    if isinstance(obj, list):
        return any(has_from_context(x) for x in obj)
    return False
