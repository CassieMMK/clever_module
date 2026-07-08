# -*- coding: utf-8 -*-
"""
【模块作用】规则槽位抽取：从用户自然语言中识别地区、机构关键词、主题、年份区间等，供数据库条件与追问判断使用。

【MySQL 迁移】
- 本模块与数据库方言无关，一般不改；若改为模型抽槽，可替换实现但保持 Slots 字段含义一致。
"""
from __future__ import annotations

import re  # 提取年份区间、单年
from dataclasses import dataclass  # Slots 数据类
from typing import Any  # to_dict 返回值

# 键：用户口语；值：与库中地址字段可能匹配的标准串（简化规则，可继续扩充）
REGION_ALIAS = {
    "成都": "成都市",
    "成都市": "成都市",
    "绵阳": "绵阳市",
    "绵阳市": "绵阳市",
    "四川": "四川省",
    "四川省": "四川省",
}

# 键：用户说法；值：用于课题名 LIKE 或业务统计口径的统一主题名
TOPIC_ALIAS = {
    "乡村振兴": "乡村振兴",
    "乡村治理": "乡村振兴",
    "基层治理": "基层治理",
}


@dataclass
class Slots:
    """一轮对话解析出的结构化条件，供 query_engine 与追问逻辑使用。"""

    region: str | None = None  # 标准化后的地区名（常带省市后缀）
    org_keyword: str | None = None  # 机构名片段，如「社科院」
    topic: str | None = None  # 标准化主题
    start_year: int | None = None  # 闭区间起点
    end_year: int | None = None  # 闭区间终点
    query_type: str | None = None  # 预留：统计/列表/趋势等（当前 query_engine 未强依赖）
    raw_time: str | None = None  # 展示用时间字符串

    def to_dict(self) -> dict[str, Any]:
        """序列化进 API 响应 filled_slots、会话记忆。"""
        return {
            "region": self.region,
            "org_keyword": self.org_keyword,
            "topic": self.topic,
            "start_year": self.start_year,
            "end_year": self.end_year,
            "query_type": self.query_type,
            "raw_time": self.raw_time,
        }


def normalize_region(text: str) -> str | None:
    """在全文 text 中找最长匹配的别名，避免短词抢先（如「四川」与「四川省」）。"""
    keys = sorted(REGION_ALIAS.keys(), key=len, reverse=True)
    for k in keys:
        if k in text:
            return REGION_ALIAS[k]
    return None


def normalize_topic(text: str) -> str | None:
    """主题别名归一，未命中返回 None。"""
    keys = sorted(TOPIC_ALIAS.keys(), key=len, reverse=True)
    for k in keys:
        if k in text:
            return TOPIC_ALIAS[k]
    return None


def extract_org_keyword(text: str) -> str | None:
    """识别机构关键词，用于 SQL 中 o.unit_name LIKE %关键词%。"""
    if "社科院" in text:
        return "社科院"
    if "社会科学院" in text:
        return "社会科学院"
    return None


def extract_years(text: str) -> tuple[int | None, int | None, str | None]:
    """
    返回 (start_year, end_year, raw 展示串)。
    优先匹配「2020-2024」「2020到2024」；否则取第一个四位年作为单年区间。
    """
    m = re.search(r"(20\d{2})\s*[-到至]\s*(20\d{2})", text)
    if m:
        a, b = int(m.group(1)), int(m.group(2))
        lo, hi = min(a, b), max(a, b)
        return lo, hi, f"{lo}-{hi}"
    m2 = re.search(r"(20\d{2})", text)
    if m2:
        y = int(m2.group(1))
        return y, y, str(y)
    return None, None, None


def extract_slots(user_text: str) -> Slots:
    """组合上述函数，生成 Slots；不调用大模型，保证可测、可复现。"""
    t = user_text or ""
    r = normalize_region(t)
    topic = normalize_topic(t)
    okw = extract_org_keyword(t)
    sy, ey, raw = extract_years(t)
    qt = None
    if "趋势" in t or "历年" in t:
        qt = "year_trend"
    elif "热点" in t or "词云" in t:
        qt = "topic_hot"
    elif "列表" in t or "哪些项目" in t:
        qt = "project_list"
    else:
        qt = "project_count"
    return Slots(region=r, org_keyword=okw, topic=topic, start_year=sy, end_year=ey, query_type=qt, raw_time=raw)


def missing_for_search(sl: Slots) -> list[str]:
    """
    返回仍缺的槽位名列表：region_or_org 表示既无地区也无机构关键词；
    time_range 表示缺起止年。编排层据此返回 clarify。
    """
    miss: list[str] = []
    if not sl.region and not sl.org_keyword:
        miss.append("region_or_org")
    if sl.start_year is None or sl.end_year is None:
        miss.append("time_range")
    return miss


def missing_topic_flow(sl: Slots, user_wants_topic_detail: bool) -> list[str]:
    """预留扩展：若产品要求「必须先选主题」可在此追加 topic 缺失。"""
    miss: list[str] = []
    if user_wants_topic_detail and not sl.topic:
        miss.append("topic")
    return miss
