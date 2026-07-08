# -*- coding: utf-8 -*-
"""
【模块作用】封装阿里云 DashScope 兼容 OpenAI 协议的 Chat Completions（含思考、可选联网）。

【MySQL 迁移】无需修改；与数据库无关。
"""
from __future__ import annotations  # 允许类型注解里引用尚未定义的类名（PEP 563）

import time  # 规划超时重试时 sleep 用秒
from typing import Any  # OpenAI 客户端构造参数、extra_body 等用宽松字典类型

from openai import APITimeoutError, OpenAI  # SDK：同步客户端 + 规划阶段需捕获的超时异常

from src.config import ensure_api_key, get_settings  # 读取 API Key 与各超时、模型名配置


def get_client(*, timeout: float | None = None) -> OpenAI:
    """
    构造指向百炼兼容端点的 OpenAI 协议客户端。
    timeout：传给 SDK 的 HTTP 总超时（秒）；None 表示使用 SDK 默认。
    """
    s = get_settings()  # 单例式读取 pydantic_settings 配置（含 base_url、key）
    kwargs: dict[str, Any] = {"api_key": ensure_api_key(), "base_url": s.dashscope_base_url}  # 必填鉴权与兼容模式 v1 根路径
    if timeout is not None:  # 仅在调用方显式传入时才覆盖默认网络超时
        kwargs["timeout"] = timeout  # 避免无参时改变 OpenAI SDK 内部默认行为
    return OpenAI(**kwargs)  # 每次新建客户端；高频场景可在外层自行缓存


def chat_complete(
    messages: list[dict[str, str]],
    *,
    enable_search: bool = False,
    temperature: float = 0.2,
    model: str | None = None,
    timeout_sec: float | None = None,
) -> str:
    """
    非流式一次对话；返回第一条 choice 的 assistant 文本。
    enable_search=True 时在 extra_body 打开百炼联网，由云端检索后再生成。
    """
    s = get_settings()  # 取默认 chat_model 与超时配置
    use_model = model or s.chat_model  # 调用方可覆盖模型名（规划/报告会传 planner_model/report_model）
    client = get_client(timeout=float(timeout_sec)) if timeout_sec is not None else get_client()  # 有超时则绑定到 httpx 层
    extra: dict[str, Any] = {"enable_thinking": True}  # 与 DashScope 示例一致：开启思考链（不需要可删）
    if enable_search:  # 仅 web_search、部分报告场景需要
        extra["enable_search"] = True  # 交给云端执行检索插件，本地不实现爬虫
    comp = client.chat.completions.create(  # 阻塞直到首包完整返回（非流式）
        model=use_model,  # 实际计费的模型 id
        messages=messages,  # OpenAI 格式的 role/content 列表
        temperature=temperature,  # 采样温度：规划偏低、报告可略高
        extra_body=extra,  # 百炼扩展字段：思考、联网等
    )
    return (comp.choices[0].message.content or "").strip()  # 防御 content 为 None；去掉首尾空白


def planner_chat_complete(
    messages: list[dict[str, str]],
    *,
    temperature: float = 0.1,
) -> str:
    """
    v2 规划专用：固定关闭联网；超时用 llm_planner_timeout_sec；遇 APITimeoutError 自动再试 1 次。
    """
    s = get_settings()  # 读取规划超时秒数与 planner_model
    m = (s.planner_model or "").strip() or s.chat_model  # 未单独配置规划模型时回落到主对话模型
    t = float(s.llm_planner_timeout_sec)  # 浮点秒：与 OpenAI 客户端 timeout 类型一致
    for attempt in range(2):  # 最多 2 次：首次 + 一次重试
        try:
            return chat_complete(  # 成功则直接返回规划 JSON 文本
                messages,
                enable_search=False,  # 规划阶段禁止联网，避免不可控外部信息进入 tools 列表
                temperature=temperature,  # 规划通常低温以减少格式漂移
                model=m,  # 使用规划模型或默认 chat_model
                timeout_sec=t,  # 与百炼网络延迟匹配的读超时
            )
        except APITimeoutError:  # 仅捕获明确超时，其它异常直接向上抛便于排查
            if attempt == 0:  # 第一次失败才等待重试
                time.sleep(1.5)  # 轻微退避，避免瞬时拥塞连续打满
                continue  # 进入第二次循环
            raise  # 第二次仍超时：把异常交给 v2_service 转成 planner_failed


def report_chat_complete(
    messages: list[dict[str, str]],
    *,
    temperature: float = 0.3,
    enable_search: bool = False,
) -> str:
    """
    v2 分析/报告：可用 report_model；超时 llm_report_timeout_sec；可按需开联网（如 web_search 工具内部）。
    """
    s = get_settings()  # 取报告超时与 report_model
    m = (s.report_model or "").strip() or s.chat_model  # 未配置报告模型则用主模型
    return chat_complete(  # 与规划共用底层实现，仅参数不同
        messages,
        enable_search=enable_search,  # 结构化 web_search 工具内部会传 True
        temperature=temperature,  # 报告略高温度使行文更顺
        model=m,
        timeout_sec=float(s.llm_report_timeout_sec),  # 报告上下文长，通常给更长超时
    )
