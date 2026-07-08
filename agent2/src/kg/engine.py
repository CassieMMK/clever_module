# -*- coding: utf-8 -*-
"""
知识图谱构建与查询服务。

V1 范围：
- 启用实体：机构、人员、省课题、省社科奖、国社科奖
- 启用关系：承担、负责、任职、获奖_省奖_个人、获奖_省奖_单位、获奖_国奖_个人、获奖_国奖_单位
- 相似边：同类型、阈值 0.7、Top5、默认查询不返回
"""
from __future__ import annotations  # 延迟类型注解，便于前后引用

import json  # 图数据持久化为 JSON
import logging  # 输出重建过程日志
import math  # 计算向量余弦相似度
import os  # 原子替换目录、读取系统路径
import gc  # 主动触发垃圾回收，尽快释放文件句柄（Windows 下目录替换更敏感）
import random  # 分数并列时随机裁剪
import shutil  # 删除/移动目录
import threading  # 后台线程执行重建任务
import time  # 重试退避与任务时间戳
import unicodedata  # 全角转半角清洗
from dataclasses import dataclass, asdict  # 轻量结构体与序列化
from datetime import datetime  # 任务开始/结束时间
from pathlib import Path  # 类型安全路径处理
from typing import Any  # 通用字典值类型

import numpy as np  # 用向量化矩阵运算加速相似度计算，避免 Python 双重循环过慢
from openai import OpenAI  # 调用兼容 OpenAI 的阿里云 Embedding 接口

from src.config import get_settings, ensure_api_key  # 读取配置 + API Key 校验
from src.kg.repository import BaseKgRepository, SQLiteKgRepository  # Repository 抽象与 SQLite 实现

LOG = logging.getLogger("kg_engine")  # 模块级 logger，便于筛选日志

# V1 期望首选模型（需求文档指定）；若账号无权限则自动回退到兼容模型。
_EMBEDDING_PRIMARY_MODEL = "Qwen/Qwen3-Embedding-8B"
# 回退模型按优先级排列：先尝试质量较高/较新版本，再尝试旧版兜底。
_EMBEDDING_FALLBACK_MODELS = ["text-embedding-v3", "text-embedding-v2", "text-embedding-v1"]
# 各模型单次请求最大 batch（根据实测/平台约束维护）；未列出的模型默认按 32 处理。
_EMBEDDING_MODEL_BATCH_LIMIT = {
    "Qwen/Qwen3-Embedding-8B": 32,
    "text-embedding-v3": 10,
    "text-embedding-v2": 25,
    "text-embedding-v1": 25,
}


class KgApiError(Exception):
    """KG 业务错误：包含前端约定的 error_code。"""

    def __init__(self, code: str, message: str):
        super().__init__(message)  # 让基类持有 message
        self.code = code  # 机器可读错误码
        self.message = message  # 人类可读描述


@dataclass
class RebuildTaskState:
    """重建任务状态对象：供状态接口直接返回。"""

    status: str  # pending | running | success | failed
    stage: str  # init/load/vectorize/build/swap/done
    progress: int  # 0-100 进度值
    message: str  # 当前阶段提示
    task_id: str  # 任务唯一 ID
    started_at: str  # ISO 格式开始时间
    ended_at: str = ""  # ISO 格式结束时间
    error_code: str = ""  # 失败时错误码


_TASK_LOCK = threading.Lock()  # 保护全局任务状态，防并发写
_TASK_STATE: RebuildTaskState | None = None  # 进程内唯一任务状态

# 前端友好关系名映射：技术 label -> 展示 label
_EDGE_LABEL_DISPLAY = {
    "承担": "承担课题",
    "负责": "课题负责人",
    "任职": "所属单位",
    "成员": "团队成员",
    "依托": "依托单位",
    "获奖_省奖_个人": "获省社科奖",
    "获奖_省奖_单位": "获省社科奖",
    "获奖_国奖_个人": "获国社科奖",
    "获奖_国奖_单位": "获国社科奖",
    "关联奖项": "课题相关奖项",
}


def _kg_store_dir() -> Path:
    """返回知识图谱存储根目录（与 SQLite data 同层）。"""
    return get_settings().agent_sqlite_path.parent / "kg_store"


def _current_graph_path() -> Path:
    """返回当前生效图 JSON 文件路径。"""
    return _kg_store_dir() / "current" / "graph.json"


def _safe_str(value: Any) -> str:
    """将任意值转为清洗后的字符串：去空格 + 全角转半角。"""
    if value is None:  # 缺失值直接返回空串
        return ""
    text = str(value).strip()  # 去除两端空白
    if not text:  # 空串直接返回
        return ""
    normalized = unicodedata.normalize("NFKC", text)  # 全角字符归一到半角形态
    return "".join(normalized.split())  # 去掉中间空白，满足“去空格”要求


def _safe_year(value: Any) -> int:
    """年份清洗：缺失时返回 0（前端显示“未知”）。"""
    if value in (None, ""):  # 缺失值直接归零
        return 0
    try:
        return int(value)  # 正常数字字符串转整数
    except Exception:
        return 0  # 非法值兜底为 0，避免构图中断


def _now_iso() -> str:
    """生成统一的 ISO 时间字符串。"""
    return datetime.now().isoformat(timespec="seconds")


def _set_task_state(**kwargs: Any) -> None:
    """线程安全更新任务状态字段。"""
    global _TASK_STATE  # 修改模块全局变量
    with _TASK_LOCK:  # 加锁避免并发覆盖
        if _TASK_STATE is None:  # 理论上不会发生，兜底保护
            return
        for key, value in kwargs.items():  # 遍历传入字段并逐个覆盖
            setattr(_TASK_STATE, key, value)


def _build_topic_summary(topic: dict[str, Any]) -> str:
    """按约定格式构造课题摘要（缺失字段跳过）。"""
    parts: list[str] = []  # 按顺序累积片段，最后以空格拼接
    if topic.get("name"):  # 名称存在才写入
        parts.append(f"名称:{topic['name']}")
    if topic.get("header_name"):  # 负责人字段可为空
        parts.append(f"负责人:{topic['header_name']}")
    if topic.get("org_name"):  # 机构字段可为空
        parts.append(f"单位:{topic['org_name']}")
    if topic.get("discipline_name"):  # 学科字段可为空
        parts.append(f"学科:{topic['discipline_name']}")
    if topic.get("project_level"):  # 课题等级字段可为空（来自省课题类别）
        parts.append(f"等级:{topic['project_level']}")
    year = _safe_year(topic.get("year"))  # 统一年份清洗
    if year:  # year=0 视为缺失，不占位
        parts.append(f"年份:{year}")
    if topic.get("name"):  # 关键词简化为课题名，满足固定模板中的关键词项
        parts.append(f"关键词:{topic['name']}")
    return " ".join(parts)  # 返回单行摘要文本


def _build_org_summary(org: dict[str, Any]) -> str:
    """机构摘要：名称 + 地址。"""
    parts: list[str] = []  # 片段列表
    if org.get("unit_name"):  # 名称缺失时不占位
        parts.append(f"名称:{org['unit_name']}")
    if org.get("unit_address"):  # 地址缺失时不占位
        parts.append(f"地址:{org['unit_address']}")
    return " ".join(parts)


def _build_person_summary(person: dict[str, Any]) -> str:
    """人员摘要：姓名 + 单位。"""
    parts: list[str] = []  # 片段列表
    if person.get("name"):
        parts.append(f"姓名:{person['name']}")
    if person.get("org_name"):
        parts.append(f"单位:{person['org_name']}")
    return " ".join(parts)


def _build_award_summary(award: dict[str, Any]) -> str:
    """奖项摘要：名称 + 获奖人 + 单位 + 学科 + 年份。"""
    parts: list[str] = []  # 片段列表
    if award.get("obj_name"):
        parts.append(f"名称:{award['obj_name']}")
    if award.get("person_name"):
        parts.append(f"获奖人:{award['person_name']}")
    if award.get("org_name"):
        parts.append(f"单位:{award['org_name']}")
    if award.get("discipline_name"):
        parts.append(f"学科:{award['discipline_name']}")
    if award.get("level_name"):  # 奖项等级可为空，缺失时跳过
        parts.append(f"等级:{award['level_name']}")
    year = _safe_year(award.get("year"))
    if year:
        parts.append(f"年份:{year}")
    return " ".join(parts)


def _cosine(a: list[float], b: list[float]) -> float:
    """计算两个向量的余弦相似度。"""
    if not a or not b or len(a) != len(b):  # 维度不一致直接返回 0
        return 0.0
    dot = 0.0  # 点积累加器
    norm_a = 0.0  # 向量 a 模长平方和
    norm_b = 0.0  # 向量 b 模长平方和
    for ai, bi in zip(a, b):  # 按维度逐项累加
        dot += ai * bi
        norm_a += ai * ai
        norm_b += bi * bi
    if norm_a <= 0.0 or norm_b <= 0.0:  # 任一零向量则相似度定义为 0
        return 0.0
    return dot / (math.sqrt(norm_a) * math.sqrt(norm_b))  # 余弦公式


def _display_name(name: str, year: int, suffix: str = "") -> str:
    """构造前端显示名：同名实体可用年份或单位后缀区分。"""
    if year > 0:  # 有年份优先显示年份后缀
        return f"{name}({year})"
    if suffix:  # 无年份但有机构等信息，用额外后缀区分
        return f"{name}({suffix})"
    return name  # 不再追加“未知”后缀，避免前端出现“机构名(未知)”干扰阅读


def _is_embedding_model_unavailable_error(ex: Exception) -> bool:
    """判断异常是否属于“模型不存在或无权限”，用于自动降级模型。"""
    err = str(ex).lower()  # 统一转小写，简化关键字匹配
    return (
        "model_not_found" in err
        or "does not exist or you do not have access" in err
        or "model does not exist" in err
    )


