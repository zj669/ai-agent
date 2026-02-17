# Blueprint: `cd-deploy.sh`

## Metadata
- file: `cd-deploy.sh`
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
  - 执行后端容器的标准部署流程：登录镜像仓库、替换旧容器、启动新容器并健康检查。
  - 通过环境变量驱动镜像标签、容器名与端口，实现流水线可参数化发布。
  - 对失败部署输出容器日志并以非零状态返回，供 CI 平台判定失败。
- 不做什么:
  - 不做滚动发布流量切换（由 `cd-deploy-rolling.sh` 负责）。
  - 不负责镜像构建，仅负责拉取并运行已构建镜像。

## 2) 核心方法
| 方法名 | 角色 | 上游调用方 | 下游依赖 | 备注 |
|---|---|---|---|---|
| `loginRegistry()` | 登录私有镜像仓库 | CI/CD 发布作业 | Docker CLI | 凭据来自环境变量 |
| `replaceContainer()` | 停旧起新容器 | 发布流水线 | Docker daemon | 非零中断保护 |
| `prepareDataMounts()` | 运行目录与权限准备 | replaceContainer 前置 | 主机文件系统 | 对齐容器 UID 1001 |
| `verifyHealth()` | 启动后健康探测 | replaceContainer 后置 | `docker exec` + `/public/health` | 失败输出日志并退出1 |

## 3) 具体方法
### `loginRegistry(): AuthResult`
- Signature: `loginRegistry(): AuthResult`
- 入参:
  - `DOCKERNAME`、`DOCKERPASSWORD`
- 出参:
  - `AuthResult` - 登录是否成功
- 功能含义:
  - 使用 `docker login --password-stdin` 对阿里云镜像仓库进行鉴权。
- 链路作用:
  - 上游: CI/CD 凭据注入
  - 下游: `docker pull` 镜像拉取权限
- 副作用/外部依赖:
  - 依赖外网与镜像仓库可用性。

### `replaceContainer(): DeployResult`
- Signature: `replaceContainer(): DeployResult`
- 入参:
  - `CONTAINER_NAME`、`PORT`、`NAMESPACE`、`REPO`、`BUILD_TAG`
- 出参:
  - `DeployResult` - 新容器运行状态
- 功能含义:
  - 删除旧容器后拉取目标镜像并以生产参数启动新容器。
- 链路作用:
  - 上游: 登录成功结果
  - 下游: Docker 容器生命周期管理
- 副作用/外部依赖:
  - 发布窗口存在短暂停机（旧容器先停止）。

### `prepareDataMounts(): MountContext`
- Signature: `prepareDataMounts(): MountContext`
- 入参:
  - 固定主机路径 `/app/data`、`/app/data/log`
- 出参:
  - `MountContext` - 挂载目录可写上下文
- 功能含义:
  - 创建并修正目录权限，保证容器内 UID 1001 可写日志与数据。
- 链路作用:
  - 上游: 部署前置步骤
  - 下游: 容器卷挂载稳定性
- 副作用/外部依赖:
  - 修改宿主机目录所有权。

### `verifyHealth(): HealthCheckResult`
- Signature: `verifyHealth(): HealthCheckResult`
- 入参:
  - `STARTUP_WAIT`、`CONTAINER_NAME`
- 出参:
  - `HealthCheckResult` - 成功/失败
- 功能含义:
  - 循环探测 `http://localhost:8080/public/health`，超时失败时输出容器最近日志并退出。
- 链路作用:
  - 上游: 新容器启动
  - 下游: CI 发布状态判断
- 副作用/外部依赖:
  - 健康检查路径契约变更会导致误判失败。

## 4) 变更记录（每次变更一段话）
- `[2026-02-15 14:20] [v0.2.0] [状态: 正常 -> 正常]`
  - 回填标准部署脚本职责语义，明确单容器替换发布与健康检查失败回滚策略（日志输出+失败退出）。

## 5) Temp缓存区（仅 status=待修改 时保留）
当前状态为 `正常`，本区留空。
