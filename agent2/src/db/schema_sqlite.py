# -*- coding: utf-8 -*-
"""
【模块作用】本地 SQLite 建表 DDL 字符串，与业务 Excel 列名对齐。

【MySQL 迁移】
1. 使用需求文档中的 MySQL 建表语句（InnoDB、注释、外键、索引）；勿直接执行本文件到生产。
2. 字段名、类型需与 query_engine 中 SQL 一致；若有重命名，同步改 query_engine.py。
3. 本地为简化类型（INTEGER/TEXT）；线上注意 VARCHAR 长度、NULL/DEFAULT 与字符集 utf8mb4。
"""
# 以下 SQL 由 sqlite_loader.executescript 一次执行；-- 为 SQL 行注释，便于对照业务表
SCHEMA_SQL = """
-- discipline_info：学科字典
CREATE TABLE IF NOT EXISTS discipline_info (
  id INTEGER PRIMARY KEY,
  disci_name TEXT NOT NULL
);
-- province_topic_category：省课题类别与分值
CREATE TABLE IF NOT EXISTS province_topic_category (
  id INTEGER PRIMARY KEY,
  type TEXT NOT NULL,
  category TEXT NOT NULL,
  score INTEGER NOT NULL
);
-- organization_info：机关/单位；unit_address 用于地区模糊查
CREATE TABLE IF NOT EXISTS organization_info (
  id INTEGER PRIMARY KEY,
  unit_name TEXT,
  unit_address TEXT
);
-- person_info：人员与所属单位
CREATE TABLE IF NOT EXISTS person_info (
  id INTEGER PRIMARY KEY,
  org_id INTEGER,
  name TEXT
);
-- province_topic_info：省课题主表；create_time 存年份整数，供 BETWEEN 过滤
CREATE TABLE IF NOT EXISTS province_topic_info (
  id INTEGER PRIMARY KEY,
  name TEXT,
  header_id INTEGER,
  topic_id INTEGER,
  discipline_id INTEGER,
  org_id INTEGER,
  create_time INTEGER,
  start_time INTEGER,
  end_time INTEGER
);
-- publish_unit_info：出版单位
CREATE TABLE IF NOT EXISTS publish_unit_info (
  id INTEGER PRIMARY KEY,
  pub_unit_name TEXT,
  pub_unit_address TEXT
);
-- social_science_aware_level：省社科奖等级
CREATE TABLE IF NOT EXISTS social_science_aware_level (
  id INTEGER PRIMARY KEY,
  level TEXT,
  score INTEGER
);
-- social_science_group_type：社科团队类型
CREATE TABLE IF NOT EXISTS social_science_group_type (
  id INTEGER PRIMARY KEY,
  type_name TEXT
);
-- social_science_group_info：社科团队
CREATE TABLE IF NOT EXISTS social_science_group_info (
  id INTEGER PRIMARY KEY,
  org_id INTEGER,
  type_id INTEGER,
  person_id INTEGER,
  discipline_id INTEGER,
  group_name TEXT,
  start_time INTEGER,
  end_time INTEGER,
  other_type TEXT,
  score INTEGER
);
-- social_science_aware_info：省社科奖成果
CREATE TABLE IF NOT EXISTS social_science_aware_info (
  id INTEGER PRIMARY KEY,
  org_id INTEGER,
  pub_unit_id INTEGER,
  level_id INTEGER,
  person_id INTEGER,
  discipline_id INTEGER,
  obj_name TEXT,
  session INTEGER,
  year INTEGER
);
-- nat_social_science_group_type：国家社科奖类型
CREATE TABLE IF NOT EXISTS nat_social_science_group_type (
  id INTEGER PRIMARY KEY,
  type_name TEXT
);
-- nat_social_science_group_categorie：国家社科奖类别
CREATE TABLE IF NOT EXISTS nat_social_science_group_categorie (
  id INTEGER PRIMARY KEY,
  categorie_name TEXT
);
-- national_social_science_aware_info：国家社科奖信息；query_engine 当前查询此表
CREATE TABLE IF NOT EXISTS national_social_science_aware_info (
  id INTEGER PRIMARY KEY,
  org_id INTEGER,
  type_id INTEGER,
  categorie_id INTEGER,
  person_id INTEGER,
  discipline_id INTEGER,
  obj_name TEXT,
  score INTEGER,
  year INTEGER,
  start_time INTEGER,
  end_time INTEGER
);
"""
