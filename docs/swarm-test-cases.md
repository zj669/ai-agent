# Swarm 多智能体协作 — 用户测试用例

> 创建时间：2026-03-05
> 最后更新：2026-03-06（新增消息乐观更新、thinking 状态、终止按钮相关用例）
> 测试范围：Swarm 模块前端交互 + 端到端协作流程
> 前置依赖：已登录、已配置至少一个 LLM 模型、后端服务与 Docker 基础设施已启动

---

## 一、Workspace 管理

> 入口页：`/swarm`（侧边栏「多Agent协作」）

### TC-SW-01 空状态展示

| 项目 | 内容 |
|------|------|
| **前置条件** | 当前用户没有任何 Workspace |
| **操作步骤** | 1. 点击侧边栏「多Agent协作」进入 `/swarm` |
| **预期结果** | 页面中央显示 Empty 空状态组件，提示「暂无 Workspace，点击上方按钮创建」；右上角显示「新建 Workspace」按钮 |
| **关联组件** | `SwarmWorkspaceListPage.tsx` |

### TC-SW-02 新建 Workspace

| 项目 | 内容 |
|------|------|
| **前置条件** | 系统中已配置至少一个 LLM 模型 |
| **操作步骤** | 1. 点击右上角「新建 Workspace」按钮<br/>2. 在弹窗中输入名称（如「小说创作协作」）<br/>3. 在下拉框中选择一个 LLM 模型配置<br/>4. 点击「确定」 |
| **预期结果** | 1. 弹窗出现，包含「名称」输入框和「模型配置」下拉框<br/>2. 下拉框中列出所有已配置的 LLM 模型（格式：名称 (provider / model)）<br/>3. 点击确定后弹窗关闭，显示「创建成功」提示<br/>4. 页面自动跳转到 `/swarm/:workspaceId` 协作主页 |
| **关联组件** | `SwarmWorkspaceListPage.tsx`, `CreateWorkspaceModal.tsx` |

### TC-SW-03 新建表单校验

| 项目 | 内容 |
|------|------|
| **前置条件** | 已打开新建 Workspace 弹窗 |
| **操作步骤** | 1. 不填写名称，不选择模型，直接点击「确定」<br/>2. 只填写名称，不选择模型，点击「确定」<br/>3. 只选择模型，不填写名称，点击「确定」 |
| **预期结果** | 1. 名称输入框下方提示「请输入名称」，模型下拉框下方提示「请选择模型配置」<br/>2. 模型下拉框下方提示「请选择模型配置」，不提交<br/>3. 名称输入框下方提示「请输入名称」，不提交 |
| **关联组件** | `CreateWorkspaceModal.tsx` |

### TC-SW-04 Workspace 卡片展示

| 项目 | 内容 |
|------|------|
| **前置条件** | 当前用户已创建至少一个 Workspace |
| **操作步骤** | 1. 进入 `/swarm` 页面 |
| **预期结果** | 每个 Workspace 以卡片形式展示，包含：团队图标、名称、Agent 数量（如「2 个 Agent」）、创建日期；底部有「进入」和「删除」两个操作按钮 |
| **关联组件** | `WorkspaceCard.tsx` |

### TC-SW-05 进入 Workspace

| 项目 | 内容 |
|------|------|
| **前置条件** | 页面上有至少一个 Workspace 卡片 |
| **操作步骤** | 1. 点击某 Workspace 卡片底部的「进入」按钮 |
| **预期结果** | 页面跳转到 `/swarm/:workspaceId`，加载协作主页 |
| **关联组件** | `WorkspaceCard.tsx`, `SwarmMainPage.tsx` |

### TC-SW-06 删除 Workspace

