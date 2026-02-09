# Requirements Document

## Introduction

工作流执行过程中，节点间的占位符引用（如 `{{nodeName.key}}`）没有被正确解析。根因是 `WorkflowGraphFactoryImpl.extractInputs()` 生成 `${sourceRef}` 格式的占位符，而 `ExecutionContext.resolve()` 只识别 `#{...}` 格式的 SpEL 表达式，导致占位符被当作普通字符串返回，永远不会被解析。此外，用户在 prompt 模板中使用 `{{nodeName.key}}` 格式引用前驱节点值的场景也未被支持。

## Glossary

- **Placeholder_Resolver**: ExecutionContext 中负责解析占位符表达式的逻辑模块
- **WorkflowGraphFactory**: 基础设施层组件，负责将 graphJson 字符串解析为 WorkflowGraph 领域对象
- **ExecutionContext**: 领域层值对象，作为工作流执行的"智能黑板"，承载全部上下文信息并提供 SpEL 表达式解析能力
- **SchedulerService**: 应用层服务，负责工作流调度编排，包括节点输入解析和执行协调
- **NodeExecutorStrategy**: 节点执行策略接口，各节点类型（LLM、HTTP 等）的具体执行器实现
- **sourceRef**: 前端 FieldSchemaDTO 中的字段，表示数据来源引用路径，如 `node-1.output.result`
- **SpEL**: Spring Expression Language，Spring 框架的表达式语言
- **Prompt_Template**: LLM 节点和 HTTP 节点配置中的模板字符串，支持占位符替换

## Requirements

### Requirement 1: 统一占位符格式

**User Story:** 作为工作流开发者，我希望 sourceRef 引用在运行时被正确解析为实际值，以便节点间能正确传递数据。

#### Acceptance Criteria

1. WHEN WorkflowGraphFactory 将 sourceRef 转换为占位符表达式, THE WorkflowGraphFactory SHALL 生成与 ExecutionContext 解析器兼容的 `#{...}` 格式
2. WHEN ExecutionContext 接收到 `#{...}` 格式的表达式, THE Placeholder_Resolver SHALL 通过 SpEL 解析该表达式并返回对应的实际值
3. WHEN sourceRef 路径引用一个已完成节点的输出值, THE Placeholder_Resolver SHALL 从 nodeOutputs 中正确提取该值

### Requirement 2: sourceRef 路径到 SpEL 表达式的映射

**User Story:** 作为工作流开发者，我希望前端配置的 sourceRef 路径（如 `node-1.output.result`）能被正确映射为 SpEL 可解析的变量路径，以便表达式引擎能定位到正确的数据。

#### Acceptance Criteria

1. WHEN sourceRef 格式为 `<nodeId>.output.<key>`, THE WorkflowGraphFactory SHALL 将其映射为 SpEL 表达式 `#{#<nodeId>['<key>']}`，其中 nodeId 对应 ExecutionContext.nodeOutputs 中注册的变量名
2. WHEN sourceRef 格式为 `state.<key>`, THE WorkflowGraphFactory SHALL 将其映射为 SpEL 表达式 `#{#sharedState['<key>']}`
3. WHEN sourceRef 格式为 `<nodeId>.<key>`（不含 output 中间段）, THE WorkflowGraphFactory SHALL 将其映射为 SpEL 表达式 `#{#<nodeId>['<key>']}`
4. THE WorkflowGraphFactory SHALL 为每种 sourceRef 格式生成语法正确的 SpEL 表达式
5. THE WorkflowGraphFactory SHALL 提供一个 Pretty Printer，将 SpEL 表达式还原为 sourceRef 路径格式
6. FOR ALL 合法的 sourceRef 路径，将其转换为 SpEL 表达式再还原回 sourceRef 路径 SHALL 产生等价的原始路径（round-trip 属性）

### Requirement 3: Prompt 模板中的 Mustache 风格占位符解析

**User Story:** 作为工作流设计者，我希望在 LLM 节点的 userPromptTemplate 中使用 `{{nodeName.key}}` 格式引用前驱节点的值，以便灵活构建 prompt。

#### Acceptance Criteria

1. WHEN Prompt_Template 包含 `{{<key>}}` 格式的占位符, THE NodeExecutorStrategy SHALL 使用 resolvedInputs 中对应 key 的值替换该占位符
2. WHEN Prompt_Template 同时包含 `#{key}` 和 `{{key}}` 两种格式的占位符, THE NodeExecutorStrategy SHALL 同时支持两种格式的替换
3. WHEN Prompt_Template 中的占位符 key 在 resolvedInputs 中不存在, THE NodeExecutorStrategy SHALL 保留原始占位符文本不做替换

### Requirement 4: 异常处理与容错

**User Story:** 作为系统运维人员，我希望占位符解析失败时系统能优雅处理并提供有用的错误信息，以便快速定位问题。

#### Acceptance Criteria

1. IF SpEL 表达式解析抛出异常（如引用的节点输出不存在）, THEN THE Placeholder_Resolver SHALL 记录包含表达式内容和错误原因的 WARN 级别日志，并返回原始表达式字符串
2. IF sourceRef 格式不符合任何已知模式, THEN THE WorkflowGraphFactory SHALL 记录 WARN 级别日志并将 sourceRef 原值作为普通字符串传递
3. IF resolvedInputs 中的值为 null, THEN THE NodeExecutorStrategy SHALL 在模板替换时跳过该占位符，保留原始占位符文本
