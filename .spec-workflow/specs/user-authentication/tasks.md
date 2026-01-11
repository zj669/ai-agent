# 任务拆解文档

- [x] 1. 领域层 - 核心聚合根与值对象
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/entity/User.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/valobj/Email.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/valobj/Credential.java
  - 实现 `User` 聚合根，包含密码验证和个人信息更新方法。
  - 实现 `Email` 值对象及格式校验。
  - 实现 `Credential` 值对象及 BCrypt 加密逻辑。
  - _技术栈: DDD patterns, Lombok_
  - _关联需求: 4.1, 4.2_
  - _Prompt: Role: Java Backend Developer | Task: Implement Domain Layer entities for User Authentication including User aggregate, Email value object, and Credential value object following DDD principles | Restrictions: Use standard Java validation, ensure domain logic is encapsulated | Success: Entities are correctly defined with behavior methods_

- [x] 2. 领域层 - 限流策略接口
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/auth/service/ratelimit/RateLimiter.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/auth/service/ratelimit/RateLimiterFactory.java
  - 定义 `RateLimiter` 接口。
  - 实现 `RateLimiterFactory` 用于创建限流器实例。
  - _关联需求: Design-RateLimit_
  - _Prompt: Role: Java Architect | Task: Implement Strategy and Factory pattern for Rate Limiting in Domain Layer | Restrictions: Define clear interface, extensible factory | Success: Interface and Factory allow for future expansion_

- [x] 3. 基础设施层 - 限流器实现
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/redis/RedisSlidingWindowRateLimiter.java
  - 使用 Redis (Template/Lua) 实现 `SlidingWindowRateLimiter`。
  - _技术栈: StringRedisTemplate_
  - _关联需求: Design-RateLimit_
  - _Prompt: Role: Backend Developer | Task: Implement SlidingWindowRateLimiter using Redis | Restrictions: Ensure atomicity using Lua scripts or Redis transactions | Success: Rate limiter correctly counts and expires windows_

- [x] 4. 基础设施层 - 仓储与端口实现
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/user/mapper/UserMapper.java
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/user/repository/UserRepositoryImpl.java
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/email/EmailServiceImpl.java
  - 使用 MyBatis-Plus 实现 `UserRepository`。
  - 实现 `EmailService` 用于发送验证码。
  - _技术栈: MyBatis-Plus, JavaMailSender_
  - _关联需求: Infra-Repo, Infra-Email_
  - _Prompt: Role: Java Developer | Task: Implement Infrastructure persistence and external services | Restrictions: Use MyBatis-Plus for DB, standard JavaMail for email | Success: Data persistence works, emails are sent_

- [x] 5. 领域层 - 领域服务
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java
  - 实现核心认证流程：验证码校验 -> 查重 -> 创建用户。
  - _关联需求: Req-2, Req-3_
  - _Prompt: Role: Java Domain Expert | Task: Implement UserAuthenticationDomainService orchestrating registration and login logic | Restrictions: Pure domain logic, no HTTP dependencies | Success: Business flows for reg/login are correct_

- [x] 6. 应用层 - 服务实现
  - 文件: ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java
  - 文件: ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/* (DTO classes)
  - 实现应用服务，协调领域层与基础设施层。
  - 处理 DTO <-> Entity 映射。
  - _关联需求: App-Service_
  - _Prompt: Role: Java Application Developer | Task: Implement UserApplicationService and DTOs | Restrictions: Transactional boundaries, DTO mapping | Success: Service methods fulfill use cases_

- [x] 7. 接口层 - REST Controller
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/controller/client/UserController.java
  - 实现设计文档中定义的标准 REST 接口。
  - 添加 Swagger/OpenAPI 注解。
  - _关联需求: Interface-API_
  - _Prompt: Role: API Developer | Task: Implement UserController with REST endpoints | Restrictions: Standard Spring MVC, validation, unified response | Success: All endpoints reachable and valid_

- [x] 8. 测试 - 单元测试与集成测试
  - 文件: ai-agent-domain/src/test/java/com/zj/aiagent/domain/user/UserTest.java
  - 文件: ai-agent-interfaces/src/test/java/com/zj/aiagent/interfaces/web/UserControllerTest.java
  - 编写 User 聚合根行为的单元测试。
  - 编写 Controller 接口的集成测试。
  - _关联需求: Test-Strategy_
  - _Prompt: Role: Test Engineer | Task: Create comprehensive tests for User module | Restrictions: High coverage, mock external deps for unit tests | Success: Tests pass and cover critical paths_

- [x] 9. 调试模式支持 (Debug Mode)
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/interceptor/AuthStrategy.java (Interface)
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/interceptor/JwtAuthStrategy.java
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/interceptor/DebugAuthStrategy.java
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/interceptor/AuthStrategyFactory.java
  - 实现基于策略模式的认证机制。
  - **DebugAuthStrategy**: 仅在配置开启时生效，验证 Header 中的 User ID 是否存在。
  - **JwtAuthStrategy**: 现有的 JWT 验证逻辑。
  - **Factory**: 根据配置 (`AUTH_DEBUG_ENABLED`) 和请求上下文决定使用的策略。
  - _关联需求: Debug Mode_
  - _Prompt: Role: Java Developer | Task: Implement Strategy and Factory pattern for Request Authentication supporting JWT and Debug modes | Restrictions: Debug mode only active if config enabled | Success: Requests with debug header pass in dev, fail in prod or if config off_
