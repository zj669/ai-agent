# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java
- Type: .java

## Responsibility
- 用户应用服务编排：验证码发送、注册登录、token 刷新、资料维护、登出与重置密码。

## Key Symbols / Structure
- 认证流程
  - `sendEmailCode(...)`
  - `registerByEmail(...)`
  - `login(...)`
  - `refreshToken(refreshToken, deviceId)`
  - `logout(token, deviceId)`
- 用户信息
  - `getUserInfo(userId)`
  - `modifyInfo(userId, request)`
- 密码
  - `resetPassword(...)`
- 内部方法
  - `buildLoginResponse(User, deviceId)`
  - `toDetailDTO(User)`

## Dependencies
- Domain services/repos: `UserAuthenticationDomainService`, `ITokenService`, `IUserRepository`
- Domain model: `User`, `UserStatus`
- DTO: `UserRequests`, `UserLoginResponse`, `TokenRefreshResponse`, `UserDetailDTO`

## Notes
- 状态: 正常
- `access-token.expiration` 配置用于计算 `expiresIn`。
