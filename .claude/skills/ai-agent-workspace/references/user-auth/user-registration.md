---
name: user-registration-sop
description: >-
  用户注册业务 SOP。用于开发、修改、排查或验证用户注册相关功能，包括邮箱验证码发送、
  注册流程、密码强度策略、限流规则、JWT 响应。此 SOP 必须与工作区运行 skill 一起使用
  以保留最近日志和全局安全规则。触发词：
  "用户注册", "邮箱注册", "发送验证码", "注册接口", "register", "sendCode",
  "验证码失效", "邮箱已注册", "密码强度", "注册限流".
---

# 用户注册 SOP

## 目的

覆盖用户邮箱注册的完整业务流程：发送验证码 → 验证 → 创建账号 → 返回 JWT。
可用于功能开发、Bug 排查和验证。

## 触发条件

- 用户提到"用户注册"、"邮箱注册"、"发送验证码"、"register"、"sendCode"
- 需要开发或修改注册相关业务逻辑
- 排查注册失败、验证码无效、邮箱已注册等问题

## 范围

- **接口**：`ai-agent-interfaces` — `UserController.java`
- **应用层**：`ai-agent-application` — `UserApplicationService.java`
- **领域层**：`ai-agent-domain` — `UserAuthenticationDomainService.java`、`User.java`、`Email.java`、`Credential.java`
- **基础设施**：`ai-agent-infrastructure` — `UserRepositoryImpl.java`、`RedisVerificationCodeRepository.java`、`EmailLogMapper.java`
- **数据库表**：`user_account`（MySQL）、验证码（Redis，TTL 5分钟）

## 必要输入

- **注册请求字段**：`email`（必填，合法邮箱格式）、`code`（必填，6位验证码）、`password`（必填，≥8位）、`username`（可选，缺省取邮箱前缀）、`deviceId`（可选，缺省自动生成 UUID）
- **凭据政策**：邮件服务凭据通过环境变量注入（不入代码），JWT secret 通过配置文件注入

## 开工前读取

1. 工作区入口：`SKILL.md`
2. 最近日志：`logs/2026-05.md`
3. 域索引：`references/user-auth/index.md`
4. 本 SOP：当前文件

## 停止条件

出现以下情况时停止，询问或准备确认包：

- 需要修改 `user_account` 表结构（DDL 变更，需确认对已有数据的影响）
- 修改密码加密算法（BCrypt → 其他），需评估历史密码兼容性
- 修改限流参数（影响所有用户注册行为）
- 邮件服务凭据缺失或有歧义

## 注册业务流

### 完整注册路径

```text
Step 1: POST /client/user/email/sendCode
  → UserController.sendEmailCode
  → UserApplicationService.sendEmailCode
  → UserAuthenticationDomainService.sendVerificationCode
      ├─ Email.of(emailStr)           # 邮箱格式校验（值对象）
      ├─ RateLimiter.tryAcquire       # 限流：1次/60秒/邮箱（Redis 计数）
      ├─ verificationCodeRepository.save(email, code, 300s)  # 先持久化到 Redis
      └─ emailService.sendVerificationCodeAsync(...)         # 再异步发邮件

Step 2: POST /client/user/email/register
  → UserController.registerByEmail
  → UserApplicationService.registerByEmail  ([@Transactional])
  → UserAuthenticationDomainService.register
      ├─ verificationCodeRepository.get(email)  # 验证码比对
      ├─ userRepository.existsByEmail(email)    # 邮箱查重
      ├─ validatePasswordStrength(password)     # 密码 ≥ 8 位
      ├─ Credential.fromEncrypted(BCryptUtil.encode(password))
      ├─ new User(username, email, credential)
      ├─ userRepository.save(newUser)           # 写 MySQL user_account
      └─ verificationCodeRepository.remove(email)  # 销毁验证码（防重复使用）
  → buildLoginResponse(user, deviceId)
      ├─ tokenService.createToken(user)          # 生成 Access JWT
      └─ tokenService.createRefreshToken(user, deviceId)  # 生成 Refresh JWT
  → 返回 UserLoginResponse {token, refreshToken, expireIn, deviceId, user}
```

### 关键限流规则

| 场景 | 限制 | 窗口 | Redis Key 前缀 |
|---|---|---|---|
| 发送验证码 | 1 次 | 60 秒 | `rate_limit:email:` |
| 登录失败锁定 | 5 次失败 | 900 秒（15分钟） | `rate_limit:login_failure:` |

### 异常码

| ErrorCode | 含义 | HTTP 场景 |
|---|---|---|
| `RATE_LIMITED` | 验证码发送频率超限 | 60秒内重复请求 |
| `INVALID_VERIFICATION_CODE` | 验证码错误或过期 | 输错 / Redis TTL 到期 |
| `EMAIL_ALREADY_REGISTERED` | 邮箱已注册 | 邮箱查重失败 |
| `WEAK_PASSWORD` | 密码强度不足（< 8位） | 密码过短 |

## 操作步骤

### 1. 定界

确认业务动作（功能开发 / Bug 排查）、目标环境（本地 / 生产）、影响面（注册流程 / 验证码系统 / JWT 策略）。

### 2. 只读调查

开发前必须先搜索真实代码，不猜测：

