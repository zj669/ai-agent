# Redis 重构 - 任务列表

## 任务概述

本任务列表按照 DDD 架构重构的最佳实践,分阶段完成 Redis 使用方式的重构。

**核心原则**:
- IRedisService 只提供基础 Redis 操作
- 业务逻辑在 Repository 实现中
- Application 层通过 Repository 接口操作数据

## 阶段 1: Application 层 - 重构 SchedulerService

### 1.1 移除直接的 Redis 依赖
- [x] 1.1.1 移除 `StringRedisTemplate` 字段
  - 位置: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
- [x] 1.1.2 移除 `RedissonClient` 字段
  - 保留对 Repository 的依赖
- [x] 1.1.3 移除 Redis key 相关常量
  - 移除 `CANCEL_KEY_PREFIX` 等常量
  - 这些常量应该在 Repository 实现中

### 1.2 重构取消相关方法
- [x] 1.2.1 重构 `cancelExecution` 方法
  - 当前: 直接使用 `redisTemplate.opsForValue().set()`
  - 目标: 通过 `ExecutionRepository` 操作
  - 实现: `execution.cancel()` + `executionRepository.update(execution)`
- [x] 1.2.2 重构 `isCancelled` 方法
  - 当前: 直接使用 `redisTemplate.hasKey()`
  - 目标: 通过 `ExecutionRepository` 查询
  - 实现: `executionRepository.findById().map(Execution::isCancelled)`

### 1.3 重构人工审核相关方法
- [x] 1.3.1 重构 `checkPause` 方法
  - 当前: 直接使用 `redissonClient.getSet().add()`
  - 目标: 通过 `ExecutionRepository` 操作
  - 实现: `execution.pause()` + `executionRepository.update()`
- [x] 1.3.2 重构 `resumeExecution` 方法
  - 当前: 直接使用 `redissonClient.getSet().remove()`
  - 目标: 通过 `ExecutionRepository` 操作
  - 实现: `execution.resume()` + `executionRepository.update()`

### 1.4 编译和验证
- [x] 1.4.1 编译 Application 层
  - 运行: `mvn clean compile -pl ai-agent-application`
- [x] 1.4.2 检查语法错误
  - 使用 getDiagnostics 工具检查

## 阶段 2: Infrastructure 层 - 重构 Repository

### 2.1 重构 RedisCheckpointRepository
- [x] 2.1.1 修改依赖注入
  - 移除 `StringRedisTemplate` 依赖
  - 注入 `IRedisService` 依赖
- [x] 2.1.2 重构 `save` 方法
  - 替换 `redisTemplate.opsForValue().set()` 为 `redisService.setString()`
  - 保持业务逻辑不变 (key 命名、序列化、TTL)
- [x] 2.1.3 重构 `findLatest` 方法
  - 替换 `redisTemplate.keys()` 为 `redisService.keys()`
  - 替换 `redisTemplate.opsForValue().get()` 为 `redisService.getString()`
- [x] 2.1.4 重构 `findPausePoint` 方法
  - 替换 `redisTemplate.opsForValue().get()` 为 `redisService.getString()`
- [x] 2.1.5 重构 `deleteByExecutionId` 方法
  - 替换 `redisTemplate.keys()` 为 `redisService.keys()`
  - 替换 `redisTemplate.delete()` 为 `redisService.delete()`
- [x] 2.1.6 编译验证
  - 运行: `mvn clean compile -pl ai-agent-infrastructure`
  - 使用 getDiagnostics 检查语法错误

### 2.2 重构 RedisExecutionRepository
- [x] 2.2.1 修改依赖注入
  - 移除 `StringRedisTemplate` 依赖
  - 注入 `IRedisService` 依赖
- [x] 2.2.2 重构字符串操作方法
  - 替换 `redisTemplate.opsForValue().set()` 为 `redisService.setString()`
  - 替换 `redisTemplate.opsForValue().get()` 为 `redisService.getString()`
  - 替换 `redisTemplate.opsForValue().multiGet()` 为 `redisService.multiGetString()`
- [x] 2.2.3 重构 Set 操作方法
  - 替换 `redisTemplate.opsForSet().add()` 为 `redisService.addToSet()`
  - 替换 `redisTemplate.opsForSet().members()` 为 `redisService.getSetMembers()`
  - 替换 `redisTemplate.opsForSet().remove()` 为 `redisService.removeFromSet()`
- [x] 2.2.4 重构键操作方法
  - 替换 `redisTemplate.keys()` 为 `redisService.keys()`
  - 替换 `redisTemplate.delete()` 为 `redisService.delete()`
- [x] 2.2.5 编译验证
  - 运行: `mvn clean compile -pl ai-agent-infrastructure`

### 2.3 重构 RedisSsePublisher
- [x] 2.3.1 修改依赖注入
  - 移除 `StringRedisTemplate` 依赖
  - 注入 `IRedisService` 依赖
- [x] 2.3.2 重构 `publish` 方法
  - 替换 `redisTemplate.convertAndSend()` 为 `redisService.publish()`
- [x] 2.3.3 编译验证
  - 运行: `mvn clean compile -pl ai-agent-infrastructure`

### 2.4 重构 RedisVerificationCodeRepository
- [x] 2.4.1 修改依赖注入
  - 移除 `StringRedisTemplate` 依赖
  - 注入 `IRedisService` 依赖
