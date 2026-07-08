# 项目仓库说明

该仓库包含多个子项目，覆盖 AI 代理、前端应用、Java 后端以及数据库导入与建表脚本。

## 项目结构

- `agent2/`：Python 版 AI 代理与知识图谱项目。
  - `run_api.py`：启动 Agent API。
  - `run_mcp.py`：启动 MCP 服务（可选）。
  - `requirements.txt`：Python 依赖。
  - `config.example.yaml`、`.env.example`：配置示例。
  - `data/`、`import_xlsx/`：数据导入与知识图谱存储。
  - `src/`：主要业务源码。

- `app1/`：Vue 3 前端应用。
  - `package.json` / `package-lock.json`：依赖与脚本。
  - `public/`、`src/`：前端页面、组件、路由与请求代码。
  - `README.md`：前端项目说明。
  - `.env`：本地环境配置（请根据实际情况修改）。

- `shekelian-spring-backend/`：Java Spring 多模块后端工程。
  - `pom.xml`：父工程构建文件。
  - `crossMJ-*`：多个 Spring 模块，包括 `service`、`repository`、`security`、`infrastructure` 等。
  - `README.md`：Spring 后端说明。
  - `table.sql`：数据库表结构。

- `数据/`：数据库建表与数据导入相关内容。
  - `建表与数据导入/`：导入说明、建表脚本、Python 数据导入脚本。

## 关键说明

### `app1` 前端

它基于 Vue 3 和 Vue CLI，组件包括：
- `src/views/AgentChatView.vue`：AI 聊天页面。
- `src/views/KnowledgeGraphPage.vue`：知识图谱页面。
- `src/views/TaskManagementView.vue`：任务管理页面。

### `agent2` AI 代理

`agent2` 具备 Python AI 代理运行入口，支持：
- 通过 `.env` 或 `.env.example` 配置 API Key 和数据目录。
- 启动 API 服务和 MCP 服务。
- Excel 数据导入与知识图谱存储。

### `shekelian-spring-backend` 后端

这是一个 Spring Boot 多模块项目，适合用于 Java Web 后端服务。通常启动方式为：
```bash
cd shekelian-spring-backend
mvn clean install
mvn spring-boot:run
```

### `数据` 目录

该目录包含数据库建表和数据导入脚本，支持将外部数据导入到本项目数据库中。

## 运行指引

### 运行 app1 前端

```bash
cd app1
npm install
npm run serve
```

### 运行 agent2 AI 服务

```bash
cd agent2
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env，填写 ALIYUN_API_KEY 等配置
python run_api.py
# 可选：python run_mcp.py
```

### 运行 Java 后端

```bash
cd shekelian-spring-backend
mvn clean install
mvn spring-boot:run
```

## 提示

- `app1/.env` 中为本地前端环境配置，请根据实际情况修改。
- 若要重新安装前端依赖，建议删除 `app1/node_modules` 后执行 `npm install`。
- 若需补全 `agent2` 的 AI Key，请参考 `agent2/.env.example`。

---