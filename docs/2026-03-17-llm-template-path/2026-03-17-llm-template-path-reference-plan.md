# LLM 模板路径引用实施计划

- **日期**: 2026-03-17
- **对应需求**: `docs/2026-03-17-llm-template-path-reference-requirements.md`
- **目标**: 支持在 LLM Prompt 模板中直接使用 `{{nodeId.output.key}}` 引用多跳上游节点输出

## 1. 总体方案

本次实施分为三层：

1. 后端模板解析层
2. LLM 执行接入层
3. 前端 Prompt 编辑交互层

原则：

- 先打通后端解析能力，再改前端交互
- 不破坏历史工作流
- 保留旧 `inputSchema` 逻辑作为兼容路径

## 2. 架构设计

### 2.1 新增后端模板解析组件

建议新增组件：

- `PromptTemplateResolver`
- `PromptValueFormatter`

职责：

- `PromptTemplateResolver`
  - 扫描模板中的 `{{...}}`
  - 解析表达式
  - 从 `resolvedInputs`、`ExecutionContext.inputs`、`ExecutionContext.nodeOutputs` 取值
  - 完成模板替换

- `PromptValueFormatter`
  - 统一处理值到字符串的序列化逻辑
  - 处理字符串、数组、对象、空值

### 2.2 LLM 执行器接入

改造 `LlmNodeExecutorStrategy`：

- `buildUserPrompt()`：
  - 当 `userPromptTemplate` 为空时，保持旧逻辑
  - 当 `userPromptTemplate` 不为空时，调用 `PromptTemplateResolver`

- 可选增强：
  - `systemPrompt` 也统一使用模板解析器

### 2.3 前端交互改造

改造 LLM 节点“输入”页：

- 默认显示一个大文本框
- 文本框绑定 `userConfig.userPromptTemplate`
- 提供变量插入器
- 变量插入器支持：
  - `inputs.xxx`
  - 多跳祖先节点输出路径

### 2.4 兼容策略

- 老图继续支持基于 `inputSchema.input` 的旧逻辑
- 新图优先使用 `userPromptTemplate`
- `contextRefNodes` 保留，用于系统提示词参考资料注入

## 3. 语法与解析规则

### 3.1 支持语法

- `{{key}}`
- `{{inputs.key}}`
- `{{nodeId.output.key}}`

### 3.2 不支持语法

- `{{nodeId.key}}`
- `{{foo[0]}}`
- `{{a.b.c.d}}` 的任意深入对象路径
- 过滤器和函数

### 3.3 失败处理

- 路径不存在：保留原占位符
- 记录 `warn` 日志
- 不中断执行

## 4. 数据模型与存储

### 4.1 前端存储

LLM 节点新增或使用：

- `userConfig.userPromptTemplate`

示例：

```json
{
  "llmConfigId": 1,
  "userPromptTemplate": "知识库结果：\n{{knowledge-1.output.knowledge_list}}\n\n用户问题：\n{{inputs.query}}",
  "contextRefNodes": ["knowledge-1"]
}
```

### 4.2 是否依赖 inputSchema

第二阶段不再要求用户通过 `inputSchema` 手工铺平所有 Prompt 变量。

但系统仍保留：

- `inputSchema` 的兼容处理
- 历史图读取能力
- 高级模式下的可编辑能力

## 5. 实施顺序

### 阶段一：后端解析器

目标：

- 实现模板路径解析能力
- 建立可靠单元测试

输出：

- `PromptTemplateResolver`
- `PromptValueFormatter`
- 单元测试

### 阶段二：LLM 接入

目标：

- `LlmNodeExecutorStrategy` 接入新解析器
- 保留旧逻辑回退

输出：

- `buildUserPrompt()` 改造
- LLM 相关测试

### 阶段三：前端输入区重构

目标：

- LLM 输入区默认改为单大文本框
- 变量插入器支持多跳祖先节点路径

输出：

- 前端组件重构
- 交互测试

### 阶段四：兼容与联调

目标：

- 校验老图不破
- 校验新图路径模板可跑通

输出：

- 回归验证结果
- 必要的归一化逻辑

## 6. 风险与应对

### 风险 1：数组 / 对象渲染效果不佳

说明：

- 知识库结果通常是数组
- 直接 JSON 化可能影响模型理解

应对：

- 第一版先统一 JSON 序列化
- 第二版再针对常见类型做更自然格式化

### 风险 2：路径引用写错难排查

说明：

- 用户写错 nodeId 或 fieldKey 时，可能不易定位

应对：

- 保留原始占位符
- 输出警告日志
- 前端优先通过点击插入路径，减少手写

### 风险 3：历史图与新交互并存导致状态复杂

说明：

- 同时存在 `inputSchema` 和 `userPromptTemplate`

应对：

- 明确优先级：
  - `userPromptTemplate` 优先
  - 为空时回退旧逻辑

## 7. 测试计划

### 7.1 后端测试

- `{{key}}` 替换
- `{{inputs.query}}` 替换
- `{{nodeId.output.key}}` 替换
- 路径不存在
- null 值处理
- 数组 / 对象序列化

### 7.2 前端测试

- LLM 节点显示 Prompt 文本框
- 点击变量插入路径
- 保存后持久化 `userPromptTemplate`

### 7.3 联调测试

- 使用真实工作流链路验证多跳路径解析

## 8. 完成定义

满足以下条件视为完成：

1. 后端支持模板路径解析并测试通过
2. 前端支持单文本框 Prompt 编辑与变量插入
3. 老工作流不受影响
4. 新工作流可直接使用 `{{nodeId.output.key}}`
5. 文档、测试、联调结果齐备
