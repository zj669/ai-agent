# 优化 HTTP 节点 — 固定输出 + JSON 提取支持

## Goal

修复 HTTP 节点在前端工作流编辑器中的输出配置问题。当前 HTTP 节点：
- `initial_schema.outputSchema` 为空数组
- 用户可以随意添加/删除/修改输出字段（policy 全开放）
- 缺少 JSON 提取能力

目标是让 HTTP 节点拥有**固定的 response 输出**，同时支持**用户通过 JSONPath 表达式提取响应中的特定字段**暴露给下游节点。

## Requirements

### 1. 固定的 response 输出（只读）

HTTP 节点执行后，输出 Schema 中必须包含一个固定的 `http_response` 字段：
- **Key**: `http_response`
- **Label**: `HTTP 响应`
- **Type**: `object`
- **System**: `true`（不可删除、不可修改）
- **Description**: `HTTP 响应的完整内容`

该字段由系统强制注入，不允许用户删除或修改。

### 2. 可选的 JSON 提取（用户配置）

用户可在**配置 Tab** 中填写 JSONPath 表达式，指定从 `http_response` 中提取的字段路径，例如：
- `$.data.items[*].name`
- `$.result`

当用户配置了提取表达式后，系统动态生成对应的提取字段到 outputSchema：
- **Key**: 提取表达式解析后的字段名（如 `extracted_data`）
- **Label**: 提取字段的显示名
- **Type**: `string`（提取结果统一转为字符串）
- **System**: `false`（用户可以删除，但不能修改 key/label）

### 3. Policy 约束

| 操作 | 允许 |
|------|------|
| 添加自定义输出字段 | ✅ 允许 |
| 删除用户添加的输出字段 | ✅ 允许 |
| 修改/删除 `http_response` | ❌ 禁止 |
| 修改/删除用户添加的提取字段 key/label | ❌ 禁止 |

即：`outputSchemaAdd: true`, `outputSchemaUpdate: true`，但在 `normalizeWorkflowNodes` 中强制保留 `http_response`。

### 4. 后端配合（Schema 更新）

同步更新 `node_template` 表中 HTTP 节点的 `initial_schema`：
```json
{
  "inputSchema": [],
  "outputSchema": [
    {
      "key": "http_response",
      "type": "object",
      "label": "HTTP 响应",
      "system": true,
      "description": "HTTP 响应的完整内容"
    }
  ]
}
```

同步更新 `default_schema_policy`：
```json
{
  "inputSchemaAdd": true,
  "inputSchemaUpdate": true,
  "outputSchemaAdd": true,
  "outputSchemaUpdate": true
}
```

## Technical Notes

### 修改文件

1. **`WorkflowEditorPage.tsx`** — `normalizeWorkflowNodes` 中增加 HTTP 节点的强制字段注入逻辑
2. **`WorkflowNode.tsx`** — 确认 Policy 和 outputSchema 正确传递
3. **`NodeConfigTabs.tsx`** — 在配置 Tab 中新增 JSONPath 提取器组件
4. **`01_init_schema.sql`** — 更新 HTTP 节点的 `initial_schema` 数据

### 关键实现点

**强制保留 system 字段**（normalizeWorkflowNodes）：
```typescript
if (data.nodeType === "HTTP") {
  const forcedOutputFields = [{
    key: "http_response",
    type: "object",
    label: "HTTP 响应",
    system: true,
    description: "HTTP 响应的完整内容",
  }];
  outputSchema = mergeRequiredFields(outputSchema, forcedOutputFields);
}
```

**JSONPath 提取器 UI**：在 NodeConfigTabs 的 config tab 中渲染，依赖 `userConfig.http_response_extractor` 字段，保存后触发 outputSchema 动态更新。

## Acceptance Criteria

- [ ] HTTP 节点展开后，输出 tab 中始终显示 `http_response` 字段（带"系统"标签）
- [ ] 用户无法删除或编辑 `http_response` 字段
- [ ] 用户可以在配置 tab 中填写 JSONPath 表达式
- [ ] 配置 JSONPath 后，输出 tab 中动态出现提取字段
- [ ] 用户可以添加/删除自定义输出字段
- [ ] 保存工作流后重新加载，`http_response` 仍然存在
- [ ] 数据库 SQL 同步更新
