# Redis 重构 - 需求文档

## 1. 项目概述

### 1.1 背景
当前项目在 Application 层直接使用 Redis 底层 SDK (`StringRedisTemplate`, `RedissonClient`),违反了 DDD (领域驱动设计) 的分层架构原则。这导致:
- Application 层包含技术实现细节
- 业务逻辑与基础设施耦合
- 难以测试和维护
- 违反依赖倒置原则 (DIP)

### 1.2 目标
将所有直接使用 Redis 底层 SDK 的代码重构为符合 DDD 架构规范的结构:
- Application 层只调用 Domain 层定义的接口
- Infrastructure 层实现这些接口,使用封装的 `IRedisService`
- 实现清晰的分层和职责分离

### 1.3 范围
**涉及模块**:
- `ai-agent-application` - 应用层 (重点)
- `ai-agent-domain` - 领域层 (新增接口)
- `ai-agent-infrastructure` - 基础设施层 (实现接口)

**涉及文件**:
- Application 层: `SchedulerService.java`
- Infrastructure 层: `RedisCheckpointRepository.java`, `RedisExecutionRepository.java`, `RedisSsePublisher.java`, `RedisVerificationCodeRepository.java`

## 2. 用户故事

### 2.1 作为开发者,我希望 Application 层不包含技术细节
**场景**: 当我查看 Application 层代码时
**期望**: 我应该只看到业务逻辑和领域接口调用,而不是 Redis 操作细节
**验收标准**:
- Application 层不直接依赖 `StringRedisTemplate` 或 `RedissonClient`
- Application 层只调用 Domain 层定义的业务接口
- 代码可读性提高,业务意图清晰

### 2.2 作为架构师,我希望遵循 DDD 分层原则
**场景**: 当我审查代码架构时
**期望**: 各层职责清晰,依赖方向正确
**验收标准**:
- Domain 层定义业务接口 (Port)
- Infrastructure 层实现这些接口
- Application 层通过接口调用基础设施能力
- 符合依赖倒置原则

### 2.3 作为测试工程师,我希望代码易于测试
**场景**: 当我编写单元测试时
**期望**: 可以轻松 Mock 依赖,隔离测试
**验收标准**:
- Application 层依赖抽象接口,易于 Mock
- 不需要启动 Redis 即可测试业务逻辑
- 测试代码简洁清晰

## 3. 功能需求

### 3.1 工作流取消功能
**需求**: 支持标记和检查工作流是否已取消
**当前问题**: Application 层直接操作 Redis
**重构目标**: 
- Domain 层定义 `WorkflowCancellationPort` 接口
- Infrastructure 层实现 `RedisWorkflowCancellationAdapter`
- Application 层通过接口调用

### 3.2 人工审核队列管理
**需求**: 管理待审核的工作流执行
**当前问题**: Application 层直接操作 Redis Set
**重构目标**:
- Domain 层定义 `HumanReviewQueuePort` 接口
- Infrastructure 层实现 `RedisHumanReviewQueueAdapter`
- Application 层通过接口调用

### 3.3 检查点持久化
**需求**: 保存和恢复工作流执行检查点
**当前问题**: Infrastructure 层直接使用 `StringRedisTemplate`
**重构目标**: 使用封装的 `IRedisService`

### 3.4 执行状态管理
**需求**: 管理工作流执行状态
**当前问题**: Infrastructure 层直接使用 `StringRedisTemplate`
**重构目标**: 使用封装的 `IRedisService`

### 3.5 SSE 消息发布
**需求**: 发布 SSE 消息到 Redis 频道
**当前问题**: Infrastructure 层直接使用 `StringRedisTemplate`
**重构目标**: 使用封装的 `IRedisService`

### 3.6 验证码管理
**需求**: 存储和验证用户验证码
**当前问题**: Infrastructure 层直接使用 `StringRedisTemplate`
**重构目标**: 使用封装的 `IRedisService`

## 4. 非功能需求

### 4.1 性能
- 重构不应降低系统性能
- Redis 操作延迟保持不变
- 不增加额外的网络开销

### 4.2 兼容性
- 保持接口向后兼容
- 不改变业务逻辑行为
- 不影响现有功能

### 4.3 可维护性
- 代码结构清晰,易于理解
- 遵循 DDD 最佳实践
- 添加必要的注释和文档

### 4.4 可测试性
- Application 层易于单元测试
- 可以 Mock 基础设施依赖
- 测试覆盖率不降低

## 5. 约束条件

### 5.1 技术约束
- 必须使用现有的 `IRedisService` 接口
- 不能修改 `IRedisService` 的核心方法签名
- 保持 Spring Boot 依赖注入机制

### 5.2 业务约束
- 不能改变现有业务逻辑
- 不能影响线上运行的功能
- 必须保持数据一致性

### 5.3 时间约束
- 分阶段重构,逐步迁移
- 每个阶段独立可编译
- 每次提交保持系统可运行

## 6. 验收标准

### 6.1 代码质量
- [ ] Application 层不包含 `StringRedisTemplate` 或 `RedissonClient` 依赖
- [ ] Domain 层定义了清晰的业务接口
- [ ] Infrastructure 层使用 `IRedisService` 实现接口
- [ ] 所有代码通过编译
- [ ] 代码符合项目规范

### 6.2 功能完整性
- [ ] 工作流取消功能正常
- [ ] 人工审核队列功能正常
- [ ] 检查点持久化功能正常
- [ ] 执行状态管理功能正常
- [ ] SSE 消息发布功能正常
- [ ] 验证码管理功能正常

### 6.3 架构合规性
- [ ] 符合 DDD 分层原则
- [ ] 依赖方向正确 (Application → Domain ← Infrastructure)
- [ ] 接口使用业务语言命名
- [ ] 技术细节封装在 Infrastructure 层

### 6.4 测试覆盖
- [ ] Application 层可以独立测试
- [ ] 关键业务逻辑有单元测试
- [ ] 集成测试通过

## 7. 风险评估

### 7.1 技术风险
**风险**: 重构过程中引入 Bug
**缓解**: 
- 逐步重构,每次改动最小化
- 每次重构后立即编译验证
- 保持现有测试通过

### 7.2 业务风险
**风险**: 影响线上功能
**缓解**:
- 不改变业务逻辑,只改变实现方式
- 充分测试后再部署
- 准备回滚方案

### 7.3 时间风险
**风险**: 重构时间超出预期
**缓解**:
- 分阶段进行,优先重构 Application 层
- 设置明确的里程碑
- 定期评估进度

## 8. 成功指标

- Application 层代码可读性提升 50%
- 单元测试编写时间减少 30%
- 代码架构符合 DDD 最佳实践
- 团队对新架构的满意度 > 80%
