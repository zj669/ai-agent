# ai-agent-application 模块蓝图

## 模块职责
用例编排层，协调领域对象完成业务用例。包含应用服务、DTO、命令对象、事件监听器。负责跨域协调（如 SchedulerService 串联 workflow + agent + knowledge + chat）。

## 关键文件

| 文件/包 | 职责 |
|---------|------|
| `application/workflow/SchedulerService.java` | 工作流调度核心：启动执行、内存注入、节点推进、完成回调 |
| `application/agent/service/AgentApplicationService.java` | Agent CRUD 编排、版本管理、图定义发布 |
| `application/agent/service/MetadataApplicationService.java` | 节点模板元数据查询 |
| `application/chat/ChatApplicationService.java` | 对话管理：创建会话、发送消息、SSE 流式响应 |
| `application/knowledge/KnowledgeApplicationService.java` | 知识库管理：数据集/文档 CRUD、文件上传 |
| `application/knowledge/AsyncDocumentProcessor.java` | 异步文档处理：解析、分块、向量化 |
| `application/knowledge/KnowledgeRetrievalServiceImpl.java` | 知识检索实现（实现 domain 层 KnowledgeRetrievalService 接口，委托 VectorStore 执行向量检索，支持 by agentId 和 by datasetId 两种模式） |
| `application/knowledge/KnowledgeApplicationServiceOptimized.java` | 知识库管理优化版 |
| `application/knowledge/FileValidator.java` | 上传文件校验 |
| `application/user/UserApplicationService.java` | 用户注册、登录、Token 刷新 |
| `application/dashboard/service/DashboardApplicationService.java` | 仪表盘统计聚合 |
| `application/llm/` | LLM 模型配置管理应用服务 |
| `application/swarm/` | Swarm 多智能体运行时编排（runtime、prompt、event） |
| `application/chat/listener/AutoTitleListener.java` | 自动生成对话标题（事件监听） |
| `application/chat/listener/ExecutionCompletedListener.java` | 执行完成后回写消息（事件监听） |

## 上下游依赖
- 上游：依赖 `ai-agent-domain`（调用领域服务和仓储接口）、`ai-agent-shared`
- 下游：被 `ai-agent-interfaces` 控制器调用
