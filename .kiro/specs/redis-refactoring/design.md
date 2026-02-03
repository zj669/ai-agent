# Redis 重构 - 设计文档

## 1. 设计概述

### 1.1 设计目标
将现有的 Redis 使用方式从直接依赖底层 SDK 重构为符合 DDD 架构的分层模式,实现:
- 清晰的分层架构
- 业务逻辑与技术实现解耦
- 提高代码可测试性和可维护性

### 1.2 设计原则
1. **依赖倒置原则 (DIP)**: Application 层依赖 Domain 层定义的抽象接口
2. **单一职责原则 (SRP)**: 每层只负责自己的职责
3. **开闭原则 (OCP)**: 对扩展开放,对修改关闭
4. **最小改动原则**: 只改变实现方式,不改变业务逻辑
5. **IRedisService 定位**: 只提供基础 Redis 操作,不包含业务逻辑

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│              Interfaces Layer (REST API)                │
│                                                          │
│  - AgentController                                      │
│  - WorkflowController                                   │
└────────────────────────┬────────────────────────────────┘
                         │ 调用
                         ↓
┌─────────────────────────────────────────────────────────┐
│           Application Layer (用例编排)                   │
│                                                          │
│  - SchedulerService                                     │
│    ✅ 依赖: CheckpointRepository (Domain 接口)          │
│    ✅ 依赖: ExecutionRepository (Domain 接口)           │
│    ❌ 移除: StringRedisTemplate                         │
│    ❌ 移除: RedissonClient                              │
└────────────────────────┬────────────────────────────────┘
                         │ 调用
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Domain Layer (业务逻辑)                     │
│                                                          │
│  Repository 接口 (定义业务能力):                         │
│  - CheckpointRepository                                 │
│    • save(Checkpoint)                                   │
│    • findLatest(executionId)                            │
│    • deleteByExecutionId(executionId)                   │
│                                                          │
│  - ExecutionRepository                                  │
│    • save(Execution)                                    │
│    • findById(executionId)                              │
│    • update(Execution)                                  │
└────────────────────────┬────────────────────────────────┘
                         │ 被实现
                         ↓
┌─────────────────────────────────────────────────────────┐
│         Infrastructure Layer (技术实现)                  │
│                                                          │
│  Repository 实现 (实现 Domain 接口):                     │
│  - RedisCheckpointRepository                            │
│    implements CheckpointRepository                      │
│    ✅ 使用: IRedisService (基础操作)                    │
│    ✅ 包含: 业务逻辑 (key 命名、序列化、TTL 等)         │
│    ❌ 移除: StringRedisTemplate                         │
│                                                          │
│  - RedisExecutionRepository                             │
│    implements ExecutionRepository                       │
│    ✅ 使用: IRedisService (基础操作)                    │
│    ✅ 包含: 业务逻辑                                     │
│    ❌ 移除: StringRedisTemplate                         │
│                                                          │
│  - RedisSsePublisher                                    │
│    ✅ 使用: IRedisService.publish()                     │
│    ❌ 移除: StringRedisTemplate                         │
│                                                          │
│  - RedisVerificationCodeRepository                      │
│    implements IVerificationCodeRepository               │
│    ✅ 使用: IRedisService                               │
│    ❌ 移除: StringRedisTemplate                         │
│                                                          │
│  ┌───────────────────────────────────────────────────┐ │
│  │ IRedisService (基础 Redis 工具)                   │ │
│  │ - 只提供基础操作: get, set, delete, lock 等       │ │
│  │ - 不包含任何业务逻辑                               │ │
│  │ - 不知道 key 的业务含义                            │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 2.2 关键设计决策

#### IRedisService 的定位
**IRedisService 是纯技术工具类**:
- ✅ 提供基础 Redis 操作 (get, set, delete, incr, lock 等)
- ✅ 封装 Redisson 客户端
- ❌ 不包含业务逻辑
- ❌ 不知道 key 的业务含义
- ❌ 不处理序列化/反序列化业务对象