| 项目 | 内容 |
|------|------|
| **前置条件** | 页面上有至少一个 Workspace 卡片 |
| **操作步骤** | 1. 点击某 Workspace 卡片底部的「删除」按钮<br/>2. 在弹出的 Popconfirm 中点击「取消」<br/>3. 再次点击「删除」<br/>4. 在 Popconfirm 中点击「确认」 |
| **预期结果** | 1. 出现确认弹窗「确认删除？」<br/>2. 点击取消后弹窗关闭，卡片仍在<br/>3-4. 点击确认后卡片消失，显示「已删除」提示；该 Workspace 下所有 Agent、群组、消息一并删除 |
| **关联组件** | `WorkspaceCard.tsx` |

---

## 二、协作主页初始状态

> 页面：`/swarm/:workspaceId`

### TC-SM-01 初始布局

| 项目 | 内容 |
|------|------|
| **前置条件** | 已创建 Workspace 并进入 |
| **操作步骤** | 1. 观察页面布局 |
| **预期结果** | 左侧 Sidebar 宽 260px（浅灰背景、右侧边框分隔），右侧 Chat Panel 占满剩余空间，全高布局；无 Graph 图谱区域 |
| **关联组件** | `SwarmMainPage.tsx` |

### TC-SM-02 默认 Agent 选中

| 项目 | 内容 |
|------|------|
| **前置条件** | 刚进入 Workspace（未进行任何操作） |
| **操作步骤** | 1. 观察侧边栏和聊天面板顶栏 |
| **预期结果** | 侧边栏中初始 Assistant Agent 处于选中态（蓝色高亮背景）；聊天面板顶栏显示 Agent 的 role 名称、紫色机器人图标、绿色「空闲」状态 Tag |
| **关联组件** | `SwarmMainPage.tsx`, `AgentTreeList.tsx`, `SwarmChatPanel.tsx` |

### TC-SM-03 默认空状态

| 项目 | 内容 |
|------|------|
| **前置条件** | 刚进入 Workspace，未发送过消息 |
| **操作步骤** | 1. 观察聊天区域 |
| **预期结果** | 聊天消息区域居中显示灰色提示文字「向 assistant 发送消息开始对话」；输入框 placeholder 显示「向 assistant 发送消息，Ctrl+Enter 发送」 |
| **关联组件** | `SwarmChatPanel.tsx`, `SwarmComposer.tsx` |

### TC-SM-04 Agent 树形列表

| 项目 | 内容 |
|------|------|
| **前置条件** | 刚创建的 Workspace（只有默认的 Human + Assistant） |
| **操作步骤** | 1. 观察侧边栏 Agent 列表 |
| **预期结果** | 树形展示两个节点：根节点为 Human（用户图标、半透明、不可选），子节点为 Assistant（机器人图标、绿色状态灯）；树默认全部展开 |
| **关联组件** | `AgentTreeList.tsx` |

---

## 三、侧边栏交互

### TC-SB-01 点击 Agent 切换对话

| 项目 | 内容 |
|------|------|
| **前置条件** | Workspace 中有至少两个非 Human Agent（初始 Assistant + 一个子 Agent），且各自有消息记录 |
| **操作步骤** | 1. 当前选中 Assistant Agent，聊天面板显示其对话<br/>2. 点击侧边栏中的子 Agent 节点 |
| **预期结果** | 1. 侧边栏选中态从 Assistant 移到子 Agent（蓝色背景切换）<br/>2. 聊天面板顶栏更新为子 Agent 的 role、description、状态<br/>3. 消息列表刷新为人类与子 Agent 的 P2P 对话内容<br/>4. 输入框 placeholder 更新为「向 {子Agent.role} 发送消息，Ctrl+Enter 发送」 |
| **关联组件** | `AgentTreeList.tsx`, `SwarmMainPage.tsx`, `SwarmChatPanel.tsx` |

### TC-SB-02 Human 不可选

| 项目 | 内容 |
|------|------|
| **前置条件** | 侧边栏可见 Human 节点 |
| **操作步骤** | 1. 点击 Human 节点 |
| **预期结果** | 无任何变化 —— 选中态不变、聊天面板不切换、顶栏不更新 |
| **关联组件** | `AgentTreeList.tsx`, `SwarmMainPage.tsx` |

