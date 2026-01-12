# Requirements Document - Conversation Context Service

## Introduction

Conversation Context 是 AI-Agent 平台的会话管理核心模块，用于提供对话历史的存储、查询与管理能力。它将作为 Orchestration Context（工作流执行）的下游记录者，和前端界面的上游数据源，从独立的视角管理会话（Conversation）与消息（Message）。

## Alignment with Product Vision

该模块支持 AI-Agent 的核心价值：
- **历史回溯**：用户需要查看与 Agent 的过往对话。
- **上下文增强**：Agent 执行时需要加载历史对话作为 Context。
- **数据分析**：为后续的对话分析、质量评估提供结构化数据基础。

符合 `ddd-design.md` 中的战略划分，将 Conversation 独立为支撑域，解耦执行逻辑与记录逻辑。

## Requirements

### Requirement 1: 会话管理 (Conversation Management)

**User Story:** 作为用户，我希望能够查看我的历史会话列表，并能创建新会话或删除旧会话，以便管理我与 Agent 的交互。

#### Acceptance Criteria

1. **创建会话**:
   - WHEN 用户发起新对话 (或首次进入 Agent 页面) THEN 系统 SHALL 创建一个新的 `Conversation` 实体。
   - IF 会话创建成功 THEN SHALL 返回会话 ID、创建时间、Agent ID 等元数据。

2. **查询会话列表**:
   - WHEN 用户查看历史记录 THEN 系统 SHALL 返回当前 Agent 下该用户的会话列表。
   - 列表 SHALL 支持分页，按最近更新时间倒序排列。

3. **删除会话**:
   - WHEN 用户请求删除会话 THEN 系统 SHALL 软删除该会话及其关联的所有消息。
   - IF 尝试访问已删除会话 THEN SHALL 返回 404 或权限错误。

### Requirement 2: 消息存储 (Message Persistence)

**User Story:** 作为系统，我需要在工作流执行过程中自动记录用户输入和 Agent 输出，以便构建完整的对话历史。特别需要记录“思维链”过程，以支持前端的高级可视化。

#### Acceptance Criteria

1. **记录用户消息**:
   - WHEN 用户发送消息 THEN 系统 SHALL 记录一条 Role 为 `USER` 的消息。

2. **记录 Agent 消息 (Rich Content)**:
   - WHEN 工作流执行产生输出 THEN 系统 SHALL 记录一条 Role 为 `ASSISTANT` 的消息。
   - **Thought Chain Persistence**: 必须存储思考过程（JSON 结构），包含步骤（Steps）、耗时、工具调用详情等，以支持前端“灰色折叠卡片”的回显。
   - **Multimedia & Citations**: 支持存储引用源（Citations，如 URL、文件名）和生成物（Artifacts，如文件 ID）。
   - **Token Usage**: 记录该条消息消耗的 Token 数量，用于上下文窗口计算。

3. **消息状态管理**:
   - 引入状态机：`PENDING` -> `STREAMING` -> `COMPLETED` / `FAILED` / `CANCELLED`。
   - 支持流式传输过程中的状态更新，确保异常中断（如关闭页面）后，消息状态能最终一致（如标记为 FAILED 或保留已生成部分）。

4. **异步/解耦写入**:
   - 消息的写入与工作流执行核心逻辑应解耦（建议通过领域事件 `ExecutionCompletedEvent` 或 `NodeCompletedEvent` 触发）。

### Requirement 3: 历史消息查询 (History/Context Retrieval)

**User Story:** 作为工作流引擎/前端，我需要获取指定会话的历史消息，以便构建 LLM 的 Prompt 上下文或在界面展示。

#### Acceptance Criteria

1. **查询会话详情**:
   - WHEN 前端加载会话 THEN 系统 SHALL 返回该会话内的消息列表。
   - 返回的数据结构必须包含 `thoughtProcess`、`citations` 等富文本字段。

2. **构建上下文 (Context Window)**:
   - WHEN 工作流引擎请求上下文 THEN 系统 SHALL 提供最近 N 条消息。
   - 注意：构建 LLM 上下文时，通常只需 `content` (最终回复)，`thoughtProcess` 一般不作为历史上下文发送给模型（除非特定 Agent 模式）。

### Requirement 4: 交互增强 (Interaction Enhancements)

**User Story:** 作为用户，我希望会话列表整洁直观，并能对不满意的回答进行重新生成。

#### Acceptance Criteria

1. **会话标题自动生成 (Auto-Summary)**:
   - WHEN 会话产生前 N 条（如 2 条）交互后 THEN 系统 SHALL 异步触发一个摘要任务。
   - THEN 调用 LLM 生成简短标题（Title），并更新到 `Conversation` 实体。

2. **重新生成 (Regeneration)**:
   - WHEN 用户点击“重新生成” THEN 系统 SHALL 标记当前最新的一条 Assistant 消息为 `OVERWRITTEN` (或软删除)。
   - THEN 发起新的工作流执行，并创建一条新的消息记录。

## Domain Model Refinement

为了支持上述需求，领域模型需包含以下核心字段：

```java
class Conversation {
    String id;
    String userId;
    String agentId;
    String title;         // Auto-generated title
    LocalDateTime updatedAt;
}

class Message {
    String id;
    String conversationId;
    MessageRole role;     // USER, ASSISTANT, SYSTEM
    
    // Core Content
    String content;       // Final Markdown output
    
    // Rich Features
    List<ThoughtStep> thoughtProcess; // JSON structure for UI visualization
    List<Citation> citations;         // References: [1][2]
    List<String> artifactIds;         // Generated files
    
    // Metadata
    String runId;         // Link to Orchestration Execution
    MessageStatus status; // STREAMING, COMPLETED, FAILED
    Integer tokenCount;   // Context window calculation
    
    LocalDateTime createdAt;
}
```

## Non-Functional Requirements

### Code Architecture and Modularity
- **Domain-Driven Design**: 严格遵循 DDD，定义 `Conversation` 聚合根和 `Message` 实体。
- **Repository Pattern**: 使用仓储模式隔离数据库实现。
- **Separation of Concerns**: 独立于 Execution 模块，仅通过 ID 关联。

### Performance
- **Query Latency**: 会话列表和详情查询 P99 < 100ms。
- **Write Throughput**: 支持高并发消息写入。

### Security
- **Data Isolation**: 严格校验 `userId`，用户只能访问自己的会话数据。

### Reliability
- **Eventual Consistency**: 允许消息记录在执行完成后有毫秒级延迟（最终一致性）。

