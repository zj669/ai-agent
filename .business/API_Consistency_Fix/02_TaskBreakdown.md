# API 一致性修正 - 任务拆解

## 开发任务清单 (CheckList)

### Phase 1: Domain Layer (领域层)

- [ ] **Task 1.1**: 创建 `ExecutionMode` 枚举
  - **路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionMode.java`
  - **内容**: 定义 `STANDARD`, `DEBUG`, `DRY_RUN` 三个枚举值
  - **预计耗时**: 15 分钟

---

### Phase 2: DTO Layer (数据传输对象层)

- [ ] **Task 2.1**: 修改 `StartExecutionRequest`
  - **路径**: `WorkflowController.StartExecutionRequest` (内部类)
  - **修改**: 添加 `private ExecutionMode mode = ExecutionMode.STANDARD;` 字段
  - **预计耗时**: 10 分钟

- [ ] **Task 2.2**: 创建 `ToolMetadataDTO`
  - **路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/meta/dto/ToolMetadataDTO.java`
  - **字段**: `toolId`, `name`, `description`, `icon`, `inputSchema`, `outputSchema`
  - **预计耗时**: 20 分钟

- [ ] **Task 2.3**: 创建 `ExecutionContextDTO`
  - **路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionContextDTO.java`
  - **字段**: `executionId`, `longTermMemories`, `chatHistory`, `executionLog`, `globalVariables`
  - **预计耗时**: 25 分钟

---

### Phase 3: Controller Layer - 修正现有接口

#### 3.1 AgentController 修正

- [ ] **Task 3.1.1**: 添加新的 `DELETE /{id}` 接口
  - **路径**: `AgentController.java`
  - **实现**: 复制现有 `deleteAgent` 逻辑，使用 `@DeleteMapping("/{id}")`
  - **预计耗时**: 15 分钟

- [ ] **Task 3.1.2**: 标记旧接口为 `@Deprecated`
  - **路径**: `AgentController.java`
  - **修改**: 在 `POST /delete/{id}` 上添加 `@Deprecated` 和响应头 `X-Deprecated-API: true`
  - **预计耗时**: 10 分钟

- [ ] **Task 3.1.3**: 删除 `debugAgent` 方法
  - **路径**: `AgentController.java`
  - **操作**: 注释或删除 `POST /debug` 端点
  - **预计耗时**: 5 分钟

#### 3.2 UserController 修正

- [ ] **Task 3.2.1**: 添加新的 `PATCH /profile` 接口
  - **路径**: `UserController.java`
  - **实现**: 复制 `modifyUserInfo` 逻辑，使用 `@PatchMapping("/profile")`
  - **预计耗时**: 15 分钟

- [ ] **Task 3.2.2**: 标记旧接口为 `@Deprecated`
  - **路径**: `UserController.java`
  - **修改**: 在 `POST /modify` 上添加 `@Deprecated`
  - **预计耗时**: 10 分钟

#### 3.3 WorkflowController 修正

- [ ] **Task 3.3.1**: 修改 `startExecution` 支持 `mode` 参数
  - **路径**: `WorkflowController.java`
  - **修改**: 
    1. 从 `request.getMode()` 获取执行模式
    2. 传递给 `schedulerService.startExecution(..., mode)`
  - **前置依赖**: Task 2.1 完成
  - **预计耗时**: 20 分钟

- [ ] **Task 3.3.2**: 添加 `GET /{executionId}/context` 接口
  - **路径**: `WorkflowController.java`
  - **实现**:
    1. 从 `ExecutionRepository` 获取 `Execution`
    2. 提取 `context` 数据
    3. 查询 `WorkflowNodeExecutionLogRepository`
    4. 组装 `ExecutionContextDTO`
  - **预计耗时**: 30 分钟

---

### Phase 4: 新增 MetadataController

- [ ] **Task 4.1**: 创建 `MetadataController`
  - **路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/meta/MetadataController.java`
  - **预计耗时**: 20 分钟

- [ ] **Task 4.2**: 实现 `GET /api/meta/tools` 接口
  - **逻辑**:
    1. 注入 `NodeExecutorFactory`
    2. 调用 `getAllRegisteredNodeTypes()` (需新增方法)
    3. 为每个 NodeType 组装 Schema
    4. 返回 `List<ToolMetadataDTO>`
  - **预计耗时**: 45 分钟

---

### Phase 5: Application Layer (应用层)

- [ ] **Task 5.1**: 修改 `SchedulerService.startExecution` 签名
  - **路径**: `SchedulerService.java`
  - **修改**: 添加 `ExecutionMode mode` 参数
  - **影响**: 需同步修改所有调用方
  - **预计耗时**: 30 分钟

- [ ] **Task 5.2**: 实现 `ExecutionMode` 的行为逻辑
  - **路径**: `SchedulerService.java`
  - **逻辑**:
    - `DEBUG`: 发布更详细的 SSE 事件
    - `DRY_RUN`: 跳过真实的外部调用，使用 Mock 结果
  - **预计耗时**: 1 小时

---

### Phase 6: Infrastructure Layer (基础设施层)

- [ ] **Task 6.1**: 在 `NodeExecutorFactory` 添加元数据获取方法
  - **路径**: `NodeExecutorFactory.java`
  - **新增**: `public Map<NodeType, NodeMetadata> getAllToolMetadata()`
  - **预计耗时**: 30 分钟

---

### Phase 7: 编译与验证

- [ ] **Task 7.1**: 编译验证
  - **命令**: `mvn clean compile -DskipTests`
  - **预计耗时**: 5 分钟

- [ ] **Task 7.2**: 手动测试各接口
  - **工具**: Postman / curl
  - **验证点**:
    - `DELETE /api/agent/{id}` 正常删除
    - `PATCH /client/user/profile` 正常更新
    - `POST /execution/start` 接受 `mode` 参数
    - `GET /meta/tools` 返回工具列表
    - `GET /execution/{id}/context` 返回上下文快照
  - **预计耗时**: 30 分钟

---

## 依赖关系图

```
Task 1.1 (ExecutionMode)
   ↓
Task 2.1 (StartExecutionRequest)
   ↓
Task 3.3.1 (WorkflowController 支持 mode)
   ↓
Task 5.1 (SchedulerService 签名修改)
   ↓
Task 5.2 (ExecutionMode 行为实现)

Task 2.2 (ToolMetadataDTO) → Task 4.2 (GET /meta/tools)
Task 2.3 (ExecutionContextDTO) → Task 3.3.2 (GET /context)
Task 6.1 (NodeExecutorFactory 元数据) → Task 4.2
```

---

## 总预计耗时
- **核心开发**: ~5 小时
- **测试验证**: ~0.5 小时
- **总计**: ~5.5 小时
