# 人工审核 — 发现的 Bug 与设计缺陷

## Bug 1：拒绝功能未实现（前后端均有问题）

**严重程度**：高

**现象**：前端有"拒绝"按钮和 `RejectExecutionRequest` DTO，但后端没有 reject 接口。前端的拒绝操作实际调用的是 `resumeReview()`（即 `/resume`），只是在 comment 中加了 `[拒绝]` 前缀，工作流仍然会继续执行。

**涉及文件**：
- `ReviewPage.tsx` L58-82：`handleReject` 调用 `resumeReview`，comment 加 `[拒绝]` 前缀
- `ReviewDetailPage.tsx` L104-123：同上
- `HumanReviewDTO.java` L66-70：`RejectExecutionRequest` 已定义但未使用
- `HumanReviewController.java`：无 reject 端点
- `SchedulerService.java`：`resumeExecution` 中 decision 硬编码为 `"APPROVE"`（L298）

**前端提示也承认了这一点**：
```tsx
// ReviewDetailPage.tsx L384-386
description="当前后端未实现真正的拒绝逻辑，拒绝操作只会在 comment 中标记，工作流仍会继续执行。"
```

**建议修复**：
- 后端新增 `POST /api/workflow/reviews/reject` 接口
- `Execution` 实体增加 `reject()` 方法，将状态转为 `FAILED` 或 `CANCELLED`
- `SchedulerService` 增加 `rejectExecution()` 方法，处理拒绝后的清理逻辑

---

## Bug 2：Controller 绕过 Domain Port 直接使用 RedissonClient

**严重程度**：中

**现象**：`HumanReviewController.getPendingReviews()` 直接注入 `RedissonClient` 并操作 `human_review:pending` key，绕过了 Domain 层定义的 `HumanReviewQueuePort`。

**涉及文件**：
- `HumanReviewController.java` L36, L46：`redissonClient.getSet("human_review:pending")`
- `RedisHumanReviewQueueAdapter.java`：已实现 `getPendingExecutionIds()` 方法但未被 Controller 使用

**问题**：
1. 违反 DDD 分层原则（Interfaces 层直接依赖基础设施细节）
2. Redis key 硬编码在两处（Controller 和 Adapter），如果 key 变更需要同步修改
3. Controller 用的是 `RedissonClient`，Adapter 用的是 `IRedisService`，两套 Redis 客户端混用

**建议修复**：
- Controller 注入 `HumanReviewQueuePort` 替代 `RedissonClient`
- 调用 `humanReviewQueuePort.getPendingExecutionIds()` 获取待审核列表

---

## Bug 3：审核详情接口的空 if 块（无效校验）

**严重程度**：低

**现象**：`HumanReviewController.getReviewDetail()` 中检查 execution 状态后没有任何处理。

**涉及文件**：
- `HumanReviewController.java` L81-83：

```java
if (execution.getStatus() != ExecutionStatus.PAUSED_FOR_REVIEW
        && execution.getStatus() != ExecutionStatus.PAUSED) {
    // 空块 — 什么都没做
}
```

**问题**：非暂停状态的 execution 也能查看审核详情，这个校验形同虚设。可能是开发时遗忘了 throw 或 return。

**建议修复**：
- 如果需要限制只能查看暂停中的 execution，加上 `throw new IllegalStateException("Execution is not paused")`
- 如果允许查看任意状态的 execution（比如查看历史），则删除这个空 if 块

---

## Bug 4：`workflow_human_review_task` 表存在但代码未使用

**严重程度**：低

**现象**：数据库中有 `workflow_human_review_task` 表（含 task_id, execution_id, node_id, status, input_data, output_data 等字段），但整个代码库中没有对应的 PO、Mapper 或任何引用。

**涉及文件**：
- `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`：历史遗留表定义曾存在于废弃 schema 文件中，现应仅以此文件为准
- 代码中无对应实现

**问题**：疑似早期设计遗留，当前审核功能完全依赖 Redis Set（待审核队列）+ `workflow_human_review_record`（审计日志），这张表成了死表。

**建议**：确认是否需要保留，如不需要则从 SQL 中移除，避免混淆。

---

## Bug 5：resumeExecution 缺少乐观锁保护

**严重程度**：中

**现象**：`SchedulerService.resumeExecution()` 代码注释中明确提到应该传 expected version 防止并发 resume，但实际未实现。

**涉及文件**：
- `SchedulerService.java` L262-264：

