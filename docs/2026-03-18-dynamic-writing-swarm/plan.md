# Plan：动态多智能体写作协作初版设计

## 1. 目标

在保留现有 swarm runtime 的前提下，引入一层面向写作场景的业务投影，形成如下能力：

1. 主 Agent 动态创建子 Agent
2. 子 Agent 作为任务执行器运行
3. 新增 `writing_session / writing_agent / writing_task / writing_result / writing_draft`
4. 前端只展示：
   - 主对话区
   - 协作面板
5. 协作面板以状态卡片为主，而不是平铺所有子 Agent 聊天流

## 2. 当前现状

当前 swarm 已有能力：

1. `swarm_workspace` 表达一个协作空间
2. `swarm_workspace_agent` 表达 workspace 下的 agent
3. `swarm_group` 和 `swarm_group_member` 表达群组
4. `swarm_message` 负责传输消息
5. `SwarmWorkspaceService.createAgent(...)` 已能创建子 Agent
6. `SwarmAgentRunner` 已有 root/sub agent 推理循环

当前问题：

1. `swarm_message` 同时承担 transport 和业务展示，语义过重。
2. 没有写作 session 概念。
3. 没有任务对象，导致前端很难展示“谁在做什么”。
4. 没有结果对象，导致结果只能从聊天消息中抽取。
5. 没有 draft 版本对象，导致主 Agent 汇总结果难以沉淀。

## 3. 设计原则

## 3.1 运行层与业务层分离

运行层继续使用现有 swarm。

业务层新增写作投影：

- transport truth：`swarm_*`
- writing truth：`writing_*`

前端主要读取 `writing_*`，而不是直接依赖 `swarm_message`。

## 3.5 Schema 采用方案 A

schema 落地采用方案 A：

1. 旧 `swarm_*` 表不迁移，直接删除并按同名新结构重建。
2. 新增 `writing_*` 作为写作业务投影层。
3. 本期以“结构简洁、语义清晰、实现稳定”为优先级，不做兼容旧历史数据的复杂改造。

本次重建范围：

- `swarm_workspace`
- `swarm_workspace_agent`
- `swarm_group`
- `swarm_group_member`
- `swarm_message`
- `writing_session`
- `writing_agent`
- `writing_task`
- `writing_result`
- `writing_draft`

## 3.2 子 Agent 动态创建

不写死固定子 Agent。

主 Agent 根据任务决定：

1. 是否创建子 Agent
2. 创建几个
3. 各自的 role / description / skill
4. 是否复用已有 writing agent

## 3.3 子 Agent 作为任务执行器

子 Agent 的定位是执行任务，不是公开群聊成员。

因此：

1. 主 Agent 给子 Agent 分配明确任务
2. 子 Agent 完成任务后返回结构化结果
3. 主 Agent 负责统一向用户输出

## 3.4 前端展示收敛

前端默认不展示子 Agent 原始聊天流。

展示对象改为：

1. 主对话区：human + root agent
2. 协作面板：`writing_agent + writing_task + writing_result`

## 4. 总体方案

## 4.1 核心链路

完整链路建议如下：

1. 用户发起写作需求
2. 系统创建或获取 `writing_session`
3. 主 Agent 分析任务
4. 主 Agent 动态创建一个或多个子 Agent
5. 每次创建时：
   - 调用现有 `SwarmWorkspaceService.createAgent(...)`
   - 同步落 `writing_agent`
6. 主 Agent 为每个子 Agent 创建 `writing_task`
7. 主 Agent 通过 swarm transport 给子 Agent 发送任务消息
8. 子 Agent 完成任务后回传
9. 后端把结果同步落到 `writing_result`
10. 主 Agent 收集多个 `writing_result`，汇总成 `writing_draft`
11. 主 Agent 在主对话区输出面向用户的自然语言回复

## 4.2 表结构设计

## 4.2.1 `writing_session`

职责：

- 表达一次创作任务的总上下文
- 作为所有 writing 业务对象的根

建议字段：

- `id`
- `workspace_id`
- `root_agent_id`
- `human_agent_id`
- `default_group_id`
- `title`
- `goal`
- `constraints_json`
- `status`
- `current_draft_id`
- `created_at`
- `updated_at`

## 4.2.2 `writing_agent`

职责：

- 把 swarm agent 映射成写作协作角色
- 作为协作面板中的 Agent 卡片数据源