- [x] 2.4.2 重构所有方法
  - 替换 `redisTemplate.opsForValue().set()` 为 `redisService.setString()`
  - 替换 `redisTemplate.opsForValue().get()` 为 `redisService.getString()`
  - 替换 `redisTemplate.delete()` 为 `redisService.delete()`
- [x] 2.4.3 编译验证
  - 运行: `mvn clean compile -pl ai-agent-infrastructure`

## 阶段 3: 测试和验证

### 3.1 单元测试
- [ ] 3.1.1 编写 SchedulerService 单元测试
  - Mock Repository 接口
  - 验证方法调用
  - 测试取消功能
  - 测试暂停/恢复功能
- [ ] 3.1.2 编写 Repository 单元测试
  - 测试 RedisCheckpointRepository
  - 测试 RedisExecutionRepository
  - 测试 RedisSsePublisher
  - 测试 RedisVerificationCodeRepository

### 3.2 集成测试
- [ ] 3.2.1 测试工作流执行功能
  - 启动 Redis
  - 测试完整的执行流程
- [ ] 3.2.2 测试取消功能
  - 测试取消标记
  - 测试取消检查
- [ ] 3.2.3 测试人工审核功能
  - 测试暂停执行
  - 测试恢复执行
- [ ] 3.2.4 测试检查点持久化
  - 测试保存检查点
  - 测试恢复检查点
- [ ] 3.2.5 测试 SSE 消息发布
  - 测试消息发布
  - 测试消息订阅

### 3.3 回归测试
- [ ] 3.3.1 运行所有现有测试
  - 运行: `mvn test`
- [ ] 3.3.2 验证功能完整性
  - 工作流执行正常
  - 取消功能正常
  - 人工审核功能正常
  - SSE 推送正常

## 阶段 4: 清理和文档

### 4.1 代码清理
- [ ] 4.1.1 移除未使用的导入
  - 移除 `StringRedisTemplate` 导入
  - 移除 `RedissonClient` 导入 (Application 层)
- [ ] 4.1.2 移除未使用的常量
  - 移除 Application 层的 Redis key 常量
- [ ] 4.1.3 代码格式化
  - 运行代码格式化工具

### 4.2 文档更新
- [ ] 4.2.1 更新架构文档
  - 更新分层架构图
  - 说明重构内容
  - 强调 IRedisService 的定位
- [ ] 4.2.2 更新 README
  - 添加 Repository 模式说明
  - 更新依赖说明
  - 说明 IRedisService 的使用方式
- [ ] 4.2.3 编写迁移指南
  - 说明重构原因
  - 提供代码示例
  - 说明注意事项

### 4.3 代码审查
- [ ] 4.3.1 自我审查
  - 检查代码质量
  - 检查注释完整性
  - 检查日志记录
  - 确认业务逻辑在 Repository 中
  - 确认 IRedisService 只提供基础操作
- [ ] 4.3.2 团队审查
  - 提交 Pull Request
  - 等待团队审查
  - 处理审查意见

## 阶段 5: 部署和监控

### 5.1 部署准备
- [ ] 5.1.1 准备部署脚本
- [ ] 5.1.2 准备回滚方案
- [ ] 5.1.3 通知相关团队

### 5.2 部署执行
- [ ] 5.2.1 部署到测试环境
  - 验证功能正常
  - 验证性能指标
- [ ] 5.2.2 部署到预生产环境
  - 验证功能正常
  - 验证性能指标
- [ ] 5.2.3 部署到生产环境
  - 灰度发布
  - 监控关键指标
  - 验证功能正常

### 5.3 监控和优化
- [ ] 5.3.1 监控 Redis 操作延迟
- [ ] 5.3.2 监控错误率
- [ ] 5.3.3 收集性能数据
- [ ] 5.3.4 根据监控数据优化

## 任务优先级

### P0 (最高优先级)
- 阶段 1: Application 层 SchedulerService 重构
- 阶段 2: Infrastructure 层 Repository 重构

### P1 (高优先级)
- 阶段 3: 测试和验证

### P2 (中优先级)
- 阶段 4: 清理和文档

### P3 (低优先级)
- 阶段 5: 部署和监控

## 预估工作量

| 阶段 | 预估时间 | 说明 |
|------|---------|------|
| 阶段 1 | 2 小时 | 重构 SchedulerService |
| 阶段 2 | 4 小时 | 重构 4 个 Repository |
| 阶段 3 | 4 小时 | 编写和运行测试 |
| 阶段 4 | 2 小时 | 清理和文档 |
| 阶段 5 | 4 小时 | 部署和监控 |
| **总计** | **16 小时** | 约 2 个工作日 |

## 风险和缓解措施

### 风险 1: 重构引入 Bug
**缓解**: 
- 逐步重构,每次改动最小化
- 充分测试
- 保留回滚方案

### 风险 2: 性能下降
**缓解**:
- 性能测试
- 监控关键指标
- 优化热点代码

### 风险 3: 团队理解成本
**缓解**:
- 编写详细文档
- 代码审查时讲解
- 提供示例代码

## 完成标准

- [ ] 所有任务完成
- [ ] 所有测试通过
- [ ] 代码审查通过
- [ ] 文档更新完成
- [ ] 成功部署到生产环境
- [ ] 监控指标正常
