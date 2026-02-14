## Metadata
- file: `.blueprint/domain/auth/AuthService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AuthService
- 该文件用于描述 AuthService 的职责边界与协作关系。

## 2) 核心方法
- `register()`
- `login()`
- `resetPassword()`
- `sendVerificationCode()`
- `createToken()`

## 3) 具体方法
### 3.1 register()
- 函数签名: `User register(String emailStr, String code, String password, String username)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `code`: 验证码
  - `password`: 密码
  - `username`: 用户名（可选）
- 出参: User 实体（创建的用户）
- 功能含义: 邮箱注册，验证验证码、邮箱查重、密码强度校验，创建用户并持久化。
- 链路作用: 在 UserApplicationService.registerByEmail() 中调用，执行注册业务逻辑。

### 3.2 login()
- 函数签名: `User login(String emailStr, String password, String ip)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `password`: 密码
  - `ip`: 登录IP
- 出参: User 实体（登录成功的用户）
- 功能含义: 用户登录，检查登录失败限流，验证密码，检查用户状态，记录登录成功。
- 链路作用: 在 UserApplicationService.login() 中调用，执行登录业务逻辑。

### 3.3 resetPassword()
- 函数签名: `void resetPassword(String emailStr, String code, String newPassword, String confirmPassword)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `code`: 验证码
  - `newPassword`: 新密码
  - `confirmPassword`: 确认密码
- 出参: 无（void）
- 功能含义: 重置密码，验证两次密码一致、验证码有效、密码强度，更新用户密码。
- 链路作用: 在 UserApplicationService.resetPassword() 中调用，执行密码重置逻辑。

### 3.4 sendVerificationCode()
- 函数签名: `void sendVerificationCode(String emailStr)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 目标邮箱
- 出参: 无（void）
- 功能含义: 发送邮箱验证码，限流检查（1分钟1次），生成安全验证码，保存到 Redis（5分钟有效），异步发送邮件。
- 链路作用: 在 UserApplicationService.sendEmailCode() 中调用，执行验证码发送逻辑。

### 3.5 createToken()
- 函数签名: `String createToken(User user)`（ITokenService）
- 入参:
  - `user`: 用户实体
- 出参: JWT Access Token 字符串
- 功能含义: 创建 JWT Access Token，包含用户ID、邮箱等信息，设置过期时间（默认2小时）。
- 链路作用: 在 UserApplicationService.buildLoginResponse() 中调用，生成登录凭证。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 UserAuthenticationDomainService.java 和 ITokenService 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
