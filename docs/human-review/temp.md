# 本轮人工审核与 LLM 输出相关修复交接说明（临时）

日期: 2026-03-17
范围: `docs/human-review` 指定的人审流程与缺陷修复

## 1. 本轮已完成内容（代码层）

### 1.1 审核拒绝真正生效（闭环）
目标: 点击“拒绝”后工作流必须终止，不能继续往下跑。

已实现:
- 后端新增拒绝接口: `POST /api/workflow/reviews/reject`
- `SchedulerService.rejectExecution(...)` 落库审核记录 `decision=REJECT`，执行置为 `FAILED`，从待审核队列移除，推送 `workflow_rejected` 事件，并 `publishFinish(failed)`，同时将聊天 assistant 消息落为失败摘要（如果存在 assistantMessageId）。
- `Execution` 聚合根新增 `reject(String nodeId)`，把整体状态置为 `FAILED`，清理 paused 状态，并把当前节点标记为失败。
- 前端审核列表页、审核详情页的“拒绝”按钮改为真实调用 reject API。

相关文件:
- `ai-agent-application/.../SchedulerService.java`
- `ai-agent-domain/.../Execution.java`
- `ai-agent-interfaces/.../HumanReviewController.java`
- `ai-agent-foward/.../ReviewPage.tsx`
- `ai-agent-foward/.../ReviewDetailPage.tsx`
- `ai-agent-foward/.../reviewAdapter.ts`

### 1.2 LLM 节点模板解析补齐（systemPrompt + userPromptTemplate）
问题: systemPrompt 内的 `{{nodeId.output.key}}` 占位符之前未解析，导致原样泄漏给模型。

已实现:
- `LlmNodeExecutorStrategy` 里 `systemPrompt` 与 `userPromptTemplate` 都统一走 `PromptTemplateResolver`
- `PromptTemplateResolver` 支持 `{{...}}` 与 `#{...}`，支持 `inputs.xxx` 与 `nodeId.output.xxx`
- 对模板解析失败的引用，会保留原占位符并输出 warn 日志（当前策略为“不硬失败”，以保证执行不中断）

相关文件:
- `ai-agent-infrastructure/.../LlmNodeExecutorStrategy.java`
- `ai-agent-infrastructure/.../template/PromptTemplateResolver.java`
- `ai-agent-infrastructure/.../LlmNodeExecutorStrategyPromptTemplateTest.java`

### 1.3 LLM 输出重复展示的“展示去重 + 提交兼容别名”
问题: 同一回答在 `llm_output / response / text` 三个字段里重复，前端展示会看到三份。

已实现:
- 前端审核详情页、聊天审核弹窗:
  - 展示时对 LLM 输出做去重，只保留“主字段”
  - 保存编辑时把主字段重新 hydration 到 `llm_output/response/text`，保持兼容（避免其他链路依赖旧字段导致断）

相关文件:
- `ai-agent-foward/.../ReviewDetailPage.tsx`
- `ai-agent-foward/.../ChatPage.tsx`

### 1.4 审核详情与恢复增加并发保护（expectedVersion）
问题: 两个审核人先后拿到锁，仍可能对同一 execution 重复恢复；仓储有乐观锁，但接口层没把“我基于哪个版本审核”的意图传下来，导致错误提示不清晰。

已实现:
- 审核详情接口 `GET /api/workflow/reviews/{executionId}` 增加返回字段 `executionVersion`
- 恢复接口请求体 `ResumeExecutionRequest` 增加 `expectedVersion`
- `SchedulerService.resumeExecution(...)` 新增参数 `expectedVersion`，在拿到 execution 后先校验 `expectedVersion == execution.version`，不一致直接抛 `OptimisticLockingFailureException`
- `GlobalExceptionHandler` 新增对 `OptimisticLockingFailureException` 的处理，返回 HTTP 409，并给前端一个明确 message: “审核状态已变化，请刷新后重试”
- 前端审核详情页、聊天审核弹窗提交恢复时都带 `expectedVersion`
- 注意: 修复了聊天页 `useCallback` 闭包问题
  - `handleResumeExecution` 原本依赖数组是 `[]`，读取 `reviewDetail.executionVersion` 会被旧闭包吃掉
  - 已改为依赖 `[reviewDetail]`

相关文件:
- 后端 DTO: `ai-agent-interfaces/.../HumanReviewDTO.java`
- Controller: `ai-agent-interfaces/.../HumanReviewController.java`
- Service: `ai-agent-application/.../SchedulerService.java`
- 异常处理: `ai-agent-interfaces/.../GlobalExceptionHandler.java`
- 前端适配: `ai-agent-foward/.../reviewAdapter.ts`
- 前端适配: `ai-agent-foward/.../chatAdapter.ts`
- 前端页面: `ai-agent-foward/.../ReviewDetailPage.tsx`
- 前端页面: `ai-agent-foward/.../ChatPage.tsx`

### 1.5 修复 AFTER_EXECUTION “空对象掩盖”的一部分（语义层）
问题: AFTER_EXECUTION 场景下，某些节点输出其实不存在，但 `getNodeOutput()` 以前默认返回 `{}`，会导致前端/模板解析误以为“存在输出但是空对象”。

