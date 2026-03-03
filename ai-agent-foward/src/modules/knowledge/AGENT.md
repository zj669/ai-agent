# 模块上下文：knowledge

## 模块定位
本文件位于前端业务模块，用于明确页面行为、交互边界与后端契约。

## 渐进式加载顺序（先全局，后模块）
1. docs/PROJECT_QUICK_CONTEXT.md
2. docs/api/knowledge.md

## 跨层联动目录
- ai-agent-foward/src/modules/knowledge
- ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge
- ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge

## 常见任务入口
- 知识库/文档管理 UI
- 上传与处理状态展示

## 使用约束
1. 先在本模块内定位，再跨模块扩展，避免全仓扫描。
2. 涉及审核链路时，优先联合 workflow + review 上下文。
3. 修改前先确认本模块对外输入输出契约不被破坏。
