## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AiAgentApplication.java
- Spring Boot 启动入口，负责启用调度/异步能力并注册 MyBatis Mapper 扫描范围；本文件不承载业务逻辑，仅定义接口层运行时装配入口。

## 2) 核心方法
- `main(String[] args)`
- `@MapperScan("com.zj.aiagent.infrastructure.**.mapper")`

## 3) 具体方法
### 3.1 main(String[] args)
- 函数签名: `main(String[] args) -> void`
- 入参: `args` 启动参数
- 出参: 无
- 功能含义: 调用 `SpringApplication.run` 启动容器，加载 `@SpringBootApplication` 及 `@EnableScheduling/@EnableAsync` 声明。
- 链路作用: 应用进程入口 -> Spring 容器初始化 -> Controller/Config Bean 注册生效。

## 4) 变更记录
- 2026-02-15: 基于源码回填启动入口职责、注解装配点与主启动链路。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
