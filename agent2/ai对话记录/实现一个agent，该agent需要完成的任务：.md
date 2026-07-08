一、实现一个agent，该agent需要完成的任务：
1.包含基础tool，三类tool ①搜索 ②分析 ③报告
①搜索：包括网页搜索和数据库搜索，网页搜索调用外部API，数据库搜索为实现重点
②分析：根据用户的输入以及搜索到的信息整合分析
③报告：以设定好的格式进行报告输出
2.将tool封装到MCP中
3.agent与用户有一个专门的对话框，用户与agent进行对话
4.调用公共大模型，明确用户需求
5.根据用户需求调用tool，返回报告给用户 5.1若信息不够，追问用户以获取细节

二、所有代码文件需要有注释，明确注明每部分代码作用，并注明后续使用MySQL时需要修改的代码位置

三、后续此agent需嵌入到前端网页中

四、数据，最终实现用mysql，现在只有本地数据：
1. "E:\计设\数据\建表与数据导入\discipline_info.xlsx"
    id	disci_name
    1	新闻学与传播学
2."E:\计设\数据\建表与数据导入\nat_social_science_group_categorie.xlsx"
    id	categorie_name
    1	成果文库
3."E:\计设\数据\建表与数据导入\nat_social_science_group_type.xlsx"
    id	type_name
    1	单列・教育
4."E:\计设\数据\建表与数据导入\national_social_science_aware_info.xlsx"
    id	org_id	type_id	categorie_id	person_id	discipline_id	obj_name	score	year	start_time	end_time
    1	203	14		1	6	侵权公平责任论——我国侵权法上公平责任的适用与立法研究	20	2019	2019	2022
5."E:\计设\数据\建表与数据导入\organization_info.xlsx"
    id	unit_name	unit_address
    1	四川省文物考古研究院	
6."E:\计设\数据\建表与数据导入\person_info.xlsx"
    id	org_id	name
    1	203	王竹
7."E:\计设\数据\建表与数据导入\province_topic_category.xlsx"
    id	type	category	score
    1	“党的十九届四中全会精神研究阐释”专项	“党的十九届四中全会精神研究阐释”专项	4
8."E:\计设\数据\建表与数据导入\province_topic_info.xlsx"
    id	name	header_id	topic_id	discipline_id	org_id	create_time	start_time	end_time
    1	大熊猫国家公园建设中行政补偿制度研究	599	1	17	203	2019	2019	2022
9."E:\计设\数据\建表与数据导入\publish_unit_info.xlsx"
    id	pub_unit_name	pub_unit_address
    1	巴蜀书社	
10."E:\计设\数据\建表与数据导入\social_science_aware_info.xlsx"
    id	org_id	pub_unit_id	level_id	person_id	discipline_id	obj_name	session	year
    1	205	1	4	1965	2	龙显昭学术论文集	17	
11."E:\计设\数据\建表与数据导入\social_science_aware_level.xlsx"
    id	level	score
    1	一等奖	20
12."E:\计设\数据\建表与数据导入\social_science_group_info.xlsx"
    id	org_id	type_id	person_id	discipline_id	group_name	start_time	end_time	other_type	score
    1		2			四川学术成果分析与应用研究中心	2015	2018	四川省社会科学重点研究基地	18
13."E:\计设\数据\建表与数据导入\social_science_group_type.xlsx"
    id	type_name
    1	四川省高水平团队（人文社科）

这个建表是告诉你表结构 用本地数据
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

# 补充

1.允许organization_info表的unit_address这列的值可以为空（单位地址暂无）

ALTER TABLE organization_info MODIFY unit_address VARCHAR(255) NULL;

2.允许person_info的name为空

ALTER TABLE person_info MODIFY name VARCHAR(255) NULL;

# 5.创建索引

后期添加

五、报告格式：
    报告由大模型生成，大模型分析数据后整理得出报告
    若无该项数据则删除该指标
    大模型生成Markdown（含表格），前端渲染展示，用户可修改源码，支持下载.md或导出Word。
    # 【报告标题】

**生成时间**：YYYY-MM-DD  
**报告类型**：项目统计 / 热点分析 / 趋势报告

---

## 一、摘要

> 2-3句话概括核心发现和结论

---

## 二、数据概览

| 指标 | 数值 |
|------|------|
| 项目总数 | XX个 |
| 涉及部门数 | XX个 |
| 时间范围 | XXXX - XXXX |
...

---

## 三、分析与结论

### 3.1 趋势分析
（基于数据得出的趋势判断）

### 3.2 热点领域
（高频关键词分析）

### 3.3 机构分布
（各部门/机构情况对比）

### 3.4...

### ...
---

## 四、建议与展望

（基于分析结论提出的建议）

---

## 五、数据来源说明
给出网页搜索的网页链接



六、API我已经获得APIkey，使用
    import os
    API_KEY = os.getenv("ALIYUN_API_KEY")  # 从系统环境变量可读取APIkey

七、现有前端为"E:\计设\app" 我的这个是要做到它的里面 在这个前端可以调用这个agent 。新增路由 /AgentChat，菜单名“智能分析助手”，独立对话页面，用户提问后Agent返回Markdown报告，支持下载Word。与现有页面独立共存，互不干扰。

八、外部API细节，这是从官网粘贴的，实现时使用六的apikey进行替换：
from openai import OpenAI
import os

