# -*- coding: utf-8 -*-
"""
知识图谱（简版）构建与查询引擎。

V1 实现范围（按需求）：
1. 只保留“作者-论文”两类实体。
2. 只保留“作者 -> 论文（撰写）”关系。
3. 共同作者关系先不实现，仅保留后续扩展位。
4. 向量库使用 Chroma 本地持久化。
5. 查询时支持关键词检索 + 向量召回兜底 + 可选 qwen-plus 意图识别。
"""
from __future__ import annotations

import json
import logging
import shutil
import threading
import time
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

from openai import OpenAI

from src.config import ensure_api_key, get_settings
from src.kg.simple_repository import BaseSimpleKgRepository, SQLiteSimpleKgRepository

LOG = logging.getLogger("kg_simple_engine")

# 按需求固定使用 text-embedding-v3。
_EMBEDDING_MODEL = "text-embedding-v3"
# 按需求固定使用 qwen-plus 做轻量实体/意图抽取。
_RAG_MODEL = "qwen-plus"
# 图谱结构版本号：当建图口径升级时递增，用于识别旧 graph.json 并提示重建。
_GRAPH_SCHEMA_VERSION = "v2-multi-source-paper"


class KgApiError(Exception):
    """知识图谱 API 业务错误。"""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass
class RebuildTaskState:
    """重建任务状态。"""

    status: str
    stage: str
    progress: int
    message: str
    task_id: str
    started_at: str
    ended_at: str = ""
    error_code: str = ""


_TASK_LOCK = threading.Lock()
_TASK_STATE: RebuildTaskState | None = None


def _now_iso() -> str:
    """返回当前 ISO 时间字符串。"""
    return datetime.now().isoformat(timespec="seconds")


def _kg_store_root() -> Path:
    """图谱数据根目录（与 sqlite 同级）。"""
    return get_settings().agent_sqlite_path.parent / "kg_store"


def _current_dir() -> Path:
    """当前生效版本目录。"""
    return _kg_store_root() / "current"


def _current_graph_path() -> Path:
    """当前 graph.json 路径。"""
    return _current_dir() / "graph.json"


def _set_task_state(**kwargs: Any) -> None:
    """线程安全更新任务状态。"""
    global _TASK_STATE
    with _TASK_LOCK:
        if _TASK_STATE is None:
            return
        for key, value in kwargs.items():
            setattr(_TASK_STATE, key, value)


def _safe_text(value: Any) -> str:
    """统一字符串清洗：None 转空串、去首尾空白。"""
    if value is None:
        return ""
    return str(value).strip()


def _to_int(value: Any) -> int:
    """安全转 int，失败返回 0。"""
    try:
        return int(value)
    except Exception:
        return 0


def _embed_texts(texts: list[str], api_key: str) -> list[list[float]]:
    """
    批量向量化文本。

    约束：
    - 模型固定 text-embedding-v3。
    - 单批 10 条，降低超时风险。
    - 每批失败最多重试 3 次。
    """
    if not texts:
        return []
    settings = get_settings()
    client = OpenAI(
        api_key=api_key,
        base_url=settings.dashscope_base_url,
        timeout=30.0,
    )
    vectors: list[list[float]] = []
    batch_size = 10
    for start in range(0, len(texts), batch_size):
        batch = texts[start : start + batch_size]
        last_error: Exception | None = None
        for retry_idx in range(3):
            try:
                resp = client.embeddings.create(model=_EMBEDDING_MODEL, input=batch)
                batch_vectors = [list(item.embedding) for item in resp.data]
                if len(batch_vectors) != len(batch):
                    raise RuntimeError("embedding response size mismatch")
                vectors.extend(batch_vectors)
                last_error = None
                break
            except Exception as ex:
                last_error = ex
                LOG.warning("embedding failed start=%s retry=%s error=%s", start, retry_idx, ex)
                time.sleep(1.0 + retry_idx)
        if last_error is not None:
            raise KgApiError("EMBEDDING_FAILED", f"向量化失败: {last_error}")
    return vectors


def _persist_chroma(node_ids: list[str], summaries: list[str], vectors: list[list[float]], types: list[str]) -> None:
    """
    将向量落盘到 Chroma。

    MySQL 切换改造点：
    - 此处与数据库类型无关，SQLite -> MySQL 不影响本函数。
    """
    import chromadb

    chroma_dir = _current_dir() / "chroma"
    chroma_dir.mkdir(parents=True, exist_ok=True)
    client = chromadb.PersistentClient(path=str(chroma_dir))

    def _upsert_all(collection: Any) -> None:
        max_batch = 1000
        for start in range(0, len(node_ids), max_batch):
            end = min(start + max_batch, len(node_ids))
            collection.upsert(
                ids=node_ids[start:end],
                documents=summaries[start:end],
                embeddings=vectors[start:end],
                metadatas=[{"entity_type": t} for t in types[start:end]],
            )

    try:
        collection = client.get_or_create_collection(name="kg_nodes_v1")
        _upsert_all(collection)
    except Exception:
        # 自愈策略：历史索引损坏时，删除并重建 collection 后重试一次。
        try:
            client.delete_collection(name="kg_nodes_v1")
        except Exception:
            pass
        collection = client.get_or_create_collection(name="kg_nodes_v1")
        _upsert_all(collection)


