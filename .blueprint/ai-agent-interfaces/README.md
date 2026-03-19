# ai-agent-interfaces 模块蓝图

## 模块职责
接口适配层，包含 REST 控制器、WebSocket 端点、Spring Boot 配置、应用入口和认证拦截器。是整个后端的最外层，负责 HTTP 请求接入和响应序列化。

## 关键文件

| 文件/包 | 职责 |
|---------|------|
| `AiAgentApplication.java` | Spring Boot 应用入口 |
| `interfaces/agent/web/AgentController.java` | Agent CRUD API（`/api/agent`） |
| `interfaces/workflow/WorkflowController.java` | 工作流执行 API（`/api/workflow/execution`） |
| `interfaces/workflow/HumanReviewController.java` | 人工审核 API（`/api/workflow/reviews`） |
| `interfaces/chat/ChatController.java` | 对话管理 API（`/api/chat`） |
| `interfaces/knowledge/web/KnowledgeController.java` | 知识库 API（`/api/knowledge`） |
| `interfaces/user/UserController.java` | 用户认证 API（`/api/user`） |
| `interfaces/meta/MetadataController.java` | 节点元数据 API（`/api/meta`），返回 node_template + sys_config_field_def + node_template_config_mapping 联查结果，驱动前端动态渲染节点配置面板 |
| `interfaces/dashboard/web/DashboardController.java` | 仪表盘 API |
| `interfaces/llm/` | LLM 模型配置 API |
| `interfaces/swarm/` | Swarm 多智能体 API |
| `interfaces/writing/WritingController.java` | 动态写作聚合 API（`/api/writing`） |
| `interfaces/common/interceptor/` | 认证拦截器：LoginInterceptor、AuthStrategyFactory（JWT/Debug 双策略） |
| `interfaces/common/advice/GlobalExceptionHandler.java` | 全局异常处理 |
| `interfaces/common/config/` | WebMvc 配置、HTTPS 配置 |
| `config/` | Spring 配置类：DataSource、EmbeddingModel、ThreadPool、RestClient、WebClient、MybatisPlus |

## 配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 主配置（profile 激活） |
| `application-local.yml` | 本地开发配置 |
| `application-dev.yml` | 开发环境配置 |
| `application-prod.yml` | 生产环境配置 |
| `logback-spring.xml` | 日志配置 |

## 当前接口契约重点

### HumanReviewController
- 路径前缀：`/api/workflow/reviews`
- 返回风格：
  - `GET /pending`：`ResponseEntity<List<PendingReviewDTO>>`
  - `GET /{executionId}`：`ResponseEntity<ReviewDetailDTO>`
  - `POST /resume`：空 `200`
  - `POST /reject`：空 `200`
- 这组接口当前不走统一 `Response<T>` 包装，因此前端 adapter 不应调用 `unwrapResponse(...)`。
- `pending/detail` DTO 已包含 `executionVersion`，恢复/拒绝请求体也已支持 `expectedVersion`。

### KnowledgeController
- 路径前缀：`/api/knowledge`
- 与 review 不同，知识库接口仍走项目统一的 `Response<T>` 包装。
- `POST /search` 返回 `Response<List<String>>`，是调试检索接口；工作流内部的策略选择仍发生在 application / infrastructure 层。

### WritingController
- 路径前缀：`/api/writing`
- 当前已提供的聚合查询接口：
  - `GET /workspace/{workspaceId}/sessions`
  - `GET /session/{sessionId}/overview`
- 这组接口面向前端协作面板，返回的是写作视角的聚合数据，不要求前端自己串联 `swarm_*` 与 `writing_*` 多张表。
- 使用场景：
  - 主页面初始化时拉取 workspace 下的写作 session 列表
  - 选中某个 session 后读取 overview，展示子 Agent 卡片、任务摘要、结果摘要、草稿

## 当前写作接口约定

- 主对话区与协作面板分离后，前端对 `swarm` 与 `writing` 接口的职责划分如下：
  - `swarm` 相关接口负责正常聊天、工作区与消息流
  - `writing` 相关接口负责协作面板聚合视图
- 如果协作面板出现“有子 Agent 运行但没有摘要”的问题，优先检查 `/api/writing/session/{sessionId}/overview` 返回值，而不是先查聊天消息流。

## 本地运行注意

- 当前最稳妥的本地启动方式是先安装再运行：
  - `./mvnw clean install -pl ai-agent-interfaces -am -Dmaven.test.skip=true`
  - `./mvnw spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local -Dmaven.test.skip=true`
- 原因：`spring-boot:run` 的依赖模块 classpath 仍可能来自本地 Maven 仓库中的 SNAPSHOT JAR。

## 上下游依赖
- 上游：依赖 `ai-agent-application`（调用应用服务）、`ai-agent-infrastructure`（Spring 自动装配）
- 下游：被前端 `ai-agent-foward` 通过 HTTP 调用