def _embed_texts(texts: list[str], api_key: str) -> tuple[list[str], list[list[float]], str]:
    """
    调用 Embedding 接口并返回成功的文本与向量。

    约束：
    - 首选模型 Qwen/Qwen3-Embedding-8B；若账号不可用自动降级到 fallback 列表
    - timeout 30s
    - 批大小 32
    - 失败重试 2 次（1s、2s）
    - 单批最终失败则跳过，不中断全量重建
    """
    settings = get_settings()  # 读取 base_url 配置
    client = OpenAI(  # 初始化兼容 OpenAI 的客户端
        api_key=api_key,
        base_url=settings.dashscope_base_url,
        timeout=30.0,
    )
    ok_texts: list[str] = []  # 成功入库的文本集合
    ok_vectors: list[list[float]] = []  # 与 ok_texts 同序的向量集合
    candidate_models = [_EMBEDDING_PRIMARY_MODEL] + [m for m in _EMBEDDING_FALLBACK_MODELS if m != _EMBEDDING_PRIMARY_MODEL]
    current_model_idx = 0  # 当前尝试模型下标（全任务共享，避免每批从头踩坑）
    active_model = candidate_models[current_model_idx]  # 当前可用模型名
    pointer = 0  # 当前处理到的文本下标
    while pointer < len(texts):  # 用指针循环，支持不同模型动态 batch 大小
        batch_success = False  # 标记当前批是否成功
        while current_model_idx < len(candidate_models):
            active_model = candidate_models[current_model_idx]  # 获取当前模型名
            model_batch_limit = _EMBEDDING_MODEL_BATCH_LIMIT.get(active_model, 32)  # 当前模型最大批大小
            batch_size = min(32, int(model_batch_limit))  # 在文档 32 基础上应用模型上限
            batch = texts[pointer : pointer + batch_size]  # 取当前模型可接受大小的批次
            switch_model = False  # 当前批是否需要切换模型重试
            for retry_idx, sleep_sec in enumerate((0, 1, 2)):  # 首次 + 两次重试
                if sleep_sec > 0:  # 重试前执行退避等待
                    time.sleep(float(sleep_sec))
                try:
                    resp = client.embeddings.create(  # 发起 Embedding 请求
                        model=active_model,
                        input=batch,
                    )
                    batch_vectors = [list(d.embedding) for d in resp.data]  # 提取向量数据
                    if len(batch_vectors) != len(batch):  # 接口返回长度异常则视为失败
                        raise RuntimeError("embedding response size mismatch")
                    ok_texts.extend(batch)  # 批量写入成功文本
                    ok_vectors.extend(batch_vectors)  # 批量写入成功向量
                    batch_success = True  # 标记成功，结束重试循环
                    pointer += len(batch)  # 成功后推进指针，继续处理下一批
                    break
                except Exception as ex:  # 捕获网络错误、鉴权错误、超时错误等
                    if _is_embedding_model_unavailable_error(ex):  # 模型不可用时立即尝试切换
                        LOG.warning(
                            "embedding model unavailable, switch model=%s error=%s",
                            active_model,
                            ex,
                        )
                        switch_model = True
                        break
                    LOG.warning(  # 打印 warning，便于排查失败批次
                        "embedding batch failed model=%s start=%s retry=%s error=%s",
                        active_model,
                        pointer,
                        retry_idx,
                        ex,
                    )
            if switch_model:  # 当前模型不可用，切换到下一候选模型
                current_model_idx += 1
                if current_model_idx < len(candidate_models):
                    LOG.warning("embedding model fallback to %s", candidate_models[current_model_idx])
                continue
            if batch_success:  # 当前批成功则退出模型循环，继续下一个批次
                break
            # 当前模型重试后仍失败但并非“模型不可用”时，按失败策略跳过该批继续后续批次
            LOG.error("embedding batch skipped model=%s start=%s size=%s", active_model, pointer, len(batch))
            pointer += len(batch)
            break
        # 若候选模型全部不可用，立即抛错而不是继续外层 while，避免出现“看似卡住”的死循环。
        if current_model_idx >= len(candidate_models):
            raise KgApiError("EMBEDDING_MODEL_UNAVAILABLE", "Embedding 模型不可用：请检查账号模型权限或配置")
    if current_model_idx >= len(candidate_models):  # 没有任何可用模型时抛业务错误
        raise KgApiError("EMBEDDING_MODEL_UNAVAILABLE", "Embedding 模型不可用：请检查账号模型权限或配置")
    return ok_texts, ok_vectors, candidate_models[current_model_idx]


def _compute_similar_candidates(
    ok_node_ids: list[str],
    ok_types: list[str],
    ok_vectors: list[list[float]],
    threshold: float,
    top_k: int,
    task_id: str,
) -> dict[str, list[tuple[str, float]]]:
    """
    计算“每个节点的候选相似邻居 TopK”。

    说明：
    - 与原逻辑保持一致：仅同类型、阈值过滤、每节点最多 top_k 条；
    - 将原先 Python O(n^2 * dim) 双重循环改为 NumPy 矩阵乘法，显著降低运行时长；
    - 分数并列时使用极小随机扰动打散，满足“并列随机裁剪”。
    """
    if not ok_node_ids or not ok_vectors:  # 无向量输入时直接返回空结果
        return {}

    rng = np.random.default_rng(abs(hash(task_id)) % (2**32))  # 以 task_id 固定随机种子，保证同任务可复现
    by_type_indices: dict[str, list[int]] = {}  # 类型 -> 节点下标列表
    for idx, entity_type in enumerate(ok_types):  # 先按类型分桶，保证“不跨类型”
        by_type_indices.setdefault(entity_type, []).append(idx)

    result: dict[str, list[tuple[str, float]]] = {}  # 输出：node_id -> [(other_id, score)]
    chunk_size = 256  # 分块行数，平衡内存占用与吞吐

    for entity_type, idx_list in by_type_indices.items():
        if len(idx_list) <= 1:  # 同类型不足 2 个，无需计算相似度
            continue
        vec = np.asarray([ok_vectors[i] for i in idx_list], dtype=np.float32)  # 取当前类型向量矩阵 [n,d]
        norm = np.linalg.norm(vec, axis=1, keepdims=True)  # 计算每行模长 [n,1]
        norm = np.clip(norm, 1e-12, None)  # 防止除零
        vec = vec / norm  # 归一化后点积即余弦相似度
        n = vec.shape[0]  # 当前类型节点数量

        for start in range(0, n, chunk_size):  # 分块计算相似度，避免一次性大矩阵占用过高
            end = min(start + chunk_size, n)  # 当前块结束下标
            sim = vec[start:end] @ vec.T  # [m,d] x [d,n] -> [m,n]
            for local_row in range(end - start):  # 遍历块内每一行，提取 TopK 邻居
                src_local = start + local_row  # 当前源节点在类型桶内下标
                row = sim[local_row]  # 当前节点到同类型所有节点的相似度向量
                row[src_local] = -1.0  # 去掉自环
                candidate_pos = np.where(row >= threshold)[0]  # 先做阈值过滤
                if candidate_pos.size == 0:  # 没有超过阈值的候选
                    continue
                # 为了“并列随机裁剪”，加入极小随机扰动（不改变有效排序，仅打散并列）。
                noise = rng.random(candidate_pos.size, dtype=np.float32) * 1e-6
                scores = row[candidate_pos] + noise
                if candidate_pos.size > top_k:  # 仅保留 top_k
                    top_idx = np.argpartition(scores, -top_k)[-top_k:]  # 线性时间选出 top_k 下标
                    candidate_pos = candidate_pos[top_idx]
                    scores = scores[top_idx]
                order = np.argsort(scores)[::-1]  # 按分数降序排列
                src_node_id = ok_node_ids[idx_list[src_local]]  # 源节点 ID
                arr: list[tuple[str, float]] = []  # 当前节点候选列表
                for ord_i in order:
                    dst_local = int(candidate_pos[ord_i])  # 目标节点在类型桶内下标
                    dst_node_id = ok_node_ids[idx_list[dst_local]]  # 目标节点 ID
                    # 注意：写入原始 row 分数（不含扰动），避免随机噪声污染真实相似度返回。
                    arr.append((dst_node_id, float(row[dst_local])))
                result[src_node_id] = arr
        LOG.info("similarity computed for type=%s count=%s", entity_type, len(idx_list))

    return result


def _persist_chroma(
    chroma_dir: Path,
    node_ids: list[str],
    summaries: list[str],
    vectors: list[list[float]],
    types: list[str],
) -> None:
    """
    将向量写入本地 Chroma。

    MySQL 切换改造点：
    - 与数据库无关，不需要因 SQLite->MySQL 切换而修改。
    """
    import chromadb  # 延迟导入，避免未安装时影响非重建接口

    client = chromadb.PersistentClient(path=str(chroma_dir))  # 指向临时目录
    collection = None  # 先占位，便于 finally 中安全释放
    try:
        collection = client.get_or_create_collection(name="kg_nodes_v1")  # 固定集合名

        # Chroma 对单次 upsert 的批大小有上限，超限会抛 ValueError（例如 5461）。
        # 这里统一采用保守批次分片，避免一次提交全部向量导致重建失败。
        # 说明：该限制与 embedding token 无关，但失败后重复重建会造成 token 重复消耗。
        max_batch = 1000  # 保守值，显著低于常见上限，兼容不同版本 Chroma
        total = len(node_ids)  # 总写入条数，便于日志观察进度
        for start in range(0, total, max_batch):  # 分批循环 upsert
            end = min(start + max_batch, total)  # 当前批终点（左闭右开）
            collection.upsert(  # 按 id 覆盖写入，支持重复重建
                ids=node_ids[start:end],
                documents=summaries[start:end],
                embeddings=vectors[start:end],
                metadatas=[{"entity_type": t} for t in types[start:end]],
            )
            LOG.info("chroma upsert chunk done: %s/%s", end, total)  # 打印批次进度，便于排查“看不见进度”
    finally:
        # Windows 上目录 rename 对打开的句柄非常敏感；显式删除对象并 gc，降低 WinError 5 概率。
        if collection is not None:
            del collection
        del client
        gc.collect()


def _replace_dir_with_retry(src: Path, dst: Path, retries: int = 5) -> None:
    """
    带重试的目录替换。

    场景：Windows 上文件句柄释放存在延迟，偶发 WinError 5（拒绝访问）。
    """
    for i in range(retries):  # 最多重试若干次，给系统时间释放句柄
        try:
            os.replace(str(src), str(dst))  # 原子替换目录
            return
        except PermissionError:
            if i == retries - 1:  # 最后一次仍失败则抛出
                raise
            time.sleep(0.3 * (i + 1))  # 递增退避等待


def _entity_category(entity_type: str) -> str:
    """实体类型转中文分类名。"""
    return {
        "organization": "机构",
        "person": "人员",
        "topic": "省课题",
        "province_award": "省社科奖",
        "national_award": "国社科奖",
    }.get(entity_type, entity_type)


def _build_graph_payload(repo: BaseKgRepository, task_id: str, api_key: str) -> dict[str, Any]:
    """按白名单实体/关系构建完整图结构。"""
    _set_task_state(stage="load", progress=15, message="正在加载结构化数据")  # 更新阶段
    orgs_raw = repo.fetch_organizations()  # 读取机构数据
    persons_raw = repo.fetch_persons()  # 读取人员数据
    topics_raw = repo.fetch_topics()  # 读取课题数据
    p_awards_raw = repo.fetch_province_awards()  # 读取省奖数据
    n_awards_raw = repo.fetch_national_awards()  # 读取国奖数据

    nodes: dict[str, dict[str, Any]] = {}  # 节点字典，键为 node_id
    edges: list[dict[str, Any]] = []  # 边列表（先存结构化，再追加相似边）
    edge_uniques: set[tuple[str, str, str]] = set()  # 去重键：from,to,label
    summaries_map: dict[str, str] = {}  # node_id -> 摘要文本，用于向量化
    node_type_map: dict[str, str] = {}  # node_id -> entity_type，用于同类型相似计算
    vectors_input_ids: list[str] = []  # 记录进入向量化的节点 id
    vectors_input_texts: list[str] = []  # 记录进入向量化的摘要文本
    vectors_input_types: list[str] = []  # 记录进入向量化的节点类型

    def add_node(
        entity_type: str,
        pk: Any,
        name: str,
        year: int,
        attributes: dict[str, Any],
        summary: str,
        display_suffix: str = "",
    ) -> str | None:
        """新增节点并准备向量化素材。"""
        clean_name = _safe_str(name)  # 名称清洗：去空格+全角转半角
        if not clean_name:  # 名称缺失按约定直接跳过节点
            return None
        node_id = f"{entity_type}_{int(pk)}"  # 节点 ID 固定格式 {type}_{id}
        node = {
            "id": node_id,
            "name": clean_name,
            "display_name": _display_name(clean_name, year, _safe_str(display_suffix)),
            "type": entity_type,
            "category": _entity_category(entity_type),
            "year": int(year),
            "attributes": attributes,
        }
        nodes[node_id] = node  # 放入节点索引
        clean_summary = _safe_str(summary)  # 摘要同样清洗
        if clean_summary:  # 摘要非空才参与向量化
            summaries_map[node_id] = clean_summary
            node_type_map[node_id] = entity_type
            vectors_input_ids.append(node_id)
            vectors_input_texts.append(clean_summary)
            vectors_input_types.append(entity_type)
        return node_id

    def add_edge(src: str | None, dst: str | None, label: str, edge_type: str = "structured") -> None:
        """新增边并做去重。"""
        if not src or not dst:  # 任意端缺失直接跳过
            return
        if src not in nodes or dst not in nodes:  # 指向不存在节点则跳过
            return
        unique_key = (src, dst, label)  # 边去重键
        if unique_key in edge_uniques:  # 重复边不重复加入
            return
        edge_uniques.add(unique_key)  # 记录去重键
        edges.append(
            {
                "from": src,
                "to": dst,
                "label": label,
                "label_display": _EDGE_LABEL_DISPLAY.get(label, label),
                "type": edge_type,
                "line_style": "solid" if edge_type == "structured" else "dashed",
            }
        )

    # -------------------- 实体构建：机构 --------------------
    for org in orgs_raw:
        org["unit_name"] = _safe_str(org.get("unit_name"))  # 清洗名称
        org["unit_address"] = _safe_str(org.get("unit_address"))  # 清洗地址
        add_node(  # 添加机构节点
            entity_type="organization",
            pk=org["id"],
            name=org.get("unit_name", ""),
            year=0,
            attributes={"unit_address": org.get("unit_address", "")},
            summary=_build_org_summary(org),
            display_suffix=org.get("unit_address", ""),
        )

    # -------------------- 实体构建：人员 --------------------
    for person in persons_raw:
        person["name"] = _safe_str(person.get("name"))  # 清洗姓名
        person["org_name"] = _safe_str(person.get("org_name"))  # 清洗单位名
        person_id = add_node(
            entity_type="person",
            pk=person["id"],
            name=person.get("name", ""),
            year=0,
            attributes={"org_name": person.get("org_name", "")},
            summary=_build_person_summary(person),
            display_suffix=person.get("org_name", ""),
        )
        add_edge(person_id, f"organization_{int(person['org_id'])}" if person.get("org_id") else None, "任职")

    # -------------------- 实体构建：省课题 --------------------
    for topic in topics_raw:
        topic["name"] = _safe_str(topic.get("name"))  # 清洗课题名
        topic["header_name"] = _safe_str(topic.get("header_name"))  # 清洗负责人
        topic["org_name"] = _safe_str(topic.get("org_name"))  # 清洗单位名
        topic["discipline_name"] = _safe_str(topic.get("discipline_name"))  # 清洗学科名
        topic["topic_type"] = _safe_str(topic.get("topic_type"))  # 清洗课题类型（类别表 type）
        topic["topic_category"] = _safe_str(topic.get("topic_category"))  # 清洗课题类别（类别表 category）
        topic["project_level"] = _safe_str(topic.get("topic_category") or topic.get("topic_type"))  # 统一课题等级字段
        year = _safe_year(topic.get("year"))  # 年份清洗
        topic_id = add_node(
            entity_type="topic",
            pk=topic["id"],
            name=topic.get("name", ""),
            year=year,
            attributes={
                "header_name": topic.get("header_name", ""),
                "org_name": topic.get("org_name", ""),
                "discipline_name": topic.get("discipline_name", ""),
                "header_id": int(topic["header_id"]) if topic.get("header_id") else 0,
                "org_id": int(topic["org_id"]) if topic.get("org_id") else 0,
                "discipline_id": int(topic["discipline_id"]) if topic.get("discipline_id") else 0,
                "topic_id": int(topic["topic_id"]) if topic.get("topic_id") else 0,
                "topic_type": topic.get("topic_type", ""),
                "topic_category": topic.get("topic_category", ""),
                "project_level": topic.get("project_level", ""),
            },
            summary=_build_topic_summary(topic),
            display_suffix=topic.get("org_name", ""),
        )
        add_edge(f"organization_{int(topic['org_id'])}" if topic.get("org_id") else None, topic_id, "承担")
        add_edge(f"person_{int(topic['header_id'])}" if topic.get("header_id") else None, topic_id, "负责")

    # -------------------- 实体构建：省社科奖 --------------------
    for award in p_awards_raw:
        award["obj_name"] = _safe_str(award.get("obj_name"))  # 清洗奖项名
        award["person_name"] = _safe_str(award.get("person_name"))  # 清洗获奖人
        award["org_name"] = _safe_str(award.get("org_name"))  # 清洗单位
        award["discipline_name"] = _safe_str(award.get("discipline_name"))  # 清洗学科
        award["level_name"] = _safe_str(award.get("level_name"))  # 清洗省奖等级名
        year = _safe_year(award.get("year"))  # 年份清洗
        award_id = add_node(
            entity_type="province_award",
            pk=award["id"],
            name=award.get("obj_name", ""),
            year=year,
            attributes={
                "person_name": award.get("person_name", ""),
                "org_name": award.get("org_name", ""),
                "discipline_name": award.get("discipline_name", ""),
                "person_id": int(award["person_id"]) if award.get("person_id") else 0,
                "org_id": int(award["org_id"]) if award.get("org_id") else 0,
                "discipline_id": int(award["discipline_id"]) if award.get("discipline_id") else 0,
                "level_id": int(award["level_id"]) if award.get("level_id") else 0,
                "level_name": award.get("level_name", ""),
            },
            summary=_build_award_summary(award),
            display_suffix=award.get("org_name", ""),
        )
        add_edge(f"person_{int(award['person_id'])}" if award.get("person_id") else None, award_id, "获奖_省奖_个人")
        add_edge(f"organization_{int(award['org_id'])}" if award.get("org_id") else None, award_id, "获奖_省奖_单位")

    # -------------------- 实体构建：国社科奖 --------------------
    for award in n_awards_raw:
        award["obj_name"] = _safe_str(award.get("obj_name"))  # 清洗奖项名
        award["person_name"] = _safe_str(award.get("person_name"))  # 清洗获奖人
        award["org_name"] = _safe_str(award.get("org_name"))  # 清洗单位
        award["discipline_name"] = _safe_str(award.get("discipline_name"))  # 清洗学科
        award["award_type_name"] = _safe_str(award.get("award_type_name"))  # 清洗国奖类型名
        award["award_category_name"] = _safe_str(award.get("award_category_name"))  # 清洗国奖类别名
        award["level_name"] = _safe_str(" / ".join([x for x in [award.get("award_type_name"), award.get("award_category_name")] if x]))  # 统一国奖等级描述
        year = _safe_year(award.get("year"))  # 年份清洗
        award_id = add_node(
            entity_type="national_award",
            pk=award["id"],
            name=award.get("obj_name", ""),
            year=year,
            attributes={
                "person_name": award.get("person_name", ""),
                "org_name": award.get("org_name", ""),
                "discipline_name": award.get("discipline_name", ""),
                "person_id": int(award["person_id"]) if award.get("person_id") else 0,
                "org_id": int(award["org_id"]) if award.get("org_id") else 0,
                "discipline_id": int(award["discipline_id"]) if award.get("discipline_id") else 0,
                "type_id": int(award["type_id"]) if award.get("type_id") else 0,
                "categorie_id": int(award["categorie_id"]) if award.get("categorie_id") else 0,
                "award_type_name": award.get("award_type_name", ""),
                "award_category_name": award.get("award_category_name", ""),
                "level_name": award.get("level_name", ""),
            },
            summary=_build_award_summary(award),
            display_suffix=award.get("org_name", ""),
        )
        add_edge(f"person_{int(award['person_id'])}" if award.get("person_id") else None, award_id, "获奖_国奖_个人")
        add_edge(f"organization_{int(award['org_id'])}" if award.get("org_id") else None, award_id, "获奖_国奖_单位")

    # -------------------- 向量化 + 相似边 --------------------
    _set_task_state(stage="vectorize", progress=45, message="正在向量化并计算相似关系")
    ok_texts, ok_vectors, embedding_model_used = _embed_texts(vectors_input_texts, api_key=api_key)  # 调用嵌入接口

    # 为了和 ok_texts 对齐，需要按文本顺序建立 id/type 映射
    ok_node_ids: list[str] = []  # 成功向量化节点 ID
    ok_types: list[str] = []  # 成功向量化节点类型
    text_to_indices: dict[str, list[int]] = {}  # 文本 -> 位置索引列表（允许重复文本）
    for idx, txt in enumerate(vectors_input_texts):  # 预构建文本位置索引
        text_to_indices.setdefault(txt, []).append(idx)
    used_pos: set[int] = set()  # 避免同一位置被重复消费
    for txt in ok_texts:  # 按成功返回文本恢复对应 node_id
        found_pos = None
        for pos in text_to_indices.get(txt, []):
            if pos not in used_pos:
                found_pos = pos
                break
        if found_pos is None:  # 极端情况下找不到位置则跳过
            continue
        used_pos.add(found_pos)
        ok_node_ids.append(vectors_input_ids[found_pos])
        ok_types.append(vectors_input_types[found_pos])

    # 计算同类型相似边：阈值0.7，双向去重，同一节点最多5条（向量化加速实现）
    _set_task_state(stage="vectorize", progress=60, message="正在计算相似关系（同类型Top5）")
    node_neighbor_top = _compute_similar_candidates(
        ok_node_ids=ok_node_ids,
        ok_types=ok_types,
        ok_vectors=ok_vectors,
        threshold=0.7,
        top_k=5,
        task_id=task_id,
    )

    similar_uniques: set[tuple[str, str]] = set()  # 相似边无向去重键
    for src, arr in node_neighbor_top.items():
        for dst, score in arr:
            if src == dst:  # 自环无意义，跳过
                continue
            pair_key = tuple(sorted([src, dst]))  # 双向去重键
            if pair_key in similar_uniques:  # 已添加过则跳过
                continue
            # 只有双方都把对方放进 Top5 才建立边，控制“毛球”程度
            dst_neighbors = {nid for nid, _ in node_neighbor_top.get(dst, [])}
            if src not in dst_neighbors:
                continue
            similar_uniques.add(pair_key)
            add_edge(src, dst, "相似", edge_type="similar")
            # 分数后端返回前端不展示：写在 attributes，前端默认不读
            edges[-1]["attributes"] = {"score": round(float(score), 6)}

    # 将向量落盘到临时 Chroma（即使无向量也保持目录存在）
    _set_task_state(stage="build", progress=72, message="正在构建图数据并写入向量库")
    graph_payload = {
        "nodes": list(nodes.values()),
        "edges": edges,
        "metadata": {
            "version": "v1",
            "generated_at": _now_iso(),
            "embedding_model_requested": _EMBEDDING_PRIMARY_MODEL,
            "embedding_model_used": embedding_model_used,
            "node_count": len(nodes),
            "edge_count": len(edges),
            "structured_edge_count": sum(1 for e in edges if e["type"] == "structured"),
            "similar_edge_count": sum(1 for e in edges if e["type"] == "similar"),
        },
    }
    graph_payload["_vector_ids"] = ok_node_ids  # 临时字段：写文件后会删除
    graph_payload["_vector_texts"] = ok_texts  # 临时字段：写文件后会删除
    graph_payload["_vector_types"] = ok_types  # 临时字段：写文件后会删除
    graph_payload["_vectors"] = ok_vectors  # 临时字段：写文件后会删除
    return graph_payload


