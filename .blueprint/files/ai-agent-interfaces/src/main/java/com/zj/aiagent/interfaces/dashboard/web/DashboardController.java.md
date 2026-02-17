## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/dashboard/web/DashboardController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: DashboardController.java
- Dashboard 统计接口入口，读取当前登录用户并调用 `DashboardApplicationService` 返回聚合统计信息。

## 2) 核心方法
- `getStats()`

## 3) 具体方法
### 3.1 getStats()
- 函数签名: `getStats() -> Response<DashboardStatsResponse>`
- 入参: 无（用户来自 `UserContext`）
- 出参: Dashboard 统计响应
- 功能含义: 完成用户鉴权判断后调用应用层查询统计并封装统一响应。
- 链路作用: 首页仪表盘加载 -> 应用服务聚合查询 -> 前端展示。

## 4) 变更记录
- 2026-02-15: 基于源码回填 Dashboard 统计接口职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
