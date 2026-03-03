# Codex 项目初始化入口（按业务模块分发）

## 启动原则
1. 先读全局：`docs/PROJECT_QUICK_CONTEXT.md`
2. 再按任务选择业务模块，只加载对应模块的 `AGENT.md`
3. 需要跨模块时，再增量加载下一个模块，避免全仓扫描

## 全局入口
1. `README.md`
2. `docs/PROJECT_QUICK_CONTEXT.md`

## 业务模块索引（先选模块，再读模块 AGENT）
1. Agent
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/AGENT.md`
- 前端：`ai-agent-foward/src/modules/agent/AGENT.md`

2. Workflow
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md`
- 前端：`ai-agent-foward/src/modules/workflow/AGENT.md`

3. Chat
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/AGENT.md`
- 前端：`ai-agent-foward/src/modules/chat/AGENT.md`

4. Knowledge
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/AGENT.md`
- 前端：`ai-agent-foward/src/modules/knowledge/AGENT.md`

5. User/Auth
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/AGENT.md`
- 后端（鉴权）：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/auth/AGENT.md`
- 前端：`ai-agent-foward/src/modules/auth/AGENT.md`

6. Review（审核）
- 后端归属：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md`
- 前端：`ai-agent-foward/src/modules/review/AGENT.md`

7. Dashboard
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/dashboard/AGENT.md`
- 前端：`ai-agent-foward/src/modules/dashboard/AGENT.md`

8. LLM Config
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/llm/AGENT.md`
- 前端：`ai-agent-foward/src/modules/llm-config/AGENT.md`

9. Swarm
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/swarm/AGENT.md`
- 前端：`ai-agent-foward/src/modules/swarm/AGENT.md`

## 执行规则
1. 任何任务先输出“选中的模块 + 已加载的模块 AGENT 列表”。
2. 未命中模块时，先回到全局入口，不直接遍历源码。
3. 涉及流程暂停/恢复/审批，默认联动 Workflow + Review 两个模块。