### TC-SB-03 选中态高亮

| 项目 | 内容 |
|------|------|
| **前置条件** | 侧边栏有多个 Agent |
| **操作步骤** | 1. 依次点击不同的 Agent 节点 |
| **预期结果** | 被选中的 Agent 显示蓝色高亮背景（`#e6f4ff`），其他 Agent 无背景色；每次只有一个 Agent 高亮 |
| **关联组件** | `AgentTreeList.tsx` |

### TC-SB-04 状态灯展示

| 项目 | 内容 |
|------|------|
| **前置条件** | 有多个 Agent 处于不同状态 |
| **操作步骤** | 1. 观察各 Agent 右侧的状态指示灯 |
| **预期结果** | IDLE → 绿色实心圆点；BUSY → 红色实心圆点（带脉冲动画，1.2s 循环闪烁）；WAKING → 黄色实心圆点；STOPPED → 灰色实心圆点；Human 节点无状态灯 |
| **关联组件** | `AgentTreeList.tsx` |

### TC-SB-05 Description 展示

| 项目 | 内容 |
|------|------|
| **前置条件** | 有 Agent 带有 description（如 Root Agent 创建子 Agent 时指定了 description） |
| **操作步骤** | 1. 观察侧边栏中带 description 的 Agent 节点 |
| **预期结果** | role 名称下方显示灰色小字 description，单行截断（超出部分用省略号），字号 11px |
| **关联组件** | `AgentTreeList.tsx` |

### TC-SB-06 搜索过滤

| 项目 | 内容 |
|------|------|
| **前置条件** | Workspace 中有多个 Agent（如 assistant、researcher、writer） |
| **操作步骤** | 1. 在侧边栏顶部搜索框中输入「res」 |
| **预期结果** | Agent 列表只显示 role 中包含「res」的 Agent（如 researcher）；不匹配的 Agent 被隐藏 |
| **关联组件** | `SwarmSidebar.tsx`, `SwarmSearchBar.tsx` |

### TC-SB-07 清除搜索

| 项目 | 内容 |
|------|------|
| **前置条件** | 搜索框中有输入文字，列表已被过滤 |
| **操作步骤** | 1. 点击搜索框右侧的清除按钮（X 图标） |
| **预期结果** | 搜索框清空，Agent 列表恢复完整展示所有 Agent |
| **关联组件** | `SwarmSearchBar.tsx`, `SwarmSidebar.tsx` |

---

## 四、消息收发与展示

### TC-MSG-01 发送消息

| 项目 | 内容 |
|------|------|
| **前置条件** | 已选中某个 Agent，输入框可用 |
| **操作步骤** | 1. 在输入框中输入「你好」<br/>2. 按 Ctrl+Enter 发送<br/>*或*<br/>2. 点击「发送」按钮 |
| **预期结果** | 1. 消息立即出现在聊天区域右侧，蓝色气泡，白色文字<br/>2. 头像为蓝色圆形，显示「我」<br/>3. 输入框清空<br/>4. 发送按钮短暂显示 loading 状态 |
| **关联组件** | `SwarmComposer.tsx`, `SwarmMessageBubble.tsx` |

### TC-MSG-02 动态 Placeholder

| 项目 | 内容 |
|------|------|
| **前置条件** | 侧边栏有多个 Agent |
| **操作步骤** | 1. 选中 role 为「assistant」的 Agent，观察输入框 placeholder<br/>2. 切换到 role 为「researcher」的 Agent，再次观察 |
| **预期结果** | 1. placeholder 显示「向 assistant 发送消息，Ctrl+Enter 发送」<br/>2. 切换后 placeholder 变为「向 researcher 发送消息，Ctrl+Enter 发送」 |
| **关联组件** | `SwarmComposer.tsx` |

### TC-MSG-03 空消息拦截