def _build_graph_payload(repo: BaseSimpleKgRepository, api_key: str) -> dict[str, Any]:
    """从 SQLite 数据构建作者-论文图谱并计算向量。"""
    _set_task_state(stage="load", progress=20, message="加载作者与论文数据")
    rows = repo.fetch_author_paper_rows()
    author_related_map = repo.fetch_author_related_info()

    nodes: dict[str, dict[str, Any]] = {}
    edges: list[dict[str, Any]] = []
    edge_seen: set[tuple[str, str, str]] = set()

    vector_ids: list[str] = []
    vector_texts: list[str] = []
    vector_types: list[str] = []
    author_papers: dict[int, list[dict[str, Any]]] = {}
    # 用于避免同一作者在多来源数据里出现重复“同名同年成果”。
    author_paper_seen: dict[int, set[tuple[str, int]]] = {}

    for row in rows:
        author_id = _to_int(row.get("author_id"))
        # paper_uid 可能是 "province_123" 这类文本主键，不能再强转 int。
        # 若历史数据没有该字段，则回退到旧字段 paper_id。
        paper_uid = _safe_text(row.get("paper_uid"))
        if not paper_uid:
            paper_uid = str(_to_int(row.get("paper_id")))
        author_name = _safe_text(row.get("author_name"))
        paper_name = _safe_text(row.get("paper_name"))
        year = _to_int(row.get("year"))
        discipline_id = _to_int(row.get("discipline_id"))
        paper_source = _safe_text(row.get("paper_source"))
        org_id = _to_int(row.get("org_id"))
        org_name = _safe_text(row.get("org_name"))

        if not author_id or not paper_uid or not author_name or not paper_name:
            continue

        # 作者详情里的论文列表做去重，避免多来源重复插入同一成果。
        author_paper_seen.setdefault(author_id, set())
        paper_dedupe_key = (paper_name, year)
        if paper_dedupe_key not in author_paper_seen[author_id]:
            author_paper_seen[author_id].add(paper_dedupe_key)
            author_papers.setdefault(author_id, []).append(
                {
                    "paper_id": paper_uid,
                    "paper_node_id": f"paper_{paper_uid}",
                    "paper_name": paper_name,
                    "year": year,
                }
            )

        author_node_id = f"author_{author_id}"
        paper_node_id = f"paper_{paper_uid}"

        if author_node_id not in nodes:
            related = author_related_map.get(author_id, {})
            province_awards = related.get("province_awards", []) if isinstance(related, dict) else []
            national_awards = related.get("national_awards", []) if isinstance(related, dict) else []
            groups = related.get("groups", []) if isinstance(related, dict) else []
            nodes[author_node_id] = {
                "id": author_node_id,
                "name": author_name,
                "display_name": author_name,
                "type": "author",
                "category": "作者",
                "year": 0,
                "attributes": {
                    "author_id": author_id,
                    "org_id": _to_int(related.get("org_id")) or org_id,
                    "org_name": _safe_text(related.get("org_name")) or org_name,
                    "province_awards": province_awards,
                    "national_awards": national_awards,
                    "groups": groups,
                    "province_award_count": len(province_awards),
                    "national_award_count": len(national_awards),
                    "group_count": len(groups),
                },
            }
            vector_ids.append(author_node_id)
            vector_texts.append(f"作者:{author_name}")
            vector_types.append("author")
        else:
            # 同一作者通常仅属于一个机构；若前值为空则用当前行补齐。
            attrs = nodes[author_node_id].setdefault("attributes", {})
            if not _safe_text(attrs.get("org_name")) and org_name:
                attrs["org_name"] = org_name
            if not _to_int(attrs.get("org_id")) and org_id:
                attrs["org_id"] = org_id

        if paper_node_id not in nodes:
            nodes[paper_node_id] = {
                "id": paper_node_id,
                "name": paper_name,
                "display_name": f"{paper_name}({year})" if year > 0 else paper_name,
                "type": "paper",
                "category": "论文",
                "year": year,
                "attributes": {
                    "paper_id": paper_uid,
                    "org_id": org_id,
                    "org_name": org_name,
                    "discipline_id": discipline_id,
                    "paper_source": paper_source,
                },
            }
            vector_ids.append(paper_node_id)
            vector_texts.append(f"论文:{paper_name} 年份:{year if year > 0 else '未知'}")
            vector_types.append("paper")

        edge_key = (author_node_id, paper_node_id, "撰写")
        if edge_key not in edge_seen:
            edge_seen.add(edge_key)
            edges.append(
                {
                    "from": author_node_id,
                    "to": paper_node_id,
                    "label": "撰写",
                    "label_display": "撰写",
                    "type": "structured",
                    "line_style": "solid",
                }
            )

    max_vector_nodes = 1200
    vector_truncated = len(vector_texts) > max_vector_nodes
    if vector_truncated:
        LOG.warning("vectorize truncated: total=%s keep=%s", len(vector_texts), max_vector_nodes)
        vector_ids = vector_ids[:max_vector_nodes]
        vector_texts = vector_texts[:max_vector_nodes]
        vector_types = vector_types[:max_vector_nodes]

    _set_task_state(stage="vectorize", progress=55, message="执行 text-embedding-v3 向量化")
    vectors = _embed_texts(vector_texts, api_key=api_key)

    _set_task_state(stage="build", progress=75, message="写入 Chroma 向量库")
    vector_ready = True
    vector_error = ""
    try:
        _persist_chroma(node_ids=vector_ids, summaries=vector_texts, vectors=vectors, types=vector_types)
    except Exception as ex:
        # 向量索引失败不阻塞图谱主流程，避免前端因向量库异常完全不可用。
        vector_ready = False
        vector_error = str(ex)
        LOG.exception("persist chroma failed, continue without vector index")

    # 二次补齐作者的论文清单与数量（便于前端“人物全信息”面板直接展示）。
    for node in nodes.values():
        if str(node.get("type")) != "author":
            continue
        attrs = node.get("attributes", {})
        if not isinstance(attrs, dict):
            attrs = {}
            node["attributes"] = attrs
        aid = _to_int(attrs.get("author_id"))
        papers = author_papers.get(aid, [])
        attrs["papers"] = papers
        attrs["paper_count"] = len(papers)

    return {
        "nodes": list(nodes.values()),
        "edges": edges,
        "metadata": {
            "version": "v1-simple",
            "schema_version": _GRAPH_SCHEMA_VERSION,
            "generated_at": _now_iso(),
            "embedding_model": _EMBEDDING_MODEL,
            "rag_model": _RAG_MODEL,
            "node_count": len(nodes),
            "edge_count": len(edges),
            "scope": "author-paper-only",
            "coauthor_enabled": False,
            "vector_node_count": len(vector_ids),
            "vector_truncated": vector_truncated,
            "vector_ready": vector_ready,
            "vector_error": vector_error,
        },
    }


