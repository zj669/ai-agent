# 用户认证业务域索引

本文件为用户认证业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

用户注册、登录、JWT 认证、用户信息管理。

## 典型触发

- 用户注册/登录问题
- JWT token 验证或刷新
- 权限相关排查
- UserController 接口排查

## 代码入口（支撑事实）

- 接口层：`UserController.java`（路径 `/client/user`，注意不是 `/api/user`）
- 应用层：`UserApplicationService.java`
- 领域层：`UserAuthenticationDomainService.java`、`User.java`、`Email.java`、`Credential.java`
- 基础设施：`UserRepositoryImpl.java`、`RedisVerificationCodeRepository.java`

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 用户注册（邮箱验证码） | `references/user-auth/user-registration.md` | ✅ 已创建 |
| 用户认证完整流程 | `references/user-auth/feature-delivery.md` | 待创建 |
