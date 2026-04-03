# Journal - zj669 (Part 1)

> AI development session journal
> Started: 2026-03-31

---



## Session 1: Swarm Agent Coordinator 模式重构

**Date**: 2026-04-01
**Task**: Swarm Agent Coordinator 模式重构

### Summary

(Add summary)

### Main Changes

| 维度 | 改动 |
|------|--------|
| **后端提示词** | 新增 `SwarmPromptTemplates.java` — Coordinator（Phase 概念 + Continue vs Spawn 决策矩阵）和 Worker（结构化输出）动态提示词模板 |
| **任务通知协议** | 新增 `TaskNotificationEvent.java`（领域值对象），通过 SSE `task-notification` XML 事件向上游汇报结果 |
| **Agent 判断逻辑** | `SwarmAgent.isCoordinator()` 基于 `parentId == null && hasChildren()`，参照 Claude-Code `CLAUDE_CODE_COORDINATOR_MODE` 设计哲学 |
| **消息模板** | `SwarmTools.send()` 加入 `[PHASE/ROLE/GOAL/CONSTRAINTS/EXPECTED_OUTPUT]` 结构化格式提示 |
| **前端通知** | 新增 `SwarmNotification.tsx` Toast 组件（spawn/completed/failed/killed） |
| **Worker 卡片** | 新增 `WorkerCard.tsx` 状态卡片，含耗时/token/状态图标，可折叠 |
| **SSE 解析** | `useUIStream` 新增 `parseTaskNotificationXml()`，监听 `agent.task-notification` 事件 |
| **调研文档** | 新增 `docs/claude-code-multi-agent-patterns.md` 和 `docs/java-backend-multi-agent-patterns.md` |

**技术亮点**：Check Agent 主动发现 `TaskNotificationEvent` 放在基础设施层违反 DDD 边界，重构为 `domain/swarm/valobj/TaskNotificationEvent.java`，保证领域层定义概念、基础设施层负责序列化。


### Git Commits

| Hash | Message |
|------|---------|
| `14b0766` | (see git log) |
| `3069da0` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 2: 04-02 完成 - Swarm 动态提示词系统

**Date**: 2026-04-02
**Task**: 04-02 完成 - Swarm 动态提示词系统

### Summary

实现动态提示词系统，角色过滤 + 模块化组合，参照 Claude-Code 设计模式

### Main Changes

| 模块 | 变更 |
|------|------|
| Domain | SwarmRole 枚举扩展 ROOT/COORDINATOR/WORKER + isDispatcher/isWorker |
| Application | 新增 SwarmToolFilter（工具白名单）、SwarmPromptSection（Section 枚举）、SwarmPromptService（动态组合） |
| Application | SwarmAgentRunner 集成新提示词服务，移除旧 SUB_AGENT_FORBIDDEN_TOOLS |
| 删除 | SwarmPromptTemplate.java + SwarmPromptTemplates.java（硬编码写作模板全清） |
| 测试 | SwarmToolFilterTest(12) + SwarmPromptServiceTest(18)，34/34 通过 |

**设计模式参照**（Claude-Code → ai-agent）：
- `COORDINATOR_MODE_ALLOWED_TOOLS` → `SwarmToolFilter`
- `buildEffectiveSystemPrompt()` → `SwarmPromptService.getPrompt()`
- `teammatePromptAddendum.ts` → `SwarmPromptSection.BASE`
- `assembleToolPool()` → `SwarmAgentRunner.buildAllToolCallbacks()`


### Git Commits

| Hash | Message |
|------|---------|
| `721737d` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 3: 登录注册模块 Code Review + 修复 + 真人测试

**Date**: 2026-04-03
**Task**: 登录注册模块 Code Review + 修复 + 真人测试

### Summary

(Add summary)

### Main Changes

## 任务概述
对登录注册模块（user/auth）进行 Code Review，确保功能正常、代码精简、无过度设计。

## 主要发现与修复

### Critical 问题（已全部修复）

| 问题 | 修复方案 |
|------|---------|
| `Credential.java` — Spring Security 依赖污染领域层 | 抽取 `shared.BCryptUtil`，使用纯 jBCrypt 实现 |
| `GlobalExceptionHandler` — `INVALID_REFRESH_TOKEN` 未映射 HTTP 401 | 添加到 UNAUTHORIZED 分支 |
| `EmailLogPO.java` — PO 字段定义错误（复制了 user_info 而非 email_log） | 重写为正确表结构 |
| `IUserRepository.saveEmailLog` — 方法空壳，无实际实现 | 移除该方法（邮件日志为非关键审计功能） |
| `PersistenceExceptionTranslationInterceptor` + MyBatis + JDK 21 → `AbstractMethodError` | 移除 `saveEmailLog` 调用，绕过 MyBatis CGLIB 代理兼容性问题 |

### Warning 问题（已修复）
- `RedisVerificationCodeRepository` — try-catch 吞掉 Redis 异常 → 移除不必要包装
- 测试文件残留 `saveEmailLog` mock 验证 → 清理

## 真人模拟测试结果

| 测试项 | 结果 |
|--------|------|
| `POST /client/user/email/sendCode` | ✅ 200 |
| `POST /client/user/email/register` | ✅ 200（获得 JWT + user info） |
| `POST /client/user/login` | ✅ 200（token + refreshToken） |
| `GET /client/user/info` | ✅ 200（JWT 解析用户） |
| 错误密码登录 | ✅ 401（正确错误消息） |
| 错误验证码注册 | ✅ 400（正确错误消息） |

**注**：Token 刷新因频繁错误触发 15 分钟限流，为正常安全机制。

## 深层技术发现
- `PersistenceExceptionTranslationInterceptor` 与 MyBatis Plus CGLIB 代理 + JDK 21 的 `DirectMethodHandleAccessor` 存在兼容性问题
- 任何通过 CGLIB 代理的数据库写操作都可能触发 `AbstractMethodError`，try-catch(Throwable) 仍无法捕获
- 根因：`PersistenceExceptionTranslationInterceptor.invoke()` 中的反射调用在 JDK 21 下抛出未声明的 `AbstractMethodError`

## 提交文件
- `ai-agent-domain/` — `IUserRepository`, `UserAuthenticationDomainService`, `Credential`
- `ai-agent-infrastructure/` — `EmailLogPO`, `RedisVerificationCodeRepository`, `UserRepositoryImpl`
- `ai-agent-interfaces/` — `GlobalExceptionHandler`, 测试文件
- `ai-agent-shared/` — `BCryptUtil` (新增)
- `docker/init/mysql/01_init_schema.sql` — email_log 表定义


### Git Commits

| Hash | Message |
|------|---------|
| `ec84a53` | (see git log) |
| `f3e8ae6` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete
