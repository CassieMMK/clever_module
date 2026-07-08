# -*- coding: utf-8 -*-
"""
【模块作用】FastAPI 应用：健康检查、v1 对话编排、v2 规划+MCP、Markdown 导出 Word。

【依赖】启动时 lifespan 调用 ensure_database，保证 SQLite 与可选 Excel 导入完成。
"""
from __future__ import annotations  # 允许类型注解引用尚未定义的类名

import logging  # 记录 chat / export 异常栈
from contextlib import asynccontextmanager  # 异步 lifespan 上下文

from fastapi import FastAPI, HTTPException  # Web 框架与 HTTP 错误响应
from fastapi.middleware.cors import CORSMiddleware  # 浏览器跨域
from fastapi.responses import StreamingResponse  # 二进制 docx 流式返回
from pydantic import BaseModel, Field  # 请求体验证与默认值

from src.db.sqlite_loader import ensure_database  # 启动时建表/导入
from src.orchestrator import run_chat_turn  # v1：槽位→库→网→分析→报告
from src.v2_service import run_chat_turn_v2_async  # v2：规划→MCP→报告
from src.api.word_export import markdown_to_docx_bytes  # Markdown 转 docx 字节
from src.kg import (  # 知识图谱服务：重建任务与子图查询
    KgApiError,
    get_all_scene_graphs,
    get_hierarchy_graph,
    get_rebuild_status,
    query_graph,
    get_scene_graph,
    get_subgraph,
    start_rebuild,
)

