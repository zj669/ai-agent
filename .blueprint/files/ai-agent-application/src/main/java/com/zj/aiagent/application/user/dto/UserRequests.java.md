# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserRequests.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserRequests.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserRequests.java
- Type: .java

## Responsibility
- 用户模块请求 DTO 容器，定义注册/登录/刷新/资料修改/重置密码入参。

## Key Symbols / Structure
- `SendEmailCodeRequest`
- `RegisterByEmailRequest`
- `LoginRequest`
- `TokenRefreshRequest`
- `ModifyUserRequest`
- `ResetPasswordRequest`
- 关键校验：`@NotBlank`, `@Email`

## Dependencies
- Jakarta Validation
- Lombok `@Data`

## Notes
- 状态: 正常
- 多设备登录相关字段：`deviceId`。