**业务逻辑在 Repository 实现中**:
- ✅ Repository 使用 IRedisService 的基础能力
- ✅ Repository 负责 key 命名规则 (如 `workflow:cancel:{id}`)
- ✅ Repository 负责业务对象的序列化
- ✅ Repository 负责 TTL 策略
- ✅ Repository 组合多个基础操作实现复杂业务逻辑

### 2.2 端口-适配器模式

**端口 (Port)**: Domain 层定义的接口,描述业务能力
**适配器 (Adapter)**: Infrastructure 层的实现,连接外部技术

```
Domain Layer (Port)          Infrastructure Layer (Adapter)
┌──────────────────┐         ┌────────────────────────────┐
│ WorkflowCancell- │◄────────│ RedisWorkflowCancellation- │
│ ationPort        │ 实现    │ Adapter                    │
│                  │         │                            │
│ + markAsCancelled│         │ - IRedisService            │
│ + isCancelled    │         │ + markAsCancelled()        │
└──────────────────┘         │ + isCancelled()            │
                             └────────────────────────────┘
```

## 3. 详细设计

### 3.1 Application 层 - SchedulerService 重构设计

**重构目标**: 移除对 `StringRedisTemplate` 和 `RedissonClient` 的直接依赖

**重构前**:
```java
@Service
public class SchedulerService {
    private final StringRedisTemplate redisTemplate;  // ❌ 直接依赖
    private final RedissonClient redissonClient;      // ❌ 直接依赖
    
    public void cancelExecution(String executionId) {
        // ❌ 包含技术细节
        redisTemplate.opsForValue().set(
            CANCEL_KEY_PREFIX + executionId, 
            "true", 
            1, 
            TimeUnit.HOURS
        );
    }
    
    private boolean isCancelled(String executionId) {
        // ❌ 包含技术细节
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(CANCEL_KEY_PREFIX + executionId)
        );
    }
    
    private boolean checkPause(...) {
        // ❌ 直接操作 Redis Set
        RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
        pendingSet.add(executionId);
    }
}
```

**重构后**:
```java
@Service
@RequiredArgsConstructor
public class SchedulerService {
    
    // ✅ 只依赖 Domain 层定义的 Repository 接口
    private final ExecutionRepository executionRepository;
    private final CheckpointRepository checkpointRepository;
    // ... 其他 Repository
    
    // ❌ 移除这些依赖
    // private final StringRedisTemplate redisTemplate;
    // private final RedissonClient redissonClient;
    
    /**
     * 取消工作流执行
     * 业务逻辑: 标记执行为已取消
     */
    public void cancelExecution(String executionId) {
        log.info("[Scheduler] Cancelling execution: {}", executionId);
        
        // ✅ 通过 Repository 操作
        // Repository 内部会处理 Redis key 命名、TTL 等技术细节
        Execution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
        
        execution.cancel();  // 领域对象方法
        executionRepository.update(execution);
    }
    
    /**
     * 检查是否已取消
     */
    private boolean isCancelled(String executionId) {
        // ✅ 通过 Repository 查询
        return executionRepository.findById(executionId)
            .map(Execution::isCancelled)
            .orElse(false);
    }
    
    /**
     * 检查是否需要暂停等待人工审核
     */
    private boolean checkPause(...) {
        // 业务逻辑...
        if (needsHumanReview) {
            // ✅ 通过 Repository 操作
            execution.pause(nodeId, phase);
            executionRepository.update(execution);
            return true;
        }
        return false;
    }
}
```

**重构要点**:
- Application 层不再直接操作 Redis
- 所有 Redis 操作都通过 Repository 接口
- 业务逻辑保持在 Application 层和 Domain 层
- Repository 负责技术实现细节

### 3.2 Infrastructure 层 - Repository 重构设计

