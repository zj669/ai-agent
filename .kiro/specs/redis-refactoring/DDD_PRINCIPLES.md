# DDD 分层架构原则

## 核心理念

**Application 层不应该包含任何技术实现细节,只负责编排领域对象完成业务用例。**

## 正确的分层职责

### Domain 层 (领域层)

**职责**:
- 定义业务实体 (Entity)
- 定义值对象 (Value Object)
- 定义领域服务 (Domain Service)
- **定义端口接口 (Port/Repository Interface)**
- 包含核心业务逻辑

**不应该**:
- 依赖任何基础设施组件
- 包含技术实现细节
- 直接操作数据库、Redis、消息队列等

**示例**:
```java
// ✅ 正确: 定义业务接口
public interface WorkflowCancellationPort {
    void markAsCancelled(String executionId);
    boolean isCancelled(String executionId);
}
```

### Application 层 (应用层)

**职责**:
- 编排领域对象完成用例
- 处理事务边界
- 发布领域事件
- **调用 Domain 层定义的接口**

**不应该**:
- 直接依赖技术组件 (如 `RedisTemplate`, `RestTemplate`)
- 包含技术实现细节 (如 Redis key 的命名规则)
- 直接操作基础设施 (数据库、缓存、消息队列)

**示例**:
```java
// ✅ 正确: 依赖抽象接口
@Service
public class SchedulerService {
    private final WorkflowCancellationPort cancellationPort;  // Domain 接口
    
    public void cancelExecution(String id) {
        cancellationPort.markAsCancelled(id);  // 调用业务方法
    }
}

// ❌ 错误: 直接依赖技术组件
@Service
public class SchedulerService {
    private final StringRedisTemplate redisTemplate;  // 技术组件
    
    public void cancelExecution(String id) {
        redisTemplate.opsForValue().set("cancel:" + id, "true");  // 技术细节
    }
}
```

### Infrastructure 层 (基础设施层)

**职责**:
- **实现 Domain 层定义的接口**
- 使用技术组件 (Redis, MySQL, HTTP Client 等)
- 处理所有技术细节
- 数据格式转换 (PO ↔ Entity)

**示例**:
```java
// ✅ 正确: 实现 Domain 接口
@Repository
public class RedisWorkflowCancellationAdapter implements WorkflowCancellationPort {
    private final IRedisService redisService;  // 使用封装的服务
    
    @Override
    public void markAsCancelled(String executionId) {
        redisService.setString("workflow:cancel:" + executionId, "true", 1, TimeUnit.HOURS);
    }
}
```

## 依赖方向

```
┌─────────────────────────────────────┐
│     Interfaces Layer (REST API)     │
└──────────────┬──────────────────────┘
               │ 调用
               ↓
┌─────────────────────────────────────┐
│     Application Layer (用例编排)     │
│  - 不包含技术细节                     │
│  - 只调用 Domain 接口                │
└──────────────┬──────────────────────┘
               │ 调用
               ↓
┌─────────────────────────────────────┐
│     Domain Layer (业务逻辑)          │
│  - 定义接口 (Port/Repository)        │
│  - 不依赖基础设施                     │
└──────────────┬──────────────────────┘
               │ 被实现
               ↓
┌─────────────────────────────────────┐
│  Infrastructure Layer (技术实现)     │
│  - 实现 Domain 接口                  │
│  - 使用技术组件                       │
└─────────────────────────────────────┘
```

## 常见错误模式

### 错误 1: Application 层直接使用技术组件

```java
// ❌ 错误
@Service
public class OrderService {
    private final RedisTemplate redisTemplate;  // 直接依赖
    private final RestTemplate restTemplate;    // 直接依赖
    
    public void createOrder(Order order) {
        // 包含技术细节
        redisTemplate.opsForValue().set("order:" + order.getId(), order);
        restTemplate.postForObject("http://api.example.com/orders", order, String.class);
    }
}
```

**为什么错误**:
- Application 层不应该知道使用的是 Redis 还是其他缓存
- Application 层不应该知道 HTTP 调用的细节
- 违反了依赖倒置原则 (DIP)

**正确做法**:
```java
// ✅ 正确
@Service
public class OrderService {
    private final OrderCachePort cachePort;      // Domain 接口
    private final PaymentGatewayPort gatewayPort; // Domain 接口
    
    public void createOrder(Order order) {
        cachePort.save(order);           // 业务语义
        gatewayPort.processPayment(order); // 业务语义
    }
}
```

### 错误 2: Domain 层依赖基础设施

```java
// ❌ 错误: Domain 层不应该有这样的实现
public class Order {
    private final RedisTemplate redisTemplate;  // 错误!
    
    public void save() {
        redisTemplate.opsForValue().set("order:" + this.id, this);
    }
}
```

**正确做法**:
```java
// ✅ 正确: Domain 层只定义接口
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
}

// Infrastructure 层实现
@Repository
public class RedisOrderRepository implements OrderRepository {
    private final IRedisService redisService;
    
    @Override
    public void save(Order order) {
        redisService.setValue("order:" + order.getId(), order);
    }
}
```

### 错误 3: 在接口中暴露技术细节

```java
// ❌ 错误: 接口名暴露了技术选型
public interface RedisOrderCache {
    void setToRedis(String key, Order order);
    Order getFromRedis(String key);
}
```

**正确做法**:
```java
// ✅ 正确: 接口使用业务语言
public interface OrderCachePort {
    void cache(Order order);
    Optional<Order> retrieve(String orderId);
}
```

## 分布式锁的特殊情况

分布式锁是一个特殊的场景,因为:
1. 锁本身是基础设施能力
2. 但锁的使用逻辑(何时加锁、锁的粒度)是业务逻辑

**推荐做法**:

```java
// Application 层
@Service
public class SchedulerService {
    private final IRedisService redisService;  // 获取锁的能力
    
    private void onNodeComplete(String executionId, ...) {
        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);  // 获取锁
        
        try {
            lock.lock(30, TimeUnit.SECONDS);  // 业务决定锁的时长
            // 业务逻辑...
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**为什么可以这样**:
- `IRedisService` 是项目封装的服务,不是底层 SDK
- 锁的使用模式(加锁时机、超时时间)是业务逻辑的一部分
- 如果未来需要更换锁的实现,只需修改 `IRedisService` 的实现

## 重构检查清单

在重构时,问自己这些问题:

- [ ] Application 层是否直接依赖了 `RedisTemplate`、`RestTemplate` 等技术组件?
- [ ] Application 层是否包含了 Redis key 命名、HTTP URL 等技术细节?
- [ ] Domain 层是否定义了清晰的业务接口?
- [ ] Infrastructure 层是否使用了封装的 `IRedisService` 而非底层 SDK?
- [ ] 接口命名是否使用了业务语言而非技术术语?

## 总结

**核心原则**: 
- Domain 层定义"做什么" (What)
- Infrastructure 层实现"怎么做" (How)
- Application 层编排"谁做什么" (Who does What)

**记住**: Application 层应该像一个指挥家,只负责协调各个乐手(领域对象)演奏,而不应该自己去拉小提琴(操作技术组件)。
