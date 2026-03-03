# Codex 项目初始化入口（渐进式加载）

## 目标
让 AI 在进入仓库后，先建立全局认知，再按任务深入，避免一次性全量扫描。

## 启动后第一步（必须）
优先阅读：
1. `docs/PROJECT_QUICK_CONTEXT.md`

## 分层加载顺序
### L0：全局认知
1. `README.md`
2. `docs/PROJECT_QUICK_CONTEXT.md`

### L1：业务闭环与接口边界
1. `docs/api/backend-api-overview.md`
2. `docs/api/workflow.md`
3. `docs/api/review.md`
4. `docs/api/agent.md`

### L2：实现与验证（按需）
1. `docs/E2E-TEST-PLAN.md`
2. `.blueprint/domain/workflow/WorkflowEngine.md`
3. `.blueprint/_overview.md`
4. `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`

## 任务驱动加载规则
1. 如果任务是“产品价值/项目介绍”，停留在 L0-L1。
2. 如果任务是“接口联调/业务流程修复”，读取 L1 + 对应模块源码。
3. 如果任务是“执行链路/状态问题/审核问题”，优先读取 workflow + review + WorkflowEngine。
4. 每新增一层读取前，先输出当前结论摘要，再继续加载下一层。