建议字段：

- `id`
- `session_id`
- `swarm_agent_id`
- `role`
- `description`
- `skill_tags_json`
- `status`
- `sort_order`
- `created_at`
- `updated_at`

关键关系：

- `swarm_agent_id -> swarm_workspace_agent.id`

## 4.2.3 `writing_task`

职责：

- 表达主 Agent 派发的业务任务
- 作为协作面板中“当前任务”数据源

建议字段：

- `id`
- `session_id`
- `writing_agent_id`
- `swarm_agent_id`
- `task_type`
- `title`
- `instruction`
- `input_payload_json`
- `expected_output_schema_json`
- `status`
- `priority`
- `created_by_swarm_agent_id`
- `created_at`
- `started_at`
- `finished_at`

## 4.2.4 `writing_result`

职责：

- 记录子 Agent 的任务产出
- 为协作面板详情弹层提供摘要与内容

建议字段：

- `id`
- `session_id`
- `task_id`
- `writing_agent_id`
- `swarm_agent_id`
- `result_type`
- `summary`
- `content`
- `structured_payload_json`
- `token_usage`
- `created_at`

## 4.2.5 `writing_draft`

职责：

- 表达主 Agent 汇总后的稿件版本

建议字段：

- `id`
- `session_id`
- `version_no`
- `title`
- `content`
- `source_result_ids_json`
- `status`
- `created_by_swarm_agent_id`
- `created_at`

## 5. 后端设计

## 5.1 服务划分

建议新增以下应用服务：

### `WritingSessionService`

负责：

1. 创建 session
2. 查询 session
3. 更新 session 状态

### `WritingAgentCoordinatorService`

负责：

1. 创建 writing agent
2. 复用已有 writing agent
3. 同步调用 swarm `createAgent`

推荐流程：

1. 主 Agent 决定创建子 Agent
2. `WritingAgentCoordinatorService` 调 `SwarmWorkspaceService.createAgent(...)`
3. 拿到 `swarm_workspace_agent.id`
4. 创建 `writing_agent`

### `WritingTaskService`

负责：

1. 创建任务
2. 更新任务状态
3. 查询当前任务和历史任务

### `WritingResultService`

负责：

1. 保存子 Agent 结果
2. 生成结果摘要
3. 按 session / task / agent 查询结果

### `WritingDraftService`

负责：

1. 生成草稿版本
2. 查询当前草稿
3. 查询历史版本

### `WritingProjectionService`

负责：

1. 把 swarm runtime 事件投影为 writing 业务对象
2. 为前端协作面板提供聚合 DTO

## 5.2 事件驱动建议

建议新增一组写作领域事件或应用事件：

1. `WritingSessionStartedEvent`
2. `WritingAgentCreatedEvent`
3. `WritingTaskCreatedEvent`
4. `WritingTaskStartedEvent`
5. `WritingTaskCompletedEvent`
6. `WritingDraftUpdatedEvent`

这些事件可以来源于：

- 主 Agent 工具执行
- 子 Agent 消息回传
- 主 Agent 汇总完成

## 5.3 与现有 swarm 的关系

建议明确职责边界：

### swarm 负责

1. 真正创建 agent
2. 发消息
3. 执行推理
4. transport 层唤醒与运行

### writing 负责

1. session 业务语义
2. task 业务语义
3. result 业务语义
4. draft 业务语义
5. 前端投影

## 5.4 `createAgent` 的推荐改法

当前已有：

- `SwarmWorkspaceService.createAgent(...)`

建议不要替换它，而是在写作场景新增包装层：

- `WritingAgentCoordinatorService.createWritingAgent(...)`

内部动作：

1. 调用 `SwarmWorkspaceService.createAgent(...)`
2. 拿到返回的 `assistantAgentId`
3. 创建 `writing_agent`
4. 返回聚合结果

这样能最大限度复用现有代码。

## 5.5 任务派发建议

推荐不要让前端或后端从 `swarm_message` 中反推 task。

正确做法：

1. 主 Agent 决定给某个子 Agent 派任务
2. 先创建 `writing_task`
3. 再发送 transport message 给子 Agent

即：

- `writing_task` 是业务事实
- `swarm_message` 是执行载体

## 5.6 结果落库建议

建议在子 Agent 返回结果时：

