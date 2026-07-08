# -*- coding: utf-8 -*-
"""
【模块作用】调用规划侧大模型 planner_chat_complete，强制只输出 JSON；
提示词约束「本地 query_* 优先、web_search 仅补充」。

【失败】extract_json_object 解析失败会抛 ValueError，由 v2_service 捕获为 planner_failed。
"""
from __future__ import annotations  # 延迟注解

import json  # loads 解析 LLM 输出中的 JSON 对象
import re  # 剥离 ```json 代码块与首尾大括号定位
from typing import Any  # 规划结果为宽松 dict

from src.llm_client import planner_chat_complete  # 带规划超时与重试的 chat

# 系统提示：工具名白名单、参数键、intent+tools 形状；勿改 name 拼写（与 MCP/registry 一致）
SYSTEM = """你是社科数据「本地库优先」查询规划器。只输出一个 JSON 对象，不要 Markdown，不要解释。

硬性规则：
1) 用户问统计、机构、课题、团队、奖项、人员时，tools 里必须包含对应的 query_*，不能只规划 web_search。
2) 需要综合对比时，在 tools 数组中列出多个 query_*（可加一个 web_search 作补充）；后端固定并行执行，无需声明顺序或 execution_mode。
3) 参数从用户原文抽取机构名、地区、人名、主题词；用户没说年份时不要在 params 里写 year_start/year_end（服务端会用 2000–2030 宽松区间）。
   机构名：用户说「四川省社科院」「四川社科院」等简称时，query_* 的 org_name 请写规范全称「四川省社会科学院」以利命中（服务端也会做少量别名扩展）。
4) web_search 仅作公开信息补充，不得替代本地 query_*；后续报告以本地 data 为主干、联网结果须逐条可溯源（url 以工具返回为准）。
5) query_projects 的 topic_type：仅当你能对应 Excel「课题类别表」的真实 category/type 文字时才填写。用户只说「省课题」「省级课题」时不要传 topic_type。

可用工具 name（一字不差）与 params 键名：
- query_organization: org_name?, address_keyword?, limit?
- query_person: person_name?, org_name?, limit?
- query_projects: org_name?, discipline?, topic_type?, header_name?, year_start?, year_end?, limit?
- query_teams: org_name?, discipline?, group_type?, year_start?, year_end?, limit?
- query_awards: org_name?, person_name?, discipline?, award_type?, year_start?, year_end?, limit?（省奖行含出版单位 publish_unit_*；国奖行含 award_categorie_name）
- web_search: query (必填)

JSON 结构（仅此形状）：
{"intent":"用一句话概括意图","tools":[{"name":"query_organization","params":{"org_name":"..."}},{"name":"web_search","params":{"query":"..."}}]}"""


def extract_json_object(text: str) -> dict[str, Any]:
    """
    容错：先去掉可选 ```json ... ``` 围栏；再在全文找第一个 { 与最后一个 } 子串 json.loads。
    若找不到合法大括号对则抛 ValueError。
    """
    s = text.strip()  # 去 BOM 旁空白
    m = re.search(r"```(?:json)?\s*([\s\S]*?)```", s)  # 非贪婪匹配代码块
    if m:
        s = m.group(1).strip()  # 只取围栏内正文
    start = s.find("{")  # JSON 对象起点
    end = s.rfind("}")  # 最后一个 }，应对模型尾部废话
    if start < 0 or end <= start:
        raise ValueError("no json object")
    return json.loads(s[start : end + 1])  # 闭区间切片含 end 的 }


def plan_with_llm(user_message: str, extra_context: str | None) -> dict[str, Any]:
    """
    user_message：用户本轮问题（可能已含 stepwise 合并后的完整句）。
    extra_context：前端 context 序列化 JSON 字符串，拼在用户消息后供规划参考。
    返回：解析后的 dict，至少含 tools 数组（具体校验在 validate_tools_plan_dict）。
    """
    u = user_message.strip()
    if extra_context:
        u = u + "\n附加上下文(JSON)：\n" + extra_context  # 避免模型忽略前端筛选条件
    raw = planner_chat_complete(
        [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": u},
        ],
        temperature=0.1,  # 低温度：工具选择更稳定
    )
    return extract_json_object(raw)  # 失败向上抛