def _write_graph(payload: dict[str, Any]) -> None:
    """将图谱结果写入 current/graph.json（Windows 友好原子替换）。"""
    current = _current_dir()
    current.mkdir(parents=True, exist_ok=True)
    graph_path = current / "graph.json"
    tmp_path = current / "graph.json.tmp"
    with tmp_path.open("w", encoding="utf-8") as fp:
        json.dump(payload, fp, ensure_ascii=False, indent=2)
    tmp_path.replace(graph_path)


def _run_rebuild(task_id: str) -> None:
    """后台重建主流程。"""
    try:
        _set_task_state(status="running", stage="init", progress=5, message="开始重建简版图谱")
        api_key = ensure_api_key().strip()
        if not api_key:
            raise KgApiError("EMBEDDING_KEY_MISSING", "ALIYUN_API_KEY 未配置")
        repo: BaseSimpleKgRepository = SQLiteSimpleKgRepository()
        payload = _build_graph_payload(repo=repo, api_key=api_key)
        _set_task_state(stage="swap", progress=90, message="写入图谱文件")
        _write_graph(payload=payload)
        _set_task_state(status="success", stage="done", progress=100, message="重建完成", ended_at=_now_iso())
    except KgApiError as ex:
        _set_task_state(
            status="failed",
            stage="done",
            progress=100,
            message=ex.message,
            ended_at=_now_iso(),
            error_code=ex.code,
        )
        LOG.exception("kg rebuild failed: %s", ex.code)
    except Exception as ex:
        _set_task_state(
            status="failed",
            stage="done",
            progress=100,
            message=str(ex),
            ended_at=_now_iso(),
            error_code="INTERNAL_ERROR",
        )
        LOG.exception("kg rebuild failed unexpected")


def start_rebuild() -> dict[str, Any]:
    """启动重建任务。"""
    global _TASK_STATE
    with _TASK_LOCK:
        if _TASK_STATE and _TASK_STATE.status in {"pending", "running"}:
            raise KgApiError("REBUILD_ALREADY_RUNNING", "已有重建任务正在执行")
        task_id = f"kg_simple_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        _TASK_STATE = RebuildTaskState(
            status="pending",
            stage="init",
            progress=0,
            message="任务已创建",
            task_id=task_id,
            started_at=_now_iso(),
        )
    threading.Thread(target=_run_rebuild, args=(task_id,), daemon=True).start()
    return {"task_id": task_id, "status": "pending"}


def get_rebuild_status(task_id: str | None = None) -> dict[str, Any]:
    """查询重建任务状态。"""
    with _TASK_LOCK:
        if _TASK_STATE is None:
            raise KgApiError("TASK_NOT_FOUND", "未找到重建任务")
        if task_id and task_id != _TASK_STATE.task_id:
            raise KgApiError("TASK_NOT_FOUND", "task_id 不存在")
        return asdict(_TASK_STATE)


def _load_graph_data() -> dict[str, Any]:
    """读取当前图谱数据。"""
    graph_path = _current_graph_path()
    if not graph_path.exists():
        raise KgApiError("GRAPH_NOT_READY", "知识图谱尚未构建，请先重建")
    with graph_path.open("r", encoding="utf-8") as fp:
        graph = json.load(fp)
    # 如果 schema_version 不匹配，说明当前 graph.json 仍是旧口径结果，必须先重建。
    metadata = graph.get("metadata", {}) if isinstance(graph, dict) else {}
    schema_version = _safe_text(metadata.get("schema_version"))
    if schema_version != _GRAPH_SCHEMA_VERSION:
        raise KgApiError("GRAPH_STALE", "知识图谱数据版本过期，请先重建图谱")
    return graph


