# 认证与授权模块 (Authentication & Authorization)

## 职责边界

**领域服务**: `UserAuthenticationDomainService`
- 用户注册 (邮箱验证码校验、密码强度检查、用户创建)
- 用户登录 (凭证验证、状态检查、登录记录)
- 密码重置 (验证码校验、密码更新)
- 验证码发送 (限流控制、邮件发送)

**端口接口**: `ITokenService`
- Token 创建 (JWT 签发)
- Token 验证 (签名验证 + 黑名单检查)
- Token 失效 (加入黑名单)
- 用户 ID 提取 (从 Token 解析)

**拦截器**: `LoginInterceptor`
- 认证策略选择 (Debug 模式 / JWT 模式)
- UserContext 设置 (ThreadLocal 用户上下文)
- 请求后清理 (防止内存泄漏)

## 核心实体与值对象

### User (聚合根)
```java
- id: Long
- username: String
- email: Email (值对象)
- credential: Credential (值对象)
- phone: String
- avatarUrl: String
- status: UserStatus (枚举)
- lastLoginIp: String
- lastLoginTime: LocalDateTime
```

### Email (值对象)
- 邮箱格式验证
- 不可变对象

### Credential (值对象)
- 密码加密存储 (BCrypt)
- 密码验证方法
- 不可变对象

### UserStatus (枚举)
- NORMAL (1): 正常
- DISABLED (0): 禁用

## 认证流程

### 1. 注册流程
```
用户请求 → 发送验证码 (限流) → 验证码存储 (Redis, TTL=5min)
       → 注册请求 → 验证码校验 → 邮箱查重 → 密码强度检查
       → 创建用户 → 生成 Token → 返回登录响应
```

### 2. 登录流程
```
用户请求 → 邮箱查找 → 密码验证 → 状态检查 → 记录登录信息
       → 生成 Token → 返回登录响应
```

### 3. Token 验证流程
```
请求拦截 → 提取 Token → JWT 签名验证 → Redis 黑名单检查
        → 解析用户 ID → 设置 UserContext → 放行请求
```

### 4. 登出流程
```
用户请求 → 提取 Token → 计算剩余有效期 → 加入 Redis 黑名单
        → 清理 UserContext
```

## 安全机制

### 1. 密码安全
- **加密算法**: BCrypt (自动加盐)
- **强度要求**: 最小长度 8 位
- **存储**: 加密后存储,不可逆

### 2. Token 安全
- **算法**: HMAC-SHA256
- **有效期**: 7 天 (604800000ms)
- **黑名单**: Redis 存储已失效 Token
- **Claims**: userId (subject), email, jti (唯一 ID)

### 3. 限流保护
- **邮件发送**: 1 次/分钟 (滑动窗口)
- **实现**: `RedisSlidingWindowRateLimiter`

### 4. 会话管理
- **上下文**: ThreadLocal 存储当前用户 ID
- **清理**: 请求完成后自动清理,防止内存泄漏

## 数据存储

### MySQL (持久化)
- `user_info` 表: 用户基本信息、凭证、状态

### Redis (临时数据)
- `verification_code:{email}`: 验证码 (TTL=5min)
- `token_blacklist:{token}`: Token 黑名单 (TTL=剩余有效期)
- `rate_limit:email:{email}`: 邮件发送限流计数

## 已知问题与优化建议

### 安全增强
1. **Token 刷新机制**: 引入 Refresh Token,避免长期有效的 Access Token
2. **登录失败限流**: 防止暴力破解 (如 5 次失败锁定 15 分钟)
3. **密码复杂度**: 增加大小写字母、数字、特殊字符要求
4. **多端登录控制**: 支持单点登录或限制同时在线设备数
5. **审计日志**: 记录登录失败、密码修改、敏感操作

### 性能优化
1. **Token 验证顺序**: 先查 Redis 黑名单,再验签 (减少 CPU 开销)
2. **用户信息缓存**: 高频查询的用户信息缓存到 Redis
3. **验证码去重**: 防止同一邮箱短时间内重复发送

### 功能完善
1. **强制下线**: 管理员可强制用户下线
2. **会话列表**: 用户查看当前登录设备
3. **密码过期**: 定期强制修改密码
4. **二次验证**: 敏感操作需要二次验证

## 依赖关系

```
UserController (interfaces)
    ↓
UserApplicationService (application)
    ↓
UserAuthenticationDomainService (domain)
    ↓
IUserRepository, ITokenService (domain ports)
    ↓
UserRepositoryImpl, JwtTokenService (infrastructure)
```

## 接口清单

### 公开接口 (无需认证)
- `POST /client/user/email/sendCode` - 发送邮箱验证码
- `POST /client/user/email/register` - 邮箱注册
- `POST /client/user/login` - 用户登录
- `POST /client/user/resetPassword` - 重置密码

### 受保护接口 (需要认证)
- `GET /client/user/info` - 获取当前用户信息
- `POST /client/user/profile` - 修改用户信息
- `POST /client/user/logout` - 用户登出
