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
| `application/writing/` | 动态写作编排：写作会话、子 Agent 规划、任务拆解、结果回填、草稿汇总、overview 聚合 |
| `application/chat/listener/AutoTitleListener.java` | 自动生成对话标题（事件监听） |
| `application/chat/listener/ExecutionCompletedListener.java` | 执行完成后回写消息（事件监听） |

## 当前关键执行链路

### SchedulerService
- `startExecution(...)`
  - 解析 Agent 已发布版本/草稿图
  - 初始化聊天消息
  - 执行记忆水合：LTM 来自 `VectorStore.search(...)`，STM 来自 `ConversationRepository`
  - 构建 `Execution` 并调度根节点
- `checkPause(...)`
  - 命中人工审核配置后写入 `Execution.pause` 状态
  - 保存 checkpoint / execution
  - 推送 `workflow_paused`
  - 加入 `HumanReviewQueuePort`
- `resumeExecution(...)`
  - 校验 `pausedNodeId`
  - 可校验 `expectedVersion`
  - 合并当前节点 edits 与多节点 `nodeEdits`
  - 写审核记录、恢复执行、移除待审核队列
  - BEFORE_EXECUTION 重新执行当前节点
  - AFTER_EXECUTION 直接把当前输出推进到下游
- `rejectExecution(...)`
  - 校验 `pausedNodeId`
  - 可校验 `expectedVersion`
  - 写审核记录 `decision=REJECT`
  - 调用 `Execution.reject(...)`
  - 移除待审核队列并推送 `workflow_rejected`

### 知识库
- `AsyncDocumentProcessor`
  - 负责 MinIO 下载、解析、分块、向量化批量写入
  - 当前写入 Milvus 的 metadata 已统一为下划线键名：`dataset_id/document_id/agent_id/chunk_index`
- `KnowledgeRetrievalServiceImpl`
  - 工作流知识库节点默认走 `retrieveByDataset(...)`
  - 支持 `SEMANTIC / KEYWORD / HYBRID` 三种按 dataset 检索策略

### 动态多智能体写作
- `WritingSessionService`
  - 创建 / 更新写作会话
  - 记录本次用户创作目标、上下文、状态
- `WritingAgentCoordinatorService`
  - 保存主 Agent 规划出的子 Agent 列表
  - 协调已有 swarm agent 创建逻辑，把写作规划映射成真实运行时 agent
- `WritingTaskService`
  - 保存主 Agent 拆解出的任务
  - 维护任务与 session / agent 的关联关系与状态流转
- `WritingResultService`
  - 保存子 Agent 的执行结果、摘要与附加结构化信息
- `WritingDraftService`
  - 保存主 Agent 汇总后的草稿
- `WritingProjectionService`
  - 聚合 session、agent、task、result、draft
  - 输出前端协作面板使用的 overview 结构，避免前端自行拼表
- `SwarmTools`
  - 已新增 `writing_session / writing_agent / writing_task / writing_result / writing_draft`
- `SwarmAgentRunner`
  - 已识别上述写作工具
  - 已限制子 Agent 不能创建 `session / agent / task / draft`
- `SwarmPromptTemplate`
  - 已切换为动态多智能体写作提示词，并补充 few-shot，约束模型按正确顺序调用工具

## 当前重要约定

- 审核详情/审核列表中的 `executionVersion` 是前后端并发保护的关键字段。
- `HumanReviewController` 返回的是原始 DTO / 空 `200`，不是 `Response<T>`；涉及聊天页审核适配器时要按这个契约实现。
- 动态写作协作当前遵循“主 Agent 统筹、子 Agent 执行、overview 聚合展示”的模式：
  - 主 Agent 创建 session / agent / task / draft
  - 子 Agent 只回填 result
  - 前端不消费完整子 Agent 消息流，而是消费 overview 聚合结果
- 当前写作链路的主要排查入口：
  - 工具调用顺序异常：先看 `SwarmPromptTemplate`
  - 子 Agent 执行权限异常：看 `SwarmAgentRunner`
  - 协作面板数据缺失：看 `WritingProjectionService`
- 已知仍需重点关注的风险：
  - AFTER_EXECUTION 恢复路径仍需持续关注 version 递增次数是否与 `onNodeComplete(...)` 保持一致。
  - 聊天页拒绝流程当前有真实 reject 能力，但 `expectedVersion` 还没有像恢复流程那样透传。

## 上下游依赖
- 上游：依赖 `ai-agent-domain`（调用领域服务和仓储接口）、`ai-agent-shared`
- 下游：被 `ai-agent-interfaces` 控制器调用
