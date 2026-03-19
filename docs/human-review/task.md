# 人工审核与相关缺陷修复任务清单

## 目标

基于 `docs/human-review/02-发现的Bug与设计缺陷.md` 中已确认的问题（Bug 1-19），输出一份按优先级排序、可直接执行的修复计划。

本清单覆盖：
- 人工审核主链路缺陷
- LLM 节点模板解析与输出展示缺陷
- ChatPage 审核交互缺陷
- 并发保护与状态一致性
- 代码质量与可维护性
- 测试与联调收口

## 优先级原则

1. 先修"业务语义错误"（用户可感知的功能缺失）
2. 再修"并发与状态一致性"
3. 再修"分层与历史遗留"
4. 最后做"展示与可维护性优化"

---

## 已完成任务

### ~~Task 1：实现真正的审核拒绝闭环~~ ✅

**对应 Bug**：Bug 1
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- 后端新增 `POST /api/workflow/reviews/reject` 接口
- `SchedulerService.rejectExecution()` 落库 `decision=REJECT`，执行置为 `FAILED`
- `Execution.reject()` 聚合根方法
- 前端 `ReviewPage` / `ReviewDetailPage` 拒绝按钮改为真实调用 reject API
- `reviewAdapter.ts` 新增 `rejectReview` 接口
- SSE 推送 `workflow_rejected` 事件
- 聊天消息落为失败摘要

---

### ~~Task 2：修复 LLM 模板未解析与输出重复展示问题~~ ✅

**对应 Bug**：Bug 7
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- `LlmNodeExecutorStrategy.buildSystemPrompt()` 统一走 `PromptTemplateResolver`
- `PromptTemplateResolver` 支持 `{{...}}` 与 `#{...}` 两种语法
- 前端 `ReviewDetailPage` 增加 `normalizeNodeOutputs` 去重展示
- 前端编辑保存时 `hydrateLlmOutputAliases` 回填三个兼容字段

---

### ~~Task 3：修复 AFTER_EXECUTION 审核详情输出缺失/空对象掩盖问题~~ ✅

**对应 Bug**：Bug 6
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- `ExecutionContext.getNodeOutput()` 改为返回 `null`（不再默认 `new HashMap`）
- `PromptTemplateResolver` 增加 null 判断避免 NPE
- 前端"编辑输出"按钮要求 `Object.keys(outputs).length > 0`

---

### ~~Task 4：为 resume 增加乐观锁保护~~ ✅

**对应 Bug**：Bug 5
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- `ReviewDetailDTO` 增加 `executionVersion` 字段
- `ResumeExecutionRequest` 增加 `expectedVersion` 字段
- `SchedulerService.resumeExecution()` 校验 expectedVersion
- `GlobalExceptionHandler` 新增 409 处理
- 前端 `ReviewDetailPage` / `ChatPage` 提交时带 `expectedVersion`
- 修复 `ChatPage.handleResumeExecution` 的 `useCallback` 闭包依赖

---

### ~~Task 5：统一审核队列访问入口，收口到 Domain Port~~ ✅

**对应 Bug**：Bug 2
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- `HumanReviewController` 注入 `HumanReviewQueuePort` 替代 `RedissonClient`
- 调用 `humanReviewQueuePort.getPendingExecutionIds()` 获取待审核列表
- 消除 Redis key 硬编码重复

---

### ~~Task 6：补齐审核详情状态校验语义~~ ✅

**对应 Bug**：Bug 3
**完成时间**：2026-03-17（第一轮修复）

**已实现内容**：
- 非暂停状态抛 `IllegalStateException`
- `__MANUAL_PAUSE__` 节点拒绝打开审核详情
- 暂停节点必须在图中存在

---

## 待完成任务

### ~~Task 7：ChatPage 审核弹窗增加拒绝功能~~ ✅

**对应 Bug**：Bug 8

**目标**：用户在聊天页遇到审核暂停时可以直接拒绝终止工作流，无需切到审核中心

