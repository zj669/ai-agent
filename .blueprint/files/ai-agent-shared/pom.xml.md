# Blueprint Mirror: ai-agent-shared/pom.xml

## Metadata
- file: `ai-agent-shared/pom.xml`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/pom.xml
- Type: .xml

## Responsibility
- 定义 `ai-agent-shared` 模块构建与依赖边界，确保 shared 层保持“无框架依赖”定位。
- 管理通用工具、枚举、设计模式抽象所需的基础三方库。
- 不承载业务逻辑与运行时配置。

## Key Symbols / Structure
- `artifactId: ai-agent-shared`
- 依赖声明：`slf4j-api`、`lombok`、`hutool-all`、`fastjson/fastjson2`、`guava`、`commons-lang3`。

## Dependencies
- 继承父 POM `com.zj:ai-agent` 统一版本管理。
- 仅基础库依赖，无 Spring/MyBatis 等框架依赖。

## Notes
- 该 POM 是 domain/application/interfaces 共享底座的依赖契约入口。
