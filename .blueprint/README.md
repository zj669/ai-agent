# AI Agent Platform 蓝图

## 项目概述
- 类型：AI Agent 编排平台（SaaS）
- 技术栈：Spring Boot 3.4.9 + Java 21（后端）、React 19 + Vite 6 + TypeScript（前端）
- 核心职责：动态创建 AI Agent、工作流编排与执行（条件分支/人工审核）、知识库管理（向量检索）、实时 SSE 流式推送
- 架构风格：DDD 分层架构（interfaces → application → domain ← infrastructure），Ports & Adapters 模式

## 模块地图

| 模块 | 路径 | 职责 |
|------|------|------|
| **ai-agent-shared** | `ai-agent-shared/` | 公共工具、设计模式（DAG/策略树/工作流状态机）、常量、响应体 |
| **ai-agent-domain** | `ai-agent-domain/` | 核心业务逻辑：实体、值对象、领域服务、仓储接口、端口定义 |
| **ai-agent-application** | `ai-agent-application/` | 用例编排：应用服务、DTO、命令、事件监听、跨域协调 |
| **ai-agent-infrastructure** | `ai-agent-infrastructure/` | 技术实现：MyBatis Plus 持久化、Redis/Milvus/MinIO 适配器、工作流执行器、SSE 流 |
| **ai-agent-interfaces** | `ai-agent-interfaces/` | REST 控制器、WebSocket、Spring 配置、应用入口、拦截器 |
| **ai-agent-foward** | `ai-agent-foward/` | React 前端：模块化页面、API 适配层、Zustand 状态管理、@xyflow 工作流画布 |

## 依赖关系

```
interfaces ──→ application ──→ domain ←── infrastructure
                                 ↑
                              shared
```

前端 `ai-agent-foward` 通过 REST API 与后端 `ai-agent-interfaces` 通信。

## 核心模块（建议优先 explore）

1. **ai-agent-domain/workflow** — 工作流执行引擎，Execution 聚合根，条件分支/剪枝/人工审核
2. **ai-agent-infrastructure/workflow** — 工作流执行器策略实现（Start/End/LLM/Condition/Http/Tool/Knowledge 共 7 种）
3. **ai-agent-application/workflow** — SchedulerService 调度编排，串联执行生命周期
4. **ai-agent-domain/swarm** — Swarm 多智能体协作运行时模型
5. **ai-agent-domain/writing** — 动态写作协作模型：会话、子 Agent、任务、结果、草稿
6. **ai-agent-foward/modules/workflow** — 前端工作流画布编辑器
7. **ai-agent-foward/modules/swarm** — 主对话区 + 协作面板的写作协作界面

## 当前已对齐的关键链路（2026-03-18）

### 0. 动态多智能体写作链路
- 当前多智能体协作已收敛为“主 Agent 编排 + 子 Agent 执行任务”的动态写作模式，不再把所有子 Agent 聊天消息平铺到主时间线。
- 主 Agent 通过 `SwarmTools` 中新增的 5 个写作工具维护协作数据：
  - `writing_session`
  - `writing_agent`
  - `writing_task`
  - `writing_result`
  - `writing_draft`
- 子 Agent 由主 Agent 根据写作规划动态创建。子 Agent 只负责执行被分配的任务并回填 `writing_result`，不允许再次创建 session / agent / task / draft。
- `SwarmPromptTemplate` 已切换为动态多智能体写作提示词，要求主 Agent 先记录本次写作任务，再创建协作者、拆解任务、汇总草稿。
- `WritingProjectionService` 负责把写作会话、子 Agent、任务、结果、草稿聚合成前端协作面板使用的 overview 数据。
- 前端 `swarm` 页面现为双层展示：
  - 主对话区：只显示用户与主 Agent 的正常聊天
  - 协作面板：显示子 Agent 状态卡片、任务摘要、结果摘要、草稿
- tool 调用在前端只显示图标徽标，不再把 `tool` 原文消息平铺出来，避免污染主聊天区。
- 数据库采用方案 A：
  - 旧 `swarm_*` 表不迁移
  - 直接删除旧 `swarm_*` 并按同名新结构重建
  - `writing_*` 表作为写作协作的主数据源
- 本次部署修复已补齐初始化 SQL，避免再次出现 `swarm_workspace_agent.description` 缺列导致的 `Unknown column 'description' in 'field list'`。
- 重建脚本位置：
  - `docker/init/mysql/20260318_rebuild_swarm_writing_schema.sql`
  - `ai-agent-infrastructure/src/main/resources/docker/init/mysql/20260318_rebuild_swarm_writing_schema.sql`