```java
// Optimistic Lock Check (Implicit via execution version match in some patterns,
// but here we just check logic)
// Ideally we pass expected version from API to ensure no concurrent resume
```

**问题**：虽然有 Redis 分布式锁（30 秒超时），但如果两个审核人几乎同时提交，且第一个锁释放后第二个获取到锁，可能导致重复 resume。`Execution.resume()` 内部虽然会检查状态，但在高并发场景下仍有风险窗口。

**建议修复**：
- 前端获取详情时返回 `execution.version`
- resume 请求携带 `expectedVersion`
- 后端比对 version，不匹配则拒绝（乐观锁模式）

---

## Bug 6：AFTER_EXECUTION 暂停时审核详情页可能缺失当前节点输出

**严重程度**：中

**现象**：用户配置节点为"执行后暂停"（AFTER_EXECUTION），进入审核详情页后，当前暂停节点没有输出可以修改。按预期行为，AFTER_EXECUTION 应该展示当前节点的输出供审核人编辑。

**排查链路**：

后端存储链路（理论上完整）：
1. 节点执行完成 → `onNodeComplete(result)` — result 中有 outputs
2. `checkPause(executionId, node, AFTER_EXECUTION, publisher, result.getOutputs())` — outputs 传入
3. `checkPause` 内部重新加载 Execution → `execution.advance(nodeId, NodeExecutionResult.paused(phase, outputs))`
4. `Execution.advance` L147-149: `context.setNodeOutput(nodeId, outputs)` — outputs 写入 context
5. `executionRepository.update(execution)` — 保存到 Redis

审核详情接口读取链路：
1. `getReviewDetail` L78: 从 Redis 加载 Execution
2. L118-121: `if (phase == AFTER_EXECUTION) { nodeOutputs = execution.getContext().getNodeOutput(nodeId); }`
3. `getNodeOutput` 返回 `nodeOutputs.getOrDefault(nodeId, new HashMap<>())`

**可能根因**：

1. **`getNodeOutput` 返回空 Map 而非 null 的掩盖效应**：
   - `ExecutionContext.getNodeOutput()` 使用 `getOrDefault(nodeId, new HashMap<>())`
   - 即使 outputs 未被存入，也返回 `{}`（空 Map），Jackson 序列化为 `{}`
   - 前端收到 `outputs: {}` 后，`node.outputs` 是 truthy（空对象在 JS 中为 true），但内容为空
   - 前端 L152 `{node.outputs && (` 会显示"编辑输出"按钮，但输出区域只显示 `{}`
   - 这可能让用户误以为"没有输出"

2. **Redis 序列化/反序列化丢失数据**：
   - Execution 通过 Jackson `ObjectMapper` 序列化存入 Redis
   - `ExecutionContext.nodeOutputs` 声明为 `ConcurrentHashMap`，Jackson 反序列化时可能降级为普通 `LinkedHashMap`
   - 如果 `setNodeOutput` 在反序列化后的实例上调用，且内部 `new HashMap<>(outputs)` 正常工作，数据不应丢失
   - 但如果 Jackson 反序列化 `ConcurrentHashMap` 时出现类型不匹配，可能导致数据静默丢失

3. **Checkpoint 与 Execution 状态不一致**：
   - `checkPause` 中先保存 Checkpoint（L509），再 update Execution（L510）
   - 如果 update 失败但 Checkpoint 已保存，后续从 Redis 加载的 Execution 可能是旧版本（不含 outputs）

**涉及文件**：
- `SchedulerService.java` L494-542: `checkPause` 方法
- `Execution.java` L142-164: `advance` 方法（暂停分支）
- `ExecutionContext.java` L95-104: `setNodeOutput` / `getNodeOutput`
- `HumanReviewController.java` L116-121: 审核详情中读取当前节点输出
- `ReviewDetailPage.tsx` L152: 前端"编辑输出"按钮显示条件

**建议修复**：
- `getNodeOutput` 改为：当 key 不存在时返回 `null` 而非空 Map，让前端能区分"无输出"和"空输出"
- `checkPause` 中增加日志：`log.info("[checkPause] Saving outputs for node {}: {}", nodeId, outputs != null ? outputs.size() : "null")`
- 前端增加对空对象的判断：`node.outputs && Object.keys(node.outputs).length > 0`
- 考虑将 `ExecutionContext` 中的 `ConcurrentHashMap` 改为普通 `HashMap`（序列化到 Redis 后不需要并发安全）

