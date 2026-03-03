# 模块上下文：chat

## 模块定位
本文件位于领域层，用于约束业务语义、状态流转和核心规则。

## 渐进式加载顺序（先全局，后模块）
1. docs/PROJECT_QUICK_CONTEXT.md
2. docs/api/chat.md
3. docs/api/workflow.md

## 跨层联动目录
- ai-agent-application/src/main/java/com/zj/aiagent/application/chat
- ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat
- ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat
- ai-agent-foward/src/modules/chat

## 常见任务入口
- 会话创建与消息发送
- 对话触发工作流执行
- 流式输出展示问题排查

## 使用约束
1. 先在本模块内定位，再跨模块扩展，避免全仓扫描。
2. 涉及审核链路时，优先联合 workflow + review 上下文。
3. 修改前先确认本模块对外输入输出契约不被破坏。
