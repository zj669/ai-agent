# PRD：动态多智能体写作协作

## 1. 背景

当前 swarm 机制已经具备：

- `workspace`
- `agent`
- `group`
- `message`

这些基础运行能力，能够支持主 Agent 创建子 Agent、发送消息、唤醒执行与回传结果。

但如果直接把它当作“多智能体群聊系统”来承载写作，会出现明显问题：

1. 子 Agent 消息过多，主时间线容易被刷屏。
2. 子 Agent 的原始沟通内容对用户价值不高，反而增加理解成本。
3. 前端如果直接平铺所有 agent 对话，体验会非常混乱。
4. 写作场景真正需要的是“分工执行 + 结果汇总”，而不是“公开群聊”。
5. 当前系统缺少围绕写作任务的业务视图，无法直接表达一次写作 session 中有哪些 Agent、任务、结果和草稿版本。

因此，本次需求希望在保留现有 swarm 运行层的前提下，新增一层“写作协作业务层”，让主 Agent 可以动态创建子 Agent，把子 Agent 当成任务执行器，而前端只展示：

- 主对话区
- 协作面板

从而实现一个简易、稳定、可扩展的多智能体写作产品形态。

## 2. 问题定义

当前系统在写作场景下存在以下问题：

1. 子 Agent 缺少“写作任务”维度，只能从 message 中硬推断业务语义。
2. 前端展示过于贴近 transport 层，容易把中间消息、工具调用、内部协作直接暴露给用户。
3. 没有一组稳定的数据结构来表达：
   - 当前写作任务
   - 当前有哪些子 Agent
   - 每个子 Agent 正在处理什么
   - 每个子 Agent 最近产出了什么
   - 当前草稿版本是什么
4. 子 Agent 如果写死为固定角色，灵活性不足，无法让主 Agent 根据任务动态编排。
5. 写作流程缺少 session、task、result、draft 等业务对象，不利于后续扩展章节写作、局部改写、版本管理。

## 3. 产品目标

本次改造的目标如下：

1. 支持主 Agent 在一次写作任务中动态创建子 Agent。
2. 子 Agent 的职责由主 Agent 决定，不能写死为固定角色集合。
3. 子 Agent 作为任务执行器存在，不再默认把全部消息直接展示给用户。
4. 新增一组写作业务对象：
   - `writing_session`
   - `writing_agent`
   - `writing_task`
   - `writing_result`
   - `writing_draft`
5. 前端只展示两层：
   - 主对话区
   - 协作面板
6. 协作面板以状态卡片为核心，支持点击卡片查看结果摘要，而不是平铺所有子 Agent 聊天内容。
7. 保留现有 swarm 的运行机制，避免重写整套 runtime。

## 4. 非目标

本次不纳入范围的内容：

1. 不重写底层 swarm runtime。
2. 不把所有子 Agent 的聊天记录做成群聊产品。
3. 不在本期实现复杂的章节树编辑器或长文档 IDE。
4. 不在本期实现跨 session 复用智能推荐 Agent 的复杂匹配算法。
5. 不在本期实现自动工作流编排引擎替代当前 Agent runner。
6. 不在本期实现完整权限系统或多人协作写作。

## 5. 目标用户

主要用户包括：

1. 想通过 AI 协作写小说、剧本、文章、报告的普通用户。
2. 想把写作过程拆分给多个专业子 Agent 的高级用户。
3. 需要可视化追踪“哪个 Agent 在做什么”的产品、测试和开发人员。

## 6. 用户故事

### 用户故事 1：发起一次创作任务

作为用户，我希望在主对话区直接描述我的写作需求，这样主 Agent 可以理解目标并自动组织后续协作。

### 用户故事 2：主 Agent 动态创建子 Agent

作为用户，我希望子 Agent 是主 Agent 根据当前任务动态创建的，而不是系统预设死的几个角色，这样协作方式可以适配不同写作题材和复杂度。

### 用户故事 3：后台协作而不是前台刷屏

作为用户，我希望主对话区仍然保持清爽，只看到我和主 Agent 的正常沟通，不希望看到一堆子 Agent 的原始消息刷满页面。

### 用户故事 4：感知协作进度

作为用户，我希望能看到哪些子 Agent 已创建、谁在执行任务、谁已经完成，这样我可以感知 AI 正在如何协作。

### 用户故事 5：展开查看子 Agent 贡献

作为用户，我希望点击某个协作卡片时，可以看到该 Agent 的任务摘要和产出结果，而不需要翻阅大量原始消息。

### 用户故事 6：查看草稿版本

作为用户，我希望主 Agent 汇总阶段能形成草稿版本，这样我可以看写作结果是如何逐步成形的。