**改动范围**：
- `chatAdapter.ts`
- `ChatPage.tsx`
- `HumanReviewModal` 组件

**实施项**：
- `chatAdapter.ts` 增加 `rejectExecution` 方法，调用 `/api/workflow/reviews/reject`
- `HumanReviewModal` 增加拒绝按钮和 `onReject` 回调
- `ChatPage` 增加 `handleRejectExecution` 处理函数
- 拒绝后添加系统消息提示"工作流已被拒绝终止"

**验收标准**：
- 聊天页审核弹窗有拒绝按钮
- 点击拒绝后工作流终止，聊天区显示终止提示

---

### ~~Task 8：修复 `chatAdapter.ts` 审核接口的响应解包问题~~ ✅

**对应 Bug**：Bug 9

**目标**：确保 ChatPage 中审核详情数据能正确解析

**改动范围**：
- `chatAdapter.ts`

**实施项**：
- 确认后端 `HumanReviewController` 的响应是否经过统一 `Response<T>` 包装
- 如果是，`getReviewDetail` 和 `resumeExecution` 需要走 `unwrapResponse`
- 如果不是（直接返回 `ResponseEntity`），确认当前实现是否正确

**验收标准**：
- ChatPage 审核弹窗能正确展示审核详情数据

---

### ~~Task 9：补齐列表页和拒绝接口的乐观锁保护~~ ✅

**对应 Bug**：Bug 10

**目标**：所有审核操作入口都有并发保护

**改动范围**：
- 后端：`HumanReviewDTO.PendingReviewDTO`、`HumanReviewDTO.RejectExecutionRequest`、`HumanReviewController`、`SchedulerService`
- 前端：`ReviewPage.tsx`、`reviewAdapter.ts`

**实施项**：
- `PendingReviewDTO` 增加 `executionVersion` 字段，`getPendingReviews` 填充
- `RejectExecutionRequest` 增加 `expectedVersion` 字段
- `SchedulerService.rejectExecution` 增加版本校验
- 前端列表页通过/拒绝时传入 `expectedVersion`

**验收标准**：
- 列表页快速通过/拒绝都携带 expectedVersion
- 并发双提交时第二个请求收到 409

---

### Task 10：ChatPage 上游节点输出 LLM 去重 【P1】

**对应 Bug**：Bug 11

**目标**：聊天页审核弹窗中 LLM 节点输出不再重复展示三遍

**改动范围**：
- `ChatPage.tsx`（`HumanReviewModal` 渲染区域）

**实施项**：
- 上游节点输出渲染时调用 `normalizeNodeOutputs` 去重
- 编辑保存时调用 `hydrateLlmOutputAliases` 回填兼容字段

**验收标准**：
- 聊天页审核弹窗中 LLM 节点输出只展示一份主回答

---

### Task 11：修复 AFTER_EXECUTION resume 路径 version 双增问题 【P1】

**对应 Bug**：Bug 12

**目标**：单次 resume 操作 version 只递增一次

**改动范围**：
- `SchedulerService.resumeExecution`

**实施项**：
- 方案 A：`onNodeComplete` 中从 resume 路径进来时跳过 `advance`，直接推进后续节点
- 方案 B：在 `resumeExecution` 中不调用 `execution.resume()` 的 version++，改为由 `onNodeComplete` 统一递增

**验收标准**：
- AFTER_EXECUTION resume 后 version 只增加 1

---

### Task 12：统一 `checkPause` 锁策略 【P1】

**对应 Bug**：Bug 13

**目标**：消除 `checkPause` 无超时阻塞的风险

**改动范围**：
- `SchedulerService.checkPause`

**实施项**：
- 将 `lock.lockInterruptibly()` 改为 `lock.tryLock(10, 30, TimeUnit.SECONDS)` 或 `lock.lock(30, TimeUnit.SECONDS)`
- 获取锁失败时记录 warn 日志并返回 false（不暂停，让节点继续执行）

