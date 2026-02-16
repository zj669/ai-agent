## Metadata
- file: `.blueprint/files/ai-agent-foward/src/types/auth.ts.md`
- version: `1.0`
- status: 修改中
- updated_at: 2026-02-16
- owner: blueprint-engineer

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: auth.ts
- 该文件定义认证域前端类型契约（登录、注册、刷新、用户信息等）。

## 2) 核心类型
- `LoginRequest`
- `LoginResponse`
- `TokenRefreshResponse`

## 3) 变更记录（记住我）
### 3.1 目标
- 在认证类型层补充 remember-me 相关字段，确保页面、服务、存储层使用统一契约。

### 3.2 影响
- 影响 `LoginRequest` 参数结构与调用方类型推断；
- 影响 remember-me 派生配置（如持久化模式）在前端内部传递。

### 3.3 验收
- 类型定义可表达 remember-me 状态；
- LoginPage 与 authService 侧编译期类型一致；
- 不引入与现有接口字段冲突的破坏性变更。

## 4) 状态流转记录
- 2026-02-16: `正常 -> 待修改`（类型契约纳入 remember-me 变更范围）
- 2026-02-16: `待修改 -> 修改中`（开始定义 remember-me 类型扩展）

## 5) Temp缓存区
- 当前状态为 `修改中`，待联调验证后转入 `修改完成`。
