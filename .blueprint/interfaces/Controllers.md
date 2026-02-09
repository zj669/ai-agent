# Controllers (Interfaces Layer) Blueprint

## 职责契约
- **做什么**: 对外暴露 REST API，处理 HTTP 请求/响应转换，参数校验，调用 Application Service
- **不做什么**: 不包含业务逻辑；不直接操作数据库；不调用 Domain 层服务

## API 清单

### AgentController
| 端点 | 方法 | 功能 | 调用 |
|------|------|------|------|
| /api/agents | POST | 创建 Agent | AgentApplicationService.createAgent |
| /api/agents/{id} | PUT | 更新 Agent | AgentApplicationService.updateAgent |
| /api/agents/{id} | GET | 获取 Agent | AgentApplicationService.getAgent |
| /api/agents | GET | 列表查询 | AgentApplicationService.listAgents |
| /api/agents/{id}/publish | POST | 发布 Agent | AgentApplicationService.publishAgent |
| /api/agents/{id}/rollback | POST | 回滚版本 | AgentApplicationService.rollbackAgent |

### WorkflowController
| 端点 | 方法 | 功能 | 调用 |
|------|------|------|------|
| /api/workflow/start | POST | 启动工作流 | SchedulerService.startExecution |
| /api/workflow/{executionId} | GET | 查询执行状态 | SchedulerService.getExecution |
| /api/workflow/{executionId}/stop | POST | 取消执行 | SchedulerService.cancelExecution |
| /api/workflow/{executionId}/stream | GET(SSE) | 流式输出 | StreamPublisher |

### HumanReviewController
| 端点 | 方法 | 功能 | 调用 |
|------|------|------|------|
| /api/human-review/pending | GET | 待审核列表 | HumanReviewRepository |
| /api/human-review/{id}/resume | POST | 审核决定 | SchedulerService.resumeExecution |
| /api/human-review/history | GET | 审核历史 | HumanReviewRepository |

### ChatController
| 端点 | 方法 | 功能 | 调用 |
|------|------|------|------|
| /api/conversations | POST | 创建会话 | ChatApplicationService |
| /api/conversations | GET | 会话列表 | ChatApplicationService |
| /api/conversations/{id}/messages | GET | 消息列表 | ChatApplicationService |

### KnowledgeController
| 端点 | 方法 | 功能 | 调用 |
|------|------|------|------|
| /api/knowledge/datasets | POST | 创建数据集 | KnowledgeApplicationService |
| /api/knowledge/datasets/{id}/documents | POST | 上传文档 | KnowledgeApplicationService |
| /api/knowledge/datasets/{id} | GET | 数据集详情 | KnowledgeApplicationService |

## 依赖拓扑
- **上游**: 前端 React 应用, 外部 API 调用
- **下游**: Application Service 层（严禁直接调用 Domain 或 Infrastructure）

## 设计约束
- 所有响应使用统一的 Response<T> 包装
- 参数校验使用 @Valid + JSR 303 注解
- 认证通过 JWT Filter 处理，Controller 通过 SecurityContext 获取 userId
- SSE 端点返回 SseEmitter / Flux<ServerSentEvent>

## 变更日志
- [初始] 从现有代码逆向生成蓝图