logging.basicConfig(level=logging.INFO)  # 根 logger 级别 INFO
LOG = logging.getLogger("agent_api")  # 本模块专用 logger 名


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期：启动阶段执行一次 ensure_database，再 yield 让服务运行；
    关闭阶段（若有清理逻辑）可写在 yield 之后。
    """
    ensure_database()  # 同步阻塞：建 SQLite 表、空库时从 Excel 导入
    yield  # 此处之后 FastAPI 开始接受请求


app = FastAPI(title="SKL Agent API", lifespan=lifespan)  # 挂载 lifespan
# 允许任意源、任意方法/头：开发环境省事；生产应收紧 origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatIn(BaseModel):
    """POST /api/agent/chat 请求体：会话 id、用户消息、可选前端上下文。"""

    session_id: str = Field(default="demo")  # 多轮对话隔离键
    message: str = ""  # 用户自然语言
    context: dict | None = None  # 如 region、time_range、output_type


class ChatV2In(BaseModel):
    """POST /api/agent/chat/v2 请求体：字段与 v1 对齐，便于前端复用。"""

    session_id: str = Field(default="demo")
    message: str = ""
    context: dict | None = None


@app.get("/api/agent/health")
def health():
    """负载均衡或运维探活：无鉴权、轻量 JSON。"""
    return {"ok": True, "service": "agent"}


@app.post("/api/agent/chat")
def chat(body: ChatIn):
    """v1 同步编排入口；异常记日志后转 500。"""
    try:
        return run_chat_turn(body.model_dump())  # Pydantic v2：转普通 dict
    except Exception as e:
        LOG.exception("chat failed")
        raise HTTPException(500, str(e)) from e


@app.post("/api/agent/chat/v2")
async def chat_v2(body: ChatV2In):
    """v2 异步：内部 await MCP 与（部分）LLM，须在 async 路由中。"""
    try:
        return await run_chat_turn_v2_async(body.model_dump())
    except Exception as e:
        LOG.exception("chat v2 failed")
        raise HTTPException(500, str(e)) from e


class ExportIn(BaseModel):
    """POST /api/agent/export-word：前端把 Markdown 报告原文 POST 上来。"""

    markdown: str = ""


class KgSubgraphIn(BaseModel):
    """GET /api/kg/subgraph 入参模型。"""

    center_id: str = Field(default="", description="中心节点 ID，格式 {type}_{id}")
    depth: int = Field(default=1, description="子图深度，仅支持 1 或 2")
    max_nodes: int = Field(default=100, description="最大节点数，<=200")
    include_similar: bool = Field(default=False, description="是否包含相似边")


class KgQueryIn(BaseModel):
    """GET /api/kg/query 入参模型。"""

    keyword: str = Field(default="", description="查询关键词")
    search_type: str = Field(default="auto", description="auto/author/paper")
    max_nodes: int = Field(default=120, description="最大节点数，<=300")
    include_similar: bool = Field(default=False, description="是否包含相似实体关系")


@app.post("/api/agent/export-word")
def export_word(body: ExportIn):
    """
    将 Markdown 粗解析为 docx，以附件流返回；
    StreamingResponse 用 iter([data]) 一次性字节块，避免大文件占内存时可改分块。
    """
    try:
        data = markdown_to_docx_bytes(body.markdown or "")  # 空串则生成空文档
        return StreamingResponse(
            iter([data]),  # 异步迭代器接口兼容：单元素迭代
            media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            headers={"Content-Disposition": 'attachment; filename="report.docx"'},
        )
    except Exception as e:
        LOG.exception("export")
        raise HTTPException(500, str(e)) from e


@app.post("/api/kg/rebuild/start")
def kg_rebuild_start():
    """启动知识图谱全量重建（后台线程执行）。"""
    try:
        return start_rebuild()
    except KgApiError as e:
        raise HTTPException(
            status_code=400,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg rebuild start failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/rebuild/status")
def kg_rebuild_status(task_id: str | None = None):
    """查询知识图谱重建任务状态。"""
    try:
        return get_rebuild_status(task_id=task_id)
    except KgApiError as e:
        raise HTTPException(
            status_code=404,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg rebuild status failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/subgraph")
def kg_subgraph(
    center_id: str,
    depth: int = 1,
    max_nodes: int = 100,
    include_similar: bool = False,
):
    """获取知识图谱子图。"""
    try:
        body = KgSubgraphIn(
            center_id=center_id,
            depth=depth,
            max_nodes=max_nodes,
            include_similar=include_similar,
        )
        return get_subgraph(
            center_id=body.center_id,
            depth=body.depth,
            max_nodes=body.max_nodes,
            include_similar=body.include_similar,
        )
    except KgApiError as e:
        # INVALID_PARAM/TOO_MANY_NODES 等参数错误按 400 返回
        if e.code in {"INVALID_PARAM", "TOO_MANY_NODES"}:
            code = 400
        # 图谱版本过期，提示前端先重建
        elif e.code in {"GRAPH_STALE"}:
            code = 409
        # CENTER_NOT_FOUND/GRAPH_NOT_READY 等资源错误按 404 返回
        elif e.code in {"CENTER_NOT_FOUND", "GRAPH_NOT_READY"}:
            code = 404
        else:
            code = 500
        raise HTTPException(
            status_code=code,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg subgraph failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/query")
def kg_query(
    keyword: str = "",
    search_type: str = "auto",
    max_nodes: int = 120,
    include_similar: bool = False,
):
    """按关键词查询作者-论文图谱。"""
    try:
        body = KgQueryIn(
            keyword=keyword,
            search_type=search_type,
            max_nodes=max_nodes,
            include_similar=include_similar,
        )
        return query_graph(
            keyword=body.keyword,
            search_type=body.search_type,
            max_nodes=body.max_nodes,
            include_similar=body.include_similar,
        )
    except KgApiError as e:
        if e.code in {"INVALID_PARAM"}:
            code = 400
        elif e.code in {"GRAPH_STALE"}:
            code = 409
        elif e.code in {"GRAPH_NOT_READY"}:
            code = 404
        else:
            code = 500
        raise HTTPException(
            status_code=code,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg query failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/scene")
def kg_scene(
    scene: str,
    max_nodes: int = 120,
    include_similar: bool = False,
    include_award_links: bool = False,
):
    """按场景获取轻量关系图（政务/产业/科研），用于前端场景切换。"""
    try:
        return get_scene_graph(
            scene=scene,  # 场景名：government / industry / research
            max_nodes=max_nodes,  # 节点上限，防止单图过于拥挤
            include_similar=include_similar,  # 是否显示相似关系（全场景可开关）
            include_award_links=include_award_links,  # 是否显示机构/人员到奖项的关联边
        )
    except KgApiError as e:
        # 参数错误（场景名非法、max_nodes 越界）按 400 返回
        if e.code in {"INVALID_PARAM", "TOO_MANY_NODES"}:
            code = 400
        # 图尚未构建按 404 返回，提示先执行重建
        elif e.code in {"GRAPH_NOT_READY"}:
            code = 404
        else:
            code = 500
        raise HTTPException(
            status_code=code,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg scene failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/scenes")
def kg_scenes(
    max_nodes: int = 120,
    include_similar: bool = False,
    include_award_links: bool = False,
):
    """一次返回三张场景图（政务/产业/科研），前端可直接切换展示。"""
    try:
        return get_all_scene_graphs(
            max_nodes=max_nodes,  # 三张图统一节点上限，避免某张图过大
            include_similar=include_similar,  # 是否展示相似关系
            include_award_links=include_award_links,  # 是否展示机构/人员到奖项的关系
        )
    except KgApiError as e:
        # 参数错误（max_nodes 非法）按 400 返回
        if e.code in {"INVALID_PARAM", "TOO_MANY_NODES"}:
            code = 400
        # 图尚未构建按 404 返回
        elif e.code in {"GRAPH_NOT_READY"}:
            code = 404
        else:
            code = 500
        raise HTTPException(
            status_code=code,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg scenes failed")
        raise HTTPException(500, str(e)) from e


@app.get("/api/kg/hierarchy")
def kg_hierarchy(
    group_by: str = "discipline",
    max_orgs: int = 12,
    max_nodes: int = 500,
    org_id: str = "",
    expand_depth: int = 2,
    include_similar: bool = False,
):
    """按“机构→分组→课题→负责人”返回层级图。"""
    try:
        return get_hierarchy_graph(
            group_by=group_by,  # 二层分组策略：discipline / topic_category
            max_orgs=max_orgs,  # 机构数量上限，控制整体密度
            max_nodes=max_nodes,  # 节点总量上限，避免前端超载
            org_id=org_id,  # 机构筛选（空表示全部）
            expand_depth=expand_depth,  # 默认展开深度 1-4
            include_similar=include_similar,  # 是否显示课题相似关系
        )
    except KgApiError as e:
        if e.code in {"INVALID_PARAM", "TOO_MANY_NODES"}:  # 参数错误按 400 返回
            code = 400
        elif e.code in {"GRAPH_NOT_READY"}:  # 图未构建按 404 返回
            code = 404
        else:
            code = 500
        raise HTTPException(
            status_code=code,
            detail={"error_code": e.code, "message": e.message},
        ) from e
    except Exception as e:
        LOG.exception("kg hierarchy failed")
        raise HTTPException(500, str(e)) from e
