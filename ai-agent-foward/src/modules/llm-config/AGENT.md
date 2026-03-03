# 模块上下文：llm-config

## 模块定位
本文件位于前端业务模块，用于明确页面行为、交互边界与后端契约。

## 渐进式加载顺序（先全局，后模块）
1. docs/PROJECT_QUICK_CONTEXT.md
2. docs/api/backend-api-overview.md

## 跨层联动目录
- ai-agent-foward/src/modules/llm-config
- ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/llm
- ai-agent-domain/src/main/java/com/zj/aiagent/domain/llm

## 常见任务入口
- 模型配置增删改查
- 连接测试与默认配置

## 使用约束
1. 先在本模块内定位，再跨模块扩展，避免全仓扫描。
2. 涉及审核链路时，优先联合 workflow + review 上下文。
3. 修改前先确认本模块对外输入输出契约不被破坏。
