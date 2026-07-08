# -*- coding: utf-8 -*-
"""
联网检索：v1 返回纯文本；v2 使用结构化 JSON results 供报告引用。
对 url 做「须在原始 raw 中出现」的清洗，减少模型臆造导致 Word/浏览器打不开。
"""
from __future__ import annotations  # 延迟注解

import json  # 解析模型输出的 JSON 片段
import re  # 提取 ```json 代码块、以及历史遗留时可用的正则（当前清洗用子串）
from typing import Any  # 工具返回体为宽松 dict

from src.llm_client import chat_complete, report_chat_complete  # v1 用 chat；v2 结构化用 report 超时配置


def tool_web_search(query: str) -> dict[str, Any]:
    """
    v1 风格：单次 chat_complete + enable_search，返回长文本摘要。
    保留给旧 orchestrator / MCP 历史路径；v2 主路径用 tool_web_search_structured。
    """
    text = chat_complete(  # 不指定 model 时用默认 chat_model
        [{"role": "user", "content": "请简要检索并总结（给出可参考的公开信息要点）：" + query}],  # 单轮 user
        enable_search=True,  # 打开百炼联网
    )
    return {"ok": True, "tool_name": "web_search", "data": {"text": text}, "source": "dashscope+search"}  # v1 形状


def tool_web_search_structured(query: str) -> dict[str, Any]:
    """
    v2：要求模型只输出 JSON；再解析为 results；最后清洗 url。
    """
    raw = report_chat_complete(  # 使用 report 超时与 report_model（若配置）
        [
            {
                "role": "system",
                "content": (
                    "只输出一个 JSON 对象：{\"results\":[...]}。results 为数组，每项必须含 title, snippet, url 三个字符串字段。\n"
                    "snippet：须从「本次模型可见的联网检索原文」压缩摘录，勿编造检索中未出现的具体数字、机构或结论。\n"
                    "url 规则（极重要）：只要在同一条目对应的检索可见原文中出现过**完整** http(s) 链接，就必须**原样抄入**该条目的 url，勿遗漏；"
                    "仅当该条对应原文中确实没有任何完整 http(s) 时，url 才置为空字符串 \"\"。禁止根据标题猜测域名、禁止编造路径。\n"
                    "若 url 非空，该字符串必须能在你的完整模型输出中被原样找到（否则后端会清空以防假链）。宁可 url 为空，也不要填写原文中不存在的地址。"
                ),
            },
            {"role": "user", "content": "检索并整理要点：" + query},  # 用户检索意图原样拼接
        ],
        enable_search=True,  # 由云端完成检索插件调用
    )
    results = _parse_web_json(raw)  # 从模型输出抠出 list[dict]
    results = _sanitize_result_urls(results, raw)  # 剔除 raw 中未出现的 url
    return {"ok": True, "tool_name": "web_search", "data": {"results": results, "raw_text": raw}, "source": "dashscope+search"}  # raw 留给排错


def _parse_web_json(raw: str) -> list[dict[str, str]]:
    """
    尝试从模型输出中解析 JSON；支持外层 ```json 包裹；支持顶层 {results:[]} 或直接数组。
    失败时退化为单条摘要，避免上游崩溃。
    """
    s = raw.strip()  # 去掉首尾空白
    m = re.search(r"```(?:json)?\s*([\s\S]*?)```", s)  # 常见模型习惯用 markdown 代码块包 JSON
    if m:
        s = m.group(1).strip()  # 只取代码块内部
    try:
        data = json.loads(s)  # 可能抛 JSONDecodeError
        if isinstance(data, dict) and "results" in data:  # 允许 {"results":[...]}
            data = data["results"]  # 归一为 list
        if isinstance(data, list):  # 期望的数组形态
            out = []
            for it in data:  # 逐项规范化字段
                if not isinstance(it, dict):
                    continue  # 跳过非对象元素
                out.append(
                    {
                        "title": str(it.get("title") or "")[:500],  # 控制长度防爆 token/DB
                        "snippet": str(it.get("snippet") or "")[:2000],
                        "url": str(it.get("url") or "")[:2000],
                    }
                )
            return out
    except Exception:
        pass  # 解析失败走下方退化
    return [{"title": "检索摘要", "snippet": raw[:2000], "url": ""}]  # 至少返回一条可展示文本


def _sanitize_result_urls(results: list[dict[str, str]], raw: str) -> list[dict[str, str]]:
    """
    若某条 url 未在当次模型原始输出 raw 中出现，则置空，避免报告里 Markdown 链接 404。
    blob_alt 处理 JSON 里可能对 / 的转义写法。
    """
    blob = raw or ""  # 防御 None
    blob_alt = blob.replace("\\/", "/")  # 部分模型会转义斜杠
    out: list[dict[str, str]] = []
    for r in results:  # 逐项拷贝修改
        rr = dict(r)  # 浅拷贝，避免改入参
        u = str(rr.get("url") or "").strip()  # 当前条声称的链接
        if u and (u not in blob and u not in blob_alt):  # 原文中完全找不到则视为幻觉链接
            rr["url"] = ""  # 强制清空，报告侧只能写摘要
        out.append(rr)
    return out
