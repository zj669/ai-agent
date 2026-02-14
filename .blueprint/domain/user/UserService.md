## Metadata
- file: `.blueprint/domain/user/UserService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: UserService
- 该文件用于描述 UserApplicationService 和 UserAuthenticationDomainService 的职责边界与协作关系。UserApplicationService 负责用例编排、事务边界、DTO 转换；UserAuthenticationDomainService 封装用户注册、登录的核心业务逻辑。

## 2) 核心方法
- `register()`
- `login()`
- `updateProfile()`
- `changePassword()`

## 3) 具体方法
### 3.1 register()
- 函数签名: `User register(String emailStr, String code, String password, String username)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `code`: 验证码
  - `password`: 密码
  - `username`: 用户名（可选）
- 出参: User 实体（创建的用户）
- 功能含义: 邮箱注册，验证验证码、邮箱查重、密码强度校验，创建用户并持久化，销毁验证码防止重复使用。
- 链路作用: 在 UserApplicationService.registerByEmail() 中调用，执行注册业务逻辑。

### 3.2 login()
- 函数签名: `User login(String emailStr, String password, String ip)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `password`: 密码
  - `ip`: 登录IP
- 出参: User 实体（登录成功的用户）
- 功能含义: 用户登录，检查登录失败限流（5次失败锁定15分钟），验证密码，检查用户状态，记录登录成功。
- 链路作用: 在 UserApplicationService.login() 中调用，执行登录业务逻辑。

### 3.3 updateProfile()
- 函数签名: `void modifyInfo(String username, String avatarUrl, String phone)`（User 实体方法）
- 入参:
  - `username`: 用户名
  - `avatarUrl`: 头像URL
  - `phone`: 手机号
- 出参: 无（void）
- 功能含义: 修改用户信息，更新用户名、头像、手机号。
- 链路作用: 在 UserApplicationService.modifyInfo() 中调用，执行用户信息修改逻辑。

### 3.4 changePassword()
- 函数签名: `void resetPassword(String emailStr, String code, String newPassword, String confirmPassword)`（UserAuthenticationDomainService）
- 入参:
  - `emailStr`: 邮箱地址
  - `code`: 验证码
  - `newPassword`: 新密码
  - `confirmPassword`: 确认密码
- 出参: 无（void）
- 功能含义: 重置密码，验证两次密码一致、验证码有效、密码强度，更新用户密码并销毁验证码。
- 链路作用: 在 UserApplicationService.resetPassword() 中调用，执行密码重置逻辑。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 UserApplicationService.java 和 UserAuthenticationDomainService.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
