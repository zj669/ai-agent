# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserLoginResponse.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserLoginResponse.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserLoginResponse.java
- Type: .java

## Responsibility
- 登录响应 DTO，承载访问令牌、多设备 refresh token 与用户信息。

## Key Symbols / Structure
- 字段：`token`, `refreshToken`, `expireIn`, `deviceId`, `user`

## Dependencies
- `UserDetailDTO`
- Lombok

## Notes
- 状态: 正常
- `expireIn` 单位为秒。