## 7. 核心产品方案

## 7.1 双层架构

系统分成两层：

### 运行层

继续复用当前 swarm 基础设施：

- `swarm_workspace`
- `swarm_workspace_agent`
- `swarm_group`
- `swarm_message`

这层负责：

- 真正创建 Agent
- 建群
- 发消息
- 唤醒运行
- 处理 LLM 推理

### 写作业务层

新增写作协作对象：

- `writing_session`
- `writing_agent`
- `writing_task`
- `writing_result`
- `writing_draft`

这层负责：

- 表达写作任务语义
- 驱动前端展示
- 提供主对话区和协作面板的数据源
- 记录草稿版本

## 7.2 Schema 落地策略

本期 schema 落地采用方案 A：

1. 旧的 `swarm_*` 表不做兼容迁移。
2. 直接删除旧的 `swarm_*` 表后，按同名新结构重建。
3. `writing_*` 表作为新增业务层表，与新的 `swarm_*` 一起落库。
4. 旧数据视为历史遗留，不作为本期保留目标。

本次重建涉及的 `swarm_*` 表包括：

- `swarm_workspace`
- `swarm_workspace_agent`
- `swarm_group`
- `swarm_group_member`
- `swarm_message`

这样做的原因是：

1. 当前 swarm 结构已经出现历史演进遗留，继续修补会让后续实现越来越重。
2. 本期重点是尽快形成稳定、简洁、可实现的多智能体写作运行底座。
3. 直接重建同名表可以保留运行时命名习惯，同时清掉旧字段和旧约束包袱。

## 7.3 动态子 Agent

本次需求明确要求：

1. 子 Agent 不能写死。
2. 子 Agent 必须由主 Agent 根据任务动态创建。
3. 需要保留“现有代码已有的创建 agent 能力”，并基于其结果同步记录写作业务数据。

系统应支持主 Agent 依据任务创建例如：

- 世界观设定 Agent
- 角色设定 Agent
- 剧情规划 Agent
- 对白润色 Agent
- 审稿 Agent
- 某个章节专属 Agent

但不要求系统预置固定集合。

## 7.4 子 Agent 的定位

子 Agent 的核心定位是：

**任务执行器**

而不是：

**公开聊天参与者**

因此系统设计应遵循：

1. 子 Agent 接收明确任务。
2. 子 Agent 尽量一次性完成任务。
3. 子 Agent 产出结构化结果。
4. 主 Agent 负责汇总、重写、对用户表达。
5. 前端默认隐藏子 Agent 原始聊天流。

## 8. 功能需求

## 8.1 写作 Session

系统需要支持创建和管理一次写作会话：

1. 每次用户发起写作任务，应关联一个 `writing_session`。
2. `writing_session` 需要记录：
   - 当前 workspace
   - 主 Agent
   - 当前创作目标
   - 题材或约束
   - 当前状态
3. 一个 workspace 中可以存在多个 writing session。

## 8.2 写作 Agent 映射

系统需要支持把 swarm agent 映射为写作业务 Agent：

1. 当主 Agent 创建子 Agent 时，同时创建 `writing_agent`。
2. `writing_agent` 需要关联已有的 `swarm_workspace_agent.id`。
3. `writing_agent` 需记录角色、能力描述、技能标签、状态和展示顺序。
4. 系统应允许主 Agent 复用已有写作 Agent，而不是无限创建新 Agent。

## 8.3 写作任务

系统需要支持主 Agent 把写作任务拆分给子 Agent：

1. 每个子任务落一条 `writing_task`。
2. `writing_task` 需记录：
   - 分配给谁
   - 任务标题
   - 任务说明
   - 输入内容
   - 预期输出结构
   - 状态
3. `writing_task` 是前端协作面板的主要展示对象。
4. 子 Agent 的 transport message 只是执行通道，不是任务的唯一事实来源。

## 8.4 写作结果

系统需要记录子 Agent 的执行结果：

1. 子 Agent 任务完成后写入 `writing_result`。
2. `writing_result` 至少包含：
   - 摘要
   - 正文内容
   - 可选结构化 JSON
3. 协作面板点击卡片后，应优先展示 `writing_result` 摘要与内容。

## 8.5 草稿版本

系统需要支持主 Agent 汇总多个结果形成稿件版本：

1. 每次主 Agent 汇总产物时，可生成一个 `writing_draft`。
2. `writing_draft` 至少包含：
   - 标题
   - 正文
   - 版本号
   - 来源结果列表
3. 用户后续继续修改时，应能基于已有 draft 继续推进。

## 8.6 前端展示

前端页面只展示两层：

### 主对话区

