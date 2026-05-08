# 业务 SOP 分类法

扫描已有 skill 或创建新 SOP 骨架时使用此分类法。

## 工作区级文件

- `workspace-entry`: 根 `SKILL.md`；拥有全局执行顺序、业务域地图、路由表、安全规则、验证、输出规则和回写。
- `recent-log`: `logs/YYYY-MM.md`；拥有带日期的近期事实、用户纠正、事故经验和简短工作记录。
- `business-domain-index`: `references/<business-domain>/index.md`；拥有宽业务区域的二级路由。
- `incident-index`: `references/incidents/index.md`；拥有失败签名和事故复盘链接。
- `script-readme`: `scripts/<family>/README.md`；拥有脚本输入、env 政策、dry-run 行为、副作用和验证。
- `supporting-context`: 业务 SOP 使用的可选仓库/服务/域名/数据库事实。

## 业务 SOP 类型

- `knowledge-base`: 架构、服务注册、仓库地图、业务域共享事实。
- `triage`: 事故响应、bug 诊断、证据收集、根因分析。
- `feature-delivery`: 业务能力的需求、设计、实现、测试、评审、发布。
- `release-verify`: preview/预发验证、生产发布、冒烟测试、回滚检查。
- `health-monitor`: 渠道/模型/服务健康检查、告警、变慢、额度耗尽、TTFT、失败率。
- `e2e-verify`: 端到端兼容性和副作用验证。
- `ops`: 基础设施、DNS、部署配置、备份、secrets、运行时运维。
- `script-automation`: skill 内置的确定性或重复性自动化。

## 分类输入

分类 SOP 时检查：

- frontmatter `name` 和 `description`；
- 触发词和别名；
- 标题和概览；
- 工作流步骤；
- 引用的仓库、服务、域名、数据库、脚本和 agent；
- 是否可能改变代码、基础设施、数据库、账号、金钱或生产状态；
- 验证和回滚要求。

## 业务 SOP 与支撑上下文

符合以下情况时创建业务 SOP：

- 用户请求面向动作；
- 工作流跨仓库或服务；
- 操作涉及账号、成本、监控、发布、财务、共享基础设施、生产支持或重复事故响应；
- 需要安全、验证或回写规则。

符合以下情况时创建支撑上下文：

- 信息主要是仓库/服务/域名/数据库事实；
- 它帮助业务 SOP 执行，但不定义工作流；
- 同一服务可参与多个业务工作流。

默认不要创建一等项目路由。仓库名可以是触发提示，但路由通常应落到拥有用户动作的业务域。