| 项目 | 内容 |
|------|------|
| **前置条件** | 输入框为空或只有空格 |
| **操作步骤** | 1. 不输入任何内容，观察发送按钮<br/>2. 输入若干空格，观察发送按钮<br/>3. 按 Ctrl+Enter |
| **预期结果** | 1-2. 发送按钮处于 disabled 状态（灰色不可点击）<br/>3. Ctrl+Enter 无反应，不发送消息 |
| **关联组件** | `SwarmComposer.tsx` |

### TC-MSG-04 Agent BUSY 时输入禁用

| 项目 | 内容 |
|------|------|
| **前置条件** | 当前选中的 Agent 状态为 BUSY（正在推理中） |
| **操作步骤** | 1. 观察输入框和发送按钮状态 |
| **预期结果** | 输入框和发送按钮均 disabled；侧边栏该 Agent 状态灯为红色脉冲；聊天面板顶栏 Tag 显示红色「忙碌」 |
| **关联组件** | `SwarmComposer.tsx`, `SwarmChatPanel.tsx`, `AgentTreeList.tsx` |

### TC-MSG-05 Agent 回复展示

| 项目 | 内容 |
|------|------|
| **前置条件** | 用户已发送消息，Agent 回复完成 |
| **操作步骤** | 1. 观察 Agent 的回复消息 |
| **预期结果** | 1. 消息左对齐，灰色气泡（`#f0f0f0`），黑色文字<br/>2. 气泡上方显示 Agent 的 role 名称（灰色小字）<br/>3. 左侧头像为彩色圆形，显示 role 首字母大写（如 A）<br/>4. 消息支持自动换行 |
| **关联组件** | `SwarmMessageBubble.tsx` |

### TC-MSG-06 流式输出

| 项目 | 内容 |
|------|------|
| **前置条件** | 用户已发送消息，Agent 正在回复中 |
| **操作步骤** | 1. 观察聊天区域 |
| **预期结果** | 1. Agent 回复内容逐字/逐句出现（SSE 流式推送）<br/>2. 文字末尾有闪烁光标「▌」<br/>3. 消息区域自动滚动到底部<br/>4. 回复完成后光标消失，消息固定 |
| **关联组件** | `SwarmMessageList.tsx`, `SwarmMainPage.tsx` |

### TC-MSG-07 工具调用展示

| 项目 | 内容 |
|------|------|
| **前置条件** | Agent 在回复过程中调用了工具（如 createAgent、send） |
| **操作步骤** | 1. 观察聊天区域中的工具调用消息 |
| **预期结果** | 工具调用显示为蓝色 Tag（ToolCallBadge），带工具图标和工具名称；不同工具有不同 emoji 前缀（如 create 为 🔧、send 为 📨） |
| **关联组件** | `SwarmMessageBubble.tsx`, `ToolCallBadge.tsx` |

### TC-MSG-08 工具调用详情展开

| 项目 | 内容 |
|------|------|
| **前置条件** | 聊天区域有工具调用 Tag |
| **操作步骤** | 1. 点击某个 ToolCallBadge Tag<br/>2. 再次点击同一个 Tag |
| **预期结果** | 1. 首次点击：Tag 下方展开浅灰背景区域，显示「参数：」（JSON 格式）和「结果：」（JSON 格式），等宽字体<br/>2. 再次点击：详情区域收起 |
| **关联组件** | `ToolCallBadge.tsx` |

### TC-MSG-09 等待子 Agent 卡片

| 项目 | 内容 |
|------|------|
| **前置条件** | Root Agent 调用 send 向子 Agent 派发任务，正在等待回复 |
| **操作步骤** | 1. 观察聊天区域输入框上方 |
| **预期结果** | 显示黄色边框、黄色背景的 WaitingCard，内容为「⏳ 正在等待 {role} 回复...」，左侧有旋转加载图标，右侧有红色「终止」按钮 |
| **关联组件** | `WaitingCard.tsx`, `SwarmChatPanel.tsx` |

