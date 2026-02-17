# Blueprint: `cd-deploy-rolling.sh`

## Metadata
- file: `cd-deploy-rolling.sh`
- version: `v0.2.0`
- status: 正常
- updated_at: 2026-02-15 14:20
- owner: backend-blueprint-shadow

### 状态机
`正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
允许回退:
`修改中 -> 待修改`
`修改完成 -> 修改中`

## 1) 整体文件职责
- 做什么:
  - 执行后端零停机近似滚动发布：新容器临时端口预热、健康验证、生产端口切换、旧容器清理。
  - 将发布失败控制在切换前阶段，降低主服务不可用风险。
  - 统一输出可观测部署日志，便于 CI/CD 追踪发布阶段。
- 不做什么:
  - 不负责镜像构建。
  - 不提供多副本负载均衡编排（仅单机双容器切换方案）。

## 2) 核心方法
| 方法名 | 角色 | 上游调用方 | 下游依赖 | 备注 |
|---|---|---|---|---|
| `preheatNewContainer()` | 新容器预热启动 | 发布流水线 | Docker daemon | 使用临时端口 8081 |
| `verifyTempHealth()` | 切换前健康校验 | preheatNewContainer | 主机 `curl` | 避免依赖容器内工具 |
| `switchTraffic()` | 生产端口切换 | verifyTempHealth 成功 | 容器停止/重命名/重启 | 旧容器保留为 `-old` |
| `finalizeRollbackGuard()` | 最终校验与清理 | switchTraffic 后 | `/public/health` + docker logs | 失败即保留现场并退出1 |

## 3) 具体方法
### `preheatNewContainer(): ContainerHandle`
- Signature: `preheatNewContainer(): ContainerHandle`
- 入参:
  - `BUILD_TAG`、`CONTAINER_NAME`、`NAMESPACE`、`REPO`
- 出参:
  - `ContainerHandle` - `${CONTAINER_NAME}-new`
- 功能含义:
  - 在临时端口启动新容器并挂载数据卷，提前完成应用初始化。
- 链路作用:
  - 上游: 镜像仓库登录与拉取
  - 下游: 临时端口健康检查
- 副作用/外部依赖:
  - 额外占用一份容器资源与端口。

### `verifyTempHealth(): HealthCheckResult`
- Signature: `verifyTempHealth(): HealthCheckResult`
- 入参:
  - `TEMP_PORT`、`STARTUP_WAIT`
- 出参:
  - `HealthCheckResult` - 临时容器是否可服务
- 功能含义:
  - 使用宿主机 `curl` 对临时端口进行多次探测，失败时立即停止并删除新容器。
- 链路作用:
  - 上游: 新容器已运行
  - 下游: 决定是否进入生产切换
- 副作用/外部依赖:
  - 依赖健康接口 `/public/health` 契约稳定。

### `switchTraffic(): CutoverResult`
- Signature: `switchTraffic(): CutoverResult`
- 入参:
  - `CONTAINER_NAME`、`PORT`
- 出参:
  - `CutoverResult` - 生产容器切换状态
- 功能含义:
  - 停止旧容器并重命名为 `-old`，释放临时容器后以正式端口拉起新生产容器。
- 链路作用:
  - 上游: 临时容器健康通过
  - 下游: 生产端口可用性恢复
- 副作用/外部依赖:
  - 切换阶段仍有极短窗口，依赖 Docker 操作成功率。

### `finalizeRollbackGuard(): DeployResult`
- Signature: `finalizeRollbackGuard(): DeployResult`
- 入参:
  - `PORT`、`CONTAINER_NAME`
- 出参:
  - `DeployResult` - 发布成功或失败
- 功能含义:
  - 对生产端口做最终健康验证，成功后清理 `-old`；失败时输出新容器日志并返回失败状态。
- 链路作用:
  - 上游: 生产容器已切换
  - 下游: CI/CD 发布结果判定
- 副作用/外部依赖:
  - 失败时保留故障现场供排查。

## 4) 变更记录（每次变更一段话）
- `[2026-02-15 14:20] [v0.2.0] [状态: 正常 -> 正常]`
  - 回填滚动部署脚本语义，明确“预热-校验-切换-最终校验”四阶段链路及失败保护策略。

## 5) Temp缓存区（仅 status=待修改 时保留）
当前状态为 `正常`，本区留空。
