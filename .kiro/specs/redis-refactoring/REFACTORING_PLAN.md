# Redis 使用重构计划

## 目标

按照 DDD 架构规范,将所有直接使用 Redis 底层 SDK 的代码迁移到 Infrastructure 层,统一使用封装的 `IRedisService`。

## 架构原则 (严格 DDD)

### 正确的依赖关系

```
Interfaces Layer (REST API)
    ↓ 调用
Application Layer (用例编排)
    ↓ 调用
Domain Layer (业务逻辑 + 接口定义)
    ↑ 实现
Infrastructure Layer (技术实现)
```

### 各层职责

1. **Domain 层**: 
   - 定义业务实体和领域服务
   - 定义 Repository/Port 接口 (不实现)
   - 包含核心业务逻辑
   - **不依赖任何基础设施**

2. **Application 层**: 
   - 编排领域对象完成用例
   - 调用 Domain 层的接口
   - **不包含具体的技术实现细节**
   - **不直接操作 Redis、数据库等基础设施**

3. **Infrastructure 层**: 
   - 实现 Domain 层定义的接口
   - 使用 `IRedisService` 等技术组件
   - 处理所有技术细节

### ❌ 错误示例

```java
// Application 层 - 错误!
@Service
public class SchedulerService {
    private final StringRedisTemplate redisTemplate;  // ❌ 直接依赖技术组件
    
    public void cancelExecution(String id) {
        redisTemplate.opsForValue().set("cancel:" + id, "true");  // ❌ 包含技术细节
    }
}
```

### ✅ 正确示例

```java
// Domain 层 - 定义接口
public interface WorkflowCancellationPort {
    void markAsCancelled(String executionId);
    boolean isCancelled(String executionId);
}

// Application 层 - 调用接口
@Service
public class SchedulerService {
    private final WorkflowCancellationPort cancellationPort;  // ✅ 依赖抽象
    
    public void cancelExecution(String id) {
        cancellationPort.markAsCancelled(id);  // ✅ 调用领域接口
    }
}

// Infrastructure 层 - 实现接口
@Repository
public class RedisWorkflowCancellationAdapter implements WorkflowCancellationPort {
    private final IRedisService redisService;  // ✅ 使用封装的服务
    
    @Override
    public void markAsCancelled(String executionId) {
        redisService.setString("workflow:cancel:" + executionId, "true", 1, TimeUnit.HOURS);
    }
}
```

## 已完成工作

### 1. 扩展 IRedisService 接口 ✅

**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/IRedisService.java`

**新增方法**:
- `setString(String key, String value, long timeout, TimeUnit timeUnit)` - 设置字符串值(带过期时间)
- `getString(String key)` - 获取字符串值
- `multiGetString(Collection<String> keys)` - 批量获取字符串值
- `keys(String pattern)` - 获取匹配模式的所有键
- `delete(Collection<String> keys)` - 删除多个键
- `delete(String key)` - 删除单个键
- `addToSet(String key, String... values)` - 添加元素到集合
- `getSetMembers(String key)` - 获取集合的所有成员
- `removeFromSet(String key, String... values)` - 从集合中移除元素
- `publish(String channel, String message)` - 发布消息到频道
- `getSet(String key)` - 获取 RSet

### 2. 实现 RedissonService ✅

**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/service/RedissonService.java`

**实现状态**: 所有新增方法已实现并编译通过

## 待重构的文件

### 优先级 1: Application 层 (严重违反 DDD 原则)

#### SchedulerService
**文件**: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

**当前问题**:
```java
// ❌ 错误: Application 层直接依赖技术组件
private final StringRedisTemplate redisTemplate;
private final RedissonClient redissonClient;

// ❌ 错误: Application 层包含技术实现细节
public void cancelExecution(String executionId) {
    redisTemplate.opsForValue().set(CANCEL_KEY_PREFIX + executionId, "true", 1, TimeUnit.HOURS);
}

private boolean isCancelled(String executionId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PREFIX + executionId));
}

// ❌ 错误: Application 层直接操作 Redis 锁
RLock lock = redissonClient.getLock(lockKey);
lock.lock(30, TimeUnit.SECONDS);

// ❌ 错误: Application 层直接操作 Redis 集合
RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
pendingSet.add(executionId);
```

**重构方案**:

**步骤 1: Domain 层定义接口**

