agent补充完成：
完整流程为：大模型拆解用户需求，给大模型每张表在做什么决定使用哪些表，使用我们的tool从数据库中拿数据，将数据给大模型，大模型进行分析和报告。
给字典与 Tool 清单 → 缺参先追问 → 规划选 Tool 与参数 → 校验规划 → 执行 Tool 取数 → 把数据作证据交给模型分析与报告 → 输出。
1.现有数据整理，将以下信息给大模型便于大模型根据用户需求判断使用哪些表
mysql -u root -p 启动数据库

# **1.创建数据库SKLData**

CREATE DATABASE SKLData; 

使用SKLData

use SKLData;

# **2.创建各数据表**

**-- 学科信息表**
CREATE TABLE discipline_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学科ID，主键，自增',
    disci_name VARCHAR(255) NOT NULL COMMENT '学科名称'
);

**-- 省课题类别表**
CREATE TABLE province_topic_category (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '省课题类别ID，主键，自增',
    type VARCHAR(255) NOT NULL COMMENT '课题类型',
    category VARCHAR(255) NOT NULL COMMENT '课题类别',
    score INT NOT NULL COMMENT '权重分数'
);

**-- 机关单位信息表**
CREATE TABLE organization_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '机关单位ID，主键，自增',
    unit_name VARCHAR(255) NOT NULL COMMENT '单位名称',
    unit_address VARCHAR(255) NOT NULL COMMENT '单位地址'
);

**-- 人员信息表**
CREATE TABLE person_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '人员ID，主键，自增',
    org_id INT NOT NULL COMMENT '单位ID，外键关联organization_info表的id',
    name VARCHAR(255) NOT NULL COMMENT '人员姓名',
    FOREIGN KEY (org_id) REFERENCES organization_info(id)
);

**-- 省课题信息表**
CREATE TABLE province_topic_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '省课题信息ID，主键，自增',
    name VARCHAR(255) NOT NULL COMMENT '课题名称',
    header_id INT NOT NULL COMMENT '负责人ID，外键关联person_info表的id',
    topic_id INT NOT NULL COMMENT '课题类别ID，外键关联province_topic_category表的id',
    discipline_id INT NOT NULL COMMENT '学科ID，外键关联discipline_info表的id',
    org_id INT NOT NULL COMMENT '机关单位ID，外键关联organization_info表的id',
    create_time INT NOT NULL COMMENT '创建年份',
    start_time INT NOT NULL COMMENT '开始年份',
    end_time INT NOT NULL COMMENT '结束年份',
    FOREIGN KEY (header_id) REFERENCES person_info(id),
    FOREIGN KEY (topic_id) REFERENCES province_topic_category(id),
    FOREIGN KEY (discipline_id) REFERENCES discipline_info(id),
    FOREIGN KEY (org_id) REFERENCES organization_info(id)
);

**-- 出版单位信息表**
CREATE TABLE publish_unit_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '出版单位ID，主键，自增',
    pub_unit_name VARCHAR(255) COMMENT '单位名称',
    pub_unit_address VARCHAR(255) COMMENT '单位地址'
);

**-- 社科学术奖等级表**
CREATE TABLE social_science_aware_level (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科学术奖等级ID，主键，自增',
    level VARCHAR(50) COMMENT '获奖等级',
    score INT COMMENT '权重分数'
);

**-- 社科团队信息表**
CREATE TABLE social_science_group_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科团队ID，主键，自增',
    org_id INT COMMENT '单位ID',
    type_id INT COMMENT '团队类型ID',
    person_id INT COMMENT '负责人ID',
    discipline_id INT COMMENT '学科ID',
    group_name VARCHAR(255) COMMENT '团队名称',
    start_time INT COMMENT '开始时间，存储为整数',
    end_time INT COMMENT '结束时间，存储为整数',
    other_type VARCHAR(255) COMMENT '其他类型',
    score INT COMMENT '权重分数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社科团队信息表';

**-- 社科团队类型表**
CREATE TABLE social_science_group_type (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科团队类型ID，主键，自增',
    type_name VARCHAR(255) COMMENT '类型名称'
);

**-- 社科学术奖信息表**
CREATE TABLE social_science_aware_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科学术奖信息ID，主键，自增',
    org_id INT COMMENT '人员单位ID',
    pub_unit_id INT COMMENT '出版单位ID',
    level_id INT COMMENT '获奖等级ID',
    person_id INT COMMENT '负责人ID',
    discipline_id INT COMMENT '学科ID',
    obj_name VARCHAR(255) COMMENT '项目名称',
    session INT COMMENT '届次',
    year INT COMMENT '获奖年份'
);

