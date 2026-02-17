## Metadata
- file: `.blueprint/files/ai-agent-foward/src/services/credentialStorageService.ts.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-16
- owner: blueprint-engineer

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: credentialStorageService.ts
- 该文件负责认证凭证的统一读写抽象，隔离 localStorage/sessionStorage 的底层差异。

## 2) 核心方法
- `saveCredential(payload, rememberMe)`
- `loadCredential()`
- `clearCredential()`

## 3) 变更记录（记住我）
### 3.1 目标
- 新建 remember-me 专用凭证存储蓝图，定义独立服务职责，避免页面或 authService 直接操作底层存储。

### 3.2 影响
- 影响登录成功后的凭证持久化路径选择；
- 影响登出和过期清理的一致性；
- 为后续审计与迁移（如从 localStorage 切换到其他方案）提供单点抽象。

### 3.3 验收
- 提供统一 save/load/clear 契约；
- 支持 remember-me=true/false 的差异化存储；
- 调用方不再直接依赖具体存储 API。

## 4) 状态流转记录
- 2026-02-16: 文件创建，状态初始化视为 `正常`
- 2026-02-16: `正常 -> 待修改`（纳入 remember-me 首次实现范围）
- 2026-02-16: `待修改 -> 修改中`（开始细化存储职责与验收标准）
- 2026-02-16: `修改中 -> 修改完成`（蓝图定义完成，验收标准明确）
- 2026-02-16: `修改完成 -> 正常`（蓝图收敛，凭证存储服务已纳入架构基线）

## 5) Temp缓存区
- 当前状态为 `正常`，本区留空。

## 6) 验收记录
- 验收时间: 2026-02-16
- 验收结果: ✅ 通过
- 验收说明: 蓝图已明确 credentialStorageService 职责边界、save/load/clear 契约、remember-me 差异化存储策略，满足架构推演要求。