---

## Bug 7：LLM 节点输出出现未解析占位符，且同一回答在输出中重复三次

**严重程度**：高

**现象**：

在审核详情页或调试输出中，LLM 节点的输出会出现两类异常：

1. 回答正文中直接出现未解析的占位符，例如：
   - `{{knowledge-1773740237927-1.output.knowledge_list}}`
2. 同一段回答会同时出现在：
   - `llm_output`
   - `response`
   - `text`

最终表现为：
- 模型像是“没拿到知识库结果”，直接把模板变量当成普通文本回复
- 前端把完整输出对象原样展示时，看起来像回答被重复了三遍

**排查结论**：

### 1. Prompt 模板解析只覆盖了 `userPromptTemplate`，没有覆盖 `systemPrompt`

- `LlmNodeExecutorStrategy.buildUserPrompt()` 已接入 `PromptTemplateResolver`
- 但 `buildSystemPrompt()` 中的 `systemPrompt` 仍然是直接 `append` 原文

这意味着如果用户把 `{{knowledge-1.output.knowledge_list}}` 写在 `systemPrompt` 中，占位符不会被替换，而是原样发送给模型。

### 2. 占位符解析失败时默认保留原文

- `PromptTemplateResolver` 的失败策略是“保留原始占位符 + 记录 warn 日志 + 不让节点失败”
- 因此只要运行时 `ExecutionContext.nodeOutputs` 中没有对应的 `nodeId/key`，模型就会收到原始占位符文本

### 3. LLM 输出被写入了三个同值字段

后端当前构造输出时，将同一份回答同时写入：

```java
outputs.put("llm_output", response);
outputs.put("response", response);
outputs.put("text", response);
```

这虽然提高了兼容性，但审核详情页和调试页如果直接展示完整 `outputs` 对象，就会把同一段内容渲染三次。

### 4. 同一份知识可能被重复注入模型上下文

- 前端会为下游 LLM 自动补充 `contextRefNodes`
- 后端会将 `contextRefNodes` 对应节点输出注入 `systemPrompt`
- 如果用户又在 `userPromptTemplate` 中手动引用相同的 `{{knowledge-x.output.knowledge_list}}`

则同一份知识会同时出现在系统提示词和用户提示词中，容易让模型产生冗余、重复表述。

**涉及文件**：
- `LlmNodeExecutorStrategy.java`：
  - `buildSystemPrompt()` 未解析模板
  - `buildUserPrompt()` 仅对 `userPromptTemplate` 做模板替换
  - `executeAsync()` 将同一回答写入 `llm_output/response/text`
- `PromptTemplateResolver.java`：
  - 路径不存在时保留原始占位符
- `ReviewDetailPage.tsx`：
  - 直接 `JSON.stringify(node.outputs)` 展示完整输出对象
- `WorkflowEditorPage.tsx`：
  - 自动为 LLM 填充 `contextRefNodes`

**问题本质**：

这不是单一显示问题，而是“输入解析机制未统一 + 输出展示未去重”叠加后的结果：

1. `sourceRef/SpEL` 与 `PromptTemplate` 是两套独立机制
2. `systemPrompt` 与 `userPromptTemplate` 的解析能力不一致
3. 输出层为了兼容塞了多个同值字段，展示层又没有做主字段收敛

**建议修复**：
- 让 `systemPrompt` 与 `userPromptTemplate` 统一走 `PromptTemplateResolver`
- 在日志中打印模板解析失败的 `nodeId/key/template`，便于定位是“模板写错”还是“上下文未写入”
- 为 LLM 输出收敛主字段：
  - 后端只保留 `llm_output` 作为主展示字段
  - 或前端优先展示 `llm_output`，隐藏兼容字段
- 在前端避免将完整输出对象直接当作最终回答展示
- 为 `contextRefNodes` 与 `userPromptTemplate` 的重复知识注入增加去重策略或明确优先级
- 增加端到端联调用例：
  - `START -> KNOWLEDGE -> LLM -> END`
  - 验证 `{{knowledge-x.output.knowledge_list}}` 在 `userPromptTemplate/systemPrompt` 中都能被正确替换

---

## Bug 8：ChatPage 审核弹窗缺少拒绝功能

**严重程度**：高