1. 更新 `writing_task.status = DONE`
2. 创建 `writing_result`
3. 更新 `writing_agent.status`
4. 触发主 Agent 汇总或继续调度

## 5.7 草稿生成建议

主 Agent 每轮汇总后：

1. 生成新的 `writing_draft`
2. 把引用的 `writing_result` 记录到 `source_result_ids_json`
3. 更新 `writing_session.current_draft_id`

## 6. Prompt 协议设计

## 6.1 主 Agent Prompt 原则

主 Agent 的规则建议收敛为：

1. 你是总编辑 / 编排者
2. 需要时可动态创建子 Agent
3. 创建前优先判断是否已有可复用 Agent
4. 每个子 Agent 都应承担清晰单一职责
5. 你为子 Agent 分配任务，不让他们自由群聊
6. 你最终负责向用户输出可读结果

## 6.2 子 Agent Prompt 原则

子 Agent 的规则建议为：

1. 你是任务执行器
2. 你只处理分配给你的任务
3. 你只返回一次完整结果
4. 不和其他 Agent 聊天
5. 不主动面向用户输出

## 6.3 任务输入输出协议

建议统一成结构化协议。

主 Agent 派发任务：

```json
{
  "goal": "为玄幻小说设计主角团",
  "constraints": ["热血成长", "群像", "避免脸谱化"],
  "output_format": ["主角", "搭档", "反派", "人物关系"]
}
```

子 Agent 返回结果：

```json
{
  "type": "agent_result",
  "task": "character_design",
  "summary": "已完成四名核心角色设定",
  "content": "完整结果正文",
  "next_suggestions": ["建议补充反派支线", "建议统一对白风格"]
}
```

## 7. 前端设计

## 7.1 页面结构

前端只展示两层：

### 主对话区

展示：

- 用户消息
- 主 Agent 回复

不展示：

- 子 Agent 原始 message
- tool_call JSON
- 中间 transport 流

### 协作面板

展示：

- `writing_agent` 卡片
- 当前任务
- 最新结果摘要
- 状态

## 7.2 协作面板卡片

每张卡片建议显示：

1. Agent 名称
2. Role
3. 技能标签
4. 当前状态
5. 当前任务标题
6. 最新结果摘要
7. 更新时间

状态颜色建议：

- `IDLE`：灰
- `RUNNING`：蓝
- `DONE`：绿
- `FAILED`：红

## 7.3 卡片展开详情

点击卡片后建议展示：

1. Agent 描述
2. 当前任务详情
3. 最近结果摘要
4. 最近结果正文
5. 对当前草稿的贡献

这里展示的是 `writing_result`，不是 `swarm_message`。

## 7.4 主时间线过滤规则

主时间线默认只保留：

1. human message
2. root agent text message

默认隐藏：

1. 子 Agent -> 主 Agent 的原始消息
2. 工具调用消息
3. 中间系统消息

## 8. 实施顺序

## 第一阶段：数据结构落地

1. 新增 `writing_*` 数据表
2. 新增对应 Domain / Repository / PO / Mapper
3. 完成初始化 SQL 与迁移 SQL

## 第二阶段：后端业务打通

1. 新增 `WritingSessionService`
2. 新增 `WritingAgentCoordinatorService`
3. 新增 `WritingTaskService`
4. 新增 `WritingResultService`
5. 新增 `WritingDraftService`
6. 接入 swarm create/send/result 事件投影

## 第三阶段：主 Agent 任务化协作

1. 调整主 Agent prompt
2. 调整子 Agent prompt
3. 引入 task / result 协议
4. 控制子 Agent 一次性返回完整结果

## 第四阶段：前端双层展示

1. 主对话区只展示 human + root
2. 新增协作面板
3. 新增 Agent 卡片
4. 新增结果摘要 Drawer / SidePanel

## 第五阶段：草稿与版本

1. 新增 `writing_draft`
2. 主 Agent 汇总结果时生成 draft version
3. 前端支持查看当前草稿

## 9. 风险与缓解

### 风险 1：数据双写复杂

问题：

- `swarm_*` 和 `writing_*` 同时维护，可能不同步

缓解：

1. 关键写操作集中在协调服务中处理
2. 通过事件投影统一更新
3. 关键链路加审计日志

### 风险 2：主 Agent 无限制创建子 Agent

问题：

- session 会不断膨胀

缓解：

