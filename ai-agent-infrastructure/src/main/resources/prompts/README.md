# ReAct 模式提示词模板

## 概述

本目录包含 ReAct 模式智能体的三个核心节点的提示词模板:

| 文件名 | 节点类型 | 用途 |
|--------|----------|------|
| `react-plan-node.txt` | PlanNode | 规划执行步骤 |
| `react-act-node.txt` | ActNode | 执行工具调用 |
| `react-summary-node.txt` | SummaryNode | 评估结果并决策 |

---

## 占位符规范

### PlanNode 占位符

| 占位符 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `{userQuestion}` | String | 用户原始问题 | "未提供用户问题" |
| `{executionHistory}` | String | 格式化的执行历史 | "暂无执行历史" |
| `{reflectionReason}` | String | Summary 节点的反思结果 | "" |
| `{availableTools}` | String | MCP 工具列表(动态获取) | 从 MCP Clients 动态生成 |
| `{loopCount}` | String | 当前循环次数 | "0" |

**executionHistory 格式示例:**
```
- [步骤1] 查询北京天气
  工具: web_search
  结果: 晴,15-25℃
  状态: SUCCESS
- [步骤2] 查询上海天气
  工具: web_search
  结果: 多云,18-22℃
  状态: SUCCESS
```

---

### ActNode 占位符

| 占位符 | 类型 | 说明 | 来源 |
|--------|------|------|------|
| `{currentStepNumber}` | String | 当前步骤编号 | `PlanStep.stepNumber` |
| `{currentStepDescription}` | String | 当前步骤描述 | `PlanStep.description` |
| `{toolName}` | String | 要调用的工具名称 | `PlanStep.toolName` |
| `{toolParams}` | String | 工具参数(JSON) | `PlanStep.toolParams` |
| `{expectedOutput}` | String | 预期输出 | `PlanStep.expectedOutput` |

**toolParams 格式示例:**
```json
{"query": "北京今日天气", "limit": 10}
```

---

### SummaryNode 占位符

| 占位符 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `{userQuestion}` | String | 用户原始问题 | "未提供用户问题" |
| `{executionHistory}` | String | 格式化的执行历史 | "暂无执行历史" |
| `{loopCount}` | String | 当前循环次数 | "0" |
| `{maxLoops}` | String | 最大循环次数 | "5" |

---

## 模板使用流程

### 1. 配置模板到提示词领域

将这些模板文件的内容配置到提示词管理系统中,例如:

```sql
INSERT INTO prompt_template (
    template_key, 
    template_name, 
    template_content, 
    category
) VALUES 
(
    'react_plan_node',
    'ReAct 规划节点提示词',
    '你是一个智能规划助手...\n{userQuestion}\n...',
    'react'
),
(
    'react_act_node',
    'ReAct 执行节点提示词',
    '你是一个工具执行助手...\n{toolName}\n...',
    'react'
),
(
    'react_summary_node',
    'ReAct 评估节点提示词',
    '你是一个评估助手...\n{executionHistory}\n...',
    'react'
);
```

### 2. 节点配置关联模板

在创建 ReAct 工作流时,为每个节点配置对应的模板:

```json
{
  "nodeId": "plan_node_1",
  "nodeType": "PLAN",
  "promptTemplateKey": "react_plan_node",
  "config": {
    "maxLoops": 5
  }
}
```

### 3. 运行时动态渲染

节点执行时,会:
1. 从提示词领域加载模板内容
2. 从 WorkflowState 提取占位符值
3. 替换占位符生成最终 Prompt
4. 调用 AI 模型

**示例代码:**
```java
// PlanNode.buildPrompt()
String template = promptService.getTemplate("react_plan_node");
String prompt = template
    .replace("{userQuestion}", state.get(ReAct.USER_QUESTION_KEY))
    .replace("{executionHistory}", formatExecutionHistory(...))
    .replace("{availableTools}", formatAvailableTools())
    // ... 其他占位符
    ;
```

---

## 模板优化建议

### 1. JSON 格式要求明确性

✅ **推荐**: 在模板中明确要求输出 JSON,并提供示例
```
严格按照以下 JSON 格式输出:
{
  "status": "SUCCESS",
  "toolResult": "..."
}
```

❌ **避免**: 模糊的输出要求
```
请返回结果
```

### 2. 占位符命名一致性

- 使用 camelCase 命名占位符
- 与 State 键名保持一致
- 避免使用特殊字符

### 3. 处理空值情况

在模板中考虑占位符为空的情况:
```
## 反思结果
{reflectionReason}
```

代码中处理:
```java
String reflectionReason = state.contains(ReAct.REFLECTION_REASON_KEY)
    ? state.get(ReAct.REFLECTION_REASON_KEY, String.class)
    : ""; // 空字符串而非 null
```

### 4. 多语言支持

可以为同一个节点创建多语言版本:
- `react-plan-node-zh_CN.txt` (简体中文)
- `react-plan-node-en_US.txt` (英文)
- `react-plan-node-ja_JP.txt` (日语)

---

## 模板测试检查清单

在更新模板后,确保:

- [ ] 所有占位符名称正确,与代码中一致
- [ ] JSON 输出格式示例完整且可被解析
- [ ] 包含成功和失败的示例
- [ ] 说明了边缘情况的处理方式
- [ ] 语言表达清晰,无歧义
- [ ] 符合 AI 模型的理解能力
- [ ] 测试了各种输入场景下的效果

---

## 版本控制

| 版本 | 日期 | 修改说明 | 修改人 |
|------|------|----------|--------|
| v1.0 | 2025-12-26 | 初始版本,创建三个节点的基础模板 | System |

---

## 参考文档

- [ReAct 模式设计文档](../../../../../.gemini/antigravity/brain/7f0a2639-fae4-4931-a020-b201f978a744/react_design.md)
- [ReAct 实现总结](../../../../../.gemini/antigravity/brain/7f0a2639-fae4-4931-a020-b201f978a744/react_implementation_summary.md)
- [Prompt 模板配置指南](../../../../../.gemini/antigravity/brain/7f0a2639-fae4-4931-a020-b201f978a744/react_prompt_templates.md)

---

## 联系方式

如有问题或建议,请联系项目维护团队。