```bash
# 定位注册主链路
grep -r "registerByEmail\|sendVerificationCode\|EmailAlreadyRegistered" \
  /home/zj669/repo/ai-agent/ai-agent-domain/src --include="*.java" -l

# 查看 Redis 验证码存储实现
grep -r "IVerificationCodeRepository" \
  /home/zj669/repo/ai-agent/ai-agent-infrastructure/src --include="*.java" -l

# 查看数据库 schema
grep -A 30 "user_account" \
  /home/zj669/repo/ai-agent/ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql
```

排查时查看应用日志中的关键字：`Sending email code`、`Invalid verification code`、`Email already registered`、`User registered successfully`。

### 3. 操作计划

按改动范围分级：

| 变更类型 | 风险 | 需确认 |
|---|---|---|
| 修改接口参数/校验规则 | 低 | 否 |
| 修改领域服务业务逻辑 | 中 | 否，但需单测 |
| 修改 `user_account` 表结构 | 高 | **是**，确认存量数据影响 |
| 修改 BCrypt 加密策略 | 高 | **是**，需要密码迁移方案 |
| 修改限流参数 | 中 | 建议确认 |

### 4. 确认（高风险变更）

如果涉及 DDL 变更，确认包应包含：
- 变更 SQL（完整）
- 执行后检查 SQL（`SELECT COUNT(*) FROM user_account`）
- 回滚方案

### 5. 执行

DDD 层次约束（必须遵守）：

- `UserController` 只做参数校验和 DTO 转换，不含业务逻辑
- `UserApplicationService` 只做用例编排和事务边界（`@Transactional`），不含业务规则
- `UserAuthenticationDomainService` 拥有所有注册业务规则（验证码、查重、密码强度、加密）
- `User`、`Email`、`Credential` 是领域实体/值对象，不依赖任何框架

代码修改后运行：

```bash
# 只跑用户相关测试
mvn test -pl ai-agent-domain -Dtest="*User*"
mvn test -pl ai-agent-application -Dtest="*User*"

# 完整构建
mvn clean install -DskipTests
```

### 6. 验证

```bash
# 发送验证码
curl -X POST http://localhost:8080/client/user/email/sendCode \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# 注册（需先从邮件/日志获取验证码）
curl -X POST http://localhost:8080/client/user/email/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"123456","password":"Test1234","username":"testuser"}'
```

预期成功响应：

```json
{
  "code": 0,
  "data": {
    "token": "...",
    "refreshToken": "...",
    "expireIn": 7200,
    "deviceId": "...",
    "user": { "id": 1, "email": "test@example.com", "username": "testuser", ... }
  }
}
```

验证边界 case：

| 场景 | 预期 |
|---|---|
| 验证码 60 秒内重发 | `RATE_LIMITED` |
| 验证码 5 分钟后使用 | `INVALID_VERIFICATION_CODE` |
| 已注册邮箱再注册 | `EMAIL_ALREADY_REGISTERED` |
| 密码 < 8 位 | `WEAK_PASSWORD` |

### 7. 回写

- 功能变更记录到 `logs/2026-05.md`（今天的事实）
- 如果发现新的重复故障签名，追加到 `references/incidents/index.md`
- 如果业务规则发生长期变化（如限流参数调整），更新本 SOP

## 安全边界

- **禁止**：在日志或文档中记录真实验证码、密码明文或 JWT token
- **禁止**：绕过 `UserAuthenticationDomainService` 直接操作 `IUserRepository` 注册用户（破坏业务规则闭环）
- **禁止**：将密码以明文形式传输或存储（必须 BCrypt 加密）
- **requires-confirmation**：修改 `user_account` 表、修改密码加密算法、修改全局限流参数

## 代码定位规则

- 注册入口：`UserController.java:35-41`（`registerByEmail`）
- 验证码发送：`UserAuthenticationDomainService.java:50-68`（`sendVerificationCode`）
- 注册核心逻辑：`UserAuthenticationDomainService.java:80-113`（`register`）
- Redis 验证码存储：`RedisVerificationCodeRepository.java`（搜索 `IVerificationCodeRepository`）
- 密码加密：`BCryptUtil.java`（搜索 `BCryptUtil`）
- 限流 Redis Key：`RedisKeyConstants.RateLimit`

## 验证

```text
验证:
- 直接验证: curl 发送验证码 + 注册接口，检查 HTTP 200 和 token 非空
- 依赖链验证: Redis 验证码 key 在注册后消失；MySQL user_account 新增记录
- 端到端验证: 注册后用返回的 token 调用 GET /client/user/info，验证用户信息正确
- 未验证: 邮件实际送达（需真实邮件服务环境）
```

## 输出形态

```text
业务动作: 用户注册
目标范围: 邮箱 + 验证码流程
执行结果: <成功/失败及原因>
验证结果: <直接验证/依赖链验证/端到端验证>
风险与回滚: <如有 DDL 变更，说明回滚方案>
SOP 回写: <如有规则变化，已更新 logs/ 或 incidents/>
```

## 知识回写

- 今天功能变更 → `logs/2026-05.md`
- 新发现的注册相关故障签名 → `references/incidents/index.md`
- 注册规则长期变化（密码策略、限流、字段新增）→ 更新本 SOP
