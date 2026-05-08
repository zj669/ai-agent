# 业务分层工作区 Skill 设计

## 目的

`super-enterprise-skill` 用于创建和维护业务分层的企业工作区运行 skill。

工作区运行 skill 是公司或大型工作区的权威入口。它给 agent 足够的结构，使其能够
正确开始工作、把请求路由到正确的业务 SOP、避免已知生产错误、验证结果，并把可复用
经验回写到知识系统。

## 核心模型

共有四层：

| 层级 | 职责 | 非职责 |
|---|---|---|
| `super-enterprise-skill` | 创建、迁移、审计和维护业务分层的工作区运行 skill | 不直接执行目标公司的业务操作 |
| 工作区运行 skill | 单一入口、全局规则、近期记忆、业务域地图、路由表、验证、知识回写 | 不内联每一个具体业务 SOP |
| 业务域索引 | 为基础设施、客户应用、运营、账号、账单、API 网关、监控、事故等较宽业务区域提供二级路由 | 不替代根级全局安全规则 |
| 业务 SOP | 具体执行流程、支撑仓库/服务事实、命令、安全边界、验证 | 不拥有整个企业地图 |

仓库、服务、域名、数据库和历史项目是支撑事实。它们帮助执行被选中的业务 SOP，
但除非用户明确要求项目导向系统，不应作为顶层主分割。

## 预期流转

```text
super-enterprise-skill
  -> 创建 / 维护业务分层工作区运行 skill
      -> 读取最近日志
      -> 通过业务域地图和 Meet->See 表路由
      -> 必要时读取业务域索引
      -> 通过具体业务 SOP 执行
      -> 只有 SOP 需要实现事实时，才读取仓库/服务上下文
      -> 将长期有效的经验回写到 logs / references / scripts
```

## 生成后 Skill 的必要形态

生成后的工作区 skill 应包含：

- YAML frontmatter，并用触发意图明确的 description 覆盖可能的业务触发短语。
- 身份和边界：skill 拥有什么、不拥有什么。
- 执行顺序：开工前必须读什么。
- 路径约定：优先相对路径，用环境变量承载可移植脚本根目录。
- 业务域地图：粗粒度区域、典型信号和路由入口。
- 边界说明：常见歧义术语以及如何路由。
- 全局安全规则。
- 如果工作区可能接触生产数据库，则包含数据库安全规则。
- 代码搜索和复用规则。
- 适用时包含部署和前端验证规则。
- 验证标准。
- 面向用户的输出标准。
- 知识回写和收尾规则。
- 业务路由表。

## 源模式，目标事实

这个元 skill 可以把已有企业 skill 作为结构示例，但必须只把它当成模式。

生成后的目标 skill 不得继承源工作区事实，除非这些事实已在目标工作区独立确认。

默认绝不迁移：

- 源工作区名称；
- 源业务域名称；
- 源仓库名称；
- 源服务名称；
- 源域名、URL、端口或 IP；
- 源数据库名或表名；
- 源服务器地址、服务 URL、凭据值或部署路径；
- 源事故历史；
- 依赖源拓扑的源部署规则。

只迁移运行结构：

- 单一入口；
- 最近日志要求；
- 业务域分层；
- 安全章节；
- 验证格式；
- 知识回写模型；
- scripts / agents 组织方式；
- 只带占位符的运维 `.env.example` 形态。

## 目录形态

为新的工作区 skill 使用以下形态：

```text
<workspace-skill>/
  SKILL.md
  .env.example
  logs/
    YYYY-MM.md
  references/
    <business-domain>/index.md
    incidents/index.md
  scripts/
    README.md
  agents/
    .gitkeep
  evals/
    evals.json
```

只有在有足够真实信息、不会制造虚假精度时，才创建更深层的 SOP 文件。占位符应说明缺失内容。

## 渐进披露

使用以下拆分：

- 根 `SKILL.md`: 稳定的全局协议和业务路由。
- `.env.example`: 给用户填写真实 `.env` 或 approved secret store 的运维数据占位。
- `logs/`: 带日期的近期事实、决策、错误和用户纠正。
- `references/<business-domain>/index.md`: 二级业务路由。
- `references/<business-domain>/<workflow>.md`: 具体业务 SOP。
- `references/<business-domain>/service-context.md`: 当业务域需要时，可选的支撑仓库/服务/域名/数据库事实。
- `scripts/<family>/README.md`: 脚本使用方式、安全要求、env 政策。
- `agents/<agent>/openai.yaml`: 专用 agent 入口。

## 维护原则

更新最窄且持久的归属文件：

- 今天的事实事件 -> `logs/YYYY-MM.md`。
- 重复流程 -> `references/<business-domain>/<workflow>.md`。
- 业务路由或归属变化 -> 根路由表。
- 跨工作区安全规则 -> 根 `SKILL.md`。
- 脚本接口变化 -> `scripts/<family>/README.md`。
- 可复用失败签名 -> `references/incidents/...`。
- 支撑仓库/服务事实 -> 相关业务域上下文文件，默认不要变成新的顶层项目路由。

当一次目标业务 SOP 更新已经足够时，不要重写整个 skill。