1. 增加复用优先策略
2. 增加单 session 子 Agent 数量软上限
3. 提供空闲 Agent 复用提示

### 风险 3：子 Agent 输出不稳定

问题：

- `writing_result.summary` 质量可能不稳定

缓解：

1. 统一输入输出协议
2. 强化 few-shot
3. 结果入库前做结构化校验或回退摘要生成

### 风险 4：前端仍然依赖旧消息流

问题：

- 页面会再次退化成群聊刷屏

缓解：

1. 明确主页面只读 writing projection DTO
2. 把原始 swarm messages 仅作为调试视图保留

## 10. MVP 范围建议

首版建议只做：

1. 动态创建子 Agent
2. `writing_session`
3. `writing_agent`
4. `writing_task`
5. `writing_result`
6. `writing_draft`
7. 主对话区 + 协作面板

先不做：

1. 高级章节树编辑器
2. 复杂 Agent 自动复用算法
3. 跨 session 复用 Agent
4. 大规模并行调度优化

## 11. 产出建议

本计划执行后，系统会从“swarm 群聊 UI”转变为“写作协作 UI”：

1. 主对话区负责人机沟通
2. 协作面板负责展示后台写作协作
3. 子 Agent 从聊天角色转为任务执行器
4. 业务语义从 `message` 提升到 `session/task/result/draft`

这是当前仓库中最适合落地的简易多智能体写作方案。

## 12. 实施 Task 拆分

下面这版任务拆分以“能尽快并行开发、又不互相阻塞”为目标。

### Task 0：Schema 与初始化脚本

目标：

1. 固化方案 A 的重建版 schema
2. 明确部署入口优先使用重建脚本

状态：

1. 已完成
2. 当前可作为后续开发基线

产出：

1. 动态写作相关 schema，现已合并进入 `docker/init/mysql/01_init_schema.sql`
2. 更新后的 PRD / plan 文档

### Task 1：Writing 领域模型与基础仓储

目标：

1. 新增 `writing_session / writing_agent / writing_task / writing_result / writing_draft`
2. 打通 domain / repository / mapper / PO
3. 先提供最基础的 CRUD 能力

建议落点：

1. `ai-agent-domain`
2. `ai-agent-infrastructure`

预期产出：

1. `WritingSessionRepository`
2. `WritingAgentRepository`
3. `WritingTaskRepository`
4. `WritingResultRepository`
5. `WritingDraftRepository`

依赖：

1. 依赖 Task 0
2. 不依赖前端

并行性：

1. 可独立并行开发
2. 完成后为 Task 2 / Task 3 / Task 4 提供数据基础

### Task 2：写作应用服务与业务编排

目标：

1. 新增写作业务服务层
2. 把 `swarm_*` 运行时动作映射到 `writing_*`
3. 提供主流程所需的业务 API

建议拆分：

1. `WritingSessionService`
2. `WritingAgentCoordinatorService`
3. `WritingTaskService`
4. `WritingResultService`
5. `WritingDraftService`
6. `WritingProjectionService`

关键动作：

1. 初始化或获取 `writing_session`
2. 主 Agent 创建子 Agent 时，同步创建 `writing_agent`
3. 主 Agent 派发任务时，先落 `writing_task`，再发 transport message
4. 子 Agent 完成后，更新 `writing_task` 并写入 `writing_result`
5. 主 Agent 汇总后生成 `writing_draft`

依赖：

1. 依赖 Task 1

并行性：

1. 可以与 Task 3 部分并行
2. 但最终联调需要 Task 3 的 prompt / tools 协议稳定

### Task 3：Swarm Runtime 改造成写作协作模式

目标：

1. 从当前“单主 AI 编排工作流”模式切回“动态多智能体写作”模式
2. 但保留当前 runtime 的主循环，不重写底层 runner
3. 解决主 Agent 等待子 Agent、回调主 Agent、继续汇总的问题

必改点：

1. `SwarmPromptTemplate`
2. `SwarmTools`
3. `SwarmAgentRunner`
4. `SwarmAgentRuntimeService`

关键子任务：

1. 定义主 Agent 可用写作工具集合：
   - `writing_session`
   - `writing_agent`
   - 现有的创建 agent 能力包装
   - `writing_task`
   - `writing_result`
   - `writing_draft`
2. 调整 root prompt：
   - 允许动态创建子 Agent
   - 先创建 task 再派发
   - 收到结果后继续汇总回复用户
