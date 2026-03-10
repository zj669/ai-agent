# Bug 修复记录

> 此目录用于记录项目开发过程中遇到的问题及其修复方案。
> 每次修复单独建立一个文件，命名格式：`YYYY-MM-DD-<模块>-<简述>.md`

## 记录列表

| 日期 | 模块 | 问题简述 | 文件 |
|------|------|----------|------|
| 2026-03-05 | Swarm / 编译 | 主工作空间编译失败：主类缺失 + 新工具方法签名不匹配 | [2026-03-05-build-compile-errors.md](2026-03-05-build-compile-errors.md) |
| 2026-03-06 | Swarm / 前端 | 消息发送后不即时显示 + Agent 无思考中状态 | [2026-03-06-swarm-chat-ux-bugs.md](2026-03-06-swarm-chat-ux-bugs.md) |
| 2026-03-06 | Swarm / 前端 | Agent 思考/流式输出时无终止按钮 | [2026-03-06-swarm-stop-button.md](2026-03-06-swarm-stop-button.md) |
| 2026-03-06 | 前端 / 测试 | Vitest 大面积失败：matchMedia / ResizeObserver / xyflow mock / UI 漂移 | [2026-03-06-frontend-test-failures.md](2026-03-06-frontend-test-failures.md) |
| 2026-03-06 | 前端 / 测试 | UI 测试漂移全量修复：12 文件 52→0 失败，重写/更新全部测试 | [2026-03-06-ui-test-drift-fix.md](2026-03-06-ui-test-drift-fix.md) |
| 2026-03-06 | Swarm / 前端 | 发送消息后页面不显示消息和 thinking 气泡（空状态条件 falsy bug） | [2026-03-06-swarm-empty-state-falsy.md](2026-03-06-swarm-empty-state-falsy.md) |
| 2026-03-06 | Swarm / 后端+前端 | 终止无效 + Agent 状态不刷新 + 工具调用不展示 | [2026-03-06-swarm-stop-status-toolcall.md](2026-03-06-swarm-stop-status-toolcall.md) |
| 2026-03-06 | Swarm / 后端 | 多 Agent 并行派发：send 后 break 导致只有一个 Agent 收到消息 | [2026-03-06-swarm-parallel-send.md](2026-03-06-swarm-parallel-send.md) |
| 2026-03-06 | 全局 | 9 项 Bug 批量修复：用户名/退出/记住我/溢出/设置页/返回/暂停点/多节点编辑 | [2026-03-06-nine-bugs-batch-fix.md](2026-03-06-nine-bugs-batch-fix.md) |