用于展示：

- 用户消息
- 主 Agent 回复

默认不展示：

- 子 Agent 原始消息
- transport 层中间消息
- 工具调用原文

### 协作面板

用于展示：

- 子 Agent 状态卡片
- 当前任务
- 最近结果摘要

每张卡片默认显示摘要信息，点击后再展开：

- Agent 描述
- 当前或最近任务
- 最新结果摘要
- 结果正文

## 8.7 协作面板展示要求

协作面板至少需要支持：

1. 按状态分组展示：
   - 执行中
   - 已完成
   - 失败
2. 每张卡片展示：
   - Agent 名称
   - Agent 角色
   - 当前状态
   - 当前任务标题
   - 最近更新时间
   - 最新结果摘要
3. 支持点击卡片展开详情。
4. 不默认平铺所有子 Agent 聊天消息。

## 9. 数据模型要求

## 9.1 `writing_session`

建议记录：

- `workspace_id`
- `root_agent_id`
- `human_agent_id`
- `default_group_id`
- `title`
- `goal`
- `constraints_json`
- `status`
- `current_draft_id`

## 9.2 `writing_agent`

建议记录：

- `session_id`
- `swarm_agent_id`
- `role`
- `description`
- `skill_tags_json`
- `status`
- `sort_order`

## 9.3 `writing_task`

建议记录：

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

## 9.4 `writing_result`

建议记录：

- `session_id`
- `task_id`
- `writing_agent_id`
- `swarm_agent_id`
- `result_type`
- `summary`
- `content`
- `structured_payload_json`

## 9.5 `writing_draft`

建议记录：

- `session_id`
- `version_no`
- `title`
- `content`
- `source_result_ids_json`
- `status`
- `created_by_swarm_agent_id`

## 10. 交互要求

系统需要满足以下交互要求：

1. 用户只和主 Agent 在主对话区直接沟通。
2. 协作面板需要实时反映子 Agent 的工作进展。
3. 用户点击某个卡片时，应看到任务与结果摘要，而不是 transport 层原始消息。
4. 子 Agent 的原始 message 不作为默认展示内容。
5. 主 Agent 汇总完成后，应以正常自然语言在主对话区回复用户。

## 11. 成功标准

满足以下条件视为本期需求完成：

1. 主 Agent 可以动态创建子 Agent。
2. 新创建的子 Agent 会同步映射到 `writing_agent`。
3. 主 Agent 可以为子 Agent 创建 `writing_task`。
4. 子 Agent 产出会落到 `writing_result`。
5. 主 Agent 汇总后可生成 `writing_draft`。
6. 前端主页面只显示主对话区和协作面板。
7. 协作面板显示状态卡片而不是平铺原始子 Agent 聊天流。

## 12. 验收标准

### 验收场景 1：动态创建子 Agent

当用户发起一项长篇写作任务时：

1. 主 Agent 可根据任务动态创建多个子 Agent。
2. 每个子 Agent 都会写入 `swarm_workspace_agent` 和 `writing_agent`。
3. 子 Agent 的角色与描述可在协作面板中查看。

### 验收场景 2：任务执行器模式

当主 Agent 给某个子 Agent 派发任务时：

1. 系统写入 `writing_task`
2. 子 Agent 被唤醒执行
3. 完成后写入 `writing_result`
4. 协作面板状态同步更新

### 验收场景 3：前端双层展示

当多个子 Agent 同时参与写作时：

1. 主对话区不出现一长串子 Agent 原始聊天消息
2. 协作面板能看到子 Agent 卡片
3. 点击卡片后能查看任务和结果摘要

### 验收场景 4：主 Agent 汇总

当多个子 Agent 返回结果后：

1. 主 Agent 汇总结果形成新的 `writing_draft`
2. 主对话区输出自然语言回复给用户
3. 用户能继续基于此草稿提出修改需求

## 13. 风险与约束

1. 如果继续把 transport 层消息直接暴露给前端，会破坏本方案的清晰度。
2. 若主 Agent 不受约束地无限创建子 Agent，可能导致 session 膨胀，需要后续增加复用或上限策略。
3. `writing_*` 和 `swarm_*` 存在双层数据，需要保证事务与事件同步逻辑清晰。
4. 若子 Agent 输出不结构化，`writing_result` 的摘要质量会影响协作面板可读性。

## 14. 版本建议

建议按以下节奏推进：

1. V1：新增 `writing_*` 结构，打通主 Agent 动态创建、任务执行、结果落库、双层前端展示
2. V1.1：补充 Agent 复用策略、任务重试、草稿版本切换
3. V1.2：扩展章节级写作、局部改写与更多协作可视化能力