创建 `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowCancellationPort.java`:
```java
/**
 * 工作流取消端口
 * Domain 层定义,Infrastructure 层实现
 */
public interface WorkflowCancellationPort {
    /**
     * 标记工作流为已取消
     */
    void markAsCancelled(String executionId);
    
    /**
     * 检查工作流是否已取消
     */
    boolean isCancelled(String executionId);
}
```

创建 `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewQueuePort.java`:
```java
/**
 * 人工审核队列端口
 * Domain 层定义,Infrastructure 层实现
 */
public interface HumanReviewQueuePort {
    /**
     * 添加到待审核队列
     */
    void addToPendingQueue(String executionId);
    
    /**
     * 从待审核队列移除
     */
    void removeFromPendingQueue(String executionId);
    
    /**
     * 检查是否在待审核队列中
     */
    boolean isInPendingQueue(String executionId);
}
```

**步骤 2: Infrastructure 层实现接口**

创建 `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisWorkflowCancellationAdapter.java`:
```java
@Repository
@RequiredArgsConstructor
public class RedisWorkflowCancellationAdapter implements WorkflowCancellationPort {
    
    private final IRedisService redisService;
    private static final String CANCEL_KEY_PREFIX = "workflow:cancel:";
    
    @Override
    public void markAsCancelled(String executionId) {
        redisService.setString(CANCEL_KEY_PREFIX + executionId, "true", 1, TimeUnit.HOURS);
    }
    
    @Override
    public boolean isCancelled(String executionId) {
        return redisService.isExists(CANCEL_KEY_PREFIX + executionId);
    }
}
```

创建 `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java`:
```java
@Repository
@RequiredArgsConstructor
public class RedisHumanReviewQueueAdapter implements HumanReviewQueuePort {
    
    private final IRedisService redisService;
    private static final String PENDING_QUEUE_KEY = "human_review:pending";
    
    @Override
    public void addToPendingQueue(String executionId) {
        redisService.addToSet(PENDING_QUEUE_KEY, executionId);
    }
    
    @Override
    public void removeFromPendingQueue(String executionId) {
        redisService.removeFromSet(PENDING_QUEUE_KEY, executionId);
    }
    
    @Override
    public boolean isInPendingQueue(String executionId) {
        return redisService.isSetMember(PENDING_QUEUE_KEY, executionId);
    }
}
```

**步骤 3: Application 层使用接口**

修改 `SchedulerService`:
```java
@Service
@RequiredArgsConstructor
public class SchedulerService {
    
    // ✅ 正确: 依赖 Domain 层定义的接口
    private final WorkflowCancellationPort cancellationPort;
    private final HumanReviewQueuePort humanReviewQueuePort;
    private final IRedisService redisService;  // 用于获取锁
    
    // ❌ 移除这些依赖
    // private final StringRedisTemplate redisTemplate;
    // private final RedissonClient redissonClient;
    
    public void cancelExecution(String executionId) {
        log.info("[Scheduler] Cancelling execution: {}", executionId);
        cancellationPort.markAsCancelled(executionId);  // ✅ 调用领域接口
    }
    
    private boolean isCancelled(String executionId) {
        return cancellationPort.isCancelled(executionId);  // ✅ 调用领域接口
    }
    
    private boolean checkPause(...) {
        // ...
        humanReviewQueuePort.addToPendingQueue(executionId);  // ✅ 调用领域接口
        return true;
    }
    
    public void resumeExecution(...) {
        // ...
        humanReviewQueuePort.removeFromPendingQueue(executionId);  // ✅ 调用领域接口
    }
    
    private void onNodeComplete(...) {
        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);  // ✅ 使用封装的服务获取锁
        
        try {
            lock.lock(30, TimeUnit.SECONDS);
            // 业务逻辑...
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 优先级 2: Infrastructure 层 Repository

这些类已经在 Infrastructure 层,只需要替换底层 SDK:

#### RedisCheckpointRepository
**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java`

**重构**: 
- 移除 `StringRedisTemplate` 依赖
- 注入 `IRedisService`
- 替换所有 `redisTemplate.opsForValue()` 为 `redisService.setString()`
- 替换所有 `redisTemplate.keys()` 为 `redisService.keys()`
- 替换所有 `redisTemplate.delete()` 为 `redisService.delete()`

