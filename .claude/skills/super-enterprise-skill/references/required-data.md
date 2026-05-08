# 必要数据收集

收集目标工作区的运行时关键数据。这些数据是 AI 能够实际操控项目的基础。

真实值存放在 `.env` 或 secret store，生成的 `.env.example` 只放占位符。

## 工作区身份

| Field | Purpose |
|---|---|
| 工作区名称 | skill frontmatter 和标题 |
| skill 写入路径 | 生成后的 skill 放在哪里 |
| 工作区根路径 | 仓库、脚本、配置的相对基准 |

## 服务器访问

| Field | Purpose |
|---|---|
| SSH host / 端口 / 用户 | 登录生产/预发服务器 |
| 部署根目录 | 服务器上的项目部署路径 |
| SSH 密钥路径 | 本地密钥文件位置 |

## 数据库

每个数据库实例收集：

| Field | Purpose |
|---|---|
| 类型（MySQL / PostgreSQL / ClickHouse / SQLite） | 确定连接方式和命令 |
| host / 端口 | 连接地址 |
| 数据库名 | 目标库 |
| 用户名 / 密码 | 读写凭据（分只读和读写角色） |
| SSL mode | 是否需要 TLS |

## 缓存 / 中间件

### Redis

| Field | Purpose |
|---|---|
| host / 端口 | 连接地址 |
| 密码 | 认证 |
| db index | 使用哪个库 |

### 消息队列（RabbitMQ / Kafka / 其他）

| Field | Purpose |
|---|---|
| 类型 | 确定操作命令和 SDK |
| 连接地址 / AMQP URL | broker 入口 |
| 用户名 / 密码 | 认证 |
| 关键 queue / topic 名称 | 业务操作目标 |

## 对外服务入口

| Field | Purpose |
|---|---|
| 生产 URL | 健康检查和验证 |
| 预发 / staging URL | 测试验证 |
| 管理后台 URL | 运营操作入口 |
| 监控 / 日志 dashboard URL | 排查问题入口 |

## 外部 API

每个上游 API 收集：

| Field | Purpose |
|---|---|
| base URL | 请求入口 |
| API key / token 变量名 | 认证 |
| 关键接口路径 | AI 操作时的目标端点 |

## 通知渠道

| Field | Purpose |
|---|---|
| Slack / 飞书 webhook URL | 告警和通知 |
| 邮件收件人 | 报告接收方 |
