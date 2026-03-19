# LLM 模板路径引用任务拆分

- **日期**: 2026-03-17
- **对应需求**: `docs/2026-03-17-llm-template-path-reference-requirements.md`
- **对应计划**: `docs/2026-03-17-llm-template-path-reference-plan.md`

## Task 1: 后端模板解析器设计与实现

**目标**

- 为 Prompt 模板提供统一路径解析能力

**涉及文件**

- Create: `ai-agent-infrastructure/src/main/java/.../workflow/template/PromptTemplateResolver.java`
- Create: `ai-agent-infrastructure/src/main/java/.../workflow/template/PromptValueFormatter.java`
- Create: 对应测试文件

**工作内容**

- 定义模板占位符匹配规则
- 支持：
  - `{{key}}`
  - `{{inputs.key}}`
  - `{{nodeId.output.key}}`
- 实现占位符扫描与替换
- 实现值格式化逻辑

**验收标准**

- 能正确替换三类占位符
- 路径不存在时保留原占位符
- 输出 warn 日志

## Task 2: LLM 执行器接入模板解析器

**目标**

- 让 LLM 节点运行时真正支持模板内路径解析

**涉及文件**

- Update: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java`
- Update: 相关测试

**工作内容**

- 改造 `buildUserPrompt()`
- `userPromptTemplate` 非空时走新解析器
- `userPromptTemplate` 为空时保留旧逻辑
- 评估并决定 `systemPrompt` 是否同步接入

**验收标准**

- 旧图仍能运行
- 新模板路径可解析
- 单测通过

## Task 3: 后端测试补齐

**目标**

- 保证解析器和 LLM 节点行为稳定

**建议测试点**

- 简单变量
- 全局输入变量
- 节点输出路径变量
- 未找到路径
- null 值
- 数组 / 对象值

**验收标准**

- 核心单测覆盖主要路径
- 回归测试通过

## Task 4: 前端 LLM 输入区改造

**目标**

- 将 LLM 输入页改成单大文本框 Prompt 模式

**涉及文件**

- Update: `ai-agent-foward/src/modules/workflow/components/NodeConfigTabs.tsx`
- Update: `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx`
- Update: 可能新增专用 Prompt 编辑组件

**工作内容**

- LLM 输入区默认显示一个大文本框
- 文本框绑定 `userConfig.userPromptTemplate`
- 将 `inputSchema` 的编辑入口移到高级模式

**验收标准**

- 默认不再出现多个普通输入字段卡片
- 用户可以直接编辑长文本 Prompt

## Task 5: 前端变量插入器

**目标**

- 降低用户手写路径的错误率

**涉及文件**

- Update: LLM 输入区相关前端组件

**工作内容**

- 展示 `inputs.xxx`
- 展示可达祖先节点输出字段
- 点击后在光标位置插入：
  - `{{inputs.query}}`
  - `{{nodeId.output.fieldKey}}`

**验收标准**

- 插入内容格式正确
- 支持多跳祖先节点

## Task 6: 前端保存与加载兼容

**目标**

- 保持历史图兼容，保证新字段正常持久化

**涉及文件**

- Update: `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`

**工作内容**

- 保存时写入 `userConfig.userPromptTemplate`
- 加载时正确回显
- 与现有 `contextRefNodes` 并存
- 保留对历史 `inputSchema` 的兼容

**验收标准**

- 新图保存 / 读取正常
- 老图不报错、不丢配置

## Task 7: 元数据模板与初始化默认值

**目标**

- 给前后端统一默认行为

**涉及文件**

- Update: 初始化 SQL
- Update: 如有需要，补充元数据映射

**工作内容**

- 评估是否为 LLM 增加 `userPromptTemplate` 的元数据字段定义
- 评估默认模板的初始化策略

**验收标准**

- 新建 LLM 节点具有合理默认 Prompt 模板

## Task 8: 联调工作流验证

**目标**

- 用真实链路验证端到端效果

**建议链路**

- `START -> KNOWLEDGE -> TOOL -> LLM -> END`

**验证内容**

- `{{inputs.query}}`
- `{{knowledge-node.output.knowledge_list}}`
- `{{tool-node.output.result}}`

**验收标准**

- 实际执行时 Prompt 已被替换
- LLM 输出符合预期

## Task 9: 文档与回归

**目标**

- 收尾并沉淀

**工作内容**

- 更新设计文档 / 变更说明
- 记录兼容策略
- 记录已知限制
- 跑前后端相关测试

**验收标准**

- 文档齐全
- 测试通过
- 风险说明清晰

## 建议执行顺序

1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5
6. Task 6
7. Task 8
8. Task 9

## 里程碑

### 里程碑 A：后端能力就绪

- Task 1 - Task 3 完成

### 里程碑 B：前端编辑体验就绪

- Task 4 - Task 6 完成

### 里程碑 C：端到端可交付

- Task 8 - Task 9 完成
