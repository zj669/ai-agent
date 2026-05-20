# Fix Workflow Agent Module Bugs

## Goal
修复 Workflow Agent 模块中发现的所有 bug，分为三个批次：
- **CRITICAL** (3个): 立即处理
- **MAJOR** (13个): 重要修复
- **MINOR** (26个): 次要修复

## Batch 1: CRITICAL 修复

### BUG-1: `selectedTarget` vs `selectedBranchId` 键名不匹配
- **文件**: `ConditionNodeExecutorStrategy.java:148,432`
- **类型**: BUG / CRITICAL
- **描述**: 执行器向 outputs 写入 `"selectedTarget"`，但 `SchedulerService.generateNodeSummary()` (line 1354) 读取的是 `"selectedBranchId"`。条件节点的分支信息永远无法正确显示
- **修复**: 将 `selectedTarget` 改为 `selectedBranchId`

### BUG-2: 静态 `ScheduledExecutorService` 资源泄漏
- **文件**: `WorkflowController.java:43`
- **类型**: BUG / CRITICAL
- **描述**: `Executors.newScheduledThreadPool(10)` 作为 static 字段，Spring 生命周期无法管理，应用停止时线程池不关闭
- **修复**: 替换为 Spring 管理的 `@Bean` 或注入 `TaskScheduler`

### BUG-3: `shared/design/` 整个目录为纯死代码
- **文件**: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/`
- **类型**: DEAD CODE / CRITICAL
- **描述**: 整个 `shared/design/` 目录（workflow/, dag/, bizlogic/, ruletree/, strategymode/）没有任何生产代码引用
- **修复**: 删除整个 `shared/design/` 目录

---

## Batch 2: MAJOR 修复

### BUG-4: Spring Data 类型泄漏到 Domain 层
- **文件**: `HumanReviewRepository.java:26`
- **类型**: OVER-DESIGN
- **描述**: Domain 端口使用 `Page<>` 和 `Pageable`（Spring Data），违反 Domain 层纯净原则
- **修复**: 移除 Spring Data 类型，使用简单 offset+limit 或领域侧分页抽象

### BUG-5: `WorkflowCancellationPort` 单实现端口（过度设计）
- **文件**: `domain/workflow/port/WorkflowCancellationPort.java`
- **类型**: OVER-DESIGN / MAJOR
- **描述**: 只有一个实现，Redis 布尔标志的简单封装无需抽象为端口
- **修复**: 移除接口，将逻辑内联到 `SchedulerService`

### BUG-6: HTTP 状态码硬编码为 200
- **文件**: `HttpNodeExecutorStrategy.java:105`
- **类型**: BUG / MAJOR
- **描述**: 无论真实 HTTP 响应如何，始终写入 `statusCode: 200`，掩盖真实错误
- **修复**: 从 `ClientResponse` 提取真实状态码

### BUG-7: Tool aborted 执行返回 success
- **文件**: `ToolNodeExecutorStrategy.java:66-71`
- **类型**: BUG / MAJOR
- **描述**: 工具被中止时返回 `success()` 导致工作流继续执行
- **修复**: `isAborted()` 时返回 `failed()`

### BUG-8: `endTime` 使用 `updatedAt` 语义错误
- **文件**: `ExecutionDTO.java:39`
- **类型**: BUG / MAJOR
- **描述**: `updatedAt` 在每次状态变更时更新，非终态执行暴露错误的"结束时间"
- **修复**: 仅在终态时设置 `endTime`，否则为 null

### BUG-9: Controller 缺少验证注解
- **文件**: `WorkflowController.java:441-470`
- **类型**: BUG / MAJOR
- **描述**: `StartExecutionRequest` 等 DTO 缺少 `@NotNull`/`@NotBlank`，验证被推至 Service 层
- **修复**: 添加 `@Valid` + 约束注解

### BUG-10: 认证失败响应格式不一致
- **文件**: `HumanReviewController.java:206-208, 230-232`
- **类型**: BUG / MAJOR
- **描述**: 使用原始 `ResponseEntity.status(401)` 而非 `Response.error()`
- **修复**: 统一使用 `Response.error(401, "Unauthorized")`

### BUG-11: `WorkflowNodeExecutionLog` 与 `NodeCompletedEvent` 字段重复
- **文件**: `entity/WorkflowNodeExecutionLog.java` + `event/NodeCompletedEvent.java`
- **类型**: REDUNDANCY / MAJOR
- **描述**: 约10个相同字段重复
- **修复**: 让 `NodeCompletedEvent` 持有 `WorkflowNodeExecutionLog` 引用

### BUG-12: 多处静默异常吞没
- **文件**:
  - `RedisSsePublisher.java:47-49`
  - `RedisSseListener.java:42-44`
  - `RedisCheckpointRepository.java:116-127`
  - `RedisExecutionRepository.java:136-146`
- **类型**: BUG / MAJOR
- **描述**: 事件可能丢失但无任何信号
- **修复**: 统一错误处理策略

### BUG-13: `sharedState` SpEL 变量从未被填充
- **文件**: `WorkflowGraphFactoryImpl.java` + `ExpressionResolver.java`
- **类型**: BUG / MAJOR
- **描述**: `#sharedState['key']` 永远解析为 null，`state.<key>` 映射为无效路径
- **修复**: 移除 `state.<key>` → `#sharedState` 映射，改为 `#inputs['key']`

