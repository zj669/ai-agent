# LLM 模板路径引用需求文档

- **日期**: 2026-03-17
- **模块**: 工作流 / LLM 节点 / 前后端联动
- **需求类型**: 能力增强
- **优先级**: 高

## 1. 背景

当前工作流编辑器中的 LLM 节点，主要通过 `inputSchema` 逐项配置输入字段，例如：

- `input`
- `knowledge_list`

这种模式虽然可以完成字段映射，但用户体验存在明显问题：

- LLM 节点输入区域会出现多个输入字段，不符合用户对“写 Prompt”的直觉。
- 用户无法直接在一段完整文本中组织上下文、知识库结果、工具结果和用户问题。
- 多跳上游节点引用虽然在工作流引擎上下文中是存在的，但前端没有提供统一的模板化表达方式。

用户期望的交互模型是：

- LLM 节点提供一个大文本框作为 Prompt 编辑区域。
- 用户可以直接输入一整段 Prompt 文本。
- Prompt 文本中允许使用 `{{}}` 语法引用多跳上游节点输出。

示例：

```text
请基于以下信息回答用户问题：

知识库结果：
{{knowledge-1.output.knowledge_list}}

工具执行结果：
{{tool-1.output.result}}

用户问题：
{{start.output.query}}
```

## 2. 目标

本需求的目标是让 LLM 节点支持“模板内直接解析路径引用”，而不是只依赖 `inputSchema` 预先铺平变量。

目标包括：

1. 在 LLM 节点中提供单一大文本框作为 Prompt 主编辑入口。
2. 支持在模板内通过 `{{...}}` 直接引用：
   - 简单变量
   - 全局输入
   - 任意可达上游节点输出
3. 保持与现有工作流图、执行上下文、历史配置兼容。
4. 保留旧图运行能力，不破坏已有依赖 `inputSchema` 的工作流。

## 3. 范围

### 3.1 本次范围内

- LLM 节点 `userPromptTemplate` 能力增强
- 模板内路径解析器
- 前端 LLM 节点输入区改造成单文本框
- 变量插入器支持多跳祖先节点输出
- 历史图兼容和保存归一化
- 配套测试与文档

### 3.2 本次范围外

- 模板语法中的函数调用
- 模板语法中的过滤器，如 `| json`
- 任意表达式执行
- 数组下标访问，如 `foo[0]`
- 对所有节点类型统一开放该能力
- 富文本 Prompt 编辑器

## 4. 当前现状

### 4.1 后端现状

- `WorkflowGraphFactoryImpl` 已支持将 `sourceRef` 路径映射为 SpEL，用于节点输入映射。
- `LlmNodeExecutorStrategy.buildUserPrompt()` 当前仅支持：
  - `{{key}}`
  - `#{key}`
- 其替换来源是 `resolvedInputs` 的平铺键值。
- 当前不支持在模板中直接解析 `{{nodeId.output.key}}` 这类路径表达式。

### 4.2 前端现状

- LLM 节点输入区仍以 `inputSchema` 字段卡片形式编辑。
- 用户看到多个输入框，不利于组织完整 Prompt。
- 前端已经支持 `contextRefNodes` 作为系统提示词参考节点。
- 当前没有将“用户提示词模板”作为主交互入口。

## 5. 需求定义

### 5.1 模板语法

第一版支持以下三类占位符：

1. 简单变量

```text
{{input}}
{{knowledge_list}}
```

2. 全局输入

```text
{{inputs.query}}
{{inputs.message}}
```

3. 节点输出路径

```text
{{start.output.query}}
{{knowledge-1.output.knowledge_list}}
{{tool-1.output.result}}
```

### 5.2 取值来源

- 简单变量：来自 `resolvedInputs`
- `inputs.xxx`：来自 `ExecutionContext.inputs`
- `nodeId.output.key`：来自 `ExecutionContext.nodeOutputs[nodeId][key]`

### 5.3 路径规则

本期统一采用显式路径：

```text
{{<nodeId>.output.<fieldKey>}}
```

不支持省略 `output`。

### 5.4 失败策略

当占位符无法解析时：

- 默认保留原始占位符文本
- 记录 `warn` 日志
- 不直接让节点失败

后续可扩展严格模式，但不在本期内。

### 5.5 值格式化规则

- `String / Number / Boolean`：直接转字符串
- `List / Map / Object`：转 JSON 字符串
- `null`：
  - 路径存在但值为 `null` 时，替换为空串
  - 路径不存在时，保留原始占位符

## 6. 交互要求

### 6.1 LLM 输入区

LLM 节点“输入”页默认改为：

- 一个 Prompt 文本框
- 文本框支持多行长文本
- 文本框内容绑定 `userConfig.userPromptTemplate`

### 6.2 变量插入器

需要提供一个“插入变量”交互面板，变量来源包括：

- 全局输入
- 当前节点所有可达祖先节点的输出字段

插入结果应为标准模板语法，例如：

- `{{inputs.query}}`
- `{{knowledge-1.output.knowledge_list}}`

### 6.3 高级模式

为兼容旧图和调试需求，保留 `inputSchema` 的高级编辑入口，但不作为默认主交互。

## 7. 兼容性要求

### 7.1 老工作流兼容

- 若 `userPromptTemplate` 为空，继续沿用现有逻辑：
  - 使用 `resolvedInputs.input`
- 老图不要求立即迁移。

### 7.2 新工作流默认行为

新建 LLM 节点时，应初始化一个默认模板，例如：

```text
{{inputs.query}}
```

或者：

```text
用户问题：
{{inputs.query}}
```

### 7.3 与 `contextRefNodes` 并存

- `userPromptTemplate`：负责用户 Prompt 组装
- `contextRefNodes`：负责系统提示词参考资料注入

两者并存，不互相替代。

## 8. 非功能要求

- 必须保持对历史数据兼容
- 模板解析逻辑应可单元测试
- 前端交互应避免重复配置入口
- 多跳祖先节点计算需性能可控
- 日志应能定位模板解析失败原因

## 9. 验收标准

### 9.1 后端验收

- 支持 `{{key}}`
- 支持 `{{inputs.key}}`
- 支持 `{{nodeId.output.key}}`
- 路径不存在时不抛异常，记录警告并保留占位符

### 9.2 前端验收

- LLM 节点默认显示单一 Prompt 文本框
- 可以从变量面板插入多跳祖先节点引用
- 保存后 `graph_json.userConfig.userPromptTemplate` 正确持久化

### 9.3 联调验收

构建如下链路：

- `START -> KNOWLEDGE -> TOOL -> LLM -> END`

在 LLM 模板中引用：

- `{{inputs.query}}`
- `{{knowledge-node.output.knowledge_list}}`
- `{{tool-node.output.result}}`

执行后模型实际收到的 Prompt 中，三个值均应被正确替换。
