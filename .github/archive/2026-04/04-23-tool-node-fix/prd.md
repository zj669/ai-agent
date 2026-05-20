# 优化 TOOL 节点 — MCP 工具选择 + 输入输出 Schema 暴露

## Goal

修复 TOOL 节点（工具节点）的前端工作流编辑器配置体验。目标：

- 用户可以从 MCP 服务列表中选择一个**工具**
- 选中后，自动将工具定义的 **inputSchema** 和 **outputSchema** 暴露到节点配置界面
- **输入 Schema**：支持用户引用上游节点的输出变量作为输入值
- **输出 Schema**：固定只读展示，不允许用户修改

## Requirements

### 1. 工具选择器

在节点**配置 Tab** 中新增工具选择下拉框：
- 从 `mcpAdapter.getAllTools()` 获取所有已连接 MCP 服务器的工具列表
- 显示格式：`{serverName} / {toolName}` — 如 `文件系统 / read_file`
- 支持按工具名或服务器名搜索过滤
- 选中工具后，记录 `userConfig.selectedTool`（包含 serverId、toolName）

### 2. 输入 Schema 展示与绑定

选中工具后，自动解析工具的 `inputSchema`（JSON Schema 格式）生成 `inputSchema`：
- 每个参数字段显示为一行
- **字段属性**：key（参数名）、label（显示名）、type（类型）、required（是否必填）、description
- **默认值填入**：用户可以直接填写默认值（字符串）
- **上游引用**：使用 `VariableRefSelector` 从上游节点选择输出变量作为输入值
- **Policy 约束**：`inputSchemaAdd: true`（允许添加额外输入字段）、`inputSchemaUpdate: true`

### 3. 输出 Schema 展示

选中工具后，自动解析工具的 `outputSchema` 生成 `outputSchema`：
- 每个输出字段显示为一行（key、label、type）
- **只读展示**：字段属性不可编辑（不可修改 key/label/type），不可删除
- **Policy 约束**：`outputSchemaAdd: false`、`outputSchemaUpdate: false`

### 4. 切换工具时重置 Schema

用户重新选择工具时：
- 清空之前的 inputSchema/outputSchema
- 用新工具的 schema 重新填充
- 如果用户有手动添加的输入字段（不在工具原始 schema 中），保留这些字段

### 5. Policy 默认值

TOOL 节点默认 policy：
```json
{
  "inputSchemaAdd": true,
  "outputSchemaAdd": false,
  "inputSchemaUpdate": true,
  "outputSchemaUpdate": false
}
```

## Technical Notes

### 数据来源

MCP 工具 API (`mcpAdapter.getAllTools()`) 返回：
```typescript
interface McpTool {
  serverId: number;
  serverName: string;
  toolName: string;
  fullName: string;
  description: string;
  inputSchema: string; // JSON string of JSON Schema
}
```

### inputSchema 解析

工具的 `inputSchema` 是 JSON Schema 格式，需解析为 `FieldSchema[]`：
```typescript
interface JSONSchemaProperty {
  type: string;
  description?: string;
  default?: unknown;
}

interface JSONSchema {
  type: "object";
  properties: Record<string, JSONSchemaProperty>;
  required?: string[];
}

// 转换为 FieldSchema
const fields: FieldSchema[] = Object.entries(schema.properties).map(([key, prop]) => ({
  key,
  label: prop.description || key,
  type: prop.type || "string",
  required: schema.required?.includes(key) ?? false,
  defaultValue: prop.default,
  sourceRef: "",
  system: false,
}));
```

### 修改文件

1. **`WorkflowEditorPage.tsx`** — `normalizeWorkflowNodes` 中 TOOL 节点默认 schema 注入
2. **`NodeConfigTabs.tsx`** — 新增 `ToolSchemaSection` 组件（工具选择 + schema 展示）
3. **`mcpAdapter.ts`** — 确认 `getAllTools` 接口存在
4. **`01_init_schema.sql`** — 更新 TOOL 节点 default_schema_policy

## Acceptance Criteria

- [ ] TOOL 节点展开后，配置 Tab 显示 MCP 工具选择下拉框
- [ ] 选择工具后，输入 Tab 自动出现该工具的输入参数字段
- [ ] 选择工具后，输出 Tab 自动出现该工具的输出字段（只读）
- [ ] 输入字段可以引用上游节点输出
- [ ] 输出字段不可编辑、不可删除
- [ ] 切换工具后 schema 正确重置
- [ ] 保存/重载后工具选择和 schema 状态保留
- [ ] lint + typecheck 通过
