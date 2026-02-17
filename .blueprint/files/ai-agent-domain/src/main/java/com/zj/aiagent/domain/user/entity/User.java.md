## Metadata
- file: `.blueprint/files/ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/entity/User.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: blueprint-team

# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/entity/User.java

## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/entity/User.java
- Type: .java

## Responsibility
- 承载对应领域/应用/基础设施的 Java 类型定义与业务职责实现。

## Key Symbols / Structure
- class User
- reconstruct(Long id, String username, String emailStr, String encryptedPassword, String phone, String avatarUrl, Integer statusCode, String lastLoginIp, LocalDateTime lastLoginTime, Boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt)
- modifyInfo(String username, String avatarUrl, String phone)
- verifyPassword(String rawPassword)
- onLoginSuccess(String ip)
- resetPassword(Credential newCredential)

## Dependencies
- com.zj.aiagent.domain.user.valobj.Credential
- com.zj.aiagent.domain.user.valobj.Email
- com.zj.aiagent.domain.user.valobj.UserStatus
- com.zj.aiagent.shared.util.XssFilterUtil
- lombok.Getter
- lombok.NoArgsConstructor
- lombok.ToString

## Notes
- updated_at: 2026-02-15 07:36
- status: 正常
- 占位符内容已按源码职责自动回填。