#### 3.2.1 RedisCheckpointRepository 重构

**重构策略**: 替换 `StringRedisTemplate` 为 `IRedisService`

**重构前**:
```java
@Repository
@RequiredArgsConstructor
public class RedisCheckpointRepository implements CheckpointRepository {
    
    private final StringRedisTemplate redisTemplate;  // ❌ 直接依赖
    private final ObjectMapper objectMapper;
    
    @Override
    public void save(Checkpoint checkpoint) {
        String key = KEY_PREFIX + checkpoint.getExecutionId() + ":" + checkpoint.getCurrentNodeId();
        String value = objectMapper.writeValueAsString(checkpoint);
        
        // ❌ 使用 StringRedisTemplate
        redisTemplate.opsForValue().set(key, value, TTL_HOURS, TimeUnit.HOURS);
    }
    
    @Override
    public Optional<Checkpoint> findLatest(String executionId) {
        String pattern = KEY_PREFIX + executionId + ":*";
        
        // ❌ 使用 StringRedisTemplate
        var keys = redisTemplate.keys(pattern);
        // ...
    }
}
```

**重构后**:
```java
@Repository
@RequiredArgsConstructor
public class RedisCheckpointRepository implements CheckpointRepository {
    
    private final IRedisService redisService;  // ✅ 使用封装的服务
    private final ObjectMapper objectMapper;
    
    private static final String KEY_PREFIX = "workflow:checkpoint:";
    private static final long TTL_HOURS = 24;
    
    @Override
    public void save(Checkpoint checkpoint) {
        try {
            // 业务逻辑: 构建 key
            String key = KEY_PREFIX + checkpoint.getExecutionId() + ":" + checkpoint.getCurrentNodeId();
            
            // 业务逻辑: 序列化对象
            String value = objectMapper.writeValueAsString(checkpoint);
            
            // ✅ 使用 IRedisService 的基础操作
            redisService.setString(key, value, TTL_HOURS, TimeUnit.HOURS);
            
            // 业务逻辑: 如果是暂停点,额外保存
            if (checkpoint.isPausePoint()) {
                String pauseKey = PAUSE_KEY_PREFIX + checkpoint.getExecutionId();
                redisService.setString(pauseKey, value, TTL_HOURS, TimeUnit.HOURS);
            }
            
            log.debug("[Checkpoint] Saved: {}", checkpoint.getCheckpointId());
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to save: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save checkpoint", e);
        }
    }
    
    @Override
    public Optional<Checkpoint> findLatest(String executionId) {
        try {
            // 业务逻辑: 构建查询模式
            String pattern = KEY_PREFIX + executionId + ":*";
            
            // ✅ 使用 IRedisService 的基础操作
            var keys = redisService.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Optional.empty();
            }
            
            // 业务逻辑: 选择最新的 key
            String latestKey = keys.stream()
                    .max(String::compareTo)
                    .orElse(null);
            
            if (latestKey == null) {
                return Optional.empty();
            }
            
            // ✅ 使用 IRedisService 的基础操作
            String value = redisService.getString(latestKey);
            
            if (value == null) {
                return Optional.empty();
            }
            
            // 业务逻辑: 反序列化对象
            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find latest: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public void deleteByExecutionId(String executionId) {
        try {
            // 业务逻辑: 构建删除模式
            String pattern = KEY_PREFIX + executionId + ":*";
            
            // ✅ 使用 IRedisService 的基础操作
            var keys = redisService.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
            }
            
            // 业务逻辑: 删除暂停点
            redisService.delete(PAUSE_KEY_PREFIX + executionId);
            
            log.debug("[Checkpoint] Deleted all for execution: {}", executionId);
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to delete: {}", e.getMessage(), e);
        }
    }
}
```

**重构要点**:
- Repository 包含业务逻辑 (key 命名、序列化、TTL 策略)
- IRedisService 只提供基础操作 (setString, getString, keys, delete)
- Repository 组合多个基础操作实现复杂业务逻辑

#### 3.2.2 RedisExecutionRepository 重构

**重构映射**:
```java
// ❌ 重构前
redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
redisTemplate.opsForValue().get(key);
redisTemplate.opsForSet().add(key, values);
redisTemplate.opsForSet().members(key);
redisTemplate.opsForValue().multiGet(keys);
redisTemplate.keys(pattern);
redisTemplate.delete(keys);

// ✅ 重构后
redisService.setString(key, value, timeout, TimeUnit.SECONDS);
redisService.getString(key);
redisService.addToSet(key, values);
redisService.getSetMembers(key);
redisService.multiGetString(keys);
redisService.keys(pattern);
redisService.delete(keys);
```

#### 3.2.3 RedisSsePublisher 重构

**重构映射**:
```java
// ❌ 重构前
redisTemplate.convertAndSend(channel, message);

// ✅ 重构后
redisService.publish(channel, message);
```

#### 3.2.4 RedisVerificationCodeRepository 重构

**重构策略**: 同 RedisCheckpointRepository,使用 IRedisService 替换 StringRedisTemplate

## 4. 数据模型设计

### 4.1 Redis Key 设计

| 功能 | Key 格式 | 类型 | 过期时间 | 说明 |
|------|---------|------|---------|------|
| 工作流取消标记 | `workflow:cancel:{executionId}` | String | 1 小时 | 标记工作流已取消 |
| 待审核队列 | `human_review:pending` | Set | 永久 | 存储待审核的执行 ID |
| 检查点数据 | `checkpoint:{executionId}` | String | 根据配置 | 工作流检查点 |
| 执行状态 | `execution:{executionId}` | String | 根据配置 | 执行状态数据 |
| SSE 频道 | `sse:channel:{userId}` | Pub/Sub | - | SSE 消息频道 |
| 验证码 | `verification:{email}` | String | 5 分钟 | 用户验证码 |

### 4.2 数据流设计

```
┌──────────────┐
│ Application  │
│   Layer      │
└──────┬───────┘
       │ 调用 Port 接口
       ↓
┌──────────────┐
│   Domain     │
│   Layer      │
│  (Port 接口)  │
└──────┬───────┘
       │ 实现
       ↓
┌──────────────┐
│Infrastructure│
│   Layer      │
│  (Adapter)   │
└──────┬───────┘
       │ 使用
       ↓
┌──────────────┐
│ IRedisService│
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    Redis     │
└──────────────┘
```

## 5. 接口设计

### 5.1 IRedisService 扩展

**已扩展的方法** (已实现):
```java
// 字符串操作
void setString(String key, String value, long timeout, TimeUnit timeUnit);
String getString(String key);
List<String> multiGetString(Collection<String> keys);

// 键操作
Set<String> keys(String pattern);
void delete(String key);
void delete(Collection<String> keys);
boolean isExists(String key);

// Set 操作
void addToSet(String key, String... values);
Set<String> getSetMembers(String key);
void removeFromSet(String key, String... values);
boolean isSetMember(String key, String value);

// 发布订阅
void publish(String channel, String message);

// 分布式锁
RLock getLock(String key);
RSet<String> getSet(String key);
```

### 5.2 Port 接口总结

| 接口名 | 方法数 | 职责 | 实现类 |
|--------|--------|------|--------|
| WorkflowCancellationPort | 2 | 工作流取消管理 | RedisWorkflowCancellationAdapter |
| HumanReviewQueuePort | 3 | 人工审核队列管理 | RedisHumanReviewQueueAdapter |

## 6. 异常处理设计

### 6.1 异常策略
- Port 接口方法不抛出检查异常
- Adapter 实现捕获 Redis 异常并转换为运行时异常
- 关键操作添加日志记录