### TC-MSG-10 终止等待

| 项目 | 内容 |
|------|------|
| **前置条件** | WaitingCard 正在显示 |
| **操作步骤** | 1. 点击 WaitingCard 右侧的「终止」按钮 |
| **预期结果** | 1. 按钮显示 loading 状态<br/>2. 调用 stopAgent API 停止目标 Agent<br/>3. WaitingCard 消失<br/>4. 被终止的 Agent 状态变为 STOPPED（灰色状态灯） |
| **关联组件** | `WaitingCard.tsx` |

---

## 五、多 Agent 协作完整流程（端到端）

> 以下用例需要后端服务完整运行，涉及 LLM 调用

### TC-E2E-01 基本对话

| 项目 | 内容 |
|------|------|
| **前置条件** | 新建 Workspace 并进入，选中初始 Assistant |
| **操作步骤** | 1. 输入「你好，请介绍一下你自己」<br/>2. 按 Ctrl+Enter 发送 |
| **预期结果** | 1. 用户消息右侧蓝色气泡<br/>2. Agent 状态灯变红（BUSY），顶栏 Tag 变为红色「忙碌」<br/>3. Agent 回复流式出现在左侧灰色气泡<br/>4. 回复完成后状态灯恢复绿色（IDLE），Tag 恢复「空闲」<br/>5. 无工具调用 Tag 出现 |
| **关联组件** | 全链路 |

### TC-E2E-02 Agent 创建子 Agent

| 项目 | 内容 |
|------|------|
| **前置条件** | 新建 Workspace 并进入 |
| **操作步骤** | 1. 输入「帮我写一本玄幻小说，请先帮我做题材调研和大纲撰写」<br/>2. 发送消息 |
| **预期结果** | 1. Root Agent 开始推理，状态变 BUSY<br/>2. 聊天区域出现 createAgent 工具调用 Tag（可展开查看参数：role、description）<br/>3. 侧边栏自动刷新，在 Assistant 节点下方出现新的子 Agent 节点（如「researcher」「writer」），带 description 小字<br/>4. 新 Agent 节点有绿色状态灯（IDLE） |
| **关联组件** | 全链路 |

### TC-E2E-03 任务派发与等待

| 项目 | 内容 |
|------|------|
| **前置条件** | TC-E2E-02 完成，子 Agent 已创建 |
| **操作步骤** | 1. 观察 Root Agent 继续推理 |
| **预期结果** | 1. Root Agent 调用 send 工具，聊天区域出现 send 工具调用 Tag<br/>2. WaitingCard 出现：「⏳ 正在等待 researcher 回复...」<br/>3. 子 Agent 状态灯变红（BUSY），表示正在处理任务<br/>4. 子 Agent 完成后 WaitingCard 消失<br/>5. Root Agent 被唤醒继续处理（可能继续派发给下一个子 Agent） |
| **关联组件** | 全链路 |

### TC-E2E-04 切换查看子 Agent 对话

| 项目 | 内容 |
|------|------|
| **前置条件** | 子 Agent 已被创建且有消息记录 |
| **操作步骤** | 1. 点击侧边栏中的子 Agent（如「researcher」） |
| **预期结果** | 1. 聊天面板切换为人类与该子 Agent 的 P2P 对话<br/>2. 顶栏显示子 Agent 的 role、description、状态<br/>3. 消息列表中可看到：<br/>　- Root Agent 通过 send 发来的任务消息<br/>　- 子 Agent 的处理回复<br/>　- 可能有 tool_call 消息 |
| **关联组件** | `SwarmMainPage.tsx`, `AgentTreeList.tsx`, `SwarmChatPanel.tsx` |

### TC-E2E-05 结果汇总

