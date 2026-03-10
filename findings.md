# 调研记录

## 2026-03-07

### 用户提供的 openclaw.json 观察
- 顶层主要包含：`agents`、`bindings`、`browser`、`channels`、`commands`、`cron`、`env`、`gateway`、`hooks`、`memory`、`messages`、`meta`、`models`、`plugins`、`session`、`skills`、`tools`、`update`、`web`、`wizard`。
- `agents.defaults` 提供默认模型、沙箱、记忆检索、workspace、timeout 等全局默认值。
- `agents.list` 中定义了两个具体 agent：`main` 与 `xiaogong`。
- 配置中包含明显敏感信息：API key、Telegram bot token、gateway token。

### 项目上下文
- 当前项目目录没有现成的 openclaw 文档。
- 该任务更偏调研与文档生成，不涉及修改业务代码。

### 待确认点
- 输出 JSON 是否需要保留原始敏感值，还是统一脱敏。
- 输出文件希望放在哪个子目录（如 `docs/`、`docs/config/`、项目根目录）。
- 是否只介绍用户贴出的配置项，还是同时补充“常见但当前未出现”的可选项。