# 从环境变量读取API Key
client = OpenAI(
    api_key=os.getenv("ALIYUN_API_KEY"),  # 改用您的环境变量名
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

messages = [{"role": "user", "content": "你是谁"}]
completion = client.chat.completions.create(
    model="deepseek-v3.2",
    messages=messages,
    extra_body={"enable_thinking": True},
    stream=True,
    stream_options={"include_usage": True},
)

reasoning_content = ""
answer_content = ""
is_answering = False
print("\n" + "=" * 20 + "思考过程" + "=" * 20 + "\n")

for chunk in completion:
    if not chunk.choices:
        print("\n" + "=" * 20 + "Token 消耗" + "=" * 20 + "\n")
        print(chunk.usage)
        continue

    delta = chunk.choices[0].delta

    if hasattr(delta, "reasoning_content") and delta.reasoning_content is not None:
        if not is_answering:
            print(delta.reasoning_content, end="", flush=True)
        reasoning_content += delta.reasoning_content

    if hasattr(delta, "content") and delta.content:
        if not is_answering:
            print("\n" + "=" * 20 + "完整回复" + "=" * 20 + "\n")
            is_answering = True
        print(delta.content, end="", flush=True)
        answer_content += delta.content

九、网页搜索：调用api时开启联网搜索。百炼兼容模式下，通过 extra_body={"enable_search": True} 开启联网搜索，不是顶层参数。
from openai import OpenAI
import os

# 从环境变量读取API Key
client = OpenAI(
    api_key=os.getenv("ALIYUN_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

messages = [{"role": "user", "content": "昨天国家发布了什么新政策？"}]

completion = client.chat.completions.create(
    model="deepseek-v3.2",
    messages=messages,
    extra_body={
        "enable_thinking": True,   # 开启思考模式
        "enable_search": True      # 开启联网搜索
    },
    stream=True,
    stream_options={"include_usage": True},
)

reasoning_content = ""
answer_content = ""
is_answering = False
print("\n" + "=" * 20 + "思考过程" + "=" * 20 + "\n")

for chunk in completion:
    if not chunk.choices:
        print("\n" + "=" * 20 + "Token 消耗" + "=" * 20 + "\n")
        print(chunk.usage)
        continue

    delta = chunk.choices[0].delta

    # 收集思考内容
    if hasattr(delta, "reasoning_content") and delta.reasoning_content is not None:
        if not is_answering:
            print(delta.reasoning_content, end="", flush=True)
        reasoning_content += delta.reasoning_content

    # 收集最终回复
    if hasattr(delta, "content") and delta.content:
        if not is_answering:
            print("\n" + "=" * 20 + "完整回复" + "=" * 20 + "\n")
            is_answering = True
        print(delta.content, end="", flush=True)
        answer_content += delta.content

十、E:\计设\app 的技术栈与接入方式:
根据 E:\计设\app 里的配置与依赖，可以这样概括：

技术栈
类别	选用
框架
Vue 3（vue ^3.2）
工程化
Vue CLI 5（@vue/cli-service ~5.0），脚本：serve / build / lint
路由
Vue Router 4（createWebHistory，单页应用）
HTTP
Axios（^1.8）
图表
ECharts 5、vue-echarts、echarts-wordcloud
样式
Sass（sass、sass-loader）
其它
core-js、ESLint（plugin:vue/vue3-essential）、Babel
接入方式
本地开发

命令：npm install → npm run serve（见 README.md）。
vue.config.js 里配置了 开发代理：浏览器请求以 /api 开头的地址时，会转发到 http://localhost:8080，并把路径里的 ^/api 去掉再交给后端（注释里说明可按真实接口前缀再调）。
生产环境变量（.env）

VUE_APP_API_URL=http://118.25.110.238:8082/
src/utiils/request.js 里用该变量建了带 baseURL 的 Axios 实例；但 HomeView.vue、TaskManagementView.vue 等处用的是默认 axios 且路径为 /api/sheke/...，不会自动带上 VUE_APP_API_URL。
因此线上常见做法是：前端静态资源与 /api 同源，由 Nginx 等把 /api 反代到后端；若前后端不同域，需要统一改成使用封装好的实例并指向正确基地址，或配置打包后的代理。
访问形态

浏览器访问构建后的 静态站点（npm run build 产出在 dist），路由为前端路由，例如 /、/TaskManagementView。
数据接口路径形态为 /api/sheke/dataview/...（以及任务页里的 POST /api/sheke/dataview/search 等）。

十一、MCP Server要支持HTTP部署，供网页后端调用。不能只做成Cursor本地的stdio模式。MCP Server独立进程（端口5000，路径/mcp），网页后端（端口8080）通过MCP Client调用它，两者分开部署但通过HTTP通信。

十二、直接用本地数据，注明后续使用mysql时需要修改的部分，启动时从Excel导入SQLite，Tool里写SQL查询。库已存在则跳过，按依赖顺序导入，失败行记录日志并跳过

十三、后端负责Word生成，版式要求“基本对齐”（标题加粗、表格有边框、段落正常）。

十四、python实现

十五、新代码放在"E:\计设\agent2"中 

十六、Agent独立部署，提供 /api/agent/* 接口；前端通过代理区分 /api/sheke（原有）和 /api/agent（Agent），两套接口并存，互不干扰。/api/agent 由您新建的独立 Python 服务（端口 5001）提供，不并入现有后端。Agent HTTP API 在 5001，MCP 在 5000，前端 dev 里 /api 常指向 8080。浏览器访问 /api/agent/* 时，开发环境如何转发到 5001（例如在 vue.config.js 里为 /api/agent 单独代理到 http://localhost:5001，避免仍被转到 8080）。生产环境若用 Nginx，可再写：/api/agent → 5001，/api/sheke → 原后端，与第十六条一致。

十七、验收合格：①查社科院项目数 ②多轮追问乡村振兴+2024 ③下载Word能打开。

十八、多轮对话状态：多轮对话用 session_id 标识，会话历史存在 Python 服务的内存字典中，不持久化，不绑定用户，服务重启即清空。

十九、当前阶段不做鉴权和限流，仅限内网演示