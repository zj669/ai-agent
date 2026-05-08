# 事故历史索引

本文件收录重复故障签名、已知错误和用户纠正。

在执行任何可能触发已知问题的操作前，检查此文件。

## 已知约束 / 历史教训

| 编号 | 标签 | 摘要 | 细节 |
|---|---|---|---|
| I-001 | domain-purity | Domain 层引入框架依赖导致编译失败 | 所有 domain 代码只依赖 shared；不得引入 Spring/MyBatis 注解或任何框架 |
| I-002 | protobuf-conflict | Milvus SDK 需要 protobuf 3.25.3，版本不匹配导致冲突 | 版本在父 POM 锁定，不要单独升级 protobuf |
| I-003 | frontend-dir | 误修改 `app/frontend/` 遗留骨架，实际无效果 | 活跃前端在 `ai-agent-foward/`，禁止修改另一个 |
| I-004 | spring-ai-autoconfig | 尝试注入 Spring AI 自动配置 bean 导致启动失败 | Spring AI 自动配置已禁用，模型通过 ChatModelPort 动态创建 |
| I-005 | workflow-graph-mutate | 运行时修改 WorkflowGraph 导致执行状态不一致 | WorkflowGraph 是不可变值对象，创建后禁止修改 |
| I-006 | no-flyway | 以为有 Flyway 管理 schema，实际没有 | 数据库 schema 只通过 `01_init_schema.sql` 管理，Docker 初始化时执行 |
| I-007 | direct-infra-call | 从应用层直接调用基础设施，绕过领域层 | 必须通过 domain repositories/ports；application → domain ← infrastructure |
| I-008 | redis-template-direct | 直接使用 RedisTemplate 而不是封装的 RedisService | 始终使用项目封装服务，不直接使用框架原生 template |
| I-009 | workflow-tool-contract | TOOL 节点前后端配置字段不一致导致 MCP 工具名为 null | 前端 `selectedTool.fullName` 与后端 `mcpToolName` 必须同步；执行器需兼容两者，详见 [I-009](I-009-workflow-tool-contract.md) |
| I-010 | maven-reactor-target-test | Maven reactor 定向测试未带正确参数导致误判 | 模块测试用 `-am` 和 `-Dsurefire.failIfNoSpecifiedTests=false`，避免旧 SNAPSHOT 和上游无同名测试失败，详见 [I-010](I-010-maven-reactor-target-test.md) |
| I-011 | workflow-start-input-default | START 节点默认空串覆盖用户启动输入 | 合并顺序必须是 schema 默认值先写入、用户输入后写入，详见 [I-011](I-011-workflow-start-input-default.md) |
| I-012 | spring-boot-run-stale-snapshot | `spring-boot:run -pl interfaces` 加载本地仓库旧 SNAPSHOT | 修改非 interfaces 模块后必须 `mvn -pl ai-agent-interfaces -am -DskipTests install` 并重启，详见 [I-012](I-012-spring-boot-run-stale-snapshot.md) |
| I-013 | workflow-duplicate-tool-schedule | 并行/汇合场景下 TOOL 节点被重复调度 | 已修复：调度前标记节点 RUNNING，避免运行中节点再次 ready，详见 [I-013](I-013-workflow-duplicate-tool-schedule.md) |
| I-014 | workflow-reference-contract-fragmentation | 前后端值引用格式散乱导致下游节点拿到空占位参数 | 标准统一为 `inputs.<key>` / `<nodeId>.output.<key>` / `sharedState.<key>`，错误引用必须失败，详见 [I-014](I-014-workflow-reference-contract-fragmentation.md) |
| I-015 | chat-ui-start-input-contract | 真实 Chat UI 仍传 `inputs.query` 导致 `start.output.inputMessage` 为空 | Chat UI 启动 workflow 必须发送 `inputs.inputMessage`，用 Browser Relay 验证真实页面，详见 [I-015](I-015-chat-ui-start-input-contract.md) |
| I-016 | workflow-review-resume-running-state | 人工审核恢复后 `Accepted 0/1 pending nodes`，LLM 不继续执行 | `BEFORE_EXECUTION` 恢复时 paused node 必须回到 `PENDING` 再派发，详见 [I-016](I-016-workflow-review-resume-running-state.md) |
| I-017 | llm-stream-empty-and-empty-assistant-history | LLM 流式空响应，非流式回退又被空 assistant 历史消息拒绝 | 流式空响应需非流式回退，历史消息链过滤空内容消息，详见 [I-017](I-017-llm-stream-empty-and-empty-assistant-history.md) |

## 事故详情

- [I-009](I-009-workflow-tool-contract.md)
- [I-010](I-010-maven-reactor-target-test.md)
- [I-011](I-011-workflow-start-input-default.md)
- [I-012](I-012-spring-boot-run-stale-snapshot.md)
- [I-013](I-013-workflow-duplicate-tool-schedule.md)
- [I-014](I-014-workflow-reference-contract-fragmentation.md)
- [I-015](I-015-chat-ui-start-input-contract.md)
- [I-016](I-016-workflow-review-resume-running-state.md)
- [I-017](I-017-llm-stream-empty-and-empty-assistant-history.md)