def _atomic_swap_graph(task_id: str, payload: dict[str, Any]) -> None:
    """将临时图数据原子替换为当前版本；失败时回滚。"""
    root = _kg_store_dir()  # kg 存储根目录
    root.mkdir(parents=True, exist_ok=True)  # 确保根目录存在
    current_dir = root / "current"  # 当前生效目录
    tmp_dir = root / f"tmp_{task_id}"  # 本次重建临时目录
    backup_dir = root / "backup"  # 回滚备份目录

    if tmp_dir.exists():  # 防御：若存在同名临时目录先清理
        shutil.rmtree(tmp_dir, ignore_errors=True)
    if backup_dir.exists():  # 防御：清理历史备份避免冲突
        shutil.rmtree(backup_dir, ignore_errors=True)

    tmp_graph_dir = tmp_dir  # 临时图根目录
    tmp_graph_dir.mkdir(parents=True, exist_ok=True)  # 创建临时目录
    tmp_graph_path = tmp_graph_dir / "graph.json"  # 图 JSON 路径
    tmp_chroma_dir = tmp_graph_dir / "chroma"  # Chroma 持久化目录
    tmp_chroma_dir.mkdir(parents=True, exist_ok=True)  # 先建目录，便于无向量场景

    vector_ids = payload.pop("_vector_ids", [])  # 取出临时字段：向量节点 ID
    vector_texts = payload.pop("_vector_texts", [])  # 取出临时字段：向量文本
    vector_types = payload.pop("_vector_types", [])  # 取出临时字段：向量类型
    vectors = payload.pop("_vectors", [])  # 取出临时字段：向量数组

    with tmp_graph_path.open("w", encoding="utf-8") as fp:  # 写临时 graph.json
        json.dump(payload, fp, ensure_ascii=False, indent=2)  # 中文可读格式输出

    if vector_ids and vectors:  # 有向量才写 Chroma，避免空 upsert
        _set_task_state(  # 写向量库通常耗时较久，先更新可视化提示
            stage="build",
            progress=80,
            message=f"正在写入向量库（{len(vector_ids)} 条向量）",
        )
        _persist_chroma(
            chroma_dir=tmp_chroma_dir,
            node_ids=vector_ids,
            summaries=vector_texts,
            vectors=vectors,
            types=vector_types,
        )
        _set_task_state(stage="build", progress=86, message="向量库写入完成，准备切换新版本")

    # 两阶段替换：current -> backup，tmp -> current；任一步失败都尝试回滚
    keep_tmp_for_retry = False  # 是否保留 tmp 目录供下次“仅恢复切换”使用
    try:
        if current_dir.exists():  # 先备份旧版本
            _replace_dir_with_retry(current_dir, backup_dir)  # Windows 句柄竞争时重试
        try:
            _replace_dir_with_retry(tmp_dir, current_dir)  # 新版本切换为 current
        except PermissionError:
            # Windows 回退路径：若 tmp_dir 因句柄被占用无法 rename，改为复制方式落地 current。
            LOG.warning("atomic rename tmp->current failed, fallback to copytree", exc_info=True)
            if current_dir.exists():
                shutil.rmtree(current_dir, ignore_errors=True)
            shutil.copytree(tmp_dir, current_dir)  # 复制到 current，避免 rename 对源目录句柄要求
            shutil.rmtree(tmp_dir, ignore_errors=True)  # 复制完成后尝试清理临时目录
        if backup_dir.exists():  # 切换成功后删除旧备份
            shutil.rmtree(backup_dir, ignore_errors=True)
    except Exception:  # 切换异常时执行回滚
        keep_tmp_for_retry = True  # 切换失败时保留 tmp，避免下次重建重复消耗 embedding token
        if current_dir.exists() and not backup_dir.exists():
            shutil.rmtree(current_dir, ignore_errors=True)
        if backup_dir.exists() and not current_dir.exists():
            try:
                _replace_dir_with_retry(backup_dir, current_dir)  # 优先原子恢复
            except PermissionError:
                shutil.copytree(backup_dir, current_dir, dirs_exist_ok=True)  # 回退为复制恢复
                shutil.rmtree(backup_dir, ignore_errors=True)
        if tmp_dir.exists() and not keep_tmp_for_retry:
            shutil.rmtree(tmp_dir, ignore_errors=True)
        elif tmp_dir.exists():
            LOG.warning("keep tmp dir for retry: %s", tmp_dir)
        raise


