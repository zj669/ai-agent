## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/NodeExecutorFactory.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/NodeExecutorFactory.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: NodeExecutorFactory
- 基于 Registry 模式注册全部 `NodeExecutorStrategy`，提供按 `NodeType` 路由策略的统一入口。
- 隔离调度层与具体执行器实现，降低新增节点类型时的耦合成本。

## 2) 核心方法
- `NodeExecutorFactory(List<NodeExecutorStrategy> strategies)`
- `getStrategy(NodeType nodeType)`
- `supports(NodeType nodeType)`
- `getRegisteredTypes()`

## 3) 具体方法
### 3.1 NodeExecutorFactory(...)
- 函数签名: `public NodeExecutorFactory(List<NodeExecutorStrategy> strategies)`
- 入参: Spring 注入的策略实现集合
- 出参: 无
- 功能含义: 遍历策略并写入 `strategyRegistry`。
- 链路作用: 应用启动时完成执行器装配。

### 3.2 getStrategy(NodeType nodeType)
- 函数签名: `public NodeExecutorStrategy getStrategy(NodeType nodeType)`
- 入参: 节点类型
- 出参: 对应执行策略
- 功能含义: 查询注册表并在缺失时抛出异常。
- 链路作用: 调度器执行节点前的关键查找步骤。

### 3.3 supports(NodeType nodeType)
- 函数签名: `public boolean supports(NodeType nodeType)`
- 入参: 节点类型
- 出参: 是否已注册
- 功能含义: 能力探测。
- 链路作用: 为运行时校验或调试接口提供支持。

## 4) 变更记录
- 2026-02-15: 回填工厂蓝图语义，补齐注册与路由职责。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