#### RedisExecutionRepository
**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisExecutionRepository.java`

**重构**: 同上

#### RedisSsePublisher
**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java`

**重构**: 
- 移除 `StringRedisTemplate` 依赖
- 注入 `IRedisService`
- 替换 `redisTemplate.convertAndSend()` 为 `redisService.publish()`

#### RedisVerificationCodeRepository
**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/user/repository/RedisVerificationCodeRepository.java`

**重构**: 同 RedisCheckpointRepository

## 重构步骤

### 第一阶段: Application 层重构 (最高优先级)

**目标**: 移除 Application 层对技术组件的直接依赖,严格遵循 DDD 分层原则

#### 步骤 1: 在 Domain 层定义端口接口

创建以下接口:
1. `WorkflowCancellationPort` - 工作流取消
2. `HumanReviewQueuePort` - 人工审核队列

**位置**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/`

**原则**: 
- 接口只定义业务语义,不包含技术细节
- 方法名使用业务术语,如 `markAsCancelled` 而非 `setRedisKey`

#### 步骤 2: 在 Infrastructure 层实现端口

创建以下适配器:
1. `RedisWorkflowCancellationAdapter` - 实现 `WorkflowCancellationPort`
2. `RedisHumanReviewQueueAdapter` - 实现 `HumanReviewQueuePort`

**位置**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/`

**原则**:
- 使用 `IRedisService` 而非 `StringRedisTemplate`
- 所有技术细节封装在 Adapter 内部
- 使用 `@Repository` 注解

#### 步骤 3: 修改 Application 层

修改 `SchedulerService`:
1. 移除 `StringRedisTemplate` 和 `RedissonClient` 依赖
2. 注入 `WorkflowCancellationPort` 和 `HumanReviewQueuePort`
3. 注入 `IRedisService` (仅用于获取分布式锁)
4. 替换所有直接的 Redis 操作为端口方法调用

**注意**: 
- 分布式锁是基础设施能力,可以通过 `IRedisService.getLock()` 获取
- 但锁的使用逻辑(何时加锁、锁的粒度)属于业务逻辑,保留在 Application 层

#### 步骤 4: 编译和测试

```bash
mvn clean compile -DskipTests
```

### 第二阶段: Infrastructure 层 Repository 重构

**目标**: 统一使用 `IRedisService`,移除对 `StringRedisTemplate` 的直接依赖

#### 重构顺序

1. **RedisCheckpointRepository**
   - 替换 `redisTemplate.opsForValue().set()` → `redisService.setString()`
   - 替换 `redisTemplate.opsForValue().get()` → `redisService.getString()`
   - 替换 `redisTemplate.keys()` → `redisService.keys()`
   - 替换 `redisTemplate.delete()` → `redisService.delete()`

2. **RedisExecutionRepository**
   - 同上
   - 替换 `redisTemplate.opsForSet().add()` → `redisService.addToSet()`
   - 替换 `redisTemplate.opsForSet().members()` → `redisService.getSetMembers()`
   - 替换 `redisTemplate.opsForValue().multiGet()` → `redisService.multiGetString()`

3. **RedisSsePublisher**
   - 替换 `redisTemplate.convertAndSend()` → `redisService.publish()`

4. **RedisVerificationCodeRepository**
   - 同 RedisCheckpointRepository

#### 重构原则

- 一次重构一个文件
- 每次重构后立即编译验证
- 保持接口不变,只改变实现
- 添加必要的日志

### 第三阶段: 其他组件重构

1. 搜索所有使用 `StringRedisTemplate` 的类
2. 逐个评估和重构
3. 确保所有 Redis 操作都通过 `IRedisService`

### 第四阶段: 清理和验证

1. 移除未使用的 `StringRedisTemplate` 依赖
2. 运行完整的测试套件
3. 更新相关文档

## 重构原则

1. **最小改动**: 只改变实现方式,不改变业务逻辑
2. **向后兼容**: 确保接口不变
3. **逐步迁移**: 一次重构一个文件,编译通过后再继续
4. **充分测试**: 每次重构后运行相关测试

## 编译状态

✅ Infrastructure 层编译通过 (2026-02-03 13:46:59)

## 下一步

1. 创建 `WorkflowCancellationRepository` 接口
2. 实现 `RedisWorkflowCancellationRepository`
3. 重构 `SchedulerService`
