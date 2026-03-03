# 模块上下文：workflow

## 模块定位
本文件位于领域层，用于约束业务语义、状态流转和核心规则。

## 渐进式加载顺序（先全局，后模块）
1. docs/PROJECT_QUICK_CONTEXT.md
2. docs/api/workflow.md
3. docs/api/review.md
4. .blueprint/domain/workflow/WorkflowEngine.md

## 跨层联动目录
- ai-agent-application/src/main/java/com/zj/aiagent/application/workflow
- ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow
- ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow
- ai-agent-foward/src/modules/workflow
- ai-agent-foward/src/modules/review

## 常见任务入口
- 执行启动/推进/暂停/恢复
- 条件分支与节点状态流转
- 人工审核节点联动

## 使用约束
1. 先在本模块内定位，再跨模块扩展，避免全仓扫描。
2. 涉及审核链路时，优先联合 workflow + review 上下文。
3. 修改前先确认本模块对外输入输出契约不被破坏。