| 项目 | 内容 |
|------|------|
| **前置条件** | 所有子 Agent 完成任务并回复 |
| **操作步骤** | 1. 点击侧边栏切回初始 Assistant Agent<br/>2. 观察聊天记录 |
| **预期结果** | 1. Root Agent 收到所有子 Agent 回复后被唤醒<br/>2. Root Agent 生成汇总回复，以文本形式出现在聊天中<br/>3. 整个流程中工具调用 Tag 清晰可展开查看<br/>4. 所有 Agent 状态恢复为 IDLE（绿色状态灯） |
| **关联组件** | 全链路 |

### TC-E2E-06 子 Agent 禁止递归

| 项目 | 内容 |
|------|------|
| **前置条件** | 已有子 Agent 被创建 |
| **操作步骤** | 1. 切换到子 Agent 对话<br/>2. 向子 Agent 发送「请创建一个新的 Agent 帮我做这件事」 |
| **预期结果** | 子 Agent 不会调用 createAgent 或 executeWorkflow 工具（Prompt 限制 + 工具拦截）；子 Agent 直接以文本回复解释自己的职责范围，或直接执行任务；不会出现新的 Agent 节点 |
| **关联组件** | 后端 `SwarmAgentRunner.java`, `SwarmPromptTemplate.java` |

---

## 六、异常与边界场景

### TC-ERR-01 SSE 连接断开恢复

| 项目 | 内容 |
|------|------|
| **前置条件** | 已进入 Workspace 协作主页，SSE 连接正常 |
| **操作步骤** | 1. 模拟网络断开（关闭 Wi-Fi 或断开网络）<br/>2. 等待 5 秒<br/>3. 恢复网络连接<br/>4. 发送一条新消息 |
| **预期结果** | 1. 断网期间页面不崩溃<br/>2. 恢复网络后 EventSource 自动重连<br/>3. 新消息可正常发送和接收<br/>4. 断网期间的消息在重新加载后可见 |
| **关联组件** | `useUIStream.ts`, `swarmService.ts` |

### TC-ERR-02 访问不存在的 Workspace

| 项目 | 内容 |
|------|------|
| **前置条件** | 无 |
| **操作步骤** | 1. 在浏览器地址栏直接输入 `/swarm/999999`（不存在的 ID）并访问 |
| **预期结果** | 页面显示 Spin 加载后，API 返回错误；页面不崩溃，展示 loading 或空状态（不应出现白屏） |
| **关联组件** | `SwarmMainPage.tsx` |

### TC-ERR-03 无 LLM 配置时创建 Workspace

| 项目 | 内容 |
|------|------|
| **前置条件** | 系统中未配置任何 LLM 模型 |
| **操作步骤** | 1. 点击「新建 Workspace」<br/>2. 观察模型配置下拉框 |
| **预期结果** | 模型配置下拉框为空，无选项可选；用户无法提交表单（模型为必填项） |
| **关联组件** | `CreateWorkspaceModal.tsx` |

### TC-ERR-04 Agent 长时间无响应

| 项目 | 内容 |
|------|------|
| **前置条件** | Agent 正在处理任务（BUSY 状态） |
| **操作步骤** | 1. 等待超过 1 分钟，Agent 仍未回复<br/>2. 观察页面状态 |
| **预期结果** | 1. Agent 状态灯保持红色脉冲（BUSY）<br/>2. 如有 WaitingCard，终止按钮始终可用<br/>3. 用户可通过终止按钮手动停止 Agent<br/>4. 页面不卡死，其他 Agent 对话仍可切换查看 |
| **关联组件** | `WaitingCard.tsx`, `AgentTreeList.tsx` |

### TC-ERR-05 快速连续发送消息

| 项目 | 内容 |
|------|------|
| **前置条件** | 已选中某个 Agent，输入框可用 |
| **操作步骤** | 1. 快速连续输入并发送 3 条消息（间隔 < 1 秒） |
| **预期结果** | 1. 3 条消息均正确出现在聊天区域，顺序与发送顺序一致<br/>2. 无重复消息<br/>3. 每条消息发送后输入框正确清空<br/>4. Agent 依次处理消息（或合并处理） |
| **关联组件** | `SwarmComposer.tsx`, `SwarmMessageList.tsx` |

