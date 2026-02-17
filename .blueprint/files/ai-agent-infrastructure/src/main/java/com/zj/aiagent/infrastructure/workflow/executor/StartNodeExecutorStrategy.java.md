## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/StartNodeExecutorStrategy.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/StartNodeExecutorStrategy.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: StartNodeExecutorStrategy
- 负责 START 节点执行：透传上游输入到节点输出，并剔除内部运行时字段（`__context__`）。
- 作为工作流执行链的起点策略，为后续节点提供标准化输入载荷。

## 2) 核心方法
- `executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- `getSupportedType()`

## 3) 具体方法
### 3.1 executeAsync(...)
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(...)`
- 入参: 节点定义、已解析输入、流式发布器
- 出参: `NodeExecutionResult.success(outputs)`
- 功能含义: 复制输入并移除 `__context__`，返回已完成 Future。
- 链路作用: 统一 START 节点输出结构，供调度器推进到下一跳。

### 3.2 getSupportedType()
- 函数签名: `NodeType getSupportedType()`
- 入参: 无
- 出参: `NodeType.START`
- 功能含义: 声明策略适配类型。
- 链路作用: 供 `NodeExecutorFactory` 按类型路由执行器。

## 4) 变更记录
- 2026-02-15: 回填执行器蓝图语义，补齐职责与方法链路说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