def _normalize_edges(edges: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """补齐 source/target 字段以兼容前端图组件。"""
    out: list[dict[str, Any]] = []
    for edge in edges:
        src = str(edge.get("from", ""))
        dst = str(edge.get("to", ""))
        item = dict(edge)
        item["source"] = src
        item["target"] = dst
        out.append(item)
    return out


def _compact_nodes_for_view(nodes: list[dict[str, Any]], full_author_ids: set[str] | None = None) -> list[dict[str, Any]]:
    """
    压缩返回给前端的节点详情体积，降低页面卡顿。

    规则：
    - 对非中心作者节点，仅保留统计字段，移除大列表字段（papers/awards/groups）。
    - 中心作者（full_author_ids）保留全量详情，满足“点击作者看完整信息”。
    """
    keep = full_author_ids or set()
    out: list[dict[str, Any]] = []
    for node in nodes:
        item = dict(node)
        if str(item.get("type")) != "author":
            out.append(item)
            continue
        attrs = item.get("attributes", {})
        if not isinstance(attrs, dict):
            out.append(item)
            continue
        if str(item.get("id")) in keep:
            out.append(item)
            continue
        compact_attrs = dict(attrs)
        compact_attrs.pop("papers", None)
        compact_attrs.pop("province_awards", None)
        compact_attrs.pop("national_awards", None)
        compact_attrs.pop("groups", None)
        item["attributes"] = compact_attrs
        out.append(item)
    return out


def _query_by_org_keyword(graph: dict[str, Any], keyword: str, max_nodes: int) -> dict[str, Any] | None:
    """
    机构关键词兜底查询（仅返回作者/论文节点，不引入机构节点）。

    适用场景：
    - 用户输入机构名，希望看到“该机构课题 + 负责人”。
    """
    nodes = graph.get("nodes", [])
    edges = graph.get("edges", [])
    node_map = {str(n.get("id")): n for n in nodes if n.get("id")}

    seed_ids: set[str] = set()
    for node in nodes:
        ntype = str(node.get("type", ""))
        if ntype not in {"author", "paper"}:
            continue
        attrs = node.get("attributes", {})
        if not isinstance(attrs, dict):
            continue
        org_name = _safe_text(attrs.get("org_name"))
        if org_name and keyword in org_name:
            seed_ids.add(str(node.get("id")))

    if not seed_ids:
        return None

    selected: set[str] = set(seed_ids)
    truncated = False
    for edge in edges:
        src = str(edge.get("from", ""))
        dst = str(edge.get("to", ""))
        if not src or not dst:
            continue
        if src in selected or dst in selected:
            if len(selected) < max_nodes:
                selected.add(src)
            else:
                truncated = True
            if len(selected) < max_nodes:
                selected.add(dst)
            else:
                truncated = True

    out_nodes = [node_map[nid] for nid in selected if nid in node_map]
    out_nodes = _compact_nodes_for_view(out_nodes, full_author_ids=set())
    out_edges = [e for e in edges if str(e.get("from")) in selected and str(e.get("to")) in selected]
    return {
        "nodes": out_nodes,
        "edges": _normalize_edges(out_edges),
        "metadata": {
            "query_type": "organization",
            "hit_mode": "organization",
            "max_nodes": max_nodes,
            "truncated": truncated,
        },
    }


def _heuristic_query_type(keyword: str) -> str:
    """无模型可用时的启发式类型判断。"""
    if any(token in keyword for token in ["研究", "课题", "论文", "路径", "机制"]):
        return "paper"
    return "author"


def _infer_query_type(keyword: str, search_type: str, api_key: str) -> str:
    """
    用 qwen-plus 识别查询意图类型（author/paper）。

    规则：
    - 用户显式传 author/paper 则直接使用。
    - auto 模式下调用 qwen-plus，失败时回退到启发式规则。
    """
    if search_type in {"author", "paper"}:
        return search_type

    # 性能优先：auto 模式使用本地启发式，不再调用外部 LLM 做意图分类。
    # 这样可避免查询时额外网络等待，显著降低“卡住”体感。
    return _heuristic_query_type(keyword)


def _infer_query_type_by_name_hits(keyword: str, nodes: list[dict[str, Any]]) -> str:
    """
    基于当前图谱名称命中情况判断查询类型（author/paper）。

    规则：
    - 先比精确命中数（name == keyword）。
    - 再比包含命中数（keyword in name）。
    - 都没有时回退启发式规则。
    """
    author_exact = 0
    paper_exact = 0
    author_fuzzy = 0
    paper_fuzzy = 0
    for node in nodes:
        ntype = _safe_text(node.get("type"))
        if ntype not in {"author", "paper"}:
            continue
        nname = _safe_text(node.get("name"))
        if not nname:
            continue
        if nname == keyword:
            if ntype == "author":
                author_exact += 1
            else:
                paper_exact += 1
            continue
        if keyword and keyword in nname:
            if ntype == "author":
                author_fuzzy += 1
            else:
                paper_fuzzy += 1
    if author_exact != paper_exact:
        return "author" if author_exact > paper_exact else "paper"
    if author_fuzzy != paper_fuzzy:
        return "author" if author_fuzzy > paper_fuzzy else "paper"
    return _heuristic_query_type(keyword)


def _search_in_chroma(keyword: str, entity_type: str, api_key: str, top_k: int = 6) -> list[str]:
    """在 Chroma 中按实体类型做相似召回，返回节点 id 列表。"""
    try:
        import chromadb
    except Exception:
        return []
    if not _safe_text(api_key):
        return []
    chroma_dir = _current_dir() / "chroma"
    if not chroma_dir.exists():
        return []
    try:
        client = chromadb.PersistentClient(path=str(chroma_dir))
        collection = client.get_or_create_collection(name="kg_nodes_v1")
    except Exception:
        return []
    try:
        query_vector = _embed_texts([keyword], api_key=api_key)[0]
    except Exception as ex:
        LOG.warning("query embedding failed, skip vector search: %s", ex)
        return []
    try:
        result = collection.query(
            query_embeddings=[query_vector],
            n_results=top_k,
            where={"entity_type": entity_type},
        )
    except Exception:
        return []
    ids = result.get("ids", [])
    if not ids or not isinstance(ids, list):
        return []
    first = ids[0] if ids else []
    return [str(x) for x in first] if isinstance(first, list) else []


def _search_similar_node_ids_by_id(node_id: str, entity_type: str, top_k: int = 6) -> list[str]:
    """
    基于节点自身向量检索同类型相似节点。

    说明：
    - 相似关系默认隐藏，仅在 include_similar=True 时按需计算。
    - 仅返回同类型（author->author / paper->paper）节点 id。
    """
    try:
        import chromadb
    except Exception:
        return []
    chroma_dir = _current_dir() / "chroma"
    if not chroma_dir.exists():
        return []
    try:
        client = chromadb.PersistentClient(path=str(chroma_dir))
        collection = client.get_or_create_collection(name="kg_nodes_v1")
    except Exception:
        return []
    try:
        got = collection.get(ids=[node_id], include=["embeddings"])
        embeddings = got.get("embeddings") or []
        if not embeddings:
            return []
        query_vector = embeddings[0]
        if not query_vector:
            return []
        result = collection.query(
            query_embeddings=[query_vector],
            n_results=max(2, top_k + 1),
            where={"entity_type": entity_type},
        )
    except Exception:
        return []
    ids = result.get("ids", [])
    if not ids or not isinstance(ids, list):
        return []
    first = ids[0] if ids else []
    if not isinstance(first, list):
        return []
    out: list[str] = []
    for cid in first:
        sid = str(cid)
        if not sid or sid == node_id:
            continue
        if sid not in out:
            out.append(sid)
        if len(out) >= top_k:
            break
    return out


def _format_score(score: float) -> str:
    """统一格式化相似分数。"""
    return f"{score:.3f}"


def _paper_similarity_detail(seed_node: dict[str, Any], candidate_node: dict[str, Any]) -> tuple[float, str, list[dict[str, Any]]]:
    """
    计算论文-论文相似分，并返回可解释依据。

    分数构成（按要求仅使用两类依据）：
    - 论文题目相似（主）
    - 类别/学科一致（辅）
    """
    seed_name = _safe_text(seed_node.get("name"))
    cand_name = _safe_text(candidate_node.get("name"))
    title_sim = _char_similarity(seed_name, cand_name)

    seed_attrs = seed_node.get("attributes", {}) if isinstance(seed_node.get("attributes"), dict) else {}
    cand_attrs = candidate_node.get("attributes", {}) if isinstance(candidate_node.get("attributes"), dict) else {}
    seed_dis = _to_int(seed_attrs.get("discipline_id"))
    cand_dis = _to_int(cand_attrs.get("discipline_id"))
    discipline_match = 1.0 if seed_dis > 0 and seed_dis == cand_dis else 0.0

    seed_source = _safe_text(seed_attrs.get("paper_source"))
    cand_source = _safe_text(cand_attrs.get("paper_source"))
    source_match = 1.0 if seed_source and cand_source and seed_source == cand_source else 0.0

    # 仅保留“题目 + 类别/学科”两类信息：其中“类别/学科”由来源类别与学科共同构成。
    category_discipline_match = 0.5 * discipline_match + 0.5 * source_match
    score = 0.8 * title_sim + 0.2 * category_discipline_match
    score = round(max(0.0, min(score, 0.999)), 3)

    basis_items = [
        {"factor": "论文题目相似", "value": round(title_sim, 3), "weight": 0.8},
        {"factor": "类别/学科一致", "value": round(category_discipline_match, 3), "weight": 0.2},
        {"factor": "学科一致", "value": discipline_match},
        {"factor": "类别一致", "value": source_match},
    ]
    basis_text = (
        f"依据：题目相似 {_format_score(title_sim)}；"
        f"类别/学科一致度 {_format_score(category_discipline_match)}"
        f"（学科{'一致' if discipline_match > 0 else '不一致/未知'}，"
        f"类别{'一致' if source_match > 0 else '不一致/未知'}）。"
    )
    return score, basis_text, basis_items


def _top_similar_papers(
    node_map: dict[str, dict[str, Any]],
    seed_paper_id: str,
    top_k: int = 6,
    min_score: float = 0.7,
) -> list[tuple[str, float, str, list[dict[str, Any]]]]:
    """返回与 seed 论文最相近的论文列表（含依据）。"""
    seed_node = node_map.get(seed_paper_id)
    if not seed_node or _safe_text(seed_node.get("type")) != "paper":
        return []

    ranked: list[tuple[str, float, str, list[dict[str, Any]]]] = []
    for nid, node in node_map.items():
        if nid == seed_paper_id:
            continue
        if _safe_text(node.get("type")) != "paper":
            continue
        score, basis_text, basis_items = _paper_similarity_detail(seed_node, node)
        if score <= min_score:
            continue
        ranked.append((nid, score, basis_text, basis_items))
    ranked.sort(key=lambda x: x[1], reverse=True)
    return ranked[:top_k]


def _build_author_paper_map(edges: list[dict[str, Any]]) -> tuple[dict[str, set[str]], dict[str, set[str]]]:
    """从结构化边构建 author->papers 与 paper->authors 映射。"""
    author_to_papers: dict[str, set[str]] = {}
    paper_to_authors: dict[str, set[str]] = {}
    for edge in edges:
        src = _safe_text(edge.get("from"))
        dst = _safe_text(edge.get("to"))
        if not src or not dst:
            continue
        if src.startswith("author_") and dst.startswith("paper_"):
            author_to_papers.setdefault(src, set()).add(dst)
            paper_to_authors.setdefault(dst, set()).add(src)
        elif src.startswith("paper_") and dst.startswith("author_"):
            author_to_papers.setdefault(dst, set()).add(src)
            paper_to_authors.setdefault(src, set()).add(dst)
    return author_to_papers, paper_to_authors


def _top_similar_authors_by_papers(
    node_map: dict[str, dict[str, Any]],
    author_to_papers: dict[str, set[str]],
    paper_to_authors: dict[str, set[str]],
    seed_author_id: str,
    top_k: int = 4,
) -> list[tuple[str, float, str, list[dict[str, Any]]]]:
    """
    基于“论文相似”推导作者相似：
    - 先找 seed 作者论文的相似论文
    - 再回溯到这些论文对应作者
    """
    seed_papers = sorted(author_to_papers.get(seed_author_id, set()))
    if not seed_papers:
        return []

    ranked: dict[str, dict[str, Any]] = {}
    for seed_paper_id in seed_papers:
        paper_hits = _top_similar_papers(node_map=node_map, seed_paper_id=seed_paper_id, top_k=8, min_score=0.7)
        for similar_paper_id, score, paper_basis_text, paper_basis_items in paper_hits:
            candidate_authors = paper_to_authors.get(similar_paper_id, set())
            for cand_author_id in candidate_authors:
                if cand_author_id == seed_author_id:
                    continue
                seed_paper = node_map.get(seed_paper_id, {})
                sim_paper = node_map.get(similar_paper_id, {})
                seed_paper_name = _safe_text(seed_paper.get("name"))
                sim_paper_name = _safe_text(sim_paper.get("name"))
                basis_text = (
                    f"依据：作者论文《{seed_paper_name}》与《{sim_paper_name}》相似，"
                    f"论文相似度 {_format_score(score)}。{paper_basis_text}"
                )
                previous = ranked.get(cand_author_id)
                if previous is None or score > float(previous.get("score", 0.0)):
                    ranked[cand_author_id] = {
                        "score": score,
                        "basis_text": basis_text,
                        "basis_items": paper_basis_items + [
                            {
                                "factor": "作者相似由论文推导",
                                "value": round(score, 3),
                                "seed_paper_id": seed_paper_id,
                                "seed_paper_name": seed_paper_name,
                                "matched_paper_id": similar_paper_id,
                                "matched_paper_name": sim_paper_name,
                            }
                        ],
                    }

    out = [
        (aid, float(info["score"]), str(info["basis_text"]), list(info["basis_items"]))
        for aid, info in ranked.items()
    ]
    out.sort(key=lambda x: x[1], reverse=True)
    return out[:top_k]


def _append_similar_neighbors(
    graph: dict[str, Any],
    selected: set[str],
    seed_ids: list[str],
    max_nodes: int,
    top_k_per_seed: int = 4,
) -> tuple[set[str], list[dict[str, Any]], bool, list[str]]:
    """
    在已选子图上追加相似实体节点与相似边（同类型）。
    """
    nodes = graph.get("nodes", [])
    edges = graph.get("edges", [])
    node_map = {str(n.get("id")): n for n in nodes if n.get("id")}
    author_to_papers, paper_to_authors = _build_author_paper_map(edges)
    sim_edges: list[dict[str, Any]] = []
    added_similar_ids: list[str] = []
    truncated = False

    for seed_id in seed_ids:
        seed_node = node_map.get(seed_id)
        if not seed_node:
            continue
        seed_type = _safe_text(seed_node.get("type"))
        if seed_type not in {"author", "paper"}:
            continue
        candidate_edges: list[tuple[str, float, str, list[dict[str, Any]]]] = []
        if seed_type == "paper":
            candidate_edges = _top_similar_papers(
                node_map=node_map,
                seed_paper_id=seed_id,
                top_k=top_k_per_seed,
                min_score=0.7,
            )
        elif seed_type == "author":
            candidate_edges = _top_similar_authors_by_papers(
                node_map=node_map,
                author_to_papers=author_to_papers,
                paper_to_authors=paper_to_authors,
                seed_author_id=seed_id,
                top_k=top_k_per_seed,
            )
        for sid, score, basis_text, basis_items in candidate_edges:
            if sid not in node_map:
                continue
            if sid not in selected:
                if len(selected) >= max_nodes:
                    truncated = True
                    continue
                selected.add(sid)
                added_similar_ids.append(sid)
            sim_edges.append(
                {
                    "from": seed_id,
                    "to": sid,
                    "label": "相似",
                    "label_display": f"相似 {_format_score(score)}",
                    "type": "similar",
                    "line_style": "dashed",
                    "similarity_score": score,
                    "similarity_basis_text": basis_text,
                    "similarity_basis_items": basis_items,
                    "similarity_kind": "author_by_papers" if seed_type == "author" else "paper_to_paper",
                    "source": seed_id,
                    "target": sid,
                }
            )
    return selected, sim_edges, truncated, added_similar_ids


def _char_similarity(a: str, b: str) -> float:
    """轻量字符相似度（交并比），用于无向量时兜底。"""
    sa = {ch for ch in _safe_text(a) if ch.strip()}
    sb = {ch for ch in _safe_text(b) if ch.strip()}
    if not sa or not sb:
        return 0.0
    inter = len(sa & sb)
    union = len(sa | sb)
    return float(inter) / float(union) if union else 0.0


def _fallback_similar_ids(
    node_map: dict[str, dict[str, Any]],
    seed_id: str,
    seed_type: str,
    top_k: int = 4,
) -> list[str]:
    """
    无向量可用时的相似兜底：
    - author：同机构优先 + 名称字符相似。
    - paper：标题字符相似 + 年份接近轻微加分。
    """
    seed = node_map.get(seed_id)
    if not seed:
        return []
    seed_name = _safe_text(seed.get("name"))
    seed_attrs = seed.get("attributes", {}) if isinstance(seed.get("attributes"), dict) else {}
    seed_org = _safe_text(seed_attrs.get("org_name"))
    seed_year = _to_int(seed.get("year"))

    scored: list[tuple[float, str]] = []
    for nid, node in node_map.items():
        if nid == seed_id:
            continue
        if _safe_text(node.get("type")) != seed_type:
            continue
        name = _safe_text(node.get("name"))
        if not name:
            continue
        score = _char_similarity(seed_name, name)

        if seed_type == "author":
            attrs = node.get("attributes", {}) if isinstance(node.get("attributes"), dict) else {}
            org_name = _safe_text(attrs.get("org_name"))
            if seed_org and org_name and seed_org == org_name:
                score += 0.35
        elif seed_type == "paper":
            year = _to_int(node.get("year"))
            if seed_year > 0 and year > 0 and abs(seed_year - year) <= 1:
                score += 0.08

        if score > 0.12:
            scored.append((score, nid))

    scored.sort(key=lambda x: x[0], reverse=True)
    return [nid for _, nid in scored[:top_k]]


def _expand_from_center(graph: dict[str, Any], center_id: str, depth: int, max_nodes: int) -> dict[str, Any]:
    """按中心节点做无向 BFS 子图扩展。"""
    nodes = graph.get("nodes", [])
    edges = graph.get("edges", [])
    node_map = {str(n.get("id")): n for n in nodes if n.get("id")}
    if center_id not in node_map:
        raise KgApiError("CENTER_NOT_FOUND", "center_id 不存在")

    if depth not in (1, 2):
        raise KgApiError("INVALID_PARAM", "depth 仅支持 1 或 2")
    if max_nodes <= 0 or max_nodes > 300:
        raise KgApiError("INVALID_PARAM", "max_nodes 必须在 1-300 之间")

    adjacency: dict[str, set[str]] = {}
    for edge in edges:
        src = str(edge.get("from", ""))
        dst = str(edge.get("to", ""))
        if not src or not dst:
            continue
        adjacency.setdefault(src, set()).add(dst)
        adjacency.setdefault(dst, set()).add(src)

    selected: set[str] = {center_id}
    frontier: set[str] = {center_id}
    truncated = False
    for _ in range(depth):
        next_frontier: set[str] = set()
        for nid in frontier:
            for nb in adjacency.get(nid, set()):
                if nb in selected:
                    continue
                if len(selected) >= max_nodes:
                    truncated = True
                    continue
                selected.add(nb)
                next_frontier.add(nb)
        frontier = next_frontier
        if not frontier:
            break

    out_nodes = [node_map[nid] for nid in selected if nid in node_map]
    full_author_ids = {center_id} if center_id.startswith("author_") else set()
    out_nodes = _compact_nodes_for_view(out_nodes, full_author_ids=full_author_ids)
    out_edges = [e for e in edges if str(e.get("from")) in selected and str(e.get("to")) in selected]
    return {
        "nodes": out_nodes,
        "edges": _normalize_edges(out_edges),
        "metadata": {
            "center_id": center_id,
            "depth": depth,
            "max_nodes": max_nodes,
            "truncated": truncated,
        },
    }


def _expand_from_centers(
    graph: dict[str, Any],
    center_ids: list[str],
    depth: int,
    max_nodes: int,
    include_similar: bool = False,
) -> dict[str, Any]:
    """
    按多个中心节点做无向 BFS 子图扩展。

    用途：
    - 关键词可能命中多个同名作者/同名论文，V1 需要尽量完整返回，不只取第一个命中。
    """
    nodes = graph.get("nodes", [])
    edges = graph.get("edges", [])
    node_map = {str(n.get("id")): n for n in nodes if n.get("id")}

    if depth not in (1, 2):
        raise KgApiError("INVALID_PARAM", "depth 仅支持 1 或 2")
    if max_nodes <= 0 or max_nodes > 300:
        raise KgApiError("INVALID_PARAM", "max_nodes 必须在 1-300 之间")

    seeds: list[str] = []
    for cid in center_ids:
        cid = _safe_text(cid)
        if cid and cid in node_map and cid not in seeds:
            seeds.append(cid)
    if not seeds:
        raise KgApiError("CENTER_NOT_FOUND", "center_id 不存在")

    adjacency: dict[str, set[str]] = {}
    for edge in edges:
        src = str(edge.get("from", ""))
        dst = str(edge.get("to", ""))
        if not src or not dst:
            continue
        adjacency.setdefault(src, set()).add(dst)
        adjacency.setdefault(dst, set()).add(src)

    selected: set[str] = set()
    truncated = False
    for cid in seeds:
        if len(selected) >= max_nodes:
            truncated = True
            break
        selected.add(cid)
    frontier: set[str] = set(selected)

    for _ in range(depth):
        next_frontier: set[str] = set()
        for nid in frontier:
            for nb in adjacency.get(nid, set()):
                if nb in selected:
                    continue
                if len(selected) >= max_nodes:
                    truncated = True
                    continue
                selected.add(nb)
                next_frontier.add(nb)
        frontier = next_frontier
        if not frontier:
            break

    sim_edges: list[dict[str, Any]] = []
    added_similar_ids: list[str] = []
    if include_similar:
        selected, sim_edges, sim_truncated, added_similar_ids = _append_similar_neighbors(
            graph=graph,
            selected=selected,
            seed_ids=seeds,
            max_nodes=max_nodes,
            top_k_per_seed=4,
        )
        if sim_truncated:
            truncated = True

    out_nodes = [node_map[nid] for nid in selected if nid in node_map]
    full_author_ids = {sid for sid in seeds if sid.startswith("author_")}
    out_nodes = _compact_nodes_for_view(out_nodes, full_author_ids=full_author_ids)
    out_edges = [e for e in edges if str(e.get("from")) in selected and str(e.get("to")) in selected] + sim_edges
    return {
        "nodes": out_nodes,
        "edges": _normalize_edges(out_edges),
        "metadata": {
            "center_ids": seeds,
            "depth": depth,
            "max_nodes": max_nodes,
            "truncated": truncated,
            "include_similar": include_similar,
            "similar_added_count": len(added_similar_ids),
            "similar_ids": added_similar_ids[:20],
        },
    }


def get_subgraph(center_id: str, depth: int, max_nodes: int, include_similar: bool = False) -> dict[str, Any]:
    """对外子图接口（支持按需追加同类型相似实体）。"""
    graph = _load_graph_data()
    result = _expand_from_centers(
        graph=graph,
        center_ids=[_safe_text(center_id)],
        depth=depth,
        max_nodes=max_nodes,
        include_similar=include_similar,
    )
    result["metadata"]["center_id"] = _safe_text(center_id)
    return result


def query_graph(keyword: str, search_type: str = "auto", max_nodes: int = 120, include_similar: bool = False) -> dict[str, Any]:
    """
    关键词查询入口：返回 nodes + edges。

    参数：
    - keyword: 检索词。
    - search_type: auto/author/paper。
    - max_nodes: 返回节点上限。
    """
    keyword = _safe_text(keyword)
    search_type = _safe_text(search_type).lower() or "auto"
    if search_type not in {"auto", "author", "paper"}:
        raise KgApiError("INVALID_PARAM", "search_type 仅支持 auto/author/paper")
    if max_nodes <= 0 or max_nodes > 300:
        raise KgApiError("INVALID_PARAM", "max_nodes 必须在 1-300 之间")

    graph = _load_graph_data()
    nodes = graph.get("nodes", [])
    node_map = {str(n.get("id")): n for n in nodes if n.get("id")}

    # auto 场景下优先尝试机构名兜底（仍返回作者-论文子图）。
    if keyword and search_type == "auto":
        org_result = _query_by_org_keyword(graph=graph, keyword=keyword, max_nodes=max_nodes)
        if org_result is not None:
            org_result["metadata"]["query_keyword"] = keyword
            return org_result

    if not keyword:
        # 空关键词时返回一个默认子图：取首个作者作为中心，方便前端初始渲染。
        default_author = next((n for n in nodes if str(n.get("type")) == "author"), None)
        if not default_author:
            return {"nodes": [], "edges": [], "metadata": {"reason": "empty_graph"}}
        base = _expand_from_center(graph=graph, center_id=str(default_author["id"]), depth=1, max_nodes=max_nodes)
        base["metadata"]["query_type"] = "author"
        base["metadata"]["query_keyword"] = ""
        base["metadata"]["hit_mode"] = "default"
        base["metadata"]["include_similar"] = False
        return base

    api_key = ""
    if search_type == "auto":
        try:
            api_key = ensure_api_key().strip()
        except Exception as ex:
            LOG.warning("ALIYUN_API_KEY missing, fallback to heuristic query type: %s", ex)
    if search_type == "auto":
        # 先用“图内名称命中”做类型判断，避免每次都走外部模型且准确率更贴近当前数据。
        query_type = _infer_query_type_by_name_hits(keyword=keyword, nodes=nodes)
    else:
        query_type = _infer_query_type(keyword=keyword, search_type=search_type, api_key=api_key)
    target_type = "author" if query_type == "author" else "paper"

    # 第一优先：名称 contains 匹配。
    exact_ids: list[str] = []
    fuzzy_ids: list[str] = []
    for node in nodes:
        nid = str(node.get("id", ""))
        ntype = str(node.get("type", ""))
        nname = _safe_text(node.get("name"))
        if not nid or ntype != target_type:
            continue
        if nname == keyword:
            exact_ids.append(nid)
        elif keyword in nname:
            fuzzy_ids.append(nid)

    candidate_ids = exact_ids + fuzzy_ids
    hit_mode = "keyword"

    # 第二优先：关键词没命中时，走 Chroma 召回。
    if not candidate_ids:
        candidate_ids = _search_in_chroma(keyword=keyword, entity_type=target_type, api_key=api_key, top_k=6)
        hit_mode = "vector" if candidate_ids else "none"

    if not candidate_ids:
        return {
            "nodes": [],
            "edges": [],
            "metadata": {
                "query_keyword": keyword,
                "query_type": query_type,
                "hit_mode": "none",
            },
        }

    # 关键词命中多个同名实体时合并返回，避免“只显示一个作者/一篇论文”导致信息不全。
    merged_centers = candidate_ids[:20]
    result = _expand_from_centers(
        graph=graph,
        center_ids=merged_centers,
        depth=1,
        max_nodes=max_nodes,
        include_similar=include_similar,
    )
    result["metadata"]["query_keyword"] = keyword
    result["metadata"]["query_type"] = query_type
    result["metadata"]["hit_mode"] = hit_mode
    result["metadata"]["center_id"] = merged_centers[0]
    result["metadata"]["center_ids"] = merged_centers
    result["metadata"]["candidate_ids"] = candidate_ids[:6]
    result["metadata"]["include_similar"] = include_similar
    return result


def _full_graph_with_limit(max_nodes: int) -> dict[str, Any]:
    """兼容旧接口：按节点上限返回全图子集。"""
    graph = _load_graph_data()
    nodes = graph.get("nodes", [])
    edges = graph.get("edges", [])
    if max_nodes <= 0 or max_nodes > 300:
        raise KgApiError("INVALID_PARAM", "max_nodes 必须在 1-300 之间")
    keep_ids = {str(n.get("id")) for n in nodes[:max_nodes]}
    out_nodes = [n for n in nodes if str(n.get("id")) in keep_ids]
    out_edges = [e for e in edges if str(e.get("from")) in keep_ids and str(e.get("to")) in keep_ids]
    return {
        "nodes": out_nodes,
        "edges": _normalize_edges(out_edges),
        "metadata": {
            "deprecated_view": True,
            "scope": "author-paper-only",
            "max_nodes": max_nodes,
        },
    }


def get_scene_graph(scene: str, max_nodes: int, include_similar: bool = False, include_award_links: bool = False) -> dict[str, Any]:
    """兼容旧接口：简版直接返回全图。"""
    return _full_graph_with_limit(max_nodes=max_nodes)


def get_all_scene_graphs(max_nodes: int, include_similar: bool = False, include_award_links: bool = False) -> dict[str, Any]:
    """兼容旧接口：三场景均返回同一份简版图。"""
    graph = _full_graph_with_limit(max_nodes=max_nodes)
    return {
        "metadata": {"deprecated_view": True, "scope": "author-paper-only"},
        "scenes": {"government": graph, "industry": graph, "research": graph},
    }


def get_hierarchy_graph(
    group_by: str = "discipline",
    max_orgs: int = 12,
    max_nodes: int = 120,
    org_id: str = "",
    expand_depth: int = 2,
    include_similar: bool = False,
) -> dict[str, Any]:
    """兼容旧接口：简版直接返回全图。"""
    return _full_graph_with_limit(max_nodes=max_nodes)

