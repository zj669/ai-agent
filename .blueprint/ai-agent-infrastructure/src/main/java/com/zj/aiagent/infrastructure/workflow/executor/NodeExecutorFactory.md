# NodeExecutorFactory.java 蓝图

## Metadata
- title: NodeExecutorFactory
- type: service
- summary: 节点执行器工厂，Registry 模式管理所有 NodeExecutorStrategy 实现，按 NodeType 分发执行

## 关键方法单元

### constructor
- location: NodeExecutorFactory(List<NodeExecutorStrategy>)
- purpose: 构造器注入所有策略实现，自动注册到 strategyRegistry（Map<NodeType, NodeExecutorStrategy>）
- input: Spring 容器自动收集的所有 NodeExecutorStrategy Bean
- output: 初始化 strategyRegistry

### getStrategy
- location: NodeExecutorFactory.getStrategy
- purpose: 按节点类型获取执行策略，找不到时抛 IllegalArgumentException
- input: NodeType
- output: NodeExecutorStrategy

## 变更记录
- 2026-03-16: 初始蓝图生成
