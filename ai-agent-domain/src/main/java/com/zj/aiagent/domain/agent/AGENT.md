# 模块上下文：agent

## 模块定位
本文件位于领域层，用于约束业务语义、状态流转和核心规则。

## 渐进式加载顺序（先全局，后模块）
1. docs/PROJECT_QUICK_CONTEXT.md
2. docs/api/agent.md
3. docs/api/backend-api-overview.md

## 跨层联动目录
- ai-agent-application/src/main/java/com/zj/aiagent/application/agent
- ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent
- ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/agent
- ai-agent-foward/src/modules/agent

## 常见任务入口
- 智能体创建/更新/发布/回滚
- graphJson 结构与版本语义对齐

## 使用约束
1. 先在本模块内定位，再跨模块扩展，避免全仓扫描。
2. 涉及审核链路时，优先联合 workflow + review 上下文。
3. 修改前先确认本模块对外输入输出契约不被破坏。