**-- 创建国家社科奖信息表**
CREATE TABLE national_social_science_aware_info (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    org_id INT COMMENT '人员单位ID',
    type_id INT COMMENT '获奖类型ID',
    categorie_id INT COMMENT '获奖类别ID',
    person_id INT COMMENT '负责人ID',
    discipline_id INT COMMENT '学科ID',
    obj_name VARCHAR(255) COMMENT '项目名称',
    score INT COMMENT '分数',
    year INT COMMENT '获奖年份',
    start_time INT COMMENT '开始时间',
    end_time INT COMMENT '结束时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国家社科奖信息表';

**-- 创建国社科奖类型表**
CREATE TABLE nat_social_science_group_type (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    type_name VARCHAR(255) COMMENT '类型名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国社科奖类型表';

**-- 创建国社科奖类别表**
CREATE TABLE nat_social_science_group_categorie (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    categorie_name VARCHAR(255) COMMENT '类别名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国社科奖类别表';

# 3.设置外键关联

-- 省课题信息表关联学科信息表
ALTER TABLE province_topic_info ADD FOREIGN KEY (discipline_id) REFERENCES discipline_info(id);

-- 省课题信息表关联省课题类别表
ALTER TABLE province_topic_info ADD FOREIGN KEY (topic_id) REFERENCES province_topic_category(id);

-- 省课题信息表关联机关单位信息表
ALTER TABLE province_topic_info ADD FOREIGN KEY (org_id) REFERENCES organization_info(id);

-- 省课题信息表关联人员信息表
ALTER TABLE province_topic_info ADD FOREIGN KEY (header_id) REFERENCES person_info(id);



----------------------------------------------------------------------------------------------------------------------------------------------------

-- 人员信息表关联机关单位信息表
ALTER TABLE person_info ADD FOREIGN KEY (org_id) REFERENCES organization_info(id);



---------------------------------------------------------------------------------------------------------------------------------------------------------------------

-- 社科奖信息表关联机关单位信息表（人员单位id）
ALTER TABLE social_science_aware_info ADD FOREIGN KEY (org_id) REFERENCES organization_info(id);

-- 社科奖信息表关联出版单位信息表
ALTER TABLE social_science_aware_info ADD FOREIGN KEY (pub_unit_id) REFERENCES publish_unit_info(id);

-- 社科奖信息表关联社科奖等级表
ALTER TABLE social_science_aware_info ADD FOREIGN KEY (level_id) REFERENCES social_science_aware_level(id);

-- 社科奖信息表关联人员信息表
ALTER TABLE social_science_aware_info ADD FOREIGN KEY (person_id) REFERENCES person_info(id);

-- 社科奖信息表关联学科信息表
ALTER TABLE social_science_aware_info ADD FOREIGN KEY (discipline_id) REFERENCES discipline_info(id);



----------------------------------------------------------------------------------------------------------

-- 社科团队信息表关联机关单位信息表
ALTER TABLE social_science_group_info ADD FOREIGN KEY (org_id) REFERENCES organization_info(id);

-- 社科团队信息表关联社科团队类型表
ALTER TABLE social_science_group_info ADD FOREIGN KEY (type_id) REFERENCES social_science_group_type(id);

-- 社科团队信息表关联人员信息表
ALTER TABLE social_science_group_info ADD FOREIGN KEY (person_id) REFERENCES person_info(id);

-- 社科团队信息表关联学科信息表
ALTER TABLE social_science_group_info ADD FOREIGN KEY (discipline_id) REFERENCES discipline_info(id);



-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

-- 国家社科奖信息表关联机关单位信息表（人员单位id）
ALTER TABLE national_social_science_aware_info ADD FOREIGN KEY (org_id) REFERENCES organization_info(id);

-- 国家社科奖信息表关联国社科奖类型表
ALTER TABLE national_social_science_aware_info ADD FOREIGN KEY (type_id) REFERENCES nat_social_science_group_type(id);

-- 国家社科奖信息表关联国社科奖类别表
ALTER TABLE national_social_science_aware_info ADD FOREIGN KEY (categorie_id) REFERENCES nat_social_science_group_categorie(id);

-- 国家社科奖信息表关联人员信息表
ALTER TABLE national_social_science_aware_info ADD FOREIGN KEY (person_id) REFERENCES person_info(id);

-- 国家社科奖信息表关联学科信息表
ALTER TABLE national_social_science_aware_info ADD FOREIGN KEY (discipline_id) REFERENCES discipline_info(id);

# 4.设置外键可以为空

这样设置是因为有部分外键列在某些行的值是空的，防止数据导入的时候报错

**省课题信息表（province_topic_info）**

sql

```
ALTER TABLE province_topic_info MODIFY discipline_id INT NULL;
ALTER TABLE province_topic_info MODIFY topic_id INT NULL;
ALTER TABLE province_topic_info MODIFY org_id INT NULL;
ALTER TABLE province_topic_info MODIFY header_id INT NULL;
```

**人员信息表（person_info）**

sql

```
ALTER TABLE person_info MODIFY org_id INT NULL;
```

**社科奖信息表（social_science_aware_info）**

sql

```
ALTER TABLE social_science_aware_info MODIFY org_id INT NULL;
ALTER TABLE social_science_aware_info MODIFY pub_unit_id INT NULL;
ALTER TABLE social_science_aware_info MODIFY level_id INT NULL;
ALTER TABLE social_science_aware_info MODIFY person_id INT NULL;
ALTER TABLE social_science_aware_info MODIFY discipline_id INT NULL;
```

**社科团队信息表（social_science_group_info）**

sql

```
ALTER TABLE social_science_group_info MODIFY org_id INT NULL;
ALTER TABLE social_science_group_info MODIFY type_id INT NULL;
ALTER TABLE social_science_group_info MODIFY person_id INT NULL;
ALTER TABLE social_science_group_info MODIFY discipline_id INT NULL;
```

**国家社科奖信息表（national_social_science_aware_info）**

sql

```
ALTER TABLE national_social_science_aware_info MODIFY org_id INT NULL;
ALTER TABLE national_social_science_aware_info MODIFY type_id INT NULL;
ALTER TABLE national_social_science_aware_info MODIFY categorie_id INT NULL;
ALTER TABLE national_social_science_aware_info MODIFY person_id INT NULL;
ALTER TABLE national_social_science_aware_info MODIFY discipline_id INT NULL;
```
2.tool
明白了。您给出了完整的表结构，现在需要基于这些表，补上**Tool清单、缺参策略、校验规则**这三块。我直接按您的表结构来写。

---

## 一、Tool清单（基于您的表结构）

### 总览

| Tool名称 | 功能 | 对应主要表 |
|----------|------|-----------|
| `query_projects` | 查询省课题信息 | `province_topic_info` + 关联表 |
| `query_teams` | 查询社科团队信息 | `social_science_group_info` + 关联表 |
| `query_awards` | 查询社科/国社科奖项 | `social_science_aware_info`、`national_social_science_aware_info` |
| `query_organization` | 查询单位信息 | `organization_info` |
| `query_person` | 查询人员信息 | `person_info` |
| `web_search` | 联网搜索补充信息 | 无 |

---

### Tool 1：`query_projects`

| 项目 | 内容 |
|------|------|
| **功能** | 查询省课题信息 |
| **对应表** | `province_topic_info` + `discipline_info` + `province_topic_category` + `organization_info` + `person_info` |
| **必填参数** | 无（全部可选） |
| **可选参数** | `org_name`（单位名称）、`discipline`（学科）、`topic_type`（课题类型）、`header_name`（负责人）、`year_start`（开始年份）、`year_end`（结束年份） |
| **返回字段** | 课题名称、负责人、单位、学科、课题类别、创建年份、开始年份、结束年份 |
| **返回格式** | `{"success": true, "data": [...], "count": N}` |

---

### Tool 2：`query_teams`

| 项目 | 内容 |
|------|------|
| **功能** | 查询社科团队信息 |
| **对应表** | `social_science_group_info` + `organization_info` + `discipline_info` + `social_science_group_type` + `person_info` |
| **必填参数** | 无 |
| **可选参数** | `org_name`、`group_name`、`type_name`（团队类型）、`discipline`、`header_name` |
| **返回字段** | 团队名称、单位、类型、负责人、学科、开始时间、结束时间 |

---

### Tool 3：`query_awards`

| 项目 | 内容 |
|------|------|
| **功能** | 查询社科奖项（含省社科奖、国社科奖） |
| **对应表** | `social_science_aware_info`、`national_social_science_aware_info` + 关联表 |
| **必填参数** | 无 |
| **可选参数** | `award_type`（省奖/国奖）、`org_name`、`person_name`、`discipline`、`year`、`level`（等级） |
| **返回字段** | 项目名称、负责人、单位、学科、获奖等级、获奖年份 |

---

### Tool 4：`query_organization`

| 项目 | 内容 |
|------|------|
| **功能** | 查询单位信息 |
| **对应表** | `organization_info` |
| **必填参数** | 无 |
| **可选参数** | `unit_name`（单位名称，支持模糊匹配） |
| **返回字段** | 单位ID、单位名称、单位地址 |

---

### Tool 5：`query_person`

| 项目 | 内容 |
|------|------|
| **功能** | 查询人员信息 |
| **对应表** | `person_info` + `organization_info` |
| **必填参数** | 无 |
| **可选参数** | `name`、`org_name` |
| **返回字段** | 人员ID、姓名、所属单位 |

---

### Tool 6：`web_search`

| 项目 | 内容 |
|------|------|
| **功能** | 联网搜索（实时政策、新闻等） |
| **必填参数** | `query` |
| **可选参数** | `result_limit`（默认5） |
| **返回格式** | `{"success": true, "results": [{"title":"...", "snippet":"..."}]}` |

---

## 二、缺参策略

### 2.1 槽位定义

| 槽位 | 对应Tool参数 | 示例值 | 是否必须 |
|------|-------------|--------|---------|
| `org_name` | 单位名称 | 社科院、川大 | 否 |
| `person_name` | 人员姓名 | 张三 | 否 |
| `discipline` | 学科 | 经济学、法学 | 否 |
| `topic_type` | 课题类型 | 重点项目、一般项目 | 否 |
| `year` | 年份 | 2024、2020-2024 | 否 |
| `award_type` | 奖项类型 | 省社科奖、国社科奖 | 否 |

### 2.2 追问规则

**规则**：只有用户意图完全无法确定时，才追问。因为所有参数都是可选的，系统可以用默认行为处理。

**需要追问的唯一情况**：

| 场景 | 追问话术 |
|------|---------|
| 用户只说“查询”或“帮我查一下”，没有任何具体指向 | “请问您想查询项目、团队、奖项、单位还是人员信息？” |

**不追问的情况**（走默认行为）：
- 只给单位名 → 查该单位的所有项目、团队、奖项
- 只给年份 → 查该年份的所有数据
- 只给学科 → 查该学科的所有数据

### 2.3 多轮槽位合并

| 规则 | 说明 |
|------|------|
| 增量叠加 | 新消息补充新的条件，不覆盖旧的 |
| 同参数覆盖 | 同一参数出现新值时，覆盖旧值 |
| 重置命令 | 用户说“重置”或“新查询”，清空所有槽位 |

### 2.4 缺参响应格式

```json
{
  "status": "need_clarify",
  "message": "请问您想查询项目、团队、奖项、单位还是人员信息？"
}
```

---

## 三、校验规则

### 3.1 Tool名白名单

```
允许的Tool名称：
- query_projects
- query_teams
- query_awards
- query_organization
- query_person
- web_search
```

### 3.2 参数类型校验

| 参数 | 类型 | 校验规则 |
|------|------|---------|
| `org_name` | string | 长度1-100 |
| `person_name` | string | 长度1-50 |
| `discipline` | string | 长度1-50 |
| `year` | int 或 string | 支持整数(2024)或区间格式(2020-2024)，范围2000-当前年 |
| `result_limit` | int | 1-20 |

### 3.3 运行时校验与重试

| 失败类型 | 重试次数 | 降级方案 |
|---------|---------|---------|
| 数据库连接失败 | 1次 | 返回错误，提示稍后重试 |
| SQL超时（>5秒） | 0次 | 返回错误，提示缩小查询范围 |
| 空结果 | 0次 | 正常返回空数组 |
| web_search超时 | 1次 | 返回部分结果或跳过 |

### 3.4 校验失败返回格式

```json
{
  "status": "error",
  "error_type": "invalid_tool" | "invalid_param" | "db_error",
  "message": "具体错误信息"
}
```

2.分析和报告设计提示词
分析：
你是一位社科数据分析专家。请根据以下信息进行分析。

【用户问题】
{user_query}

【本地数据查询结果】
{db_search_results}

【联网搜索结果】（如有）
{web_search_results}

请完成以下分析任务：
1. 总结数据中的核心发现（3-5点）
2. 识别数据中的趋势或规律（如年份变化、机构分布等）
3. 指出数据中的异常或亮点
4. 如果涉及对比，给出对比结论

输出要求：
- 使用中文
- 分点列出，每点不超过2句话
- 不要编造数据中没有的信息
- 不要给出建议或对策（只做分析）

报告：你是一位社科数据分析专家。请根据以下信息生成一份完整的分析报告。

【用户问题】
{user_query}

【本地数据】
{db_search_results}

【联网搜索结果】（如有）
{web_search_results}

【分析结论】（如有）
{analysis_result}

请按以下Markdown模板生成报告：

# 【报告标题】（根据用户问题自动生成）

**生成时间**：{当前日期}
**数据来源**：本地社科数据库 / 联网搜索

## 一、摘要

用2-3句话概括本报告的核心发现。

## 二、数据概览

| 指标 | 数值 |
|------|------|
| 项目总数 | XX个 |
| 涉及单位数 | XX个 |
| 主要学科 | XX、XX |
| 时间范围 | XXXX年 |

## 三、分析结论

### 3.1 核心发现
（分点列出）

### 3.2 趋势分析
（如有趋势数据）

### 3.3 机构/人员分布
（如有分布数据）

## 四、数据来源说明

- 本地数据库：{表名列表}
- 联网搜索：{搜索关键词}

---
报告结束

3.并行保留：/api/agent/chat（规则+固定顺序）和 /api/agent/chat/v2（LLM规划+动态选Tool）并存，不替换不合并。

4.规划输出是一次JSON，不搞多轮ReAct

5.支持并行，由规划LLM决定 execution_mode（parallel/sequential）。并行时各Tool独立超时，结果独立合并。默认顺序保守执行。

6.必须对齐文档示例，返回结构化JSON（title+snippet+url）。如果百炼返回纯文本，写解析函数转换。格式统一为 {"success": true, "results": [...]}。

7.硬上限100条，默认20条，默认近3年。超限自动截断并提示。配置文件集中管理。

8.开发以SQLite为准，表结构按DDL建。

9.
已阅读 `E:\计设\agent2\agent补充完成：.md` 与 `E:\计设\agent2\实现一个agent，该agent需要完成的任务：.md`（`E:\计设\agent\...` 该路径在本机未找到）。下面是对 **问题 1（规划阶段）** 的定稿：**一次规划、单份 JSON、可白名单校验、并行语义写死**，并与文档中的 6 个 Tool 及参数对齐。

---

## 1. 语义约定（写校验代码时按此实现）

| 字段 | 含义 |
|------|------|
| `schema_version` | 协议版本，便于以后演进 |
| `intent` | 一句话概括用户目标（日志/审计） |
| `reason` | 可选，说明为何选这些 Tool、并行还是顺序（调试） |
| `execution_mode` | **`parallel`**：对 `steps` 中每一步**并发**执行，**忽略**步骤顺序，仅作展示；**`sequential`**：严格按 `steps` **数组顺序**执行，上一步结束再执行下一步 |
| `steps` | 每一步包含稳定 `id`、`tool`、`arguments`、可选 `reason` |

**约束建议：**

- `parallel` 时：各步**不得**隐含“后一步依赖前一步结果”；若存在依赖，必须设为 `sequential` 或拆成两轮规划。
- `steps` 内 `id` 全局唯一，建议 `step_1`、`step_2` 或 UUID 短码。
- Tool 名仅允许文档白名单：`query_projects` | `query_teams` | `query_awards` | `query_organization` | `query_person` | `web_search`。

---

## 2. 完整示例（含 parallel + 多 Tool）

```json
{
  "schema_version": "1.0",
  "intent": "对比某单位近年的省课题与社科奖项情况，并补充一条公开政策背景",
  "reason": "课题与奖项分属不同表，彼此无依赖，可并行取数；联网搜索独立，可并行。",
  "execution_mode": "parallel",
  "steps": [
    {
      "id": "step_projects",
      "tool": "query_projects",
      "arguments": {
        "org_name": "四川省社会科学院",
        "discipline": "法学",
        "year_start": 2020,
        "year_end": 2024
      },
      "reason": "拉取该院法学方向近年省课题"
    },
    {
      "id": "step_awards",
      "tool": "query_awards",
      "arguments": {
        "award_type": "省社科奖",
        "org_name": "四川省社会科学院",
        "year": 2023
      },
      "reason": "拉取该院省社科奖记录"
    },
    {
      "id": "step_web",
      "tool": "web_search",
      "arguments": {
        "query": "四川省哲学社会科学规划 2024 课题 通知",
        "result_limit": 5
      },
      "reason": "补充一条可引用的公开政策或通知线索"
    }
  ]
}
```

**sequential 示例（强调顺序，例如先查人再查项目——若你业务里需要链式）：**

```json
{
  "schema_version": "1.0",
  "intent": "先定位人员所属单位，再查该单位课题",
  "reason": "后一步依赖前一步解析出的单位信息时应用 sequential；此处仅作格式示例。",
  "execution_mode": "sequential",
  "steps": [
    {
      "id": "step_person",
      "tool": "query_person",
      "arguments": { "name": "王竹" },
      "reason": "先解析人员与单位"
    },
    {
      "id": "step_projects",
      "tool": "query_projects",
      "arguments": { "org_name": "四川省文物考古研究院", "year_start": 2018, "year_end": 2024 },
      "reason": "再按单位查课题"
    }
  ]
}
```

---

## 3. JSON Schema（校验用，可与 Ajv/Pydantic 对齐）

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://example.local/schemas/agent-plan-v1.json",
  "title": "AgentToolPlan",
  "type": "object",
  "additionalProperties": false,
  "required": ["schema_version", "intent", "execution_mode", "steps"],
  "properties": {
    "schema_version": { "const": "1.0" },
    "intent": { "type": "string", "minLength": 1, "maxLength": 500 },
    "reason": { "type": "string", "maxLength": 2000 },
    "execution_mode": { "type": "string", "enum": ["parallel", "sequential"] },
    "steps": {
      "type": "array",
      "minItems": 1,
      "items": { "$ref": "#/$defs/PlanStep" }
    }
  },
  "$defs": {
    "PlanStep": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "tool", "arguments"],
      "properties": {
        "id": {
          "type": "string",
          "pattern": "^[a-zA-Z0-9_-]{1,64}$"
        },
        "tool": {
          "type": "string",
          "enum": [
            "query_projects",
            "query_teams",
            "query_awards",
            "query_organization",
            "query_person",
            "web_search"
          ]
        },
        "arguments": { "$ref": "#/$defs/ToolArguments" },
        "reason": { "type": "string", "maxLength": 1000 }
      }
    },
    "ToolArguments": {
      "oneOf": [
        { "$ref": "#/$defs/ArgsQueryProjects" },
        { "$ref": "#/$defs/ArgsQueryTeams" },
        { "$ref": "#/$defs/ArgsQueryAwards" },
        { "$ref": "#/$defs/ArgsQueryOrganization" },
        { "$ref": "#/$defs/ArgsQueryPerson" },
        { "$ref": "#/$defs/ArgsWebSearch" }
      ],
      "discriminator": {
        "propertyName": "_tool",
        "mapping": {
          "query_projects": "#/$defs/ArgsQueryProjects",
          "query_teams": "#/$defs/ArgsQueryTeams",
          "query_awards": "#/$defs/ArgsQueryAwards",
          "query_organization": "#/$defs/ArgsQueryOrganization",
          "query_person": "#/$defs/ArgsQueryPerson",
          "web_search": "#/$defs/ArgsWebSearch"
        }
      }
    },
    "ArgsQueryProjects": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "_tool": { "const": "query_projects" },
        "org_name": { "type": "string", "maxLength": 100 },
        "discipline": { "type": "string", "maxLength": 50 },
        "topic_type": { "type": "string", "maxLength": 100 },
        "header_name": { "type": "string", "maxLength": 50 },
        "year_start": { "type": "integer", "minimum": 2000, "maximum": 2100 },
        "year_end": { "type": "integer", "minimum": 2000, "maximum": 2100 }
      }
    },
    "ArgsQueryTeams": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "_tool": { "const": "query_teams" },
        "org_name": { "type": "string", "maxLength": 100 },
        "group_name": { "type": "string", "maxLength": 255 },
        "type_name": { "type": "string", "maxLength": 255 },
        "discipline": { "type": "string", "maxLength": 50 },
        "header_name": { "type": "string", "maxLength": 50 }
      }
    },
    "ArgsQueryAwards": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "_tool": { "const": "query_awards" },
        "award_type": { "type": "string", "maxLength": 50 },
        "org_name": { "type": "string", "maxLength": 100 },
        "person_name": { "type": "string", "maxLength": 50 },
        "discipline": { "type": "string", "maxLength": 50 },
        "year": {
          "oneOf": [
            { "type": "integer", "minimum": 2000, "maximum": 2100 },
            { "type": "string", "pattern": "^\\d{4}(-\\d{4})?$" }
          ]
        },
        "level": { "type": "string", "maxLength": 50 }
      }
    },
    "ArgsQueryOrganization": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "_tool": { "const": "query_organization" },
        "unit_name": { "type": "string", "maxLength": 100 }
      }
    },
    "ArgsQueryPerson": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "_tool": { "const": "query_person" },
        "name": { "type": "string", "maxLength": 50 },
        "org_name": { "type": "string", "maxLength": 100 }
      }
    },
    "ArgsWebSearch": {
      "type": "object",
      "additionalProperties": false,
      "required": ["query"],
      "properties": {
        "_tool": { "const": "web_search" },
        "query": { "type": "string", "minLength": 1, "maxLength": 500 },
        "result_limit": { "type": "integer", "minimum": 1, "maximum": 20 }
      }
    }
  }
}
```

**说明：**  
- Schema 里用 **`arguments._tool`** 与外层 **`steps[].tool` 双重一致** 时，校验最省事；若你希望 JSON 更干净，可以去掉 `_tool`，在代码里用 `steps[i].tool` 选 `oneOf` 分支做参数校验（推荐生产里用这种，少一层冗余）。  
- `year` 支持整数或 `"2020-2024"` 字符串，与补充文档一致。

---

## 4. 与「追问」分支的关系（避免混在一个结构里）

文档里 **缺参追问** 是 `status: need_clarify` 的另一种响应。若希望**同一次 LLM 只输出一种**，建议规划阶段二选一：

- **执行取数**：上文 `AgentToolPlan`（`steps` 非空）。  
- **追问用户**：例如 `{ "schema_version":"1.0", "status":"need_clarify", "message":"..." }`，不走 `execution_mode`。

若你需要「单一 envelope」把两者合并成一个顶层 `oneOf`，可以再单独定一版，当前已满足「问题 1」对 **Tool + 参数 + execution_mode + reason/intent** 的定稿。

---

**小结：** 规划 LLM 应**只输出一段合法 JSON**；`execution_mode` 为 **`parallel` 或 `sequential` 两枚举**；`steps[]` 内写明 **`tool` + `arguments`**，并固定 **`intent`（必选）** 与 **`reason`（可选，全局或逐步）**，即可据此写白名单与类型校验。

10.

下面按你在补充文档里的设定（**v1 = 规则 + 固定顺序**，**v2 = LLM 规划 + 动态选 Tool**，两接口并存）整理 **`POST /api/agent/chat/v2` 的 HTTP 契约**：请求/响应字段表、与 v1 的差异、**共用 `session_id`** 约定，以及 **need_clarify / invalid_tool** 等与前端展示方式（并补全 v2 示例）。

---

## 1. 与 v1 的差异（概念层）

| 维度 | v1 `POST /api/agent/chat` | v2 `POST /api/agent/chat/v2` |
|------|---------------------------|------------------------------|
| 取数策略 | 规则引擎 + **固定 Tool 顺序** | **规划 LLM 输出 JSON** → 校验 → 按 `execution_mode` 执行 |
| 响应是否暴露规划 | 通常不暴露「规划 JSON」 | 建议 **可选** 返回 `plan`（调试/验收），生产可关 |
| 并行 | 由后端固定逻辑决定 | 由规划里的 **`execution_mode`**（`parallel` / `sequential`）决定 |
| 校验失败 | 同类错误码 | 多一类 **`invalid_plan`**（JSON Schema / 业务规则不通过） |
| 会话 | 同一 Agent 服务、同一套内存会话 | **与 v1 共用 `session_id` 机制**（见下） |

**`session_id` 是否共用：**  
- **建议共用**：同一 Python 服务（如 5001）内，**v1 与 v2 使用同一套 `session_id → 对话历史` 字典。  
- **语义**：`session_id` 标识多轮对话状态；**不区分**「这是 v1 还是 v2 开的会话」——用户从 v1 页切到 v2 页只要带同一 `session_id`，历史仍可复用（若你产品上不推荐混用，可在前端为两路由各生成前缀，但后端无需两套 ID）。

---

## 2. `POST /api/agent/chat/v2` — 请求体字段表

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | string | 是 | 用户本轮输入（含回答追问时的补充） |
| `session_id` | string \| null | 否 | 首次不传或 `null`：服务端创建新会话并在响应里返回；后续轮次原样带回 |
| `client_request_id` | string | 否 | 幂等/排错用（前端 UUID），服务端可原样回显 |
| `options` | object | 否 | 见下表 |

**`options`（可选，均可给默认值）**

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `include_plan_in_response` | boolean | `false` | 是否在响应中带解析后的规划（调试用） |
| `stream` | boolean | `false` | 是否 SSE 流式返回报告（若你实现流式） |
| `locale` | string | `"zh-CN"` | 文案语言（追问话术等） |

**与 v1 请求体关系：**  
- v1 若仅有 `message` + `session_id`，**v2 是超集**：多 `options` / `client_request_id`；**相同字段语义一致**，便于前端封装一层 `postChat({ version: 'v1'|'v2', ... })`。

---

## 3. `POST /api/agent/chat/v2` — 响应体（统一信封）

所有 JSON 响应建议**顶层结构一致**，用 `status` 区分分支（便于前端 `switch`）。

| 字段 | 类型 | 何时出现 | 说明 |
|------|------|----------|------|
| `status` | string | 始终 | `success` \| `need_clarify` \| `error` |
| `session_id` | string | 建议始终 | 本轮会话 ID；首次由服务端生成 |
| `client_request_id` | string | 若请求带了 | 原样回显 |
| `schema_version` | string | 可选 | 如 `"1.0"`，与规划 JSON 协议对齐 |

以下为各 `status` 的**专用字段**。

---

### 3.1 `status: "success"`（取数 + 报告完成）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `report` | string | 是 | Markdown 报告正文 |
| `report_meta` | object | 否 | 如生成时间、模型名、是否含联网等 |
| `tool_trace` | array | 否 | 每步执行摘要（见下），用于前端「展开证据」 |
| `plan` | object | 否 | **仅当** `options.include_plan_in_response === true`：与规划 LLM 对齐的结构化规划 |

**`tool_trace[]` 建议元素：**

| 字段 | 说明 |
|------|------|
| `step_id` | 与规划里 `steps[].id` 一致 |
| `tool` | 工具名 |
| `ok` | 是否成功 |
| `execution_mode` | 本轮实际 `parallel` / `sequential`（与规划一致） |
| `summary` | 人类可读一行摘要 |
| `result_ref` | 可选：证据在服务端缓存的 key 或截断预览，避免响应过大 |

**示例（补全 v2，含可选 `plan`）：**

```json
{
  "status": "success",
  "schema_version": "1.0",
  "session_id": "sess_8f3c2a1b",
  "client_request_id": "req_uuid_xxx",
  "report": "# 【报告标题】\n\n...",
  "report_meta": {
    "generated_at": "2026-04-18T12:00:00+08:00",
    "model": "deepseek-v3.2",
    "web_search_used": true
  },
  "tool_trace": [
    {
      "step_id": "step_projects",
      "tool": "query_projects",
      "ok": true,
      "execution_mode": "parallel",
      "summary": "返回 12 条课题记录（已截断至配置上限）"
    },
    {
      "step_id": "step_awards",
      "tool": "query_awards",
      "ok": true,
      "execution_mode": "parallel",
      "summary": "返回 3 条奖项记录"
    }
  ],
  "plan": {
    "schema_version": "1.0",
    "intent": "对比某单位课题与奖项并补充政策背景",
    "execution_mode": "parallel",
    "steps": [
      {
        "id": "step_projects",
        "tool": "query_projects",
        "arguments": { "org_name": "四川省社会科学院", "year_start": 2020, "year_end": 2024 },
        "reason": "拉取省课题"
      }
    ]
  }
}
```

---

### 3.2 `status: "need_clarify"`（追问，与文档对齐并补全 v2）

与补充文档一致，并**显式带上 `session_id`**，便于前端继续同会话 POST。

```json
{
  "status": "need_clarify",
  "schema_version": "1.0",
  "session_id": "sess_8f3c2a1b",
  "client_request_id": "req_uuid_xxx",
  "message": "请问您想查询项目、团队、奖项、单位还是人员信息？",
  "clarify": {
    "kind": "ambiguous_intent",
    "choices": [
      { "id": "projects", "label": "省课题" },
      { "id": "teams", "label": "社科团队" },
      { "id": "awards", "label": "奖项" },
      { "id": "org", "label": "单位" },
      { "id": "person", "label": "人员" }
    ]
  }
}
```

| 字段 | 说明 |
|------|------|
| `message` | 主文案：对话框里展示为 **助手气泡** |
| `clarify` | **可选**；无则仅展示 `message`；有则可用快捷按钮（`choices`）减少打字 |

**前端展示建议：**  
- 主区域：显示 `message`。  
- 若有 `clarify.choices`：渲染一排 **Chip/按钮**，点击后把对应 `label` 或固定话术拼进下一轮 `message` 再请求。  
- **必须**保留并回传 **`session_id`**。

---

### 3.3 `status: "error"`（规划/工具/DB/LLM 等）

在原文档的 `error_type` 上，为 v2 **增加 `invalid_plan`**。

```json
{
  "status": "error",
  "schema_version": "1.0",
  "session_id": "sess_8f3c2a1b",
  "client_request_id": "req_uuid_xxx",
  "error_type": "invalid_plan",
  "message": "规划 JSON 无法通过校验：steps[0].tool 不在白名单",
  "details": {
    "path": "steps[0].tool",
    "constraint": "enum"
  }
}
```

**`error_type` 枚举建议：**

| 值 | 含义 | 前端提示 |
|----|------|----------|
| `invalid_plan` | 规划 JSON Schema / 业务校验失败 | 「规划格式无效，已记录；请简化问题重试」；**不向用户展开 JSON** |
| `invalid_tool` | 工具名不在白名单或路由失败 | 「内部工具配置错误」或「请求无法执行」（对内网演示可简短） |
| `invalid_param` | 参数类型/范围不合法 | 「查询条件不合法：…」展示 `message` |
| `llm_error` | 规划模型或报告模型调用失败 | 「模型暂时不可用，请稍后重试」 |
| `db_error` | SQLite/MySQL 错误 | 「数据暂不可用」；可带 `retryable: true` |
| `timeout` | 工具或整体超时 | 「查询超时，请缩小范围」 |
| `rate_limited` | 若以后有限流 | 友好限流文案 |

**前端展示建议：**  
- 统一用 **非阻塞 Toast / Alert** 显示 `message`；对话区可追加一条 **系统错误气泡**（红色或警告色）。  
- `details` **默认不展示**给最终用户；开发模式或「复制错误信息」时可序列化。  
- **`session_id` 仍返回**：用户可继续发新消息，不必强制新开会话。

---

## 4. v1 响应（对照，不写死实现细节）

为对齐「差异」说明，v1 成功响应可与 v2 **同形**（仅通常无 `plan`、`tool_trace` 较简或固定），例如同样：

- `status`, `session_id`, `report`, `report_meta?`

这样前端可共用 **`AgentChatResponse`** 类型，`plan` / `tool_trace` 为可选字段即可。

---

## 5. 小结表

| 项目 | 约定 |
|------|------|
| **v2 请求** | `message` + 可选 `session_id` + 可选 `options`（含是否返回 `plan`） |
| **v2 成功** | `report` + 建议 `tool_trace`；`plan` 仅调试开启 |
| **追问** | `need_clarify` + `message` + 可选 `clarify.choices`；**必须带 `session_id`** |
| **错误** | `error` + `error_type`（含 **`invalid_plan`**）+ `message` + 可选 `details` |
| **session** | **v1 / v2 共用同一 `session_id` 语义**（同一服务内存字典） |

若你下一步要落代码，可以把 **`include_plan_in_response`** 与 **`stream`** 是否实现写进 `README` 或配置，避免前端误以为始终有 `plan` 或流式。需要的话我可以按你选的框架（Flask/FastAPI）直接写出 Pydantic 模型与 OpenAPI 片段。

11.下面补一条**可实现的规格**，把「sequential + 后一步依赖前一步」说清楚；否则链式 Tool 只能依赖人肉多轮，无法自动化。

---

## 1. 结论（先给答案）

| 方案 | 是否采用 | 说明 |
|------|----------|------|
| **`arguments` 内用 `{{step_id.xxx}}` 占位** | **可作为受支持的一种** | 必须限定：**仅出现在字符串类型的参数值里**，且 `step_id` 与规划里 `steps[].id` 一致，解析在**服务端执行前**完成。 |
| **结构化绑定（推荐与占位二选一或并存）** | **推荐为主** | 用显式对象描述「从哪一步、哪条路径取值」，便于校验与报错，不依赖字符串拼接。 |
| **只允许两轮规划（第一轮查人、第二轮用户再说单位）** | **不作为唯一方式** | 可作为**降级/产品策略**（例如依赖解析失败时追问用户），但**不应**作为链式取数的唯一机制，否则无法实现「一次用户输入 → 自动链式」。 |

**落地建议：** 服务端同时支持 **(A) 结构化绑定** 与 **(B) 字符串模板**；校验器先归一化成同一内部表示再执行。LLM 提示词里优先教 **结构化绑定**，减少幻觉。

---

## 2. 执行期上下文（每步结束后必有）

每步 Tool 返回并经服务端**规范化**后，写入执行上下文：

```text
context[step_id] = <NormalizedToolResult>
```

`NormalizedToolResult` 建议统一外形（便于写 `path`）：

```json
{
  "tool": "query_person",
  "ok": true,
  "data": {
    "rows": [ { "person_id": 1, "name": "王竹", "org_id": 203, "unit_name": "四川省文物考古研究院" } ],
    "count": 1
  },
  "error": null
}
```

- 失败时：`ok: false`，`error` 有码与文案；**后续若引用该步**，按 §5 处理。
- **路径表达式**默认指向 **`data` 子树**（见下）。

---

## 3. 结构化绑定（推荐）

在某一 `step` 上，参数除字面量外，允许 **绑定对象**（示例字段名用 `from_context`，你可改成 `bind`，但全项目统一即可）：

```json
{
  "id": "step_projects",
  "tool": "query_projects",
  "arguments": {
    "org_name": {
      "from_context": { "step": "step_person", "path": "$.rows[0].unit_name" }
    },
    "year_start": 2018,
    "year_end": 2024
  },
  "reason": "用上一步解析出的单位名称查课题"
}
```

**字段约定：**

| 字段 | 含义 |
|------|------|
| `from_context.step` | 必须是**当前计划中本步之前**已出现的某个 `steps[].id` |
| `from_context.path` | **JSON Pointer**（RFC 6901）或 **JMESPath** 二选一；实现里**定死一种**（建议 JSON Pointer，子集即可） |

**JSON Pointer 示例（相对 `data`）：**

- `/rows/0/unit_name` → 第一个单位的名称  
- `/rows/0/org_id` → 第一个 org_id  

若你希望指针从根写：`/data/rows/0/unit_name`，也应在规格里**写死一种**，不要混用。

**校验规则：**

- `sequential` 时：引用只允许指向前序 step；**禁止**引用后续或 `parallel` 同批次里「未定义顺序」的互相引用（并行步之间**禁止** `from_context` 互引用）。
- `parallel`：**禁止**任何 `from_context`（或模板引用），否则视为 `invalid_plan`。

---

## 4. 字符串占位（与文档里「{{step_person.xxx}}」对齐）

允许**仅**在**字符串类型**的参数值中使用占位，语法建议定死为：

```text
{{step_id:/json/pointer}}
```

示例：

```json
"arguments": {
  "org_name": "{{step_person:/rows/0/unit_name}}"
}
```

- `step_id` 与 `steps[].id` 一致。  
- 冒号后为 JSON Pointer（相对 `data`），与 §3 一致。  
- **禁止**在 `{{ }}` 外混用未转义的大括号；解析失败 → `invalid_plan`。

**解析顺序：** 先展开所有占位 → 再做强类型校验（如 `year` 为 int）。

---

## 5. 依赖失败时（空结果、多行、解析失败）

| 情况 | 建议行为 |
|------|----------|
| 被引用步 `ok: false` | `status: error`，`error_type: "dependency_failed"`，`message` 说明哪一步失败。 |
| 路径解析不到（`null` / 缺字段） | `error_type: "invalid_binding"` 或 `dependency_empty`。 |
| 引用步成功但 `rows` 为空 | 同上或 `need_clarify`：若业务允许，可带 `message`「未找到人员，请提供更具体姓名或单位」。 |
| `rows` 多条且未指定取哪条 | 规格上应要求规划要么 **先限缩查询**，要么显式绑定 `/rows/0/...`；若仍多条，可实现为 **`need_clarify`**（请选择具体一条）或 **错误**（由产品选）。 |

---

## 6. 与「两轮对话」的关系（规格位置）

- **自动链式**：单次用户请求 → 一次规划 → `sequential` + §3/§4 → 执行器按序跑完。  
- **两轮对话**：当 **绑定无法完成** 或 **意图仍空** 时，走已有的 `need_clarify`；用户第二轮 `message` 带上槽位，**同一 `session_id`** 再规划。这是**兜底**，不是唯一传参方式。

---

## 7. 给实现者的一行实现要点

1. 只对 `execution_mode === "sequential"` 的 plan 做 `from_context` / `{{...}}` 解析。  
2. 解析在**每步调用 Tool 之前**执行，得到**纯字面量** `arguments` 再调白名单校验与 Tool。  
3. 在 OpenAPI/内部文档里列出每个 Tool 的 **`data` 结构**（至少 `rows[]` 字段名），否则 LLM 不知道能绑什么。

这样「后一步用前一步结果」有**唯一、可校验**的传参方式；`{{step_person.xxx}}` 若采用，建议统一成 **`{{step_id:/rows/0/unit_name}}`** 这种**带 step_id + JSON Pointer** 的形式，避免歧义。若你希望完全禁止花括号模板、只保留 `from_context`，也可以在实现里关掉 §4，只实现 §3，规格仍然自洽。

12.
下面给一份**可直接写进 `config.example.yaml` + `.env.example` 的「集中管理」清单**：统一前缀、环境变量名、`config.yaml` 键路径、默认值与说明。实现时约定：**YAML 为默认值，环境变量覆盖同名逻辑键**（override）。

---

## 1. 命名约定

| 约定 | 说明 |
|------|------|
| **前缀** | 统一使用 `AGENT_`（若与现有系统冲突可改为 `SKL_AGENT_`，全文一致即可）。 |
| **环境变量** | 大写 + 下划线；嵌套键用双下划线 `__`，例如 `AGENT_DB__PATH`。 |
| **YAML** | 小驼峰或 `snake_case` 二选一；下表用 **`snake_case`** 与 Python 常见风格一致。 |
| **覆盖规则** | `AGENT_DB__PATH` → `db.path`；列表/复杂结构优先 YAML，env 只覆盖标量。 |

---

## 2. 总览表（检索 / 数据库 / 时间窗 / 限流）

| 含义 | 默认值（建议） | `config.yaml` 键（嵌套） | 环境变量（覆盖） |
|------|----------------|--------------------------|------------------|
| 查询结果硬上限（条） | `100` | `query.hard_max_rows` | `AGENT_QUERY__HARD_MAX_ROWS` |
| 默认返回条数 | `20` | `query.default_limit` | `AGENT_QUERY__DEFAULT_LIMIT` |
| 默认「近年」窗口（年数，含当前年向前推） | `3` | `query.default_recent_years` | `AGENT_QUERY__DEFAULT_RECENT_YEARS` |
| 年份合法范围下限 | `2000` | `query.year_min` | `AGENT_QUERY__YEAR_MIN` |
| 年份合法范围上限 | 当前年（运行时注入） | `query.year_max` | `AGENT_QUERY__YEAR_MAX`（可选固定，否则代码用 `date.today().year`） |
| 超限时的策略文案 key 或模板 | （实现定） | `query.truncation_notice_template` | `AGENT_QUERY__TRUNCATION_NOTICE_TEMPLATE` |
| `web_search` 默认 `result_limit` | `5` | `web_search.default_result_limit` | `AGENT_WEB_SEARCH__DEFAULT_RESULT_LIMIT` |
| `web_search` 最大 `result_limit` | `20` | `web_search.max_result_limit` | `AGENT_WEB_SEARCH__MAX_RESULT_LIMIT` |

**说明：** 「默认近 3 年」在查询层建议落成 **`default_recent_years: 3`**；当用户未给 `year_start/year_end/year` 时，由服务计算例如 `[current_year - 2, current_year]`（共 3 个日历年）或按你业务定义「滑动 36 个月」——**二选一定死在代码 + 注释**，配置只存**年数**。

---

## 3. 数据库与数据路径

| 含义 | 默认值（建议） | `config.yaml` 键 | 环境变量 |
|------|----------------|------------------|----------|
| SQLite 文件路径（开发） | `./data/skl_data.sqlite` | `db.sqlite_path` | `AGENT_DB__SQLITE_PATH` |
| 连接池 / 单次查询超时（秒） | `5` | `db.query_timeout_sec` | `AGENT_DB__QUERY_TIMEOUT_SEC` |
| Excel 导入目录（启动导入） | （你的盘符路径） | `data.excel_import_dir` | `AGENT_DATA__EXCEL_IMPORT_DIR` |
| 库已存在则跳过导入 | `true` | `data.skip_import_if_db_exists` | `AGENT_DATA__SKIP_IMPORT_IF_DB_EXISTS` |
| MySQL DSN（后续生产） | 空 | `db.mysql_dsn` | `AGENT_DB__MYSQL_DSN` |

---

## 4. LLM（规划 + 报告 + 联网）

| 含义 | 默认值（建议） | `config.yaml` 键 | 环境变量 |
|------|----------------|------------------|----------|
| API Key | 空（必填） | `llm.api_key` | **`ALIYUN_API_KEY`**（文档已有，**建议保留此名**，另可加 `AGENT_LLM__API_KEY` 作为别名） |
| Base URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `llm.base_url` | `AGENT_LLM__BASE_URL` |
| 规划模型 | `deepseek-v3.2` | `llm.planner_model` | `AGENT_LLM__PLANNER_MODEL` |
| 报告模型 | `deepseek-v3.2` | `llm.report_model` | `AGENT_LLM__REPORT_MODEL` |
| 规划调用超时（秒） | `60` | `llm.planner_timeout_sec` | `AGENT_LLM__PLANNER_TIMEOUT_SEC` |
| 报告调用超时（秒） | `120` | `llm.report_timeout_sec` | `AGENT_LLM__REPORT_TIMEOUT_SEC` |
| 是否开启思考链（若 SDK 支持） | `true` | `llm.enable_thinking` | `AGENT_LLM__ENABLE_THINKING` |
| 报告阶段是否开启联网 | `false`（与独立 `web_search` 区分） | `llm.report_enable_search` | `AGENT_LLM__REPORT_ENABLE_SEARCH` |

---

## 5. 服务、MCP、会话

| 含义 | 默认值（建议） | `config.yaml` 键 | 环境变量 |
|------|----------------|------------------|----------|
| Agent HTTP 监听端口 | `5001` | `server.agent_port` | `AGENT_SERVER__AGENT_PORT` |
| MCP HTTP 监听端口 | `5000` | `server.mcp_port` | `AGENT_SERVER__MCP_PORT` |
| MCP 路径前缀 | `/mcp` | `server.mcp_path` | `AGENT_SERVER__MCP_PATH` |
| 会话内存 TTL（秒，可选） | `86400`（24h）或 `0` 表示不设 | `session.ttl_sec` | `AGENT_SESSION__TTL_SEC` |
| 单次聊天总超时（秒） | `180` | `server.chat_total_timeout_sec` | `AGENT_SERVER__CHAT_TOTAL_TIMEOUT_SEC` |
| 并行下单 Tool 超时（秒） | `30` | `server.per_tool_timeout_sec` | `AGENT_SERVER__PER_TOOL_TIMEOUT_SEC` |

---

## 6. API 行为（v2）

| 含义 | 默认值 | `config.yaml` 键 | 环境变量 |
|------|--------|------------------|----------|
| v2 响应是否默认附带 `plan` | `false` | `api.v2_include_plan_by_default` | `AGENT_API__V2_INCLUDE_PLAN_BY_DEFAULT` |
| 是否允许 `stream` | `true` | `api.allow_stream` | `AGENT_API__ALLOW_STREAM` |

---

## 7. `config.yaml` 示例骨架（键名集中可见）

```yaml
# config.yaml — 键名以本文件为唯一事实来源；环境变量见上表覆盖

