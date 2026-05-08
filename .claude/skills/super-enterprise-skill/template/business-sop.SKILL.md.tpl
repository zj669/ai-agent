---
name: <business-sop-skill-name>
description: >-
  <business-domain-name> 业务 SOP。用于 Codex 需要执行、调查、维护、验证或记录
  该业务域工作流时，包括路由、安全边界、脚本、输出和回写。此 SOP 必须与工作区
  运行 skill 一起使用，以保留最近日志和全局规则。触发词：
  "<business-term>", "<action-alias>", "<common-request>", "<domain-specific-wording>".
---

# <business-domain-name> SOP

## 目的

用于 `<business-domain-name>` 工作流。该工作流可能触及多个仓库或服务，但业务动作
拥有路由。

仓库、服务、域名、数据库和历史项目等实现细节应放在支撑上下文文件中，除非它们定义了
可复用业务工作流。

只包含目标工作区事实。如果仓库、服务、域名、数据库或业务标签只来自源示例，保留占位符。

## 触发条件

- 用户提到 `<business-term>` 或等价动作。
- 任务涉及 `<workflow-scope>`。
- 该工作流可能触及多个仓库、服务、数据库、脚本或外部系统。

## 范围

- repos: `<repos>`
- services: `<services>`
- domains: `<domains>`
- databases: `<databases>`
- scripts: `<scripts>`
- supporting context: `<supporting-context-paths>`

## 必要输入

- required inputs: `<required-inputs>`
- optional inputs: `<optional-inputs>`
- credential policy: `<credential-policy>`
- time window / customer / account scope: `<scope-fields>`

## 开工前读取

- workspace entry: `SKILL.md`
- recent logs: `logs/YYYY-MM.md`
- business index: `references/<business-domain>/index.md`
- this SOP: `<this-file>`
- supporting context when needed: `<supporting-context-paths>`

## 停止条件

出现以下情况时停止，并询问或准备确认包：

- 需要生产数据库写入；
- 账号状态、权益、账单、客户可见数据或告警可能变化；
- 凭据或用户提供的认证材料缺失或有歧义；
- 目标路由与另一个 SOP 冲突；
- 回滚/补偿方案未知。

## 工作流

按 `references/sop-authoring-spec.md` 的 operation-spec 闭环填写：

1. 定界：`<confirm business action, target, environment, impact>`
2. 只读调查：`<evidence collection steps>`
3. 操作计划：`<planned actions, risk levels, confirmation package>`
4. 执行：`<minimal-scope execution steps>`
5. 验证：`<direct, dependency-chain, e2e verification>`
6. 回写：`<logs, SOP, scripts README, incidents, route table>`

## 安全边界

- capabilities: `<capabilities>`
- safety: `<safety>`
- confirmations: `<confirmation-points>`
- forbidden actions: `<forbidden-actions>`
- rollback / compensation: `<rollback-or-compensation>`

## 脚本使用

如果此工作流使用内置脚本：

- script family: `scripts/<family>/`
- README: `scripts/<family>/README.md`
- dry-run/mock command: `<dry-run-command>`
- real side effects: `<side-effects>`
- env policy: `<env-policy>`

## 代码定位规则

代码、命令和脚本只能作为执行步骤的支撑事实：

- code entrypoints: `<routes, services, handlers, jobs, scripts>`
- search hints: `<function names, route names, model names, config names>`
- commands: `<safe read-only commands or approved workflow commands>`
- forbidden code content: 不粘贴大段源码，不按文件逐段解释实现，不把函数实现当成 SOP 主体。

## 验证

- direct verification: `<data, command, report, script output>`
- dependency-chain verification: `<related systems>`
- end-to-end verification: `<business-visible result>`
- unverified: `<known blockers>`

## 输出形态

- `业务动作`
- `目标范围`
- `执行结果`
- `验证结果`
- `风险与回滚`
- `SOP / 域索引回写`
