## Metadata
- file: `.blueprint/files/ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: blueprint-team

# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java

## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java
- Type: .java

## Responsibility
- 承载对应领域/应用/基础设施的 Java 类型定义与业务职责实现。

## Key Symbols / Structure
- class UserAuthenticationDomainService
- sendVerificationCode(String emailStr)
- register(String emailStr, String code, String password, String username)
- login(String emailStr, String password, String ip)
- generateSecureCode()
- extractUsernameFromEmail(String email)
- resetPassword(String emailStr, String code, String newPassword, String confirmPassword)
- validatePasswordStrength(String password)

## Dependencies
- com.zj.aiagent.domain.auth.service.ratelimit.RateLimiter
- com.zj.aiagent.domain.auth.service.ratelimit.RateLimiterFactory
- com.zj.aiagent.domain.user.entity.User
- com.zj.aiagent.domain.user.exception.AuthenticationException
- com.zj.aiagent.domain.user.exception.AuthenticationException.ErrorCode
- com.zj.aiagent.domain.user.repository.IUserRepository
- com.zj.aiagent.domain.user.repository.IVerificationCodeRepository
- com.zj.aiagent.domain.user.valobj.Credential
- com.zj.aiagent.domain.user.valobj.Email
- com.zj.aiagent.domain.user.valobj.UserStatus

## Notes
- updated_at: 2026-02-15 07:36
- status: 正常
- 占位符内容已按源码职责自动回填。