**现象**：`ChatPage.tsx` 中的 `HumanReviewModal` 只有"修改并恢复执行"和"取消"两个按钮，没有拒绝按钮。用户在聊天页遇到审核暂停时无法直接拒绝终止工作流，必须切到审核中心页面操作。

**涉及文件**：
- `ChatPage.tsx` L1627-1633：`HumanReviewModal` 只传了 `onResume`，无 `onReject`
- `chatAdapter.ts`：无 `rejectReview` 接口

**建议修复**：
- `chatAdapter.ts` 增加 `rejectExecution` 方法
- `HumanReviewModal` 增加拒绝按钮和 `onReject` 回调
- `ChatPage` 增加 `handleRejectExecution` 处理函数

---

## Bug 9：`chatAdapter.ts` 的 `getReviewDetail` 可能未解包 ApiResponse 包装

**严重程度**：高

**现象**：`chatAdapter.ts` 中 `getReviewDetail` 直接返回 `response.data`，没有走 `unwrapResponse`，而其他接口（如 `getConversationList`）都走了 `unwrapResponse`。如果后端该接口使用了统一响应包装 `{ code, data, message }`，前端拿到的会是包装对象而非实际数据，导致所有字段为 `undefined`。

**涉及文件**：
- `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts`：`getReviewDetail` / `resumeExecution`

**建议修复**：
- 确认后端 `HumanReviewController` 的响应是否经过统一包装
- 如果是，`chatAdapter.ts` 中的审核相关接口需要走 `unwrapResponse`

---

## Bug 10：ReviewPage 快速通过/拒绝未传 `expectedVersion`，乐观锁保护有缺口

**严重程度**：中

**现象**：
1. `ReviewPage.tsx` 的 `handleApprove` 调用 `resumeReview` 时没有传 `expectedVersion`，因为 `PendingReviewDTO` 中没有 `executionVersion` 字段
2. `handleReject` 调用 `rejectReview` 时，后端 `rejectExecution` 方法本身就没有 `expectedVersion` 参数
3. 后端 `resumeExecution` 中 `expectedVersion == null` 时直接跳过校验，保护失效

**涉及文件**：
- `ReviewPage.tsx` L64-68：`handleApprove` 无 `expectedVersion`
- `HumanReviewDTO.java`：`PendingReviewDTO` 缺少 `executionVersion` 字段
- `HumanReviewDTO.java`：`RejectExecutionRequest` 缺少 `expectedVersion` 字段
- `SchedulerService.java`：`rejectExecution` 无版本校验

**建议修复**：
- `PendingReviewDTO` 增加 `executionVersion` 字段，`getPendingReviews` 填充该字段
- `RejectExecutionRequest` 增加 `expectedVersion` 字段
- `rejectExecution` 增加版本校验逻辑，与 `resumeExecution` 对齐
- 前端列表页通过/拒绝时传入 `expectedVersion`

---

## Bug 11：ChatPage 上游节点输出未做 LLM 去重

**严重程度**：中

**现象**：`ChatPage.tsx` 中 `HumanReviewModal` 渲染上游节点时直接使用 `n.outputs`，没有经过 `normalizeNodeOutputs` 去重处理。LLM 节点的 `llm_output`/`response`/`text` 三个同值字段会同时展示。

对比 `ReviewDetailPage.tsx` 中上游节点渲染时已调用 `normalizeNodeOutputs`，这里是遗漏。

**涉及文件**：
- `ChatPage.tsx` L618-658：上游节点输出渲染区域

**建议修复**：
- 在 `HumanReviewModal` 中对上游节点输出也调用 `normalizeNodeOutputs`

---

## Bug 12：AFTER_EXECUTION resume 路径 version 被递增两次

**严重程度**：中

**现象**：`SchedulerService.resumeExecution` 中 AFTER_EXECUTION 恢复流程：
1. `execution.resume()` — version++ (第一次)
2. `executionRepository.update(execution)` — 持久化
3. `onNodeComplete()` — 重新从 Redis 加载 Execution
4. `execution.advance()` — version++ (第二次)

单次逻辑操作 version +2，可能导致乐观锁校验的 expectedVersion 与实际 version 偏差超出预期。

**涉及文件**：
- `SchedulerService.java` L412-457：`resumeExecution` AFTER_EXECUTION 分支
- `Execution.java` L227, L166-172：`resume()` 和 `advance()` 各自递增 version

**建议修复**：
- 方案 A：`onNodeComplete` 中如果是从 resume 路径进来的，跳过 `advance` 直接推进后续节点
- 方案 B：接受 version 多增一次的现状，但在文档中明确说明