### 1. 人工审核链路
- 暂停入口仍在 `SchedulerService.checkPause(...)`，恢复/拒绝入口分别是 `SchedulerService.resumeExecution(...)` 和 `SchedulerService.rejectExecution(...)`。
- `GET /api/workflow/reviews/pending`、`GET /api/workflow/reviews/{executionId}`、`POST /resume`、`POST /reject` 当前都由 `HumanReviewController` 直接返回 `ResponseEntity` 原始 DTO / 空响应体，不走项目里的 `Response<T>` 包装。
- 审核详情和待审核列表都包含 `executionVersion`。审核中心恢复/拒绝接口已支持 `expectedVersion`，用于乐观锁校验；聊天页恢复已透传该字段，聊天页拒绝当前仍只传 `executionId/nodeId/reason`。
- AFTER_EXECUTION 审核详情现在会展示当前暂停节点输出；BEFORE_EXECUTION 不展示当前节点输出，只展示输入和上游上下文。
- `__MANUAL_PAUSE__` 不再被当作“可审核节点”，审核详情接口会直接拒绝打开。

### 2. 知识库检索链路
- 工作流中的知识库节点执行链路为：`KnowledgeNodeExecutorStrategy -> KnowledgeRetrievalServiceImpl -> VectorStore.searchKnowledgeByDataset(...)`。
- Milvus 中知识库 metadata 的当前标准键名是下划线风格：
  - `dataset_id`
  - `document_id`
  - `agent_id`
  - `chunk_index`
- 2026-03-18 已补兼容逻辑：检索和删除会同时兼容历史驼峰键名 `datasetId/documentId/agentId/chunkIndex`，避免旧向量数据查不到或删不掉。
- 如果知识库“明明文档是 COMPLETED 但检索为空”，优先检查：
  - MySQL 中 `knowledge_document.status` 是否为 `COMPLETED`
  - 查询是不是走到了正确的 `datasetId`
  - 是否命中了历史 metadata 键名不一致问题

### 3. 本地运行约定
- 当前 `spring-boot:run -pl ai-agent-interfaces` 运行时，`application/domain/infrastructure` 依赖仍从本地 Maven 仓库取 JAR。
- 因此后端改代码后，必须先重新安装模块，再启动服务；否则会出现“源码已改但运行仍是旧逻辑”的假象。
- 当前推荐顺序：
  - `./mvnw clean install -pl ai-agent-interfaces -am -Dmaven.test.skip=true`
  - `./mvnw spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local -Dmaven.test.skip=true`
- `.zed/tasks.json` 已按这个顺序更新。

## 热点文件

- 人工审核：
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
  - `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java`
  - `ai-agent-foward/src/modules/review/`
  - `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx`
- 知识库：
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java`
  - `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java`
  - `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java`
  - `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- 动态写作协作：
  - `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/writing/`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmTools.java`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmAgentRunner.java`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/prompt/SwarmPromptTemplate.java`
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/writing/WritingController.java`
  - `ai-agent-foward/src/modules/swarm/pages/SwarmMainPage.tsx`
  - `ai-agent-foward/src/modules/swarm/components/panel/CollaborationPanel.tsx`
  - `docker/init/mysql/20260318_rebuild_swarm_writing_schema.sql`

## 领域边界

- **workflow**: 工作流执行引擎（可依赖 agent 读取图定义、knowledge 做检索）
- **agent**: Agent 管理（独立，不依赖其他业务域）
- **chat**: 对话管理（独立）
- **knowledge**: 知识库（独立）
- **user/auth**: 用户认证（独立）
- **swarm**: 多智能体协作（可依赖 agent、llm）
- **writing**: 动态写作协作业务域，维护写作会话、子 Agent、任务、结果、草稿，供 swarm runtime 编排与前端协作面板聚合展示
- **llm**: LLM 模型配置管理（独立）
- **dashboard**: 统计面板（只读聚合）

## 蓝图使用说明

- 熟悉代码上下文时，使用 `/claude-shadow-context:explore` 先通过蓝图理解职责和边界
- 代码改动后，使用 `/claude-shadow-context:align` 检查蓝图是否仍然对齐
- 文件级蓝图由 align 在会话结束时按需沉淀，init 只负责根蓝图和模块入口蓝图