### 6.2 异常示例
```java
@Override
public void markAsCancelled(String executionId) {
    try {
        String key = CANCEL_KEY_PREFIX + executionId;
        redisService.setString(key, "true", CANCEL_EXPIRY_HOURS, TimeUnit.HOURS);
        log.info("[WorkflowCancellation] Marked as cancelled: {}", executionId);
    } catch (Exception e) {
        log.error("[WorkflowCancellation] Failed to mark as cancelled: {}", executionId, e);
        throw new InfrastructureException("Failed to mark workflow as cancelled", e);
    }
}
```

## 7. 测试设计

### 7.1 单元测试策略

#### Application 层测试
```java
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    
    @Mock
    private WorkflowCancellationPort cancellationPort;
    
    @Mock
    private HumanReviewQueuePort humanReviewQueuePort;
    
    @InjectMocks
    private SchedulerService schedulerService;
    
    @Test
    void should_mark_as_cancelled_when_cancel_execution() {
        // Given
        String executionId = "exec-123";
        
        // When
        schedulerService.cancelExecution(executionId);
        
        // Then
        verify(cancellationPort).markAsCancelled(executionId);
    }
}
```

#### Infrastructure 层测试
```java
@SpringBootTest
class RedisWorkflowCancellationAdapterTest {
    
    @Autowired
    private RedisWorkflowCancellationAdapter adapter;
    
    @Autowired
    private IRedisService redisService;
    
    @Test
    void should_mark_and_check_cancellation() {
        // Given
        String executionId = "exec-123";
        
        // When
        adapter.markAsCancelled(executionId);
        
        // Then
        assertTrue(adapter.isCancelled(executionId));
        
        // Cleanup
        redisService.delete("workflow:cancel:" + executionId);
    }
}
```

### 7.2 集成测试策略
- 使用 Testcontainers 启动 Redis
- 测试完整的调用链路
- 验证数据一致性

## 8. 部署设计

### 8.1 部署策略
- 分阶段部署,逐步替换
- 每个阶段独立可回滚
- 保持向后兼容

### 8.2 部署步骤
1. 部署 Domain 层新增接口
2. 部署 Infrastructure 层 Adapter 实现
3. 部署 Application 层重构代码
4. 验证功能正常
5. 移除旧代码

### 8.3 回滚方案
- 保留旧代码分支
- 数据格式保持兼容
- 可快速切换回旧实现

## 9. 监控设计

### 9.1 日志记录
- 关键操作添加 INFO 日志
- 异常情况添加 ERROR 日志
- 包含执行 ID 等上下文信息

### 9.2 指标监控
- Redis 操作延迟
- 操作成功率
- 异常发生率

## 10. 性能考虑

### 10.1 性能目标
- Redis 操作延迟 < 10ms (P99)
- 不增加额外的网络开销
- 内存使用保持不变

### 10.2 性能优化
- 使用连接池
- 批量操作优化
- 合理设置过期时间

## 11. 安全考虑

### 11.1 数据安全
- 敏感数据不存储在 Redis
- 使用合理的过期时间
- 定期清理过期数据

### 11.2 访问控制
- 通过 Spring Security 控制访问
- 验证执行 ID 的合法性
- 防止未授权访问

## 12. 文档设计

### 12.1 代码文档
- 所有 Port 接口添加 Javadoc
- 关键方法添加注释
- 说明设计意图

### 12.2 架构文档
- 更新架构图
- 说明重构原因和目标
- 提供迁移指南

## 13. 总结

### 13.1 设计优势
- 清晰的分层架构
- 业务逻辑与技术实现解耦
- 易于测试和维护
- 符合 DDD 最佳实践

### 13.2 设计权衡
- 增加了接口层,代码量略有增加
- 需要团队理解 DDD 概念
- 初期重构需要投入时间

### 13.3 后续优化
- 考虑引入缓存策略
- 优化批量操作性能
- 完善监控和告警
