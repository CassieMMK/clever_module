# -*- coding: utf-8 -*-
"""
LIMIT、LIKE、默认年份区间、机构名扩展。
未传 year_start/year_end 时使用 config.query_default_year_start/end（默认 2000–2030），
避免「近 3 年」把旧 Excel 数据全部过滤掉。
【MySQL】占位符由 connection.run_query 处理。
"""
from __future__ import annotations  # 延迟注解

from typing import Any  # pick_year_range 的 args 为宽松 dict

from src.config import get_settings  # 默认年份上下界
from src.runtime_limits import get_runtime_limits  # default_limit / hard_max_rows 等


def clamp_limit(n):
    """
    将用户或规划传入的 limit 限制在 [1, hard_max_rows]；None 时用默认条数。
    """
    lim = get_runtime_limits()  # 合并 yaml 与 Settings 的运行时上限
    v = int(n) if n is not None else lim.default_limit  # 未传则用配置默认
    return max(1, min(v, lim.hard_max_rows))  # 至少 1 条，至多硬上限


def expand_org_name_for_search(org_name: str | None) -> str | None:
    """
    将用户常用机构简称替换为库内常见全称片段，便于 LIKE 命中。
    仅处理明确映射，避免误改「中国社科院」等其它机构。
    """
    if org_name is None:  # 未传机构名
        return None  # 保持 None，调用方用 like_param(None) 得到不加条件
    t = str(org_name).strip()  # 去空白
    if not t:  # 空串视为无有效机构关键词
        return None
    for abbr, full in (  # 元组列表：先匹配长的子串规则可在此扩展
        ("四川省社科院", "四川省社会科学院"),
        ("四川社科院", "四川省社会科学院"),
    ):
        if abbr in t:  # 子串命中即替换（同一串里只按顺序第一次命中）
            return t.replace(abbr, full)
    return t  # 无映射则原样返回


def like_param(s):
    """
    把用户关键词变成 SQL LIKE 的 %keyword% 参数；None/空串返回 None 表示不加该条件。
    """
    if s is None:  # 调用方未提供该槽位
        return None
    t = str(s).strip()  # 统一 str 并去首尾空白
    if not t:  # 全空白等价于未提供
        return None
    return "%" + t + "%"  # SQLite LIKE 通配


def default_year_range():
    """
    当 args 未同时给出 year_start/year_end 时使用的宽松整年区间（闭区间）。
    """
    s = get_settings()  # 读 Settings 默认起止年
    return int(s.query_default_year_start), int(s.query_default_year_end)  # 二元组供 BETWEEN 使用


def pick_year_range(args: dict[str, Any]):
    """
    从规划参数中取年份区间：两者齐全则用用户值，否则回落 default_year_range。
    """
    ys, ye = args.get("year_start"), args.get("year_end")  # 可能为 None
    if ys is not None and ye is not None:  # 必须成对出现才认为用户显式指定
        return int(ys), int(ye)  # 转 int 防字符串进 SQL
    return default_year_range()  # 宽松默认，避免旧数据被近年过滤掉
