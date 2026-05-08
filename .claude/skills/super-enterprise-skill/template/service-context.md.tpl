# <business-domain-name> 支撑服务上下文

本文件存放 `<business-domain-name>` 业务域的支撑实现事实。

它不是一等路由归属。Agent 应先按业务动作路由，只有被选中的业务 SOP 需要仓库、服务、
域名、数据库、部署或脚本事实时，才读取本文件。

本文件中的所有事实都必须来自目标工作区。不要用源示例仓库名、服务、域名、端口、数据库
或脚本预填充。

## 支撑仓库

| Repo | Role | Notes |
|---|---|---|
| `<repo-name>` | `<business role>` | `<notes>` |

## 运行服务

| Service | Host / Compose / Runtime | Role | Notes |
|---|---|---|---|
| `<service-name>` | `<runtime-location>` | `<business role>` | `<notes>` |

## 域名与入口

| Domain / URL / API | Backing Service | Business Use |
|---|---|---|
| `<domain-or-entry>` | `<service>` | `<use>` |

## 数据库与表

| Database / Table | Access Pattern | Business Use | Safety |
|---|---|---|---|
| `<db.table>` | `<read/write/readonly>` | `<use>` | `<safety>` |

## 脚本与 Agents

| Asset | Path | Purpose | Side Effects |
|---|---|---|---|
| `<script-or-agent>` | `<path>` | `<purpose>` | `<none/read/write/deploy>` |

## 备注

- `<fact>`
- `<known ambiguity>`
- `<stale or missing information>`
