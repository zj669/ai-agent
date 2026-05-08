---
name: ai-agent-workspace
description: >-
  AI Agent 平台业务分层工作区运行入口。用于任何 AI Agent 平台业务域、
  服务、部署、服务器、数据库、账号池、监控、事故、脚本、SOP 或运维任务。
  始终从这里开始：读取最近工作区记忆，应用全局安全规则，通过业务域地图和
  Meet->See 表路由，并在行动前选择最具体的业务 SOP。触发词：
  "ai-agent", "workflow", "工作流", "agent管理", "对话", "知识库",
  "用户认证", "部署", "运维", "前端", "数据库", "Redis", "Milvus",
  "MinIO", "业务路由", "熟悉工作区", "初始化工作区".
---

# AI Agent 平台工作区

这是 AI Agent 平台的权威业务分层工作区运行 skill。

所有全局规则、安全边界、验证标准和业务路由归属都在这里。具体业务流程放在
`references/` 下。仓库、服务、域名、数据库和历史项目都是业务域内的支撑事实。

## 执行顺序

1. 任何风险操作前先读最近工作日志。
2. 判断该任务能否由根文件直接处理。
3. 使用业务域地图和 `Meet -> See` 表选择业务路由。
4. 如果目标是业务域 `index.md`，继续在那里路由。
5. 代码问题在选定业务路由后再定位真实代码。
6. 变更完成后验证，并把长期有效的经验回写到正确文件。

## 开工前必读

在 SSH、SQL、curl、git、部署、代码变更或生产排查前，读取最近日志：

- 当月：`logs/YYYY-MM.md`
- 如果今天在月初 30 天内，也读取上个月日志

不要只 grep 关键词。近期用户纠正和事故经验可能不包含当前任务关键词。

如果日志与更旧 SOP 冲突，优先采用更新日志，并在确认规则后更新可复用 SOP。

## 路径约定

- Skill 资源使用相对路径：`logs/...`、`references/...`、`scripts/...`、`agents/...`。
- 工作区根目录：`/home/zj669/repo/ai-agent`（或通过 `AI_AGENT_WORKSPACE_ROOT` 传入）。
- 运维占位符放在 `.env.example`；真实值放在 `.env` 或批准的 secret store。
- 不要把真实 secrets、密码、tokens、sessions 或 webhook URL 写进 skill 文档。
- 不要在 SOP 中硬编码特定机器安装路径。
- 业务仓库/模块应按工作区根目录的相对路径记录为支撑事实。

## 业务域地图

| 业务域 | 典型信号 | 路由 |
|---|---|---|
| 工作流引擎 | workflow, execution, 节点, 条件分支, SSE, ExecutionContext, SchedulerService | `references/workflow/index.md` |
| Agent管理 | agent, 模型配置, ChatModelPort, AgentController | `references/agent/index.md` |
| 对话管理 | chat, conversation, message, 聊天, 历史消息 | `references/chat/index.md` |
| 知识库 | knowledge, 知识库, 向量搜索, Milvus, 文档切片 | `references/knowledge/index.md` |
| 用户认证 | user, auth, 登录, 注册, JWT, token | `references/user-auth/index.md` |
| 基础设施运维 | Docker, MySQL, Redis, MinIO, etcd, 部署, 容器, 服务重启 | `references/infra-ops/index.md` |
| 前端开发 | React, Vite, 前端, 页面, 组件, TypeScript, 样式 | `references/frontend/index.md` |
| 事故历史 | 已知错误, 用户纠正, 重复故障签名 | `references/incidents/index.md` |

边界说明：

- `agent` 默认表示 Agent管理业务域；若用户说"执行工作流的代理节点"则路由到工作流引擎。
- `chat` 既可以是对话管理（REST API），也可以是前端聊天页面——按用户动作判断路由。
- `Milvus` 是知识库的支撑基础设施；若问题是向量搜索业务逻辑路由到知识库，若是容器运维路由到基础设施运维。
- 仓库/模块名（`ai-agent-domain`、`ai-agent-foward` 等）只是触发提示，路由仍按业务动作判断。
- 高风险域（数据库写入、生产部署、账号操作）写入前需要明确 SOP 和用户确认。

## 全局安全规则

1. 版本控制下的生产配置必须在本地修改、commit、push、服务器 pull，然后只重启目标服务。
2. 数据库写入在执行前需要只读确认、影响范围、完整 SQL、执行后检查 SQL 和用户批准。
3. 不可逆变更必须说明回滚或补偿限制。
4. 凭据、tokens、webhook URL、用户 sessions 和 refresh tokens 不得进入通用模板或日志。
5. 用户提供的认证材料只能在授权范围内使用。
6. 不要为了处理局部冲突而重启无关服务或全局 Docker daemon。
7. 未知高风险动作默认标记为 `requires-confirmation`。
8. Domain 层代码不得引入 Spring、MyBatis 或任何框架依赖（DDD 纯域规则）。
9. 修改 `01_init_schema.sql` 等数据库初始化文件前，必须确认对已有数据的影响。

