# 代码质量规范 (Code Quality Standards)

## 代码审查清单

### 编码前检查
- [ ] 是否搜索了现有代码？（参考 codeReuse.md）
- [ ] 是否检查了数据库 Schema？
- [ ] 是否使用了项目封装的 Service 而非底层 SDK？

### 编码中检查
- [ ] 类名、方法名是否符合命名规范？
- [ ] 是否添加了必要的注释（中文可接受）？
- [ ] 是否处理了异常情况？
- [ ] 是否考虑了并发安全？

### 编码后检查
- [ ] 是否使用 getDiagnostics 检查了语法错误？
- [ ] 是否验证了编译通过？
- [ ] 是否考虑了单元测试？（用户明确要求时）

## 禁止的反模式

### 1. 直接使用底层 SDK
❌ **错误**：
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void saveData(String key, Object value) {
    redisTemplate.opsForValue().set(key, value);
}
```

✅ **正确**：
```java
@Autowired
private RedisService redisService; // 使用项目封装的服务

public void saveData(String key, Object value) {
    redisService.set(key, value);
}
```

### 2. 创建重复的数据表
❌ **错误**：
```sql
-- 已有 workflow_node_execution_log 表
CREATE TABLE workflow_execution ( -- 功能重叠！
    id BIGINT PRIMARY KEY,
    workflow_id VARCHAR(64),
    status VARCHAR(20)
);
```

✅ **正确**：
```java
// 扩展现有表或建立关联
ALTER TABLE workflow_node_execution_log 
ADD COLUMN parent_execution_id BIGINT;
```

### 3. 不处理异常
❌ **错误**：
```java
public User getUser(Long id) {
    return userRepository.findById(id).get(); // 可能抛出 NoSuchElementException
}
```

✅ **正确**：
```java
public User getUser(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));
}
```

## 性能优化原则

### 1. 数据库查询
- 避免 N+1 查询问题
- 使用批量操作代替循环单条操作
- 合理使用索引

### 2. 缓存策略
- 优先使用项目的 RedisService
- 设置合理的过期时间
- 考虑缓存穿透、击穿、雪崩

### 3. 并发控制
- 使用分布式锁（Redisson）
- 避免长事务
- 合理使用线程池

## 安全规范

### 1. 输入验证
- 所有外部输入必须验证
- 使用 @Valid 注解进行参数校验
- 防止 SQL 注入（使用 MyBatis 参数化查询）

### 2. 敏感信息
- 密码必须加密存储
- API Key 不能硬编码
- 日志中不能打印敏感信息

### 3. 权限控制
- 检查用户权限
- 使用 JWT 进行身份验证
- 实现细粒度的资源访问控制

## 日志规范

### 日志级别
- **ERROR**：系统错误，需要立即处理
- **WARN**：警告信息，可能影响功能
- **INFO**：关键业务流程
- **DEBUG**：详细调试信息（生产环境关闭）

### 日志内容
```java
// ✅ Good - 包含上下文信息
log.info("创建 Agent 成功, agentId={}, userId={}", agentId, userId);
log.error("调用 OpenAI API 失败, model={}, error={}", model, e.getMessage(), e);

// ❌ Bad - 信息不足
log.info("创建成功");
log.error("失败");
```

## 测试规范

### 单元测试
- 测试类命名：`XxxTest.java`
- 测试方法命名：`should_xxx_when_yyy()`
- 使用 Mock 隔离外部依赖

### 集成测试
- 使用 @SpringBootTest
- 使用测试数据库
- 清理测试数据

## 代码审查要点

在提交代码前，我会自动检查：
1. 是否符合项目架构规范
2. 是否复用了现有代码
3. 是否使用了封装的 Service
4. 是否处理了异常
5. 是否添加了必要的注释
6. 是否通过了编译检查
