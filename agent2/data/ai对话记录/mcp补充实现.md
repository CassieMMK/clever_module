用mcp（可网页可cursor）：
1.MCP Server 独立进程，和 FastAPI 分开跑。
2.FastAPI 调用规划 LLM，得到 {tools: [{name, params}]
FastAPI 遍历 tools，对每个调用 MCP Client.callTool(name, params)
MCP Server 执行 Tool，返回结果
FastAPI 汇总结果，再调大模型生成报告
边界：FastAPI 管“调哪些 Tool”，MCP Server 管“怎么执行 Tool”。
3.FastAPI 里不再有直接调 Tool 的代码，全部走 MCP。
4.MCP Server 默认只监听 127.0.0.1，加 Token 鉴权。FastAPI 和 MCP Server 在同一台机器，通过 localhost 通信。
5.
## 一、最终 Tool 名称表 以代码为准

**是。就是现有 v2 的六个 `query_*` + `web_search`。**

| MCP Tool 名 | 对应 v2 函数 |
|-------------|-------------|
| `query_organization` | `query_organization` |
| `query_person` | `query_person` |
| `query_projects` | `query_projects` |
| `query_teams` | `query_teams` |
| `query_awards` | `query_awards` |
| `web_search` | `web_search` |

**不合并、不拆分、不改名。一字不差。**

---

## 二、每个 Tool 的入参 JSON Schema

---

## 三、每个 Tool 的返回结构

**与现有 registry 信封一致。** 以现有 registry 为准。



---

## 四、并行与顺序规则

**全部可以并行。**

原因：所有 Tool 都是只读查询，没有依赖关系，不需要顺序执行。

`execution_mode` 固定为 `parallel`，不需要 `sequential` 逻辑。

---

> **6 个 Tool，名与 v2 一致。全部可并行。参数以 src/sql_tools 与 web_search 实现为准，规划 LLM 与 MCP 注册与之同步**

6.execution_mode	删掉，后端固定 parallel
step_id	删掉
重试策略	后端管
格式兼容	彻底替换
分析/报告	合并

7.传输方式	Streamable HTTP（和现在一样）
启动方式	手动双进程，不要降级策略
Token	本机可关，部署时开 Bearer Token

8.迁哪个接口	只改 v2，v1 不动
旧 MCP Tool	废弃，删掉
Tool 逻辑放哪	共享库，MCP Server 和 FastAPI 调同一份
数据库配置	两个进程读同一 .db 文件 + 同一 .env

9.超时	callTool 10秒，总请求 30秒，web_search 15秒
日志	记录 tool_name、耗时、脱敏 params、session_id
并发	连接池 10，最大并行 5

10.一次 LLM 调用，同时输出分析结论 + Markdown 报告。前端直接展示全文，或根据标记拆分展示。