## 数据库安全

查询大型生产表前：

```sql
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'ai_agent' AND TABLE_NAME = '<table>';

SHOW INDEX FROM ai_agent.<table>;
```

规则：

- 避免对大表做无索引全表扫描。
- 索引使用不确定时先 `EXPLAIN`。
- DDL/DML 都视为写入操作，需要用户确认。
- 当用户只要 SQL 时，只输出 SQL，不执行。
- 数据库 schema 通过 `docker/init/mysql/01_init_schema.sql` 管理，无 Flyway。

## 代码与开发规则

- 深入代码探索前先选择业务路由。
- 回答代码问题前必须先用 Grep/Augment/Fast-Context 定位真实代码，禁止猜测。
- 添加新抽象前，先搜索已有 service、helper、script 和模式。
- Domain 层只能依赖 shared 模块，禁止依赖 Spring/MyBatis/infrastructure。
- 前端代码在 `ai-agent-foward/`，禁止修改 `app/frontend/`（遗留骨架）。
- 优先复用项目封装的 `RedisService`、`HttpService` 等，不直接用 `RedisTemplate`、`RestTemplate`。
- WorkflowGraph 创建后不可变更（值对象）。
- 新增代码前运行架构对齐检查：确认复用已有组件/实体/业务逻辑。

## 前端验证

如果变更涉及页面、样式、交互、路由、表单或请求联调，必须在真实浏览器验证：

1. 打开 `http://localhost:5173` 实际页面。
2. 走一遍被修改的工作流。
3. 检查 console 和 network 结果。
4. 布局受影响时增加移动端视口检查。

如果浏览器验证受阻，明确报告为未验证。

## 部署规则

标准部署流程：

1. 修改本地仓库。
2. 运行相关测试并审查 diff：`mvn test -pl <module>` 或 `npm run build`。
3. Commit 并 push。
4. SSH 到服务器。
5. Pull 代码和配置。
6. 只重启目标服务：`docker-compose up -d <service>`。
7. 检查健康端点：`curl http://localhost:8080/actuator/health`。

默认禁止：

- 直接编辑版本控制下的服务器文件；
- 重启 Docker daemon 或无关服务；
- 使用未确认镜像 tag 绕过正常流程；
- 杀掉未知进程释放端口。

## 验证标准

每次变更结束时给出清晰边界：

```text
验证:
- 直接验证: ...
- 依赖链验证: ...
- 端到端验证: ...
- 未验证: <reason>
```

## 输出规则

使用中文输出，结论先行。

诊断或评估时：

- 结论和置信度；
- 已检查证据；
- 未知项；
- 下一步动作。

代码或 SOP 变更时：

- 变更文件；
- 已执行验证；
- 未验证项及原因。

## 知识回写

把长期有效的经验写入最窄归属，设计 SOP 的新增与变更需加载 `super-enterprise-skill` 来进行流程化操作：

| 内容 | 目标文件 |
|---|---|
| 今天的实际工作或决策 | `logs/YYYY-MM.md` |
| 用户纠正或近期错误 | `logs/YYYY-MM.md` 和可复用 SOP（如果长期有效） |
| 可复用业务流程 | `references/<business-domain>/...` |
| 支撑仓库/服务事实 | `references/<business-domain>/service-context.md` |
| 事故签名 | `references/incidents/...` |
| 脚本接口或副作用变化 | `scripts/<family>/README.md` |
| 业务路由或归属变化 | 根路由表 |

## 收尾规则

当用户说等价于"收工"的话时：

1. 有必要时把实际变更追加到今天日志。
2. 记录已确认错误或长期有效的用户纠正。
3. 如果这是工作区惯例且用户没有反对，commit/push 被修改仓库。
4. 使用 git 时检查本地/远端一致性。

## Meet -> See 业务路由表

只有在读取最近日志和根文件后使用此表。

| Meet | See |
|---|---|
| 工作区规则、安全、验证、知识回写 | this file |
| workflow, 工作流, execution, 节点执行, 条件分支, SSE | `references/workflow/index.md` |
| agent, Agent管理, 模型配置, ChatModelPort | `references/agent/index.md` |
| chat, 对话, 聊天, conversation, message | `references/chat/index.md` |
| knowledge, 知识库, 向量搜索, 文档, Milvus | `references/knowledge/index.md` |
| user, auth, 用户, 登录, 注册, JWT | `references/user-auth/index.md` |
| Docker, 部署, 运维, MySQL, Redis, MinIO, etcd, 容器 | `references/infra-ops/index.md` |
| React, 前端, Vite, 组件, 页面, TypeScript | `references/frontend/index.md` |
| 历史事故, 重复故障, 用户纠正 | `references/incidents/index.md` |

路由优先级：

1. 用户点名的 SOP 优先。
2. 更具体的业务工作流优先于宽泛业务域归属。
3. 仓库/模块/服务名是在业务路由选择后的支撑提示。
4. 高风险未知项需要确认或创建 SOP。