已实现:
- `ExecutionContext.getNodeOutput(String)` 改为: 缺失返回 `null`（不再默认 new HashMap）
- 对应修复:
  - `PromptTemplateResolver` 里对 `nodeOutput` 增加 null 判断，避免 NPE
  - 前端审核详情页: “编辑输出”按钮和“输出展示”现在要求 `outputs` 不是空对象（`Object.keys(...).length > 0`），避免误导

相关文件:
- `ai-agent-domain/.../ExecutionContext.java`
- `ai-agent-infrastructure/.../PromptTemplateResolver.java`
- `ai-agent-foward/.../ReviewDetailPage.tsx`

### 1.6 审核详情状态校验语义补齐（Task 6 的一部分）
问题: `HumanReviewController.getReviewDetail()` 之前存在空 if，导致非暂停状态也能打开详情，语义不一致。

已实现:
- 只有 `ExecutionStatus.PAUSED_FOR_REVIEW` / `PAUSED` 才能打开审核详情，否则抛 `IllegalStateException`
- `pausedNodeId` 必须存在且不能是 `__MANUAL_PAUSE__`
- 暂停节点必须在图里存在，否则报错

注意:
- 这会导致“手动暂停（nodeId=__MANUAL_PAUSE__）”无法再打开审核详情，这是刻意的语义收紧，但可能需要产品/交互再确认。

相关文件:
- `ai-agent-interfaces/.../HumanReviewController.java`

## 2. 本轮已完成内容（测试与验证）

已新增/更新单测:
- `SchedulerServiceTest`:
  - 已把 `resumeExecution(...)` 的签名变化（新增 expectedVersion）更新到原有测试用例
  - 新增 “expectedVersion 不匹配抛冲突异常” 用例
- `LlmNodeExecutorStrategyPromptTemplateTest`:
  - 覆盖 systemPrompt 模板解析

已知: 暂未执行全量编译/测试（交接给下一个同学补跑）

## 3. 本轮尚未完成内容（待接手）

### 3.1 需要补跑的验证命令
后端:
- 仅跑接口层相关测试建议:
  - `./mvnw -q -pl ai-agent-interfaces -am -Dtest=SchedulerServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 建议再补一次编译级别校验（至少 interfaces + application + infrastructure）:
  - `./mvnw -q -pl ai-agent-interfaces,ai-agent-application,ai-agent-infrastructure -am test`

前端:
- `cd ai-agent-foward && npm run typecheck`
- 如有 e2e / build 流程，建议跑一次 `npm run build`

### 3.2 还没彻底收口的缺陷点（来自 task.md 的后续）
- Task 3（AFTER_EXECUTION 输出缺失）仍建议做进一步收口:
  - 现在语义层已经区分 null / {}，但 Controller 侧/前端交互仍需要更明确的“没有输出 vs 输出为空对象”策略
  - 审核详情页目前只把“输出为空对象”当作不可编辑，是否要允许人工创建输出，需要再定规则
- Task 4（resume 乐观锁）已完成 expectedVersion 主链路，但还缺:
  - 前端遇到 409 时可更友好地提示“该审核已被处理”，并提供刷新按钮
  - 如果还有其他入口调用 resume，需要同步 expectedVersion（当前仅发现 reviews 和 chat 两处）
- Task 2 的深层问题（重复注入/提示词膨胀）尚未处理:
  - `contextRefNodes` + userPromptTemplate + RAG + LTM + executionLog 可能导致重复/冗长
  - 目前只解决“占位符未解析”和“展示重复”
- Task 7（`workflow_human_review_task` 表）仍未处理，只在文档记录

### 3.3 需要注意的兼容性风险点
- `ExecutionContext.getNodeOutput()` 改为可能返回 `null`
  - 任何新代码如果写成 `context.getNodeOutput(id).containsKey(...)` 会 NPE
  - 本轮已修 `PromptTemplateResolver`，但其他调用点未来改动要注意
- `HumanReviewController.getReviewDetail()` 现在会拒绝 `__MANUAL_PAUSE__`:
  - 如果产品期望“手动暂停也能走审核详情 UI”，需要再设计一套只读或单独接口
- `GlobalExceptionHandler` 新增 409:
  - 前端 `httpClient` 的 errorMapper 会把 409 归类为 `UNKNOWN_ERROR`，但 message 会透出（本轮页面已尽量展示 message）

## 4. 与文档的对应关系
- Bug 文档: `docs/human-review/02-发现的Bug与设计缺陷.md`（已记录 Bug 7）
- 任务拆解: `docs/human-review/task.md`

## 5. 本轮关键改动文件清单（便于 review）
- 后端:
  - `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`
  - `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java`
  - `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java`
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java`
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java`
  - `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java`
  - `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/template/PromptTemplateResolver.java`
- 前端:
  - `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts`
  - `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts`
  - `ai-agent-foward/src/modules/review/pages/ReviewDetailPage.tsx`
  - `ai-agent-foward/src/modules/review/pages/ReviewPage.tsx`
  - `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx`
- 测试:
  - `ai-agent-interfaces/src/test/java/com/zj/aiagent/application/workflow/SchedulerServiceTest.java`
  - `ai-agent-infrastructure/src/test/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategyPromptTemplateTest.java`

