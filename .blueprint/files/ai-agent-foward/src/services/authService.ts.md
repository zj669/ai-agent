## Metadata
- file: `.blueprint/files/ai-agent-foward/src/services/authService.ts.md`
- version: `1.0`
- status: 修改中
- updated_at: 2026-02-16
- owner: blueprint-engineer

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: authService.ts
- 该文件负责认证相关 API 调用与登录态副作用管理（token、refresh token、过期时间、用户信息）。

## 2) 核心方法
- `login(data)`
- `logout(token, deviceId)`
- `refreshToken(data)`

## 3) 变更记录（记住我）
### 3.1 目标
- 将 remember-me 语义接入登录链路，决定凭证持久化策略（会话级或持久级）。

### 3.2 影响
- 影响登录成功后的本地存储写入策略；
- 影响 refresh token 与 deviceId 的可恢复性；
- 与 `credentialStorageService` 职责边界建立明确分层（authService 编排，storage service 落盘）。

### 3.3 验收
- authService 能根据 remember-me 参数走不同存储路径；
- 默认行为与旧逻辑兼容（未传时按既定默认策略）；
- token 刷新、登出流程不因 remember-me 引入回归问题。

## 4) 状态流转记录
- 2026-02-16: `正常 -> 待修改`（认证服务纳入 remember-me 变更范围）
- 2026-02-16: `待修改 -> 修改中`（开始定义服务层副作用与存储策略）

## 5) Temp缓存区
- 当前状态为 `修改中`，待实现完成并联调通过后转入 `修改完成`。
