## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionContextDTO.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ExecutionContextDTO.java
- 工作流执行上下文调试 DTO，封装 LTM、STM、执行日志和全局变量快照。

## 2) 核心方法
- 无显式方法（含 `ChatMessage` 内部结构）

## 3) 具体方法
### 3.1 结构契约
- 函数签名: `N/A`
- 入参: 无
- 出参: `executionId/longTermMemories/chatHistory/executionLog/globalVariables`
- 功能含义: 为 `/context` 调试接口提供稳定响应结构。
- 链路作用: `WorkflowController.getExecutionContext` -> DTO 序列化 -> 调试端可视化。

## 4) 变更记录
- 2026-02-15: 基于源码回填执行上下文 DTO 结构语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
