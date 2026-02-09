# Implementation Plan: Fix Placeholder Resolution

## Overview

修复工作流节点间占位符引用无法被正确解析的 bug。采用最小改动原则，修改三层代码：Infrastructure 层的 sourceRef→SpEL 映射、Domain 层的异常处理、以及节点执行器的模板替换。同步更新 `.blueprint/` 蓝图文件。

## Tasks

- [x] 1. 实现 sourceRef 到 SpEL 的映射方法
  - [x] 1.1 在 `WorkflowGraphFactoryImpl` 中新增 `mapSourceRefToSpEL(String sourceRef)` 静态方法
    - 实现三种 sourceRef 格式的映射规则：`nodeId.output.key` → `#{#nodeId['key']}`、`state.key` → `#{#sharedState['key']}`、`nodeId.key` → `#{#nodeId['key']}`
    - 格式不匹配时记录 WARN 日志并返回原始 sourceRef
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.2_
  - [x] 1.2 在 `WorkflowGraphFactoryImpl` 中新增 `mapSpELToSourceRef(String spel)` 静态方法
    - 实现 SpEL 表达式到 sourceRef 路径的逆向映射（Pretty Printer）
    - _Requirements: 2.5_
  - [x] 1.3 修改 `WorkflowGraphFactoryImpl.extractInputs()` 方法
    - 将 `"${" + field.getSourceRef() + "}"` 替换为 `mapSourceRefToSpEL(field.getSourceRef())`
    - _Requirements: 1.1_
  - [ ]* 1.4 编写 `mapSourceRefToSpEL` 和 `mapSpELToSourceRef` 的 unit tests
    - 测试三种 sourceRef 格式的映射、无效格式的容错、null/空字符串处理
    - _Requirements: 2.1, 2.2, 2.3, 4.2_
  - [ ]* 1.5 编写 sourceRef 映射正确性的 property test
    - **Property 1: sourceRef 映射正确性**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
  - [ ]* 1.6 编写 sourceRef round-trip 的 property test
    - **Property 2: sourceRef 到 SpEL 的 Round-Trip**
    - **Validates: Requirements 2.5, 2.6**

- [x] 2. 增强 ExecutionContext 异常处理
  - [x] 2.1 修改 `ExecutionContext.resolve()` 方法
    - 在 SpEL 解析逻辑外层添加 try-catch，捕获异常后记录 WARN 日志并返回原始表达式
    - 添加 `@Slf4j` 注解（如尚未存在）
    - _Requirements: 4.1_
  - [ ]* 2.2 编写 ExecutionContext 异常处理的 unit tests
    - 测试引用不存在节点、语法错误表达式、null 表达式等场景
    - _Requirements: 4.1_
  - [ ]* 2.3 编写 SpEL 解析异常返回原始表达式的 property test
    - **Property 6: SpEL 解析异常返回原始表达式**
    - **Validates: Requirements 4.1**

- [x] 3. Checkpoint - 确保核心解析逻辑正确
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 端到端解析验证
  - [ ]* 4.1 编写端到端解析正确性的 property test
    - 构建 ExecutionContext，存入随机 nodeOutputs，生成对应 sourceRef，映射为 SpEL，调用 resolve()，验证返回值等于原始存入值
    - **Property 3: 端到端解析正确性**
    - **Validates: Requirements 1.1, 1.2, 1.3**

- [x] 5. 增强节点执行器模板替换
  - [x] 5.1 修改 `LlmNodeExecutorStrategy.buildUserPrompt()` 方法
    - 增加 `{{key}}` Mustache 风格占位符替换支持
    - 跳过 `__` 前缀的内部变量
    - null 值跳过替换，保留原始占位符
    - _Requirements: 3.1, 3.2, 3.3, 4.3_
  - [x] 5.2 修改 `HttpNodeExecutorStrategy.resolveTemplate()` 方法
    - 同 5.1，增加 `{{key}}` 支持、跳过内部变量、null 值处理
    - _Requirements: 3.1, 3.2, 3.3, 4.3_
  - [ ]* 5.3 编写模板占位符替换的 unit tests
    - 测试 `#{key}` 和 `{{key}}` 混合替换、缺失 key 保留、null 值跳过、内部变量跳过
    - _Requirements: 3.1, 3.2, 3.3, 4.3_
  - [ ]* 5.4 编写模板占位符替换的 property test
    - **Property 4: 模板占位符替换**
    - **Validates: Requirements 3.1, 3.2**
  - [ ]* 5.5 编写缺失 key 保留原始占位符的 property test
    - **Property 5: 缺失 key 保留原始占位符**
    - **Validates: Requirements 3.3**

- [x] 6. 更新蓝图文件
  - [x] 6.1 更新 `.blueprint/domain/workflow/ExecutionContext.md`
    - 在 SpEL 表达式支持部分补充异常处理行为说明
    - 在变更日志中记录本次修改
    - _Requirements: 4.1_
  - [x] 6.2 更新 `.blueprint/infrastructure/adapters/NodeExecutors.md`（如存在模板替换相关描述）
    - 补充 `{{key}}` Mustache 风格占位符支持说明
    - _Requirements: 3.1, 3.2_

- [x] 7. Final checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 修改遵循复用优先原则，不新建类，仅在现有类中添加/修改方法
- Domain 层（ExecutionContext）不引入新的框架依赖，仅增加 try-catch 和日志
- Property tests 使用 jqwik 库，每个 test 至少 100 次迭代
- 蓝图更新在代码修改完成后进行，确保蓝图与代码同步