3. 调整 sub prompt：
   - 子 Agent 只做任务执行器
   - 一次返回完整结果
   - 不做开放式群聊
4. 修复 root agent 等待与唤醒机制：
   - 子 Agent 执行完成后必须可回调唤醒主 Agent
   - 主 Agent 不应错误 stop
5. 收敛 send 的参数协议和 few-shot，避免再出现非法 JSON 调用

依赖：

1. 可先于 Task 2 开始
2. 最终需要 Task 1 的仓储支持，才能把写作动作落库

并行性：

1. 可以由独立后端线程推进
2. 是整个项目的最高风险任务包

### Task 4：面向前端的聚合查询 API

目标：

1. 不让前端直接依赖底层 `swarm_message` 解释业务语义
2. 提供“主对话区 + 协作面板”所需聚合 DTO

建议接口：

1. 查询当前 `writing_session`
2. 查询协作面板卡片列表
3. 查询某个卡片详情
4. 查询当前 draft

建议返回：

1. `rootConversation`
2. `collaborationCards`
3. `latestDraft`
4. `sessionStatus`

依赖：

1. 依赖 Task 1 和 Task 2

并行性：

1. 可和 Task 5 并行定义接口 shape

### Task 5：前端双层页面改造

目标：

1. 主对话区只保留 user + root agent
2. 协作面板显示子 Agent 卡片，不再平铺子 Agent 完整消息流
3. 点击卡片查看摘要和结果详情

建议落点：

1. `ai-agent-foward/src/modules/swarm`

关键改动：

1. `SwarmMainPage` 改成双栏或主区 + 侧栏结构
2. 主消息列表增加过滤规则：
   - 仅展示 human 与 root assistant
   - 工具消息只显示图标，不显示 tool 原文
3. 新增协作面板组件：
   - agent 状态卡
   - task 摘要
   - result 摘要
4. 新增详情抽屉或侧滑面板展示 `writing_result`

依赖：

1. 可以先做静态 UI 壳子
2. 接口联调依赖 Task 4

并行性：

1. 可与 Task 2 / Task 4 并行

### Task 6：联调、回归与验收

目标：

1. 确保主 Agent 能创建子 Agent、派发任务、等待结果、继续回复用户
2. 确保前端不再刷屏展示子 Agent 原始消息
3. 确保写作业务表成为主数据源

重点验证：

1. 创建写作 session
2. 动态创建多个子 Agent
3. 主 Agent 拆任务并发执行
4. 子 Agent 回传结果
5. 主 Agent 汇总生成 draft
6. 前端协作面板状态更新

## 13. 并行执行建议

为了减少互相等待，建议按下面 3 条工作流并行推进：

### 线程 A：后端数据层

负责：

1. Task 1
2. Task 2 中不依赖 prompt 的部分

目标：

1. 先把 `writing_*` 数据读写能力补齐
2. 先把聚合对象结构稳定下来

### 线程 B：后端运行时层

负责：

1. Task 3

目标：

1. 修正 prompt
2. 增加 few-shot
3. 调整 root/sub agent 执行协议
4. 打通等待、唤醒、回调、汇总链路

### 线程 C：前端展示层

负责：

1. Task 5
2. 配合 Task 4 对齐接口 shape

目标：

1. 先把双层页面壳子搭出来
2. 再接聚合接口

## 14. 推荐执行顺序

建议实际推进顺序如下：

1. 先完成 Task 1，确保 `writing_*` 仓储落地
2. 同时启动 Task 3，尽快把 prompt / runtime 从单工作流模式拉回写作协作模式
3. 再完成 Task 2，把 runtime 动作投影到 `writing_*`
4. 补 Task 4 聚合 API
5. 并行接入 Task 5 前端双层页面
6. 最后做 Task 6 联调与回归

## 15. 当前建议的第一批开工任务

如果现在就开始改代码，我建议第一批直接做这 4 个：

1. 建 `writing_*` 的 domain / repository / mapper / PO
2. 建 `WritingAgentCoordinatorService` 和 `WritingTaskService`
3. 重写 `SwarmPromptTemplate`，从“单工作流编排”切换到“动态多智能体写作”
4. 在前端先搭出“主对话区 + 协作面板”静态骨架

这样一轮做完后，项目就会从“方案已经明确”进入“主干实现已经成型”的状态。
