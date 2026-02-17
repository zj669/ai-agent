# Blueprint: `Dockerfile`

## Metadata
- file: `Dockerfile`
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
  - 定义后端服务容器化构建流程：Maven 多阶段构建 + JRE 运行时镜像。
  - 通过离线依赖下载与分层复制提升构建缓存命中率。
  - 以非 root 用户运行应用并暴露健康检查所需运行环境。
- 不做什么:
  - 不管理业务配置中心逻辑（仅通过环境变量传入启动参数）。
  - 不负责编排数据库/Redis/Milvus 等依赖服务（由 docker-compose 负责）。

## 2) 核心方法
| 方法名 | 角色 | 上游调用方 | 下游依赖 | 备注 |
|---|---|---|---|---|
| `builderStage()` | 构建阶段打包 JAR | Docker build / CI | Maven + 各模块源码 | 负责产出可运行 jar |
| `runtimeStage()` | 运行阶段镜像装配 | builderStage 产物 | Temurin JRE Alpine | 以最小运行时承载应用 |
| `configureRuntimeUser()` | 安全运行上下文配置 | runtimeStage | Linux 用户/目录权限 | 非 root 运行 |
| `bootstrapApp()` | 应用启动入口定义 | docker run / k8s | `JAVA_OPTS` + Spring profile | 最终 ENTRYPOINT |

## 3) 具体方法
### `builderStage(): BuildArtifact`
- Signature: `builderStage(): BuildArtifact`
- 入参:
  - 构建上下文中的根 POM、子模块 POM 与源码目录
- 出参:
  - `BuildArtifact` - `ai-agent-interfaces/target/*.jar`
- 功能含义:
  - 在 Maven 镜像内执行 `dependency:go-offline` 和 `clean package -DskipTests`，输出后端可执行 JAR。
- 链路作用:
  - 上游: CI/CD 镜像构建流程
  - 下游: runtime 阶段 `COPY --from=builder` 取产物
- 副作用/外部依赖:
  - 依赖远程 Maven 仓库；构建缓存失效会显著增加构建时长。

### `runtimeStage(): RuntimeImage`
- Signature: `runtimeStage(): RuntimeImage`
- 入参:
  - `BuildArtifact`
- 出参:
  - `RuntimeImage` - 仅包含 JRE、curl、app.jar 的运行镜像
- 功能含义:
  - 在 `eclipse-temurin:21-jre-alpine` 基础上完成运行时镜像组装，减少运行镜像体积与攻击面。
- 链路作用:
  - 上游: builder 阶段产物
  - 下游: 生产容器启动与健康检查
- 副作用/外部依赖:
  - 运行镜像依赖 Alpine 包管理源获取 curl。

### `configureRuntimeUser(): SecurityContext`
- Signature: `configureRuntimeUser(): SecurityContext`
- 入参:
  - 无
- 出参:
  - `SecurityContext` - appuser/appgroup 与 `/app/data/log` 权限
- 功能含义:
  - 创建 UID/GID 1001 运行用户并修正日志目录权限，避免容器 root 进程运行。
- 链路作用:
  - 上游: runtimeStage 初始化
  - 下游: Java 进程文件写入权限
- 副作用/外部依赖:
  - 运行时挂载卷若 UID 不匹配可能产生写权限问题。

### `bootstrapApp(): Entrypoint`
- Signature: `bootstrapApp(): Entrypoint`
- 入参:
  - `JAVA_OPTS` 环境变量、固定 `spring.profiles.active=dev`
- 出参:
  - `Entrypoint` - `java -jar app.jar` 启动命令
- 功能含义:
  - 提供统一容器启动入口，支持 JVM 内存参数注入并暴露 8080 服务端口。
- 链路作用:
  - 上游: `docker run` / 容器编排平台
  - 下游: Spring Boot 应用进程
- 副作用/外部依赖:
  - profile 与资源限制配置错误会导致启动失败或性能退化。

## 4) 变更记录（每次变更一段话）
- `[2026-02-15 14:20] [v0.2.0] [状态: 正常 -> 正常]`
  - 回填 Dockerfile 多阶段构建、运行安全与启动入口语义，清理占位模板并补齐链路说明。

## 5) Temp缓存区（仅 status=待修改 时保留）
当前状态为 `正常`，本区留空。
