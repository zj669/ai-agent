# 9 项 Bug 批量修复

- **日期**: 2026-03-06
- **模块**: 全局 / 前端 + 后端
- **严重程度**: 功能缺陷

## 修复内容

### Bug 1: 用户名显示硬编码"管理员"
- `authAdapter.ts`: 登录后保存 `session.user` 到 `localStorage('userInfo')`，新增 `getSavedUserInfo()` / `clearSavedUserInfo()`
- `AppShell.tsx`: 用 `useMemo` 读取真实用户名替换硬编码
- `DashboardPage.tsx`: 欢迎语改为动态

### Bug 2: 退出登录闪烁后自动重登录
- `AppShell.tsx`: 退出时先调用 `clearAccessToken()` + `clearSavedUserInfo()`

### Bug 3: "记住我"未生效
- `LoginPage.tsx`: `rememberMe` 初始值从 `false` 改为 `!!rememberedEmail`

### Bug 4: 模型配置操作按钮溢出
- `LlmConfigPage.tsx`: 操作列改为"编辑"按钮 + Dropdown 更多菜单；Table 增加 `scroll={{ x: 'max-content' }}`

### Bug 5: 设置页不存在
- 新建 `SettingsPage.tsx`（个人信息 + 安全设置）
- `router.tsx` 添加 `/settings` 路由
- `AppShell.tsx` 下拉菜单处理 `settings` 点击

### Bug 6: Swarm 协作页无返回按钮
- `SwarmSidebar.tsx`: 侧边栏顶部新增"返回工作区列表"按钮

### Bug 7: 暂停点切换会话后显示"..."
- `SchedulerService.java`: `checkPause` 暂停时调用 `finalizeMessage` 保存已完成节点摘要

### Bug 8: 执行后暂停未显示当前节点输出
- 确认后端链路正确
- `ChatPage.tsx`: 修复 `useEffect` 依赖项

### Bug 9: 人工审核仅当前节点可编辑（二次修复）
- 上游节点输入部分也改为 `Input.TextArea` 可编辑
- `upstreamEdits` 初始化时同时包含 inputs 和 outputs
- 后端 `ResumeExecutionRequest` 增加 `nodeEdits` 字段
- `SchedulerService.resumeExecution` 支持多节点 edits

### 附加: Embedding 404 错误处理增强
- `SchedulerService.hydrateMemory`: 增强 catch 日志，提示检查 embedding 模型配置
- 这是配置问题（embedding API baseUrl 或模型名称不正确），不是代码 bug

## 修改文件

| 文件 | 改动 |
|------|------|
| `authAdapter.ts` | 保存/读取/清除 userInfo |
| `AppShell.tsx` | 动态用户名 + 退出清 token + 设置菜单 |
| `LoginPage.tsx` | rememberMe 初始值 |
| `DashboardPage.tsx` | 动态欢迎语 |
| `LlmConfigPage.tsx` | 操作列 Dropdown + scroll |
| `router.tsx` | /settings 路由 |
| `SettingsPage.tsx` | 新建 |
| `SwarmSidebar.tsx` | 返回按钮 |
| `ChatPage.tsx` | 上游节点可编辑 + useEffect 依赖修复 |
| `chatAdapter.ts` | nodeEdits 字段 |
| `SchedulerService.java` | checkPause 保存消息 + resumeExecution 多节点 + embedding 日志 |
| `HumanReviewDTO.java` | nodeEdits 字段 |
| `SchedulerServiceTest.java` | 更新 resumeExecution 调用签名 |
