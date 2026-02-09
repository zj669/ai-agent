# UserService Blueprint

## 职责契约
- **做什么**: 管理用户账户——注册、登录、个人信息维护、密码管理、登录记录
- **不做什么**: 不负责 JWT 令牌的生成与验证（那是 AuthService 的职责）；不负责业务权限控制

## 核心聚合根

### User
- 用户聚合根，持有认证信息和个人资料
- 包含: username, email, password(加密), avatar, bio

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| register | RegisterCmd | User | 写DB, 发送验证邮件 | email唯一 |
| login | LoginCmd | TokenPair | 验证密码, 记录登录 | 速率限制 |
| getProfile | userId | UserProfile | 无 | - |
| updateProfile | userId, UpdateProfileCmd | User | 写DB | - |
| changePassword | userId, oldPwd, newPwd | void | 写DB | 验证旧密码 |

## 依赖拓扑
- **上游**: UserController, AuthFilter
- **下游**: IUserRepository(端口), ITokenService(端口), IEmailService(端口)

## 领域事件
- 发布: 无
- 监听: 无

## 设计约束
- 密码使用 BCrypt 加密存储
- 用户数据存储在 user_info 表
- 支持调试模式（通过 X-User-Id header 跳过认证）

## 变更日志
- [初始] 从现有代码逆向生成蓝图