server:
  agent_port: 5001
  mcp_port: 5000
  mcp_path: /mcp
  chat_total_timeout_sec: 180
  per_tool_timeout_sec: 30

session:
  ttl_sec: 86400

db:
  sqlite_path: ./data/skl_data.sqlite
  query_timeout_sec: 5
  mysql_dsn: ""

data:
  excel_import_dir: ""
  skip_import_if_db_exists: true

query:
  hard_max_rows: 100
  default_limit: 20
  default_recent_years: 3
  year_min: 2000
  truncation_notice_template: "结果已按系统上限截断为前 {limit} 条，请缩小条件。"

web_search:
  default_result_limit: 5
  max_result_limit: 20

llm:
  api_key: ""           # 优先使用环境变量 ALIYUN_API_KEY
  base_url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
  planner_model: deepseek-v3.2
  report_model: deepseek-v3.2
  planner_timeout_sec: 60
  report_timeout_sec: 120
  enable_thinking: true
  report_enable_search: false

api:
  v2_include_plan_by_default: false
  allow_stream: true
```

---

## 8. `.env.example` 片段（只放敏感与部署差异）

```env
# 与文档一致：百炼
ALIYUN_API_KEY=

# 可选：覆盖端口与库路径
AGENT_SERVER__AGENT_PORT=5001
AGENT_DB__SQLITE_PATH=./data/skl_data.sqlite