def _try_recover_from_tmp_snapshot() -> bool:
    """
    尝试从历史 tmp 快照恢复到 current，成功则可跳过本轮 embedding 计算。

    目标：当上次仅在 swap 阶段失败时，避免重复调用 Embedding 浪费 token。
    """
    root = _kg_store_dir()  # kg 存储根目录
    if not root.exists():  # 目录不存在直接无可恢复
        return False
    current_dir = root / "current"  # 当前生效目录
    backup_dir = root / "backup"  # 复用统一备份目录
    candidates = sorted(  # 按修改时间倒序，优先尝试最新失败快照
        [p for p in root.iterdir() if p.is_dir() and p.name.startswith("tmp_kg_")],
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    for tmp_dir in candidates:
        graph_file = tmp_dir / "graph.json"  # 快照最小可恢复条件：存在 graph.json
        if not graph_file.exists():
            continue
        try:
            if backup_dir.exists():  # 清理旧 backup，避免冲突
                shutil.rmtree(backup_dir, ignore_errors=True)
            if current_dir.exists():  # 先备份 current
                _replace_dir_with_retry(current_dir, backup_dir)
            try:
                _replace_dir_with_retry(tmp_dir, current_dir)  # 优先原子替换
            except PermissionError:
                if current_dir.exists():
                    shutil.rmtree(current_dir, ignore_errors=True)
                shutil.copytree(tmp_dir, current_dir)  # 回退复制
                shutil.rmtree(tmp_dir, ignore_errors=True)
            if backup_dir.exists():  # 成功后删除 backup
                shutil.rmtree(backup_dir, ignore_errors=True)
            LOG.info("recovered kg snapshot from %s", graph_file)
            return True
        except Exception:
            LOG.exception("recover from tmp snapshot failed: %s", tmp_dir)
            # 恢复失败时尽量还原 backup，继续尝试下一个 tmp
            if backup_dir.exists() and not current_dir.exists():
                try:
                    _replace_dir_with_retry(backup_dir, current_dir)
                except Exception:
                    LOG.exception("rollback after recover failed")
    return False


def _run_rebuild(task_id: str) -> None:
    """后台线程主逻辑：按阶段执行重建，写入任务状态。"""
    try:
        _set_task_state(status="running", stage="init", progress=5, message="开始初始化重建任务")
        # 先尝试恢复历史快照：若上次仅在 swap 失败，可直接恢复，避免重复 embedding 消耗 token。
        _set_task_state(stage="init", progress=8, message="检查是否可复用上次重建快照")
        if _try_recover_from_tmp_snapshot():
            _set_task_state(
                status="success",
                stage="done",
                progress=100,
                message="已从上次失败快照恢复，无需重复向量化",
                ended_at=_now_iso(),
            )
            return
        api_key = ensure_api_key().strip()  # 仅允许 ALIYUN_API_KEY，缺失会抛错
        if not api_key:  # 双保险：空串时按业务错误返回
            raise KgApiError("EMBEDDING_KEY_MISSING", "ALIYUN_API_KEY 未配置，无法执行向量化")
        repo: BaseKgRepository = SQLiteKgRepository()  # V1 固定 SQLite 仓储实现
        payload = _build_graph_payload(repo=repo, task_id=task_id, api_key=api_key)  # 构建图数据
        _set_task_state(stage="swap", progress=90, message="正在原子替换图数据")
        _atomic_swap_graph(task_id=task_id, payload=payload)  # 原子切换 current 版本
        _set_task_state(
            status="success",
            stage="done",
            progress=100,
            message="重建完成",
            ended_at=_now_iso(),
        )
    except KgApiError as ex:  # 业务错误：返回可预期错误码
        _set_task_state(
            status="failed",
            stage="done",
            progress=100,
            message=ex.message,
            ended_at=_now_iso(),
            error_code=ex.code,
        )
        LOG.exception("kg rebuild failed: %s", ex.code)
    except Exception as ex:  # 非预期错误：统一归类内部错误
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
    """启动重建任务：同一时间只允许一个任务运行。"""
    global _TASK_STATE  # 需要覆盖全局状态
    with _TASK_LOCK:  # 加锁检查并发
        if _TASK_STATE and _TASK_STATE.status in {"pending", "running"}:  # 有任务进行中
            raise KgApiError("REBUILD_ALREADY_RUNNING", "已有重建任务正在执行")
        task_id = f"kg_{datetime.now().strftime('%Y%m%d_%H%M%S')}"  # 生成任务 ID
        _TASK_STATE = RebuildTaskState(  # 初始化任务状态
            status="pending",
            stage="init",
            progress=0,
            message="任务已创建，等待执行",
            task_id=task_id,
            started_at=_now_iso(),
        )
    thread = threading.Thread(target=_run_rebuild, args=(task_id,), daemon=True)  # 后台线程
    thread.start()  # 异步执行重建，不阻塞 API 请求
    return {"task_id": task_id, "status": "pending"}  # 立即返回任务 ID


def get_rebuild_status(task_id: str | None = None) -> dict[str, Any]:
    """查询重建状态；若传 task_id 且不匹配，返回任务不存在错误。"""
    with _TASK_LOCK:  # 读取全局状态加锁
        if _TASK_STATE is None:  # 尚未创建任务
            raise KgApiError("TASK_NOT_FOUND", "未找到重建任务")
        if task_id and task_id != _TASK_STATE.task_id:  # 指定了其他 task_id
            raise KgApiError("TASK_NOT_FOUND", "task_id 不存在")
        return asdict(_TASK_STATE)  # dataclass -> dict


def _load_graph_data() -> dict[str, Any]:
    """加载 current 图 JSON，若不存在则提示先重建。"""
    graph_path = _current_graph_path()  # 当前图路径
    if not graph_path.exists():  # 未构建过图
        raise KgApiError("GRAPH_NOT_READY", "知识图谱尚未构建，请先执行重建")
    with graph_path.open("r", encoding="utf-8") as fp:  # 读取 UTF-8 JSON
        return json.load(fp)


def _normalize_edge_for_echarts(edge: dict[str, Any]) -> dict[str, Any]:
    """
    统一边字段，兼容 ECharts graph：
    - 保留后端原字段 from/to；
    - 同时补齐 source/target，避免前端直接使用 edges 时 tooltip 读取 node1 报错。
    """
    normalized = dict(edge)  # 复制一份，避免原地修改影响上游缓存
    src = str(normalized.get("from", ""))  # 读取后端标准起点字段
    dst = str(normalized.get("to", ""))  # 读取后端标准终点字段
    normalized["source"] = src  # 补齐 ECharts 约定字段 source
    normalized["target"] = dst  # 补齐 ECharts 约定字段 target
    normalized["value"] = str(normalized.get("label_display") or normalized.get("label") or "")  # value 用于默认 tooltip
    return normalized


def _topic_discipline_name(node: dict[str, Any]) -> str:
    """读取课题节点的领域名（来自 attributes.discipline_name）。"""
    attrs = node.get("attributes") if isinstance(node.get("attributes"), dict) else {}  # 防御：attributes 可能缺失/非字典
    return _safe_str(attrs.get("discipline_name"))  # 清洗后返回，空值会得到空串


def _prune_edges_by_per_node_limit(edges: list[dict[str, Any]], per_node_limit: int) -> list[dict[str, Any]]:
    """按“每个起点最多保留 N 条边”裁剪，降低毛球感。"""
    if per_node_limit <= 0:  # 保护：非法上限时直接返回空边集
        return []
    kept: list[dict[str, Any]] = []  # 存放通过裁剪的边
    from_counter: dict[str, int] = {}  # 统计每个起点已经保留了多少条边
    for edge in edges:  # 按传入顺序扫描边，尽量保持原始业务优先级
        src = str(edge.get("from", ""))  # 读取起点 id
        if not src:  # 起点缺失直接跳过，避免脏数据污染结果
            continue
        used = from_counter.get(src, 0)  # 当前起点已保留条数
        if used >= per_node_limit:  # 超过上限就不再保留该起点后续边
            continue
        kept.append(edge)  # 保留该边
        from_counter[src] = used + 1  # 起点计数 +1
    return kept


def _assemble_scene_result(scene: str, node_map: dict[str, dict[str, Any]], selected_edges: list[dict[str, Any]], max_nodes: int) -> dict[str, Any]:
    """将边集转成最终响应结构：补齐节点、做节点上限裁剪、输出标准边字段。"""
    ordered_node_ids: list[str] = []  # 按边遍历顺序记录节点，保证裁剪时优先保留主关系节点
    selected_node_ids: set[str] = set()  # 去重集合，避免同一节点重复进入 ordered 列表
    for edge in selected_edges:  # 遍历边，提取端点
        src = str(edge.get("from", ""))  # 起点 id
        dst = str(edge.get("to", ""))  # 终点 id
        if src and src not in selected_node_ids:  # 起点首次出现时按顺序加入
            selected_node_ids.add(src)
            ordered_node_ids.append(src)
        if dst and dst not in selected_node_ids:  # 终点首次出现时按顺序加入
            selected_node_ids.add(dst)
            ordered_node_ids.append(dst)

    result_nodes = [node_map[nid] for nid in ordered_node_ids if nid in node_map]  # 将“有序 id 列表”映射为节点对象
    truncated = False  # 记录节点是否被裁剪
    if len(result_nodes) > max_nodes:  # 超过节点上限时执行裁剪
        keep_ids = {n["id"] for n in result_nodes[:max_nodes]}  # 先保留前 max_nodes 个节点 id
        result_nodes = [n for n in result_nodes if n["id"] in keep_ids]  # 节点按 keep_ids 过滤
        selected_edges = [e for e in selected_edges if e.get("from") in keep_ids and e.get("to") in keep_ids]  # 仅保留端点都存在的边
        truncated = True  # 标记本次发生了裁剪

    result_edges = [_normalize_edge_for_echarts(e) for e in selected_edges]  # 统一补齐 source/target/value
    return {
        "scene": scene,
        "nodes": result_nodes,
        "edges": result_edges,
        "metadata": {
            "scene": scene,  # 回显场景名，便于前端调试
            "max_nodes": max_nodes,  # 回显节点上限参数
            "node_count": len(result_nodes),  # 实际节点数
            "edge_count": len(result_edges),  # 实际边数
            "truncated": truncated,  # 是否触发节点裁剪
            "truncated_reason": "达到 max_nodes 上限，结果已截断" if truncated else "",  # 人类可读裁剪原因
        },
    }


def _build_scene_graph(
    scene: str,
    max_nodes: int,
    include_similar: bool = False,
    include_award_links: bool = False,
) -> dict[str, Any]:
    """
    按业务场景构建“轻量核心关系图”。

    关键约束：
    - 相似关系可开关，且严格“不跨类型”；
    - 奖项与机构/人员关系可开关，默认关闭；
    - 不构建“课题-奖项”推断边，只使用表内既有外键关系。
    """
    scene = _safe_str(scene)  # 清洗场景字符串，避免空格/全角干扰匹配
    if scene not in {"government", "industry", "research"}:  # 限定只支持三类场景
        raise KgApiError("INVALID_PARAM", "scene 仅支持 government / industry / research")
    if max_nodes <= 0:  # 参数保护：节点上限必须大于 0
        raise KgApiError("INVALID_PARAM", "max_nodes 必须大于 0")
    if max_nodes > 300:  # 参数保护：限制上限避免前端浏览器卡死
        raise KgApiError("TOO_MANY_NODES", "max_nodes 超过上限 300")

    graph = _load_graph_data()  # 读取当前全量图
    base_nodes = graph.get("nodes", [])  # 全量节点列表
    base_edges = graph.get("edges", [])  # 全量边列表
    node_map = {str(n.get("id")): n for n in base_nodes if n.get("id")}  # 按 id 建立节点索引

    selected_edges: list[dict[str, Any]] = []  # 场景候选边（后续按规则裁剪）
    edge_seen: set[tuple[str, str, str]] = set()  # 边去重键，避免同边重复加入

    def add_edge_if_valid(
        src: str,
        dst: str,
        label: str,
        label_display: str,
        edge_type: str = "structured",
        layer: str = "structured",
        attributes: dict[str, Any] | None = None,
    ) -> None:
        """公共加边函数：做节点存在校验 + 去重 + 统一字段。"""
        if not src or not dst:  # 端点为空时忽略
            return
        if src not in node_map and not src.startswith("discipline_"):  # 起点既不在图里也不是领域虚拟点则忽略
            return
        if dst not in node_map and not dst.startswith("discipline_"):  # 终点既不在图里也不是领域虚拟点则忽略
            return
        key = (src, dst, label)  # 方向性去重键
        if key in edge_seen:  # 已加入过则不重复添加
            return
        edge_seen.add(key)  # 记录去重键
        edge = {
            "from": src,  # 后端标准起点字段
            "to": dst,  # 后端标准终点字段
            "label": label,  # 关系技术标签
            "label_display": label_display,  # 前端展示标签
            "type": edge_type,  # structured 或 similar
            "layer": layer,  # 关系层：structured/similar/award_link
            "line_style": "dashed" if edge_type == "similar" else "solid",  # 用线型区分关系类型
        }
        if attributes:  # 如果传入扩展属性则挂到边上
            edge["attributes"] = attributes
        selected_edges.append(edge)  # 追加到场景边列表

    def collect_selected_node_ids() -> set[str]:
        """从已选边中提取当前场景节点集合。"""
        ids: set[str] = set()  # 去重集合
        for edge in selected_edges:  # 遍历场景边
            src = str(edge.get("from", ""))  # 起点 id
            dst = str(edge.get("to", ""))  # 终点 id
            if src:
                ids.add(src)
            if dst:
                ids.add(dst)
        return ids

    # -------------------- 先构建各场景核心结构化关系 --------------------
    if scene == "government":  # 政务：机构-课题 + 课题-领域
        discipline_nodes: dict[str, dict[str, Any]] = {}  # 领域虚拟节点缓存，避免重复建点
        for edge in base_edges:  # 遍历全量边，抽取“机构-课题”主关系
            if edge.get("label") != "承担":
                continue
            src = str(edge.get("from", ""))  # 起点 id（机构）
            dst = str(edge.get("to", ""))  # 终点 id（课题）
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            if src_node.get("type") != "organization" or dst_node.get("type") != "topic":
                continue
            add_edge_if_valid(src, dst, "承担", "承担课题", edge_type="structured", layer="structured")

            discipline_name = _topic_discipline_name(dst_node)  # 从课题属性读取领域名
            if not discipline_name:
                continue
            discipline_id = f"discipline_{discipline_name}"  # 用稳定 id 表示领域节点
            if discipline_id not in discipline_nodes:
                discipline_nodes[discipline_id] = {
                    "id": discipline_id,
                    "name": discipline_name,
                    "display_name": discipline_name,
                    "type": "discipline",
                    "category": "领域",
                    "year": 0,
                    "attributes": {"source": "topic.attributes.discipline_name"},
                }
            add_edge_if_valid(dst, discipline_id, "属于领域", "课题领域", edge_type="structured", layer="structured")
        node_map.update(discipline_nodes)  # 领域虚拟节点并入节点索引
        selected_edges[:] = _prune_edges_by_per_node_limit(selected_edges, per_node_limit=6)  # 控制单节点连接数

    elif scene == "industry":  # 产业：课题-机构
        for edge in base_edges:  # 遍历边，抽取机构-课题核心关系
            src = str(edge.get("from", ""))
            dst = str(edge.get("to", ""))
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            src_type = str(src_node.get("type", ""))
            dst_type = str(dst_node.get("type", ""))
            if edge.get("label") == "承担" and src_type == "organization" and dst_type == "topic":
                add_edge_if_valid(src, dst, "承担", "课题依托机构", edge_type="structured", layer="structured")
        selected_edges[:] = _prune_edges_by_per_node_limit(selected_edges, per_node_limit=5)

    else:  # research：人员-课题
        for edge in base_edges:  # 抽取“负责”边构建科研主链路
            if edge.get("label") != "负责":
                continue
            src = str(edge.get("from", ""))  # 起点 id（人员）
            dst = str(edge.get("to", ""))  # 终点 id（课题）
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            if src_node.get("type") != "person" or dst_node.get("type") != "topic":
                continue
            add_edge_if_valid(src, dst, "负责", "课题负责人", edge_type="structured", layer="structured")
        selected_edges[:] = _prune_edges_by_per_node_limit(selected_edges, per_node_limit=6)

    # -------------------- 可选关系层：奖项关联（默认关闭） --------------------
    if include_award_links:
        selected_ids = collect_selected_node_ids()  # 获取当前场景已入选节点
        for edge in base_edges:  # 扫描全量边，补充机构/人员 -> 奖项关系
            src = str(edge.get("from", ""))  # 起点一般是人员或机构
            dst = str(edge.get("to", ""))  # 终点一般是奖项
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            src_type = str(src_node.get("type", ""))  # 起点类型
            dst_type = str(dst_node.get("type", ""))  # 终点类型
            if src_type not in {"person", "organization"}:  # 奖项关联仅允许人员/机构作为起点
                continue
            if dst_type not in {"province_award", "national_award"}:  # 奖项关联终点必须是奖项节点
                continue
            if src not in selected_ids:  # 只给“场景中已有机构/人员”补奖项连接，避免无关节点涌入
                continue
            label = str(edge.get("label", ""))  # 使用原始标签透传，保证与现有图谱语义一致
            add_edge_if_valid(
                src,
                dst,
                label,
                _EDGE_LABEL_DISPLAY.get(label, label),
                edge_type="structured",
                layer="award_link",
            )

    # -------------------- 可选关系层：相似关系（默认关闭，严格同类型） --------------------
    if include_similar:
        selected_ids = collect_selected_node_ids()  # 获取当前场景节点范围
        for edge in base_edges:  # 从全量图中抽取相似边
            if str(edge.get("type", "")) != "similar" and str(edge.get("label", "")) != "相似":
                continue
            src = str(edge.get("from", ""))
            dst = str(edge.get("to", ""))
            if src not in selected_ids or dst not in selected_ids:  # 仅在当前场景节点子集内显示相似
                continue
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            src_type = str(src_node.get("type", ""))
            dst_type = str(dst_node.get("type", ""))
            if src_type != dst_type:  # 强约束：相似关系绝不跨类型
                continue
            similar_display = "关键词聚类" if (scene == "industry" and src_type == "topic") else "相似关系"
            add_edge_if_valid(
                src,
                dst,
                "相似",
                similar_display,
                edge_type="similar",
                layer="similar",
                attributes=edge.get("attributes") if isinstance(edge.get("attributes"), dict) else None,
            )

    # 为避免节点截断阶段“只剩核心边”，这里按关系层交错重排，保证可选层打开后也能进入前部节点集合。
    if include_award_links or include_similar:
        structured_edges = [e for e in selected_edges if e.get("layer") == "structured"]  # 核心结构化边
        award_edges = [e for e in selected_edges if e.get("layer") == "award_link"]  # 奖项关联边
        similar_edges = [e for e in selected_edges if e.get("layer") == "similar"]  # 相似关系边
        merged_edges: list[dict[str, Any]] = []  # 交错后的边序列
        max_len = max(len(structured_edges), len(award_edges), len(similar_edges))
        for idx in range(max_len):  # 每轮取多层各一条，避免某一层被完全挤掉
            if idx < len(structured_edges):
                merged_edges.append(structured_edges[idx])
            if idx < len(award_edges):
                merged_edges.append(award_edges[idx])
            if idx < len(similar_edges):
                merged_edges.append(similar_edges[idx])
        selected_edges = merged_edges

    # 最后统一裁剪，避免开启可选层后边数陡增导致渲染压力。
    selected_edges = _prune_edges_by_per_node_limit(selected_edges, per_node_limit=10)
    result = _assemble_scene_result(scene=scene, node_map=node_map, selected_edges=selected_edges, max_nodes=max_nodes)
    result["metadata"]["include_similar"] = bool(include_similar)  # 回显开关状态，便于前端显示
    result["metadata"]["include_award_links"] = bool(include_award_links)  # 回显开关状态，便于前端显示
    return result


def get_all_scene_graphs(
    max_nodes: int,
    include_similar: bool = False,
    include_award_links: bool = False,
) -> dict[str, Any]:
    """一次性返回三张图，前端可直接做 tab/按钮切换，不再依赖中心点。"""
    if max_nodes <= 0:  # 参数保护：节点上限必须大于 0
        raise KgApiError("INVALID_PARAM", "max_nodes 必须大于 0")
    if max_nodes > 300:  # 参数保护：限制最大规模，防止接口返回过大
        raise KgApiError("TOO_MANY_NODES", "max_nodes 超过上限 300")
    government_graph = _build_scene_graph(  # 政务图：按开关决定是否追加可选关系层
        scene="government",
        max_nodes=max_nodes,
        include_similar=include_similar,
        include_award_links=include_award_links,
    )
    industry_graph = _build_scene_graph(  # 产业图：按开关决定是否追加可选关系层
        scene="industry",
        max_nodes=max_nodes,
        include_similar=include_similar,
        include_award_links=include_award_links,
    )
    research_graph = _build_scene_graph(  # 科研图：按开关决定是否追加可选关系层
        scene="research",
        max_nodes=max_nodes,
        include_similar=include_similar,
        include_award_links=include_award_links,
    )
    return {
        "metadata": {  # 汇总元信息，便于前端统一展示统计
            "max_nodes": max_nodes,
            "scene_count": 3,
            "include_similar": bool(include_similar),
            "include_award_links": bool(include_award_links),
        },
        "scenes": {  # 三个固定键，前端可直接按 key 读取
            "government": government_graph,
            "industry": industry_graph,
            "research": research_graph,
        },
    }


def get_subgraph(center_id: str, depth: int, max_nodes: int, include_similar: bool) -> dict[str, Any]:
    """按中心节点返回子图：支持 1/2 度、最大 200 节点、可选相似边。"""
    center_id = _safe_str(center_id)  # 入参清洗
    if not center_id:  # 必填校验
        raise KgApiError("INVALID_PARAM", "center_id 不能为空")
    if depth not in (1, 2):  # 深度限制 1/2
        raise KgApiError("INVALID_PARAM", "depth 仅支持 1 或 2")
    if max_nodes <= 0:  # 必须正数
        raise KgApiError("INVALID_PARAM", "max_nodes 必须大于 0")
    if max_nodes > 200:  # 接口硬限制 <= 200
        raise KgApiError("TOO_MANY_NODES", "max_nodes 超过上限 200")

    graph = _load_graph_data()  # 加载全量图
    nodes = graph.get("nodes", [])  # 取节点列表
    edges = graph.get("edges", [])  # 取边列表
    node_map = {n["id"]: n for n in nodes}  # 建立节点索引
    if center_id not in node_map:  # 中心节点不存在
        raise KgApiError("CENTER_NOT_FOUND", "center_id 不存在")

    allow_edge_types = {"structured", "similar"} if include_similar else {"structured"}  # 边类型过滤
    adjacency: dict[str, set[str]] = {}  # 无向邻接表，便于度扩展
    for e in edges:  # 遍历全量边构建邻接关系
        if e.get("type") not in allow_edge_types:  # 不允许的边类型跳过
            continue
        src = e.get("from")  # 起点
        dst = e.get("to")  # 终点
        if src not in node_map or dst not in node_map:  # 异常边跳过
            continue
        adjacency.setdefault(src, set()).add(dst)  # src -> dst
        adjacency.setdefault(dst, set()).add(src)  # dst -> src（子图按无向扩展）

    selected: set[str] = {center_id}  # 已选节点集合，先放中心节点
    frontier: set[str] = {center_id}  # 当前层扩展边界
    truncated = False  # 是否发生截断
    for _ in range(depth):  # 按层级扩展 1 或 2 度
        next_frontier: set[str] = set()  # 下一层边界
        for nid in frontier:
            for nb in adjacency.get(nid, set()):  # 遍历邻居
                if nb in selected:  # 已选过则跳过
                    continue
                if len(selected) >= max_nodes:  # 达到上限后不再新增
                    truncated = True
                    continue
                selected.add(nb)  # 新增节点
                next_frontier.add(nb)  # 加入下一层待扩展
        frontier = next_frontier  # 进入下一层
        if not frontier:  # 无可扩展节点则提前结束
            break

    sub_nodes = [node_map[nid] for nid in selected if nid in node_map]  # 生成子图节点列表
    sub_edges: list[dict[str, Any]] = []  # 子图边列表
    for e in edges:  # 过滤保留子图内部边
        if e.get("type") not in allow_edge_types:
            continue
        if e.get("from") in selected and e.get("to") in selected:
            sub_edges.append(e)

    # 二次边去重：相同 from+to+label 只保留一条
    deduped_edges: list[dict[str, Any]] = []  # 去重后边
    seen: set[tuple[str, str, str]] = set()  # 去重键
    for e in sub_edges:
        key = (str(e.get("from")), str(e.get("to")), str(e.get("label")))
        if key in seen:
            continue
        seen.add(key)
        deduped_edges.append(e)

    normalized_edges = [_normalize_edge_for_echarts(e) for e in deduped_edges]  # 补齐 source/target，兼容 ECharts graph

    return {
        "nodes": sub_nodes,
        "edges": normalized_edges,
        "metadata": {
            "center_id": center_id,
            "depth": depth,
            "max_nodes": max_nodes,
            "include_similar": bool(include_similar),
            "truncated": truncated,
            "truncated_reason": "达到 max_nodes 上限，结果已截断" if truncated else "",
        },
    }


def get_scene_graph(
    scene: str,
    max_nodes: int,
    include_similar: bool = False,
    include_award_links: bool = False,
) -> dict[str, Any]:
    """对外暴露场景图入口：供 API 层直接调用。"""
    return _build_scene_graph(
        scene=scene,
        max_nodes=max_nodes,
        include_similar=include_similar,
        include_award_links=include_award_links,
    )


def _award_entry_from_node(award_node: dict[str, Any]) -> dict[str, Any]:
    """把奖项节点转成详情面板可直接渲染的简表条目。"""
    attrs = award_node.get("attributes") if isinstance(award_node.get("attributes"), dict) else {}  # 读取奖项 attributes，防御空值
    return {
        "id": str(award_node.get("id", "")),  # 奖项节点 ID，前端可用于跳转或定位
        "name": str(award_node.get("display_name") or award_node.get("name") or ""),  # 优先展示 display_name，保证同名可区分
        "year": int(award_node.get("year") or 0),  # 年份字段，缺失时返回 0
        "level_name": str(attrs.get("level_name") or ""),  # 等级名（省奖等级或国奖类型/类别组合）
        "discipline_name": str(attrs.get("discipline_name") or ""),  # 学科名，便于面板筛选
        "person_name": str(attrs.get("person_name") or ""),  # 奖项记录中的人员名
        "org_name": str(attrs.get("org_name") or ""),  # 奖项记录中的单位名
    }


def get_hierarchy_graph(
    group_by: str = "discipline",
    max_orgs: int = 12,
    max_nodes: int = 500,
    org_id: str = "",
    expand_depth: int = 2,
    include_similar: bool = False,
) -> dict[str, Any]:
    """
    构建“机构为根”的分层图：
    - L1：机构
    - L2：机构下分组（默认学科，可切课题类别）
    - L3：具体课题
    - L4：课题负责人（仅负责人）

    奖项不进层级，不新增奖项层节点：
    - 在机构/人员节点属性里返回“单位/人员 × 省/国”统计与列表，供右侧详情面板展示。
    """
    group_by = _safe_str(group_by) or "discipline"  # 清洗分组模式，空值回退到 discipline
    if group_by not in {"discipline", "topic_category"}:  # 限制仅两种分组策略
        raise KgApiError("INVALID_PARAM", "group_by 仅支持 discipline 或 topic_category")
    if max_orgs <= 0:  # 参数保护：机构数必须为正
        raise KgApiError("INVALID_PARAM", "max_orgs 必须大于 0")
    if max_orgs > 100:  # 参数保护：避免一次性返回过多机构
        raise KgApiError("TOO_MANY_NODES", "max_orgs 超过上限 100")
    if max_nodes <= 0:  # 参数保护：节点总量必须为正
        raise KgApiError("INVALID_PARAM", "max_nodes 必须大于 0")
    if max_nodes > 1200:  # 参数保护：上限控制，避免前端渲染超载
        raise KgApiError("TOO_MANY_NODES", "max_nodes 超过上限 1200")
    if expand_depth not in {1, 2, 3, 4}:  # 参数保护：前端只允许展开 1-4 级
        raise KgApiError("INVALID_PARAM", "expand_depth 仅支持 1-4")

    graph = _load_graph_data()  # 读取当前生效的全量图
    base_nodes = graph.get("nodes", [])  # 全量节点列表
    base_edges = graph.get("edges", [])  # 全量边列表
    node_map = {str(n.get("id")): n for n in base_nodes if n.get("id")}  # 节点索引：id -> node

    # -------------------- 先按结构化边建立关系索引 --------------------
    org_to_topics: dict[str, set[str]] = {}  # 机构 -> 课题集合（承担边）
    topic_to_headers: dict[str, set[str]] = {}  # 课题 -> 负责人集合（负责边，通常 1 个）
    org_to_persons: dict[str, set[str]] = {}  # 机构 -> 人员集合（任职边）
    org_to_awards_unit: dict[str, dict[str, set[str]]] = {}  # 机构 -> {province/national -> 奖项 id 集合}（单位获奖）
    person_to_awards: dict[str, dict[str, set[str]]] = {}  # 人员 -> {province/national -> 奖项 id 集合}（个人获奖）

    for edge in base_edges:  # 扫描全量边，提取层级图与奖项统计需要的关系
        if str(edge.get("type", "")) != "structured":  # 层级主链仅使用结构化边
            continue
        label = str(edge.get("label", ""))  # 读取边标签
        src = str(edge.get("from", ""))  # 起点 id
        dst = str(edge.get("to", ""))  # 终点 id
        if not src or not dst:  # 端点缺失跳过
            continue

        if label == "承担":  # 机构 -> 课题
            org_to_topics.setdefault(src, set()).add(dst)
            continue
        if label == "负责":  # 人员 -> 课题（存反向索引时按 topic 聚合）
            topic_to_headers.setdefault(dst, set()).add(src)
            continue
        if label == "任职":  # 人员 -> 机构（转成机构 -> 人员集合）
            org_to_persons.setdefault(dst, set()).add(src)
            continue
        if label in {"获奖_省奖_单位", "获奖_国奖_单位"}:  # 机构 -> 奖项（单位维度）
            level_key = "province" if "省奖" in label else "national"  # 按标签拆省/国
            org_to_awards_unit.setdefault(src, {"province": set(), "national": set()})[level_key].add(dst)
            continue
        if label in {"获奖_省奖_个人", "获奖_国奖_个人"}:  # 人员 -> 奖项（个人维度）
            level_key = "province" if "省奖" in label else "national"  # 按标签拆省/国
            person_to_awards.setdefault(src, {"province": set(), "national": set()})[level_key].add(dst)

    # 机构按“课题数降序”选前 max_orgs，保证默认进入图的是信息量更高的机构。
    org_ids_sorted = sorted(
        org_to_topics.keys(),
        key=lambda oid: len(org_to_topics.get(oid, set())),
        reverse=True,
    )
    org_id = _safe_str(org_id)  # 清洗机构筛选参数
    if org_id:  # 用户指定机构时只展示该机构子树
        if org_id not in node_map or str(node_map.get(org_id, {}).get("type", "")) != "organization":
            raise KgApiError("CENTER_NOT_FOUND", "org_id 不存在或不是机构节点")
        selected_org_ids = [org_id]
    else:
        selected_org_ids = org_ids_sorted[:max_orgs]  # 最终入图机构集合

    result_nodes: list[dict[str, Any]] = []  # 输出节点列表
    result_edges: list[dict[str, Any]] = []  # 输出边列表
    node_seen: set[str] = set()  # 节点去重集合
    edge_seen: set[tuple[str, str, str]] = set()  # 边去重键（from,to,label）
    group_node_id_map: dict[tuple[str, str, str], str] = {}  # (org_id,group_by,group_name) -> group_node_id
    group_node_seq = 1  # 分组虚拟节点递增序号，确保 ID 稳定唯一

    def add_node(node: dict[str, Any]) -> None:
        """向结果集中安全添加节点（按 id 去重）。"""
        node_id = str(node.get("id", ""))  # 读取节点 id
        if not node_id or node_id in node_seen:  # 空 id 或已存在时跳过
            return
        node_seen.add(node_id)  # 记录去重
        result_nodes.append(node)  # 追加结果

    def add_edge(src: str, dst: str, label: str, label_display: str) -> None:
        """向结果集中安全添加结构化边（并补齐前端字段）。"""
        if not src or not dst:  # 端点为空直接跳过
            return
        key = (src, dst, label)  # 去重键
        if key in edge_seen:  # 重复边直接跳过
            return
        edge_seen.add(key)  # 记录去重
        result_edges.append(
            _normalize_edge_for_echarts(
                {
                    "from": src,
                    "to": dst,
                    "label": label,
                    "label_display": label_display,
                    "type": "structured",
                    "layer": "structured",
                    "line_style": "solid",
                }
            )
        )

    # -------------------- 构建层级节点与边 --------------------
    for org_id in selected_org_ids:  # 逐机构构建子树
        org_node = node_map.get(org_id)  # 机构节点原始数据
        if not org_node:  # 理论上不应缺失，防御跳过
            continue

        # 先收集机构相关奖项统计：单位获奖 + 机构人员个人获奖。
        unit_awards = org_to_awards_unit.get(org_id, {"province": set(), "national": set()})  # 单位获奖映射
        org_person_ids = org_to_persons.get(org_id, set())  # 该机构人员集合
        person_awards_province: set[str] = set()  # 机构人员个人省奖集合（去重）
        person_awards_national: set[str] = set()  # 机构人员个人国奖集合（去重）
        for pid in org_person_ids:  # 汇总机构内人员个人奖项
            person_awards_province.update(person_to_awards.get(pid, {}).get("province", set()))
            person_awards_national.update(person_to_awards.get(pid, {}).get("national", set()))

        org_award_lists = {  # 机构详情面板奖项列表（四块）
            "unit_province": [_award_entry_from_node(node_map[aid]) for aid in sorted(unit_awards.get("province", set())) if aid in node_map],
            "unit_national": [_award_entry_from_node(node_map[aid]) for aid in sorted(unit_awards.get("national", set())) if aid in node_map],
            "person_province": [_award_entry_from_node(node_map[aid]) for aid in sorted(person_awards_province) if aid in node_map],
            "person_national": [_award_entry_from_node(node_map[aid]) for aid in sorted(person_awards_national) if aid in node_map],
        }

        org_node_out = dict(org_node)  # 复制机构节点，避免改动原图内存对象
        org_attrs = org_node_out.get("attributes") if isinstance(org_node_out.get("attributes"), dict) else {}  # 读取原 attributes
        org_attrs = dict(org_attrs)  # 复制 attributes，避免共享引用
        org_attrs["award_stats"] = {  # 机构奖项统计（单位/人员 × 省/国）
            "unit_province_count": len(unit_awards.get("province", set())),
            "unit_national_count": len(unit_awards.get("national", set())),
            "person_province_count": len(person_awards_province),
            "person_national_count": len(person_awards_national),
        }
        org_attrs["award_lists"] = org_award_lists  # 机构奖项详情列表
        org_attrs["hierarchy_level"] = 1  # 标记层级，前端布局直接用
        org_node_out["attributes"] = org_attrs
        add_node(org_node_out)  # 加入机构节点

        topics = sorted(org_to_topics.get(org_id, set()))  # 当前机构承担课题集合
        for topic_id in topics:  # 逐课题进入 L2/L3/L4 构建
            topic_node = node_map.get(topic_id)  # 读取课题节点
            if not topic_node:
                continue
            topic_attrs = topic_node.get("attributes") if isinstance(topic_node.get("attributes"), dict) else {}  # 读取课题属性

            if group_by == "discipline":  # 默认按学科分组
                group_name = _safe_str(topic_attrs.get("discipline_name")) or "未标注学科"
            else:  # 可切换按课题类别分组
                group_name = (
                    _safe_str(topic_attrs.get("topic_category"))
                    or _safe_str(topic_attrs.get("topic_type"))
                    or _safe_str(topic_attrs.get("project_level"))
                    or "未标注类别"
                )

            group_key = (org_id, group_by, group_name)  # 机构内分组唯一键
            if group_key not in group_node_id_map:  # 首次遇到分组时创建 L2 虚拟节点
                group_node_id = f"group_{group_by}_{group_node_seq}"  # 分组节点 ID
                group_node_seq += 1  # 自增序号
                group_node_id_map[group_key] = group_node_id  # 写入映射
                add_node(
                    {
                        "id": group_node_id,
                        "name": group_name,
                        "display_name": group_name,
                        "type": "group",
                        "category": "分组",
                        "year": 0,
                        "attributes": {
                            "group_by": group_by,  # 当前分组模式
                            "group_name": group_name,  # 分组名称
                            "org_id": org_id,  # 所属机构 id
                            "hierarchy_level": 2,  # L2 分组层
                        },
                    }
                )
                add_edge(org_id, group_node_id, "分组", "机构分组")  # L1 -> L2

            group_node_id = group_node_id_map[group_key]  # 获取当前课题所属分组节点 id

            topic_node_out = dict(topic_node)  # 复制课题节点用于输出
            topic_attrs_out = topic_node_out.get("attributes") if isinstance(topic_node_out.get("attributes"), dict) else {}
            topic_attrs_out = dict(topic_attrs_out)  # 复制 attributes
            topic_attrs_out["hierarchy_level"] = 3  # 标记 L3 课题层
            topic_attrs_out["group_by"] = group_by  # 记录分组模式，便于前端调试
            topic_attrs_out["group_name"] = group_name  # 记录分组名，便于前端做簇标题
            topic_node_out["attributes"] = topic_attrs_out
            add_node(topic_node_out)  # 加入课题节点
            add_edge(group_node_id, topic_id, "包含课题", "分组课题")  # L2 -> L3

            header_ids = sorted(topic_to_headers.get(topic_id, set()))  # 该课题负责人集合（通常只有一个）
            for person_id in header_ids:  # 逐负责人构建 L4
                person_node = node_map.get(person_id)  # 负责人节点
                if not person_node:
                    continue
                person_awards = person_to_awards.get(person_id, {"province": set(), "national": set()})  # 个人奖项映射
                person_award_lists = {
                    "person_province": [_award_entry_from_node(node_map[aid]) for aid in sorted(person_awards.get("province", set())) if aid in node_map],
                    "person_national": [_award_entry_from_node(node_map[aid]) for aid in sorted(person_awards.get("national", set())) if aid in node_map],
                }
                person_node_out = dict(person_node)  # 复制人员节点用于输出
                person_attrs = person_node_out.get("attributes") if isinstance(person_node_out.get("attributes"), dict) else {}
                person_attrs = dict(person_attrs)  # 复制 attributes
                person_attrs["award_stats"] = {  # 人员奖项统计（个人 × 省/国）
                    "person_province_count": len(person_awards.get("province", set())),
                    "person_national_count": len(person_awards.get("national", set())),
                }
                person_attrs["award_lists"] = person_award_lists  # 人员奖项详情列表
                person_attrs["hierarchy_level"] = 4  # 标记 L4 人员层
                person_node_out["attributes"] = person_attrs
                add_node(person_node_out)  # 加入人员节点
                add_edge(topic_id, person_id, "负责人", "课题负责人")  # L3 -> L4（仅负责人）

    if include_similar:
        selected_topic_ids = {str(n.get("id", "")) for n in result_nodes if str(n.get("type", "")) == "topic"}  # 先取已入图课题 id
        for edge in base_edges:  # 从全量边中补“课题-课题相似边”
            if str(edge.get("type", "")) != "similar" and str(edge.get("label", "")) != "相似":
                continue
            src = str(edge.get("from", ""))  # 起点课题 id
            dst = str(edge.get("to", ""))  # 终点课题 id
            if src not in selected_topic_ids or dst not in selected_topic_ids:
                continue
            src_node = node_map.get(src)
            dst_node = node_map.get(dst)
            if not src_node or not dst_node:
                continue
            if str(src_node.get("type", "")) != "topic" or str(dst_node.get("type", "")) != "topic":
                continue
            key = (src, dst, "相似")
            if key in edge_seen:
                continue
            edge_seen.add(key)
            result_edges.append(
                _normalize_edge_for_echarts(
                    {
                        "from": src,
                        "to": dst,
                        "label": "相似",
                        "label_display": "相似课题",
                        "type": "similar",
                        "layer": "similar",
                        "line_style": "dashed",
                        "attributes": edge.get("attributes") if isinstance(edge.get("attributes"), dict) else {},
                    }
                )
            )

    if expand_depth < 4:
        keep_ids: set[str] = set()  # 按展开层级保留节点
        for node in result_nodes:
            attrs = node.get("attributes") if isinstance(node.get("attributes"), dict) else {}
            level = int(attrs.get("hierarchy_level") or 0)
            if 1 <= level <= expand_depth:
                keep_ids.add(str(node.get("id", "")))
        result_nodes = [n for n in result_nodes if str(n.get("id", "")) in keep_ids]
        result_edges = [e for e in result_edges if str(e.get("from", "")) in keep_ids and str(e.get("to", "")) in keep_ids]

    # 节点上限保护：若超限则按“机构->分组->课题->人员”优先级裁剪。
    truncated = False  # 是否发生裁剪
    if len(result_nodes) > max_nodes:
        truncated = True
        level_order = {"organization": 1, "group": 2, "topic": 3, "person": 4}  # 层级优先级映射
        result_nodes.sort(key=lambda n: level_order.get(str(n.get("type", "")), 99))  # 按层级优先保留
        keep_ids = {str(n.get("id", "")) for n in result_nodes[:max_nodes]}  # 取前 max_nodes 节点 id
        result_nodes = [n for n in result_nodes if str(n.get("id", "")) in keep_ids]  # 节点裁剪
        result_edges = [e for e in result_edges if str(e.get("from", "")) in keep_ids and str(e.get("to", "")) in keep_ids]  # 边同步裁剪

    return {
        "nodes": result_nodes,
        "edges": result_edges,
        "metadata": {
            "view_mode": "organization_hierarchy",  # 视图模式标记，前端可据此走层级布局
            "group_by": group_by,  # 当前分组模式：discipline / topic_category
            "org_id": org_id,  # 当前机构筛选 id（空表示全部）
            "expand_depth": expand_depth,  # 当前展开层级
            "include_similar": bool(include_similar),  # 是否显示相似边
            "max_orgs": max_orgs,  # 机构上限参数回显
            "max_nodes": max_nodes,  # 节点上限参数回显
            "node_count": len(result_nodes),  # 实际节点数
            "edge_count": len(result_edges),  # 实际边数
            "truncated": truncated,  # 是否发生裁剪
            "truncated_reason": "达到 max_nodes 上限，结果已截断" if truncated else "",  # 裁剪说明
            "organization_options": [
                {
                    "id": str(n.get("id", "")),
                    "name": str(n.get("display_name") or n.get("name") or ""),
                }
                for n in sorted(
                    [n for n in base_nodes if str(n.get("type", "")) == "organization"],
                    key=lambda x: str(x.get("display_name") or x.get("name") or ""),
                )
            ],  # 机构下拉可选项（前端筛选用）
        },
    }

