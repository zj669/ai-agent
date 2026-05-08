---
name: super-enterprise-skill
description: >-
  创建、初始化、审计和维护业务分层的企业工作区运行 skill：一个权威工作区总入口、
  最近工作日志、业务域地图、Meet->See 路由表、全局安全规则、验证标准、知识回写、
  references、scripts，以及可选的 agent 资产。当用户想要创建一个功能类似既有企业
  运行 skill 的公司级工作区 skill、维护这类 skill、更新业务路由、把单体 skill
  迁移为分层业务 SOP、审计 SOP 冲突，或初始化缺失的业务域 SOP 骨架时使用。
  触发词："super-enterprise", "企业skill", "公司skill", "工作区总入口",
  "初始化skill", "维护skill", "维护SOP", "业务路由表", "业务分层", "工作区地图",
  "知识回写", "工作区skill模板", "更新企业sop".
---

# Super Enterprise Skill

## 身份定位

这是一个元 skill，用于创建和维护业务分层的企业工作区运行 skill。

它不直接执行目标企业的业务工作。它负责构建和维护另一个 skill，而那个 skill
之后会负责路由和治理目标企业的业务工作。

目标产物应遵循业务分层的工作区运行模式：

- 一个权威的 `SKILL.md` 作为工作区总入口。
- 风险操作前必须读取的最近工作日志。
- 业务域地图和 `Meet -> See` 路由表。
- 覆盖服务器、数据库、生产用户、部署和验证的全局安全规则。
- 通过 `references/<business-domain>/...` 渐进披露细节。
- `scripts/<family>/...` 下的可复用自动化。
- `agents/<agent-name>/...` 下的可选专用 agent 资产。
- 知识回写规则，使 skill 在真实工作后持续改进。

业务域是第一层分层单位。仓库、服务、域名、数据库和历史上的“项目”只是某个
业务域内的支撑事实，不是一等路由归属。

## 运行模式

选择能匹配用户请求的最窄模式。

- `bootstrap`: 从零创建业务分层的工作区 skill；也用于将已有单体 skill 或分散 SOP 迁移为分层结构。
- `maintain`: 修改已有工作区 skill，包括更新路由、添加/调整 SOP、写日志、检查结构冲突和安全缺口。

如果目标 skill 目录已经存在，默认选择 `maintain`，不要重新创建。

## 必须先读

只读取所选模式需要的文件。

- bootstrap 模式：读 `references/bootstrap-flow.md`。
- maintain 模式：读 `references/maintain-flow.md`。
- 了解设计模型时，读 `references/business-layered-design.md`。
- 初始化或审计数据完整性前，读 `references/required-data.md`。
- 分类 SOP 前，读 `references/sop-taxonomy.md`。
- 生成或维护业务 SOP 前，读 `references/sop-authoring-spec.md`。
- 收尾前，读取并应用 `references/validation-checklist.md`。

模板只作为脚手架使用：

- `template/business-workspace.SKILL.md.tpl`: 工作区运行 skill 总入口。
- `template/business-sop.SKILL.md.tpl`: 业务 SOP 骨架。
- `template/service-context.md.tpl`: 支撑仓库/服务上下文说明。
- `.env.example`: 给生成后的工作区 skill 使用的运维数据占位模板。

## 流程

- `bootstrap` 模式：按 `references/bootstrap-flow.md` 执行。
- `maintain` 模式：按 `references/maintain-flow.md` 执行。

## 输出格式

创建、维护、迁移或审计任务结束时，输出：

- `目标`: bootstrap 或 maintain。
- `扫描结果`: 已检查的现有 skill 文件、SOP、脚本、日志和缺口。
- `变更结果`: 创建或修改的文件。
- `业务路由变化`: 路由表新增、更新、删除或冲突。
- `安全边界`: 新增或确认的安全规则。
- `验证`: 结构检查、链接检查、触发检查和剩余缺口。
- `待确认`: 未解决的业务归属、术语、凭据政策或高风险执行边界。