---

## 七、消息 UX 专项（2026-03-06 新增）

> 涉及改动：乐观更新、thinking 状态气泡、主动终止按钮
> 关联 bugfix：[2026-03-06-swarm-chat-ux-bugs.md](bugfix-log/2026-03-06-swarm-chat-ux-bugs.md)、[2026-03-06-swarm-stop-button.md](bugfix-log/2026-03-06-swarm-stop-button.md)

### TC-UX-01 消息发送乐观更新

| 项目 | 内容 |
|------|------|
| **前置条件** | 已进入 Workspace，选中某个 Agent，输入框可用 |
| **操作步骤** | 1. 在输入框输入「帮我写一本玄幻小说」<br/>2. 点击「发送」或按 Ctrl+Enter |
| **预期结果** | 1. 消息在点击瞬间（无网络延迟感）立即出现在聊天区域右侧蓝色气泡中<br/>2. 输入框同时清空<br/>3. 不需要等待后端 API 返回才渲染消息 |
| **关联组件** | `useSwarmMessages.ts`（乐观更新逻辑） |

### TC-UX-02 Thinking 状态气泡显示

| 项目 | 内容 |
|------|------|
| **前置条件** | 用户消息已发送（TC-UX-01 完成） |
| **操作步骤** | 1. 发送消息后立即观察聊天区域 |
| **预期结果** | 1. 用户消息出现的同时（或紧接着），Agent 侧出现灰色气泡<br/>2. 气泡内显示旋转 Loading 图标 + 灰色文字「正在思考...」<br/>3. 气泡左侧有 Agent 头像（角色首字母）和角色名称<br/>4. 当 SSE 开始推送内容时，该气泡自动切换为流式文字输出（逐字 + ▌光标） |
| **关联组件** | `SwarmMainPage.tsx`、`SwarmMessageList.tsx`、`SwarmMessageBubble.tsx` |

### TC-UX-03 主动终止 Agent

| 项目 | 内容 |
|------|------|
| **前置条件** | Agent 处于 thinking 或流式输出状态（`streamingContent !== null`） |
| **操作步骤** | 1. 观察输入区域的按钮<br/>2. 点击红色「终止」按钮 |
| **预期结果** | 1. 当 Agent 处于 thinking/streaming 时，发送按钮被替换为红色「终止」按钮（StopOutlined 图标）<br/>2. 点击后按钮显示 loading 状态<br/>3. 后端 Agent Runner 收到停止信号并终止推理<br/>4. thinking/流式气泡消失<br/>5. 消息列表刷新（已生成的部分内容若已保存则显示） |
| **关联组件** | `SwarmComposer.tsx`、`SwarmMainPage.tsx`（handleStop） |

### TC-UX-04 终止后状态恢复

| 项目 | 内容 |
|------|------|
| **前置条件** | TC-UX-03 终止操作已完成 |
| **操作步骤** | 1. 观察终止后的界面状态 |
| **预期结果** | 1. 发送按钮重新出现（红色终止按钮消失）<br/>2. 输入框恢复可用（不再 disabled）<br/>3. 侧边栏中该 Agent 的状态灯恢复绿色（IDLE）<br/>4. 顶栏状态 Tag 恢复显示「空闲」<br/>5. 可以继续发送新消息 |
| **关联组件** | `SwarmMainPage.tsx`、`AgentTreeList.tsx`、`SwarmChatPanel.tsx` |

### TC-UX-05 终止按钮切换逻辑

| 项目 | 内容 |
|------|------|
| **前置条件** | 已进入 Workspace |
| **操作步骤** | 1. 初始状态观察按钮<br/>2. 发送一条消息，观察 Agent 开始 thinking 时的按钮<br/>3. Agent 回复完成后，观察按钮 |
| **预期结果** | 1. 初始：发送按钮（蓝色，SendOutlined 图标）<br/>2. Thinking/Streaming：替换为终止按钮（红色，StopOutlined 图标）；输入框 disabled<br/>3. 完成后：自动切回发送按钮；输入框恢复可用 |
| **关联组件** | `SwarmComposer.tsx`（isStreaming 条件渲染） |