---

## Bug 13：`checkPause` 使用 `lockInterruptibly()` 无超时，与其他方法不一致

**严重程度**：中

**现象**：`SchedulerService.checkPause` 使用 `lock.lockInterruptibly()`（无超时限制），而 `resumeExecution` 和 `rejectExecution` 使用 `lock.lock(30, TimeUnit.SECONDS)`（30 秒自动释放）。如果锁被长期持有，`checkPause` 会无限阻塞工作流执行线程。

**涉及文件**：
- `SchedulerService.java` L838：`lock.lockInterruptibly()`

**建议修复**：
- 统一使用 `lock.tryLock(waitTime, leaseTime, unit)` 或 `lock.lock(30, TimeUnit.SECONDS)`

---

## Bug 14：`checkPause` phase 匹配逻辑冗余

**严重程度**：低

**现象**：`SchedulerService.checkPause` L816-826 的 phase 判断存在冗余的双重检查：

```java
if (config.getTriggerPhase() != phase) {
    TriggerPhase configuredPhase = config.getTriggerPhase() != null
        ? config.getTriggerPhase()
        : TriggerPhase.BEFORE_EXECUTION;
    if (configuredPhase != phase) return false;
}
```

外层 `if` 与内层逻辑重复，可简化为单一判断。

**建议修复**：
```java
TriggerPhase configuredPhase = config.getTriggerPhase() != null
    ? config.getTriggerPhase()
    : TriggerPhase.BEFORE_EXECUTION;
if (configuredPhase != phase) return false;
```

---

## Bug 15：`GlobalExceptionHandler` 中 HTTP 状态码不一致

**严重程度**：低

**现象**：
1. `AuthenticationException` 处理器使用 `@ResponseStatus(HttpStatus.BAD_REQUEST)` 返回 HTTP 400，但 body 中 code 字段是 401/429。客户端按 HTTP 状态码判断会误判。
2. `IllegalStateException` 映射为 400 Bad Request，但"当前执行未处于暂停状态"等状态冲突错误语义上应该是 409 Conflict。

**涉及文件**：
- `GlobalExceptionHandler.java` L43：`@ResponseStatus(HttpStatus.BAD_REQUEST)` 但 body code=401
- `GlobalExceptionHandler.java`：`IllegalStateException` → 400

**建议修复**：
- `AuthenticationException` 改用 `ResponseEntity` 返回正确的 HTTP 状态码
- `IllegalStateException` 中涉及状态冲突的场景考虑返回 409

---

## Bug 16：`Execution.java` 残留注释代码块

**严重程度**：低

**现象**：`Execution.java` L272-289 有一大段注释掉的伪代码，是开发过程中的思考笔记，应清理。

**建议修复**：删除注释代码块。

---

## Bug 17：`buildUserPrompt` 被调用两次，模板重复解析

**严重程度**：低

**现象**：`LlmNodeExecutorStrategy.executeAsync` L152 调用 `buildUserPrompt` 获取 `userInput` 用于 RAG 检索，然后 `buildMessageChain` L393 内部又调用一次 `buildUserPrompt`。模板被解析两遍。

**涉及文件**：
- `LlmNodeExecutorStrategy.java` L152, L393

**建议修复**：将第一次调用的结果缓存，传入 `buildMessageChain` 复用。

---

## Bug 18：`ExecutionContext.executionLog` 是 `StringBuilder`，Jackson 序列化不友好

**严重程度**：低

**现象**：`ExecutionContext` 通过 Jackson 序列化到 Redis，但 `StringBuilder` 默认序列化为对象结构而非字符串。可能导致反序列化异常或数据丢失。

**涉及文件**：
- `ExecutionContext.java` L81：`private StringBuilder executionLog`

**建议修复**：
- 改为 `String` 类型，提供 `appendLog` 时内部拼接
- 或添加 `@JsonSerialize`/`@JsonDeserialize` 自定义序列化

---

## Bug 19：`ResumeExecutionResponse` DTO 是死代码

**严重程度**：低

**现象**：`HumanReviewDTO.ResumeExecutionResponse` 已定义但从未使用。Controller 返回 `ResponseEntity<Void>`。

**涉及文件**：
- `HumanReviewDTO.java` L79-87

**建议修复**：删除该类，或改造 Controller 返回该 DTO 以提供更丰富的响应信息。