# 查询上限（与文档「硬上限100、默认20、默认近3年」对齐）
AGENT_QUERY__HARD_MAX_ROWS=100
AGENT_QUERY__DEFAULT_LIMIT=20
AGENT_QUERY__DEFAULT_RECENT_YEARS=3
```

---

## 9. 落地注意（避免各写各的）

1. **代码里禁止魔法数字**：只从 `settings.query.hard_max_rows` 等读取。  
2. **单一加载入口**：例如 `pydantic-settings` / `dynaconf` 合并 YAML + env。  
3. **文档与实现同步**：README 里只列 **`ALIYUN_API_KEY` + 上表「高频三项」**（`HARD_MAX_ROWS` / `DEFAULT_LIMIT` / `DEFAULT_RECENT_YEARS`），其余指向 `config.example.yaml`。  

若你希望前缀从 `AGENT_` 改成项目名（例如 `SKL_`），整表批量替换即可，关键是 **「YAML 键 = 唯一结构、env = 覆盖映射」** 只维护一份。

13.

下面可直接作为文档中的 **「实现映射」** 一节（或 `docs/tool_sql_mapping.md`），与现有 Tool 清单对齐；若代码落在 `query_projects.py` 等文件，在节末用一行 **代码锚点** 指向即可。

---

## 实现映射（Tool → SQL）

**约定（全局）：**

- 数据库：开发阶段 **SQLite**；表名、字段与 DDL 一致（MySQL 迁移时主要改连接与少量方言）。
- 模糊匹配：统一 **`LIKE '%' || ? || '%'`**（SQLite）；参数需由调用方做**最小清洗**（去首尾空格、长度上限见校验表）。
- 空参：不传该条件（不拼 `AND`）。
- 条数：**`LIMIT min(requested_limit, config.default_limit)`**，且 **`<= config.hard_max_rows`**；超限截断在应用层打日志并附 `truncation_notice`。
- **排序**：每 Tool 给默认 `ORDER BY`；无用户自定义排序需求时实现保持一致，便于验收对比。

---

### 1. `query_organization`

| 项 | 说明 |
|----|------|
| **主表** | `organization_info`（别名 `o`） |
| **JOIN** | 无 |
| **筛选** | `o.unit_name LIKE :pattern`（`:pattern` 为 `%keyword%`）；可选 `o.id = :id`（若以后扩展） |
| **返回列（建议）** | `o.id`, `o.unit_name`, `o.unit_address` |
| **ORDER BY** | `o.unit_name ASC`, `o.id ASC` |

---

### 2. `query_person`

| 项 | 说明 |
|----|------|
| **主表** | `person_info`（`p`） |
| **JOIN** | `INNER JOIN organization_info o ON p.org_id = o.id`（`org_id` 为空时若库内存在，应用层应用 `LEFT JOIN` 或单独策略；与 DDL「可空」一致时用 **`LEFT JOIN o`**，无单位则 `unit_name` 为空） |
| **筛选** | `p.name LIKE :pattern`（可选）；`o.unit_name LIKE :pattern_org`（可选） |
| **返回列（建议）** | `p.id`, `p.name`, `p.org_id`, `o.unit_name` |
| **ORDER BY** | `p.name ASC`, `p.id ASC` |

---

### 3. `query_projects`（省课题）

| 项 | 说明 |
|----|------|
| **主表** | `province_topic_info`（`pti`） |
| **JOIN（维表）** | `LEFT JOIN discipline_info di ON pti.discipline_id = di.id`<br>`LEFT JOIN province_topic_category ptc ON pti.topic_id = ptc.id`<br>`LEFT JOIN organization_info o ON pti.org_id = o.id`<br>`LEFT JOIN person_info hdr ON pti.header_id = hdr.id` |
| **LIKE 字段** | `o.unit_name` ← `org_name`；`di.disci_name` ← `discipline`；`ptc.type`、`ptc.category` ← `topic_type`（实现二选一或 `(ptc.type LIKE OR ptc.category LIKE)`）；`hdr.name` ← `header_name` |
| **年份** | 用表字段 `pti.create_time` / `pti.start_time` / `pti.end_time`（均为整型年）。推荐区间语义：**课题与时间窗有交集**<br>`NOT (pti.end_time < :year_start OR pti.start_time > :year_end)`<br>若产品更简单，可改为只筛 `pti.create_time BETWEEN :year_start AND :year_end`（二选一定死在代码与注释）。 |
| **返回列（建议）** | `pti.name`, `hdr.name AS header_name`, `o.unit_name`, `di.disci_name`, `ptc.type`, `ptc.category`, `pti.create_time`, `pti.start_time`, `pti.end_time`, `pti.id` |
| **ORDER BY** | `pti.create_time DESC`, `pti.id DESC` |

---

### 4. `query_teams`（社科团队）

| 项 | 说明 |
|----|------|
| **主表** | `social_science_group_info`（`sg`） |
| **JOIN** | `LEFT JOIN organization_info o ON sg.org_id = o.id`<br>`LEFT JOIN discipline_info di ON sg.discipline_id = di.id`<br>`LEFT JOIN social_science_group_type gt ON sg.type_id = gt.id`<br>`LEFT JOIN person_info lead ON sg.person_id = lead.id` |
| **LIKE 字段** | `o.unit_name` ← `org_name`；`sg.group_name` ← `group_name`；`gt.type_name` ← `type_name`；`di.disci_name` ← `discipline`；`lead.name` ← `header_name` |
| **返回列（建议）** | `sg.group_name`, `o.unit_name`, `gt.type_name`, `lead.name AS header_name`, `di.disci_name`, `sg.start_time`, `sg.end_time`, `sg.other_type`, `sg.id` |
| **ORDER BY** | `sg.start_time DESC`, `sg.id DESC`（`NULL` 年份排在后或前，SQLite 用 `NULLS LAST` 若版本支持，否则 `CASE WHEN sg.start_time IS NULL THEN 1 ELSE 0 END`） |

---

### 5. `query_awards`（省社科奖 + 国社科奖）

文档参数：`award_type`, `org_name`, `person_name`, `discipline`, `year`, `level`。

**5.1 省社科奖子查询（`award_type` 为空或为「省社科奖」类）**

| 项 | 说明 |
|----|------|
| **主表** | `social_science_aware_info`（`a`） |
| **JOIN** | `LEFT JOIN organization_info o ON a.org_id = o.id`<br>`LEFT JOIN person_info p ON a.person_id = p.id`<br>`LEFT JOIN discipline_info di ON a.discipline_id = di.id`<br>`LEFT JOIN social_science_aware_level lv ON a.level_id = lv.id`<br>`LEFT JOIN publish_unit_info pub ON a.pub_unit_id = pub.id` |
| **LIKE / 等值** | `o.unit_name` ← `org_name`；`p.name` ← `person_name`；`di.disci_name` ← `discipline`；`lv.level` ← `level`（可用 `LIKE` 或 `=`，产品定）；`a.year` ← `year`（整年等值；若参数为区间字符串则在应用层拆成 `BETWEEN`） |
| **返回列（建议）** | `'provincial' AS scope`, `a.obj_name`, `p.name AS person_name`, `o.unit_name`, `di.disci_name`, `lv.level`, `a.year`, `a.session`, `pub.pub_unit_name`, `a.id` |
| **ORDER BY** | `a.year DESC NULLS LAST`, `a.id DESC` |

**5.2 国社科奖子查询（`award_type` 为空或为「国社科奖」类）**

| 项 | 说明 |
|----|------|
| **主表** | `national_social_science_aware_info`（`n`） |
| **JOIN** | `LEFT JOIN organization_info o ON n.org_id = o.id`<br>`LEFT JOIN person_info p ON n.person_id = p.id`<br>`LEFT JOIN discipline_info di ON n.discipline_id = di.id`<br>`LEFT JOIN nat_social_science_group_type nt ON n.type_id = nt.id`<br>`LEFT JOIN nat_social_science_group_categorie nc ON n.categorie_id = nc.id` |
| **LIKE / 等值** | 同上；等级可用 `n.score` 或扩展显示 `nt.type_name`/`nc.categorie_name`（报告用） |
| **返回列（建议）** | `'national' AS scope`, `n.obj_name`, `p.name`, `o.unit_name`, `di.disci_name`, `nt.type_name`, `nc.categorie_name`, `n.year`, `n.score`, `n.id` |
| **ORDER BY** | `n.year DESC`, `n.id DESC` |

**合并策略：**

- `award_type` **未指定**：`UNION ALL` 两段子查询（列需 **对齐同名**），外层再 `ORDER BY year DESC, scope, id`；或分两次查询在应用层合并（更易调试）。
- **仅省 / 仅国**：只跑对应子查询。

---

### 6. `web_search`

| 项 | 说明 |
|----|------|
| **SQL** | 无；调用外部搜索 API；结果规范为 `{"success": true, "results": [{"title","snippet","url"}]}`。 |

---

## LIKE / 等值与索引（开发备注）

| 参数 | 典型落点 | 匹配方式 |
|------|-----------|----------|
| `org_name` | `organization_info.unit_name` | `LIKE` |
| `unit_name`（organization tool） | 同上 | `LIKE` |
| `discipline` | `discipline_info.disci_name` | `LIKE` |
| `topic_type` | `province_topic_category.type` / `category` | `LIKE`（常 OR） |
| `header_name` / `person_name` / `name` | `person_info.name` | `LIKE` |
| `group_name` | `social_science_group_info.group_name` | `LIKE` |
| `type_name`（团队） | `social_science_group_type.type_name` | `LIKE` |
| `level` | `social_science_aware_level.level` | `LIKE` 或 `=`（二选一定死） |
| `year` | 各事实表的 `year` / `create_time` | 等值或区间（应用层解析） |

**索引（MySQL 阶段「后期添加」时优先）：**  
`province_topic_info(org_id, discipline_id, create_time)`，`social_science_aware_info(org_id, year)`，`national_social_science_aware_info(org_id, year)`，`person_info(name)`，`organization_info(unit_name)` —— 精确前缀匹配才能吃索引；`%keyword%` 仍以全表扫描为主，演示数据量可接受。

---

## 代码锚点（避免反复猜）

在仓库中保持 **「一个 Tool 一个实现模块」** 时，建议：

| Tool | 建议文件 |
|------|----------|
| `query_organization` | `query_organization.py` 或 `sql/organization.py` |
| `query_person` | `query_person.py` |
| `query_projects` | `query_projects.py` |
| `query_teams` | `query_teams.py` |
| `query_awards` | `query_awards.py`（内含 provincial / national 或子模块） |
| 配置项 LIMIT/年份 | `config` + 各 query 入口统一调用 `apply_limit_and_year_defaults()` |

文档中写一句即可：  
**「实现以 `query_*.py`（或 `src/agent/sql/`）为准；上表为关系与语义契约，变更 JOIN 或 LIKE 列时需同步改本节。」**

---

若你希望与 **Pydantic 参数名** 一字不差对应到 **SQL 参数名**，可以再附一张「参数 → WHERE 子句模板」表；需要的话我可以按你选的 `query_awards` 合并方式（`UNION` vs 两次查询）把伪代码也补成一段。