---

## 附录：测试用例汇总表

| 编号 | 模块 | 用例名称 | 优先级 |
|------|------|----------|--------|
| TC-SW-01 | Workspace 管理 | 空状态展示 | P2 |
| TC-SW-02 | Workspace 管理 | 新建 Workspace | P0 |
| TC-SW-03 | Workspace 管理 | 新建表单校验 | P1 |
| TC-SW-04 | Workspace 管理 | 卡片展示 | P2 |
| TC-SW-05 | Workspace 管理 | 进入 Workspace | P0 |
| TC-SW-06 | Workspace 管理 | 删除 Workspace | P1 |
| TC-SM-01 | 协作主页 | 初始布局 | P1 |
| TC-SM-02 | 协作主页 | 默认 Agent 选中 | P0 |
| TC-SM-03 | 协作主页 | 默认空状态 | P2 |
| TC-SM-04 | 协作主页 | Agent 树形列表 | P1 |
| TC-SB-01 | 侧边栏交互 | 点击 Agent 切换 | P0 |
| TC-SB-02 | 侧边栏交互 | Human 不可选 | P1 |
| TC-SB-03 | 侧边栏交互 | 选中态高亮 | P2 |
| TC-SB-04 | 侧边栏交互 | 状态灯展示 | P1 |
| TC-SB-05 | 侧边栏交互 | Description 展示 | P2 |
| TC-SB-06 | 侧边栏交互 | 搜索过滤 | P2 |
| TC-SB-07 | 侧边栏交互 | 清除搜索 | P2 |
| TC-MSG-01 | 消息收发 | 发送消息 | P0 |
| TC-MSG-02 | 消息收发 | 动态 Placeholder | P2 |
| TC-MSG-03 | 消息收发 | 空消息拦截 | P1 |
| TC-MSG-04 | 消息收发 | BUSY 时禁用 | P1 |
| TC-MSG-05 | 消息收发 | Agent 回复展示 | P0 |
| TC-MSG-06 | 消息收发 | 流式输出 | P0 |
| TC-MSG-07 | 消息收发 | 工具调用展示 | P0 |
| TC-MSG-08 | 消息收发 | 工具调用展开 | P1 |
| TC-MSG-09 | 消息收发 | 等待卡片 | P0 |
| TC-MSG-10 | 消息收发 | 终止等待 | P1 |
| TC-E2E-01 | 端到端 | 基本对话 | P0 |
| TC-E2E-02 | 端到端 | 创建子 Agent | P0 |
| TC-E2E-03 | 端到端 | 任务派发与等待 | P0 |
| TC-E2E-04 | 端到端 | 切换查看子 Agent | P0 |
| TC-E2E-05 | 端到端 | 结果汇总 | P0 |
| TC-E2E-06 | 端到端 | 子 Agent 禁止递归 | P0 |
| TC-ERR-01 | 异常场景 | SSE 连接断开恢复 | P1 |
| TC-ERR-02 | 异常场景 | 不存在的 Workspace | P2 |
| TC-ERR-03 | 异常场景 | 无 LLM 配置 | P2 |
| TC-ERR-04 | 异常场景 | Agent 长时间无响应 | P1 |
| TC-ERR-05 | 异常场景 | 快速连续发送消息 | P1 |
| TC-UX-01 | 消息 UX | 发送乐观更新 | P0 |
| TC-UX-02 | 消息 UX | Thinking 状态显示 | P0 |
| TC-UX-03 | 消息 UX | 主动终止 Agent | P0 |
| TC-UX-04 | 消息 UX | 终止后状态恢复 | P1 |
| TC-UX-05 | 消息 UX | 终止按钮切换逻辑 | P1 |
