# -*- coding: utf-8 -*-
"""
知识图谱模块入口。

本文件只做导出，避免业务方直接依赖内部实现细节。
"""

from src.kg.simple_engine import (  # 对外暴露统一服务入口，API 层只需 import 这些符号
    KgApiError,
    get_all_scene_graphs,
    get_hierarchy_graph,
    get_scene_graph,
    get_subgraph,
    query_graph,
    start_rebuild,
    get_rebuild_status,
)

