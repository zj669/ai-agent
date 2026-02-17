# Blueprint: `pom.xml`

## Metadata
- file: `pom.xml`
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
  - 作为 Maven 聚合根 POM，定义后端五层模块（shared/domain/application/infrastructure/interfaces）的统一构建入口。
  - 统一管理 Java/Spring/MyBatis/Redis/Milvus 等依赖版本，避免子模块版本漂移。
  - 通过 `dependencyManagement` 与 `pluginManagement` 约束构建契约，保障 CI/CD 可重复构建。
- 不做什么:
  - 不承载任何运行时业务逻辑。
  - 不替代各子模块 `pom.xml` 的模块内依赖声明。

## 2) 核心方法
| 方法名 | 角色 | 上游调用方 | 下游依赖 | 备注 |
|---|---|---|---|---|
| `declareModules()` | 声明聚合模块拓扑 | Maven reactor / CI 构建任务 | `ai-agent-*` 各子模块 | 决定构建顺序与聚合边界 |
| `manageVersions()` | 统一版本与属性管理 | 子模块依赖解析 | Spring Boot Parent、Spring AI BOM、protobuf | 解决跨模块版本冲突 |
| `manageDependencies()` | 统一依赖管理入口 | 子模块 `dependencies` | 内部模块坐标 + 第三方依赖 | 提供版本锁定，不直接引入运行依赖 |
| `managePlugins()` | 统一插件管理 | 子模块构建插件解析 | `spring-boot-maven-plugin` 等 | 保持构建行为一致 |

## 3) 具体方法
### `declareModules(): MavenReactor`
- Signature: `declareModules(): MavenReactor`
- 入参:
  - 无
- 出参:
  - `MavenReactor` - 聚合构建模块图
- 功能含义:
  - 在 `<modules>` 中声明后端分层模块，保证 DDD 结构的物理拆分与统一构建。
- 链路作用:
  - 上游: `mvn clean install`、IDEA Maven 生命周期
  - 下游: 各子模块的编译、测试、打包流程
- 副作用/外部依赖:
  - 影响全仓库构建顺序与失败传播范围。

### `manageVersions(): VersionCatalog`
- Signature: `manageVersions(): VersionCatalog`
- 入参:
  - 无
- 出参:
  - `VersionCatalog` - 统一版本属性集合
- 功能含义:
  - 通过 `<properties>` 和 BOM 引入，统一 Spring AI、MyBatis Plus、protobuf 等关键组件版本。
- 链路作用:
  - 上游: 子模块依赖解析阶段
  - 下游: 编译期依赖收敛、冲突规避（尤其 Milvus/protobuf）
- 副作用/外部依赖:
  - 错误升级会影响全部子模块兼容性。

### `manageDependencies(): DependencyManagement`
- Signature: `manageDependencies(): DependencyManagement`
- 入参:
  - 无
- 出参:
  - `DependencyManagement` - 依赖版本约束表
- 功能含义:
  - 为内部模块和关键第三方库提供统一版本约束，避免子模块重复声明版本。
- 链路作用:
  - 上游: 子模块 POM 的依赖声明
  - 下游: Maven 依赖解析与最终类路径
- 副作用/外部依赖:
  - 变更会触发全局依赖树变化。

### `managePlugins(): PluginManagement`
- Signature: `managePlugins(): PluginManagement`
- 入参:
  - 无
- 出参:
  - `PluginManagement` - 构建插件策略
- 功能含义:
  - 统一 Spring Boot 打包插件配置入口，约束子模块构建插件行为。
- 链路作用:
  - 上游: Maven package/repackage 生命周期
  - 下游: 可执行 JAR 产物构建
- 副作用/外部依赖:
  - 影响产物结构与部署兼容性。

## 4) 变更记录（每次变更一段话）
- `[2026-02-15 14:20] [v0.2.0] [状态: 正常 -> 正常]`
  - 回填根级聚合 POM 的职责语义，明确模块拓扑、版本治理与插件治理链路，移除模板占位内容。

## 5) Temp缓存区（仅 status=待修改 时保留）
当前状态为 `正常`，本区留空。
