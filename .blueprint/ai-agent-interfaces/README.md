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

## 上下游依赖
- 上游：依赖 `ai-agent-application`（调用应用服务）、`ai-agent-infrastructure`（Spring 自动装配）
- 下游：被前端 `ai-agent-foward` 通过 HTTP 调用