### BUG-14: SpEL 解析代码重复 ~120行
- **文件**: `WorkflowGraphFactoryImpl.java` + `ConditionNodeExecutorStrategy.java`
- **类型**: REDUNDANCY / MAJOR
- **描述**: `parseLegacySpelToItem()` 等方法在两处完全相同
- **修复**: 提取为 `SpelToConditionConverter` 工具类，Factory 中的版本为死代码应删除

### BUG-15: `ConditionNodeExecutorStrategy` 缺少 URL 规范化
- **文件**: `ConditionNodeExecutorStrategy.java:515`
- **类型**: BUG / MAJOR
- **描述**: 未处理 `/v1` 后缀，导致重复路径
- **修复**: 添加 `apiUrl.replaceAll("/v1/?$", "")`

---

## Batch 3: MINOR 修复

### 类别A: Bug (11个)
- `HumanReviewRepositoryImpl.toEntity:85` — NPE: `po.getId().toString()` 无 null 检查
- `RedisCheckpointRepository.findLatest:69` — Redis `KEYS` 阻塞 O(N) 操作，高并发风险
- `RedisCheckpointRepository.findLatest:69` — 依赖字典序排序获取"最新"检查点，脆弱
- `RedisCheckpointRepository.save:42` — `save()` 抛异常但 `findLatest()` 返回 empty，错误处理不一致
- `RedisExecutionRepository.update:111` — 乐观锁检查非原子（读→版本检查→写三步分离）
- `LlmNodeExecutorStrategy:200` — 流错误后 `blockLast()` 返回部分响应为 success
- `PendingReviewDTO.userId` — 字段声明但从未赋值，永远为 null
- `WorkflowController:403` — chat message 时间戳硬编码为 `System.currentTimeMillis()`
- `SchedulerService:1026` — `buildPauseSummary` 中 `values().iterator().next()` 依赖 HashMap 无序迭代

### 类别B: Over-Design (7个)
- `WorkflowGraph.java:106-185` — `calculateInDegrees()` + `topologicalSort()` 从未被调用（YAGNI）
- `WorkflowGraph.java:68-72` — `getStartNodes()` 从未被调用
- `WorkflowGraphFactory.java` — 单实现端口，JSON 解析无需抽象
- `Checkpoint.java:67-71` — `createPausePoint()` 仅4行包装方法
- `ExecutionMode.java` — `code`/`description` 字段 + `fromCode()` 重复内置 enum 能力
- `ConditionNodeExecutorStrategy:180-257` — 多 default 边时最后者胜出，行为难以推理
- `WorkflowGraphFactoryImpl:183-230` — `mapSpELToSourceRef()` 是死代码（从未被调用）

### 类别C: Redundancy (6个)
- `Execution.java:91` — `isNodeReviewed()` 中 `reviewedNodes != null` 死代码
- `valobj/Branch.java` — 整个类未被引用（legacy/abandoned）
- `ExecutionRepository.delete()` — 接口定义且实现存在，但无调用方
- `HumanReviewConfigDTO` — 定义但从未被使用
- `WorkflowGraphFactoryImpl.extractOutputs()` — 读 outputSchema 后丢弃，返回 `key==value` 的无意义映射
- `ConditionNodeExecutorStrategy:88-103` — `evaluateByStructuredCondition` 和 `evaluateByLlmMode` 中分支配置解析重复

---

## Acceptance Criteria
- [ ] Batch 1 (CRITICAL): 3个问题全部修复
- [ ] Batch 2 (MAJOR): 12个问题全部修复
- [ ] Batch 3 (MINOR): 26个问题全部修复
- [ ] `mvn compile -pl ai-agent-domain,ai-agent-infrastructure,ai-agent-application,ai-agent-interfaces -am` 编译通过
- [ ] `mvn test -pl ai-agent-infrastructure` 测试通过

## Technical Notes
- **Batch 1** 修复必须先完成，因涉及关键功能正确性
- **Batch 2/3** 可并行处理
- **Batch 3** 中的死代码删除（shared/design/目录、Branch.java、delete()方法等）需要先确认无任何引用
- 乐观锁竞态问题在单线程调度模型下实际风险低，但应记录技术债务