**验收标准**：
- `checkPause` 有明确的超时限制
- 锁获取失败不会导致线程永久阻塞

---

### Task 13：代码清理与小修 【P2】

**对应 Bug**：Bug 14, 16, 17, 19

**目标**：清理代码噪音，提升可维护性

**实施项**：
- 简化 `checkPause` phase 判断逻辑（Bug 14）
- 删除 `Execution.java` L272-289 残留注释代码块（Bug 16）
- `buildUserPrompt` 结果缓存复用，避免重复解析（Bug 17）
- 删除 `ResumeExecutionResponse` 死代码（Bug 19）

**验收标准**：
- 无残留注释代码
- 无死代码 DTO
- `buildUserPrompt` 只调用一次

---

### Task 14：修复 `GlobalExceptionHandler` HTTP 状态码不一致 【P2】

**对应 Bug**：Bug 15

**目标**：HTTP 状态码与语义一致

**改动范围**：
- `GlobalExceptionHandler.java`

**实施项**：
- `AuthenticationException` 改用 `ResponseEntity` 返回正确的 401/429 HTTP 状态码
- 评估 `IllegalStateException` 中涉及状态冲突的场景是否应返回 409

**验收标准**：
- 认证失败返回 HTTP 401
- 状态冲突返回 HTTP 409

---

### Task 15：评估 `ExecutionContext.executionLog` 序列化安全性 【P2】

**对应 Bug**：Bug 18

**目标**：确保 `StringBuilder` 字段在 Redis 序列化/反序列化中不丢失数据

**改动范围**：
- `ExecutionContext.java`

**实施项**：
- 验证当前 Jackson 对 `StringBuilder` 的序列化行为
- 如有问题，改为 `String` 类型或添加自定义序列化注解

**验收标准**：
- `executionLog` 在 Redis 存取后内容完整

---

### Task 16：清理或启用 `workflow_human_review_task` 表 【P2】

**对应 Bug**：Bug 4

**目标**：解决数据模型中的"死表"问题

**改动范围**：
- `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`

**实施项**：
- 方案 A：明确弃用并从 SQL 中移除
- 方案 B：将其作为正式审核任务表接入业务

**待确认**：后续是否需要"审核任务分配、状态跟踪"能力

---

### Task 17：补齐单测、集成测试与联调用例 【P2】

**目标**：确保本轮修复有回归测试覆盖

**建议覆盖**：
- 审核通过（含 expectedVersion 校验）
- 审核拒绝（含 expectedVersion 校验）
- BEFORE_EXECUTION 恢复
- AFTER_EXECUTION 恢复
- 并发 resume 冲突（409）
- LLM `systemPrompt` + `userPromptTemplate` 模板解析
- `START -> KNOWLEDGE -> LLM -> END` 端到端链路

**验收标准**：
- 后端核心链路具备回归测试
- 前端审核页关键交互有测试覆盖

---

### Task 18：补日志与排障信息 【P2】

**目标**：关键问题都能通过日志快速定位

**实施项**：
- 为模板解析失败打印 expression、nodeId、可用 keys
- 为 pause/resume/reject 打印 executionId、nodeId、phase、version
- 为审核拒绝打印最终状态转换

---

## 建议执行顺序

1. Task 10：ChatPage LLM 去重（P1，改动小）
2. Task 11：version 双增修复（P1）
3. Task 12：锁策略统一（P1）
4. Task 13：代码清理（P2，可并行）
5. Task 14-16：异常处理 / 序列化 / 死表（P2，可并行）
6. Task 17-18：测试与日志（P2，收口）

## 里程碑

### 里程碑 A：ChatPage 审核交互完整 ✅
- Task 7、Task 8、Task 9

### 里程碑 B：并发保护全覆盖
- Task 11、Task 12

### 里程碑 C：展示一致性
- Task 10

### 里程碑 D：代码质量与可持续维护
- Task 13、Task 14、Task 15、Task 16、Task 17、Task 18
