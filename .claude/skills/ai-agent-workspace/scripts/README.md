# Scripts

此目录存放 AI Agent 平台工作区的可复用自动化脚本。

## 环境变量

所有脚本通过以下环境变量定位工作区：

```bash
export AI_AGENT_WORKSPACE_ROOT=/home/zj669/repo/ai-agent
export SKILL_ROOT=$AI_AGENT_WORKSPACE_ROOT/.claude/skills/ai-agent-workspace
```

## 脚本家族

| 家族 | 目录 | 状态 |
|---|---|---|
| 健康检查 | `scripts/health/` | 待创建 |
| 数据库操作 | `scripts/db/` | 待创建 |
| 部署辅助 | `scripts/deploy/` | 待创建 |

## 通用规则

- 所有写入操作脚本必须支持 `--dry-run` 参数
- 不在脚本中硬编码密码或 token
- 真实凭据通过 `.env` 或环境变量注入
- 脚本副作用在对应 family README 中说明
- 新增脚本前在此 README 登记
