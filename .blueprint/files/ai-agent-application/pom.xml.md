# Blueprint Mirror: ai-agent-application/pom.xml

## Metadata
- file: `ai-agent-application/pom.xml`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/pom.xml
- Type: .xml

## Responsibility
- 定义 application 模块的 Maven 坐标与依赖边界。
- 作为应用层编排模块，连接 domain/shared，并引入 infrastructure 适配能力。

## Key Symbols / Structure
- `artifactId`: `ai-agent-application`
- 关键依赖：
  - `com.zj:ai-agent-domain`
  - `com.zj:ai-agent-shared`
  - `com.zj:ai-agent-infrastructure`
  - `spring-boot-starter-web`, `spring-boot-starter-webflux`, `spring-tx`
  - `spring-boot-starter-validation`, `httpclient5`, `lombok`
  - `spring-boot-starter-test`(test)

## Dependencies
- 通过父工程 `com.zj:ai-agent:1.0.0-SNAPSHOT` 继承统一版本与插件管理。

## Notes
- 状态: 正常
- 该模块已显式依赖 infrastructure，当前项目采用 application 直接编排基础设施实现的运行形态。
