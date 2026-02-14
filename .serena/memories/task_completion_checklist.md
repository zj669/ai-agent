# 任务完成检查清单

## 后端改动后
- 至少执行：`mvn test`（或受影响模块测试）。
- 若改动接口/架构：同步更新 `.blueprint/` 对应文件。
- 检查分层依赖是否越界（特别是 application 直接依赖 infrastructure）。

## 前端改动后
- 至少执行：`cd ai-agent-foward && npm run build`。
- 手工验证受影响页面流程（路由、鉴权、接口调用、异常态）。

## 提交前
- 变更聚焦单一主题。
- 提交信息建议 Conventional Commits（feat/fix/refactor/chore/test）。
- 如有跨模块改动，记录影响面与验证证据。