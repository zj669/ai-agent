import { useCallback, useEffect, useRef, useState } from "react";
import { cn } from "../../../lib/utils";
import { mcpAdapter, type McpTool } from "../../mcp/api/mcpAdapter";
import type { NodeTemplateDTO } from "../../../shared/api/adapters/metadataAdapter";
import type {
  SchemaPolicy,
  FieldSchema,
  WorkflowNodeType,
  ReferenceNodeOption,
  PromptTemplateVariableOption,
} from "./WorkflowNode";
import VariableRefSelector, {
  type UpstreamVariable,
} from "./VariableRefSelector";
import FieldRenderer from "./FieldRenderer";

const inputClass = 'w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-sm text-slate-800 transition focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none';

/**
 * 从 JSONPath 表达式中提取字段名。
 * 例: "$.data.items" → "items" | "$.result" → "result" | "$.data[0].name" → "name"
 */
function parseJsonPathFieldName(path: string): string {
  const trimmed = path.trim();
  if (!trimmed || !trimmed.startsWith("$.")) return "新字段";
  const parts = trimmed.slice(2).split(".");
  const last = parts[parts.length - 1];
  return last.replace(/\[\d+\]/g, "") || "新字段";
}

export interface JsonExtractorField {
  key: string;
  label: string;
  path: string;
  type: string;
  system?: boolean;
}

function extractFieldsFromJsonPaths(paths: string[]): JsonExtractorField[] {
  return paths
    .map((path) => path.trim())
    .filter((path) => path.length > 0)
    .map((path) => {
      const fieldName = parseJsonPathFieldName(path);
      return {
        key: `extracted_${fieldName}`,
        label: `提取: ${fieldName}`,
        path,
        type: "string",
        system: true,
      };
    });
}

function HttpOutputSection({
  outputSchema,
  userConfig,
  onConfigChange,
  onOutputSchemaChange,
  canAddOutput,
  canUpdateOutput,
}: {
  outputSchema: FieldSchema[];
  userConfig: Record<string, unknown>;
  onConfigChange: (key: string, value: unknown) => void;
  onOutputSchemaChange?: (schema: FieldSchema[]) => void;
  canAddOutput: boolean;
  canUpdateOutput: boolean;
}) {
  const rawPaths = userConfig.httpResponseExtractPaths;
  const paths: string[] = Array.isArray(rawPaths)
    ? (rawPaths as string[]).filter((p): p is string => typeof p === "string" && p.trim().length > 0)
    : [];

  const httpResponseFields = outputSchema.filter((f) => f.key === "http_response");
  const userCustomFields = outputSchema.filter((f) => !f.system);

  const syncSchema = (fields: JsonExtractorField[]) => {
    if (!onOutputSchemaChange) return;
    onOutputSchemaChange([...httpResponseFields, ...fields, ...userCustomFields]);
  };

  const updatePaths = (next: string[]) => {
    onConfigChange("httpResponseExtractPaths", next);
    syncSchema(extractFieldsFromJsonPaths(next));
  };

  const addPath = () => updatePaths([...paths, `$.field_${Date.now()}`]);

  const updatePath = (index: number, value: string) => {
    const next = [...paths];
    next[index] = value;
    updatePaths(next);
  };

  const removePath = (index: number) => {
    const next = paths.filter((_, i) => i !== index);
    updatePaths(next);
  };

  return (
    <div className="space-y-3">
      {/* 系统输出字段 */}
      {httpResponseFields.map((field) => (
        <div
          key={field.key}
          className="rounded-lg bg-slate-100 px-3 py-2 flex items-center gap-2"
        >
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-medium text-slate-700">
                {field.label || field.key}
              </span>
              <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
                系统
              </span>
            </div>
            <div className="text-[10px] text-slate-400">
              {field.key} · {field.type}
              {field.description && ` — ${field.description}`}
            </div>
          </div>
        </div>
      ))}

      {/* JSONPath 提取配置 */}
      <div className="space-y-2">
        <div>
          <h4 className="text-xs font-semibold text-slate-500">JSON 字段提取</h4>
          <p className="mt-0.5 text-[11px] leading-4 text-slate-400">
            使用 JSONPath 表达式从 HTTP 响应中提取字段，供下游节点引用。
          </p>
        </div>

        {paths.length === 0 ? (
          <button
            type="button"
            className="w-full rounded-lg border border-dashed border-slate-300 py-2 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
            onClick={addPath}
          >
            + 添加提取字段
          </button>
        ) : (
          <>
            <div className="space-y-2">
              {paths.map((path, i) => (
                <div key={i} className="flex items-center gap-2">
                  <div className="flex-1 min-w-0">
                    <input
                      className={inputClass}
                      placeholder="$.data.fieldName"
                      value={path}
                      onChange={(e) => updatePath(i, e.target.value)}
                    />
                    {path && (
                      <div className="mt-1 text-[10px] text-slate-400">
                        提取为字段:{" "}
                        <span className="font-mono text-blue-500">
                          {parseJsonPathFieldName(path)}
                        </span>
                      </div>
                    )}
                  </div>
                  <button
                    type="button"
                    className="shrink-0 text-slate-400 hover:text-red-500 text-sm transition"
                    onClick={() => removePath(i)}
                    title="移除"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
            <button
              type="button"
              className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
              onClick={addPath}
            >
              + 添加提取字段
            </button>
          </>
        )}
      </div>

      {/* 用户自定义字段 */}
      {userCustomFields.map((field) => (
        <OutputFieldCard
          key={field.key}
          field={field}
          canUpdate={canUpdateOutput}
          canDelete={canAddOutput}
          onUpdate={(updated) => {
            const next = [...outputSchema];
            const idx = next.findIndex((f) => f.key === field.key);
            if (idx >= 0) next[idx] = updated;
            onOutputSchemaChange?.(next);
          }}
          onDelete={() => {
            const next = outputSchema.filter((f) => f.key !== field.key);
            onOutputSchemaChange?.(next);
          }}
        />
      ))}

      {canAddOutput && (
        <button
          className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
          onClick={() => {
            const newField: FieldSchema = {
              key: `field_${Date.now()}`,
              label: "新字段",
              type: "string",
            };
            onOutputSchemaChange?.([...outputSchema, newField]);
          }}
        >
          + 添加自定义输出字段
        </button>
      )}
    </div>
  );
}

// ─── ToolSection: MCP 工具选择 + 输入输出 Schema ─────────────────────────────

/** 将 MCP 工具的 JSON Schema input 字符串解析为 FieldSchema[] */
function parseToolInputSchema(inputSchemaStr: string): FieldSchema[] {
  if (!inputSchemaStr) return [];
  try {
    const schema = JSON.parse(inputSchemaStr);
    if (!schema || schema.type !== "object" || !schema.properties) return [];
    return Object.entries(schema.properties).map(([key, prop]) => {
      const p = prop as { type?: string; description?: string; default?: unknown };
      return {
        key,
        label: p.description || key,
        type: p.type || "string",
        required: Array.isArray(schema.required) && schema.required.includes(key),
        defaultValue: p.default,
        sourceRef: "",
        system: true,
      } satisfies FieldSchema;
    });
  } catch {
    return [];
  }
}

/** 返回 TOOL 节点的固定输出字段 — 工具执行后的原始返回值 */
function getToolOutputSchema(): FieldSchema[] {
  return [{
    key: "tool_response",
    type: "string",
    label: "工具响应",
    system: true,
    description: "MCP 工具执行后的原始返回值",
  }];
}

interface SelectedToolInfo {
  serverId: number;
  serverName: string;
  toolName: string;
  fullName: string;
  description: string;
  inputSchemaStr: string;
}

function ToolSection({
  userConfig,
  inputSchema,
  outputSchema,
  onConfigChange,
  onInputSchemaChange,
  onOutputSchemaChange,
  upstreamVariables,
}: {
  userConfig: Record<string, unknown>;
  inputSchema: FieldSchema[];
  outputSchema: FieldSchema[];
  onConfigChange: (key: string, value: unknown) => void;
  onInputSchemaChange?: (schema: FieldSchema[]) => void;
  onOutputSchemaChange?: (schema: FieldSchema[]) => void;
  upstreamVariables: UpstreamVariable[];
}) {
  const [tools, setTools] = useState<McpTool[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");

  const selected = userConfig.selectedTool as SelectedToolInfo | undefined;

  useEffect(() => {
    setLoading(true);
    mcpAdapter.getAllTools()
      .then((data) => {
        setTools(data);
      })
      .finally(() => setLoading(false));
  }, []);

  const filteredTools = tools.filter((t) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return t.toolName.toLowerCase().includes(q)
      || t.serverName.toLowerCase().includes(q)
      || t.fullName.toLowerCase().includes(q);
  });

  const handleSelectTool = useCallback((tool: McpTool | null) => {
    if (!tool) {
      onConfigChange("selectedTool", null);
      onInputSchemaChange?.([]);
      onOutputSchemaChange?.([]);
      return;
    }
    const toolInfo: SelectedToolInfo = {
      serverId: tool.serverId,
      serverName: tool.serverName,
      toolName: tool.toolName,
      fullName: tool.fullName,
      description: tool.description,
      inputSchemaStr: tool.inputSchema,
    };
    const newInputSchema = parseToolInputSchema(tool.inputSchema);
    const newOutputSchema = getToolOutputSchema();
    onConfigChange("selectedTool", toolInfo);
    onInputSchemaChange?.(newInputSchema);
    onOutputSchemaChange?.(newOutputSchema);
  }, [onConfigChange, onInputSchemaChange, onOutputSchemaChange]);

  const handleUpdateInputField = (key: string, updated: Partial<FieldSchema>) => {
    const next = inputSchema.map((f) => f.key === key ? { ...f, ...updated } : f);
    onInputSchemaChange?.(next);
  };

  const handleClearInputField = (key: string) => {
    const next = inputSchema.map((f) =>
      f.key === key ? { ...f, sourceRef: "", defaultValue: undefined } : f,
    );
    onInputSchemaChange?.(next);
  };

  return (
    <div className="space-y-4">
      {/* 工具选择 */}
      <div className="space-y-2">
        <label className="text-xs font-semibold text-slate-500">选择工具</label>
        <input
          className={inputClass}
          placeholder="搜索工具名称或服务器..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className={inputClass}
          value={selected?.fullName ?? ""}
          onChange={(e) => {
            const tool = tools.find((t) => t.fullName === e.target.value) ?? null;
            handleSelectTool(tool);
          }}
          disabled={loading}
        >
          <option value="">{loading ? "加载中..." : "请选择工具"}</option>
          {filteredTools.map((tool) => (
            <option key={`${tool.serverId}-${tool.toolName}`} value={tool.fullName}>
              {tool.serverName} / {tool.toolName}
            </option>
          ))}
          {!loading && filteredTools.length === 0 && search && (
            <option value="" disabled>未找到匹配的工具</option>
          )}
        </select>
      </div>

      {/* 工具说明 */}
      {selected && (
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
          <div className="text-xs font-medium text-slate-700">{selected.fullName}</div>
          <div className="mt-1 text-[11px] text-slate-400 leading-4">
            {selected.description || "无描述"}
          </div>
        </div>
      )}

      {/* 输入参数说明 */}
      {selected && (
        <div className="space-y-2">
          <div className="text-xs font-semibold text-slate-500">
            工具输入参数
            <span className="ml-2 font-normal text-slate-400">
              （可引用上游变量或填写默认值）
            </span>
          </div>
          {inputSchema.length === 0 ? (
            <p className="text-xs text-slate-400">该工具无输入参数</p>
          ) : (
            inputSchema.map((field) => (
              <div
                key={field.key}
                className="rounded-lg bg-slate-100 px-3 py-2 space-y-1.5"
              >
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-medium text-slate-700">
                    {field.label || field.key}
                  </span>
                  <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
                    系统
                  </span>
                  <span className="text-[10px] text-slate-400">{field.type}</span>
                  {field.required && (
                    <span className="text-[10px] text-red-400">*</span>
                  )}
                </div>
                {field.description && (
                  <div className="text-[10px] text-slate-400 leading-3">
                    {field.description}
                  </div>
                )}
                <VariableRefSelector
                  value={field.sourceRef ?? ""}
                  variables={upstreamVariables}
                  onSelect={(ref) => handleUpdateInputField(field.key, { sourceRef: ref, defaultValue: undefined })}
                  onClear={() => handleClearInputField(field.key)}
                />
                {!field.sourceRef && (
                  <input
                    className="w-full rounded border border-slate-300 px-2 py-1 text-xs placeholder:text-slate-300"
                    placeholder="默认值"
                    value={field.defaultValue != null ? String(field.defaultValue) : ""}
                    onChange={(e) => handleUpdateInputField(field.key, { defaultValue: e.target.value || undefined })}
                  />
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* 输出字段说明 */}
      {selected && (
        <div className="space-y-2">
          <div className="text-xs font-semibold text-slate-500">
            工具输出字段
            <span className="ml-2 font-normal text-slate-400">（只读，由工具返回）</span>
          </div>
          {outputSchema.length === 0 ? (
            <p className="text-xs text-slate-400">该工具无定义输出（由执行结果决定）</p>
          ) : (
            outputSchema.map((field) => (
              <div
                key={field.key}
                className="rounded-lg bg-slate-100 px-3 py-2 flex items-center gap-2"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-medium text-slate-700">
                      {field.label || field.key}
                    </span>
                    <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
                      系统
                    </span>
                  </div>
                  <div className="text-[10px] text-slate-400">
                    {field.key} · {field.type}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

interface NodeConfigTabsProps {
  nodeType?: WorkflowNodeType;
  template: NodeTemplateDTO | null;
  policy?: SchemaPolicy;
  inputSchema: FieldSchema[];
  outputSchema: FieldSchema[];
  userConfig: Record<string, unknown>;
  upstreamVariables?: UpstreamVariable[];
  contextReferenceNodes?: ReferenceNodeOption[];
  promptTemplateVariables?: PromptTemplateVariableOption[];
  onConfigChange: (key: string, value: unknown) => void;
  onInputSchemaChange?: (schema: FieldSchema[]) => void;
  onOutputSchemaChange?: (schema: FieldSchema[]) => void;
}

type TabKey = "input" | "output" | "config";

const TABS: { key: TabKey; label: string }[] = [
  { key: "input", label: "输入" },
  { key: "output", label: "输出" },
  { key: "config", label: "配置" },
];

const FIELD_TYPES = ["string", "number", "boolean", "array", "object"];
const KNOWLEDGE_QUERY_FIELD: FieldSchema = {
  key: "query",
  label: "查询词",
  type: "string",
  system: true,
  required: true,
  sourceRef: "",
  description: "请选择上游节点的输出变量作为查询词",
};
const DEFAULT_LLM_PROMPT_TEMPLATE = "{{inputs.inputMessage}}";

function ContextReferenceSection({
  options,
  value,
  onChange,
}: {
  options: ReferenceNodeOption[];
  value: unknown;
  onChange: (nodeIds: string[]) => void;
}) {
  const selectedNodeIds = Array.isArray(value)
    ? value.filter(
        (item): item is string =>
          typeof item === "string" && item.trim().length > 0,
      )
    : [];

  const toggleNode = (nodeId: string) => {
    if (selectedNodeIds.includes(nodeId)) {
      onChange(selectedNodeIds.filter((id) => id !== nodeId));
      return;
    }
    onChange([...selectedNodeIds, nodeId]);
  };

  return (
    <div className="space-y-2">
      <div>
        <h4 className="text-xs font-semibold text-slate-500">参考节点</h4>
        <p className="mt-1 text-[11px] leading-4 text-slate-400">
          选择要注入到 LLM 系统提示词中的上游节点输出，支持多跳祖先节点。
        </p>
      </div>

      {options.length === 0 ? (
        <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-400">
          暂无可引用的上游节点
        </div>
      ) : (
        <div className="space-y-2">
          {options.map((option) => {
            const checked = selectedNodeIds.includes(option.nodeId);
            return (
              <label
                key={option.nodeId}
                className={cn(
                  "flex cursor-pointer items-start gap-2 rounded-lg border px-3 py-2 transition",
                  checked
                    ? "border-blue-200 bg-blue-50/70"
                    : "border-slate-200 bg-slate-50/70 hover:border-slate-300",
                )}
              >
                <input
                  type="checkbox"
                  className="mt-0.5 h-4 w-4 rounded border-slate-300 text-blue-600"
                  checked={checked}
                  onChange={() => toggleNode(option.nodeId)}
                  aria-label={`引用节点-${option.nodeId}`}
                />
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-medium text-slate-700">
                    {option.nodeName}
                  </div>
                  <div className="text-[11px] text-slate-400">
                    {option.nodeId} · {option.nodeType}
                  </div>
                </div>
              </label>
            );
          })}
        </div>
      )}

      {selectedNodeIds.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {selectedNodeIds.map((nodeId) => (
            <span
              key={nodeId}
              className="rounded bg-blue-100 px-2 py-1 text-[11px] text-blue-700"
            >
              {nodeId}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function PromptTemplateEditor({
  value,
  variables,
  onChange,
}: {
  value: string;
  variables: PromptTemplateVariableOption[];
  onChange: (value: string) => void;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const selectionRef = useRef({
    start: value.length,
    end: value.length,
  });
  const inputVariables = variables.filter(
    (variable) => variable.category === "inputs",
  );
  const nodeVariables = variables.filter(
    (variable) => variable.category === "node",
  );

  const syncSelection = () => {
    const textarea = textareaRef.current;
    if (!textarea) {
      selectionRef.current = {
        start: value.length,
        end: value.length,
      };
      return;
    }

    selectionRef.current = {
      start: textarea.selectionStart ?? value.length,
      end: textarea.selectionEnd ?? value.length,
    };
  };

  const insertTemplate = (template: string) => {
    const textarea = textareaRef.current;
    if (!textarea) {
      const suffix = value && !value.endsWith("\n") ? "\n" : "";
      onChange(`${value}${suffix}${template}`);
      return;
    }

    const { start, end } = selectionRef.current;
    const nextValue = `${value.slice(0, start)}${template}${value.slice(end)}`;
    onChange(nextValue);

    requestAnimationFrame(() => {
      textarea.focus();
      const cursor = start + template.length;
      textarea.setSelectionRange(cursor, cursor);
      selectionRef.current = {
        start: cursor,
        end: cursor,
      };
    });
  };

  const renderVariableSection = (
    title: string,
    items: PromptTemplateVariableOption[],
  ) => {
    if (items.length === 0) return null;
    return (
      <div className="space-y-2">
        <div className="text-[11px] font-semibold text-slate-500">{title}</div>
        <div className="space-y-2">
          {items.map((item) => (
            <button
              key={item.template}
              type="button"
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-left transition hover:border-blue-300 hover:bg-blue-50/50"
              onClick={() => insertTemplate(item.template)}
            >
              <div className="text-xs font-medium text-slate-700">
                {item.label}
              </div>
              <div className="mt-0.5 text-[11px] text-slate-400">
                {item.detail}
              </div>
            </button>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-3">
      <div className="space-y-1">
        <h4 className="text-xs font-semibold text-slate-500">Prompt 模板</h4>
        <p className="text-[11px] leading-4 text-slate-400">
          直接编写发送给 LLM 的用户提示词，可使用 {`{{}}`}{" "}
          引用全局输入或多跳上游节点输出。
        </p>
      </div>

      <textarea
        ref={textareaRef}
        className="mt-1 w-full rounded-md border border-slate-200 px-2.5 py-1.5 text-xs shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
        placeholder="请输入 Prompt 模板，例如：&#10;用户问题：{{inputs.query}}"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onSelect={syncSelection}
        onClick={syncSelection}
        onKeyUp={syncSelection}
        onFocus={syncSelection}
      />

      <div className="rounded-lg border border-slate-200 bg-slate-50/70 p-3 space-y-3">
        <div className="text-xs font-semibold text-slate-500">插入变量</div>
        {renderVariableSection("全局输入", inputVariables)}
        {renderVariableSection("祖先节点输出", nodeVariables)}
        {variables.length === 0 && (
          <div className="text-xs text-slate-400">暂无可插入的变量</div>
        )}
      </div>
    </div>
  );
}

function InputFieldCard({
  field,
  canUpdate,
  canDelete,
  upstreamVariables,
  onUpdate,
  onDelete,
}: {
  field: FieldSchema;
  canUpdate: boolean;
  canDelete: boolean;
  upstreamVariables: UpstreamVariable[];
  onUpdate: (updated: FieldSchema) => void;
  onDelete: () => void;
}) {
  const [editingMeta, setEditingMeta] = useState(false);
  const isSystem = field.system === true;
  const editable = canUpdate && !isSystem;
  const hasRef = !!field.sourceRef;

  return (
    <div
      className={cn(
        "rounded-lg px-3 py-2 space-y-1.5",
        isSystem ? "bg-slate-100" : "bg-slate-50",
      )}
    >
      <div className="group flex items-center gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-medium text-slate-700">
              {field.label || field.key}
            </span>
            {isSystem && (
              <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
                系统
              </span>
            )}
            {field.required && (
              <span className="text-red-400 text-[10px]">*</span>
            )}
            <span className="text-[10px] text-slate-400">{field.type}</span>
          </div>
        </div>
        <div className="hidden group-hover:flex items-center gap-1">
          {editable && (
            <button
              className="text-xs text-slate-400 hover:text-blue-500"
              onClick={() => setEditingMeta(!editingMeta)}
              title="编辑字段定义"
            >
              ✏️
            </button>
          )}
          {canDelete && !isSystem && (
            <button
              className="text-xs text-slate-400 hover:text-red-500"
              onClick={onDelete}
              title="删除"
            >
              🗑️
            </button>
          )}
        </div>
      </div>

      {editingMeta && editable && (
        <div className="rounded border border-blue-200 bg-blue-50/50 px-2 py-1.5 space-y-1.5">
          <div className="flex gap-2">
            <input
              className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs"
              placeholder="字段名"
              defaultValue={field.key}
              onBlur={(e) => onUpdate({ ...field, key: e.target.value })}
            />
            <input
              className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs"
              placeholder="标签"
              defaultValue={field.label}
              onBlur={(e) => onUpdate({ ...field, label: e.target.value })}
            />
          </div>
          <div className="flex gap-2 items-center">
            <select
              className="rounded border border-slate-300 px-2 py-1 text-xs"
              defaultValue={field.type}
              onChange={(e) => onUpdate({ ...field, type: e.target.value })}
            >
              {FIELD_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-1 text-xs text-slate-500">
              <input
                type="checkbox"
                defaultChecked={field.required ?? false}
                onChange={(e) =>
                  onUpdate({ ...field, required: e.target.checked })
                }
              />
              必填
            </label>
            <button
              className="ml-auto text-xs text-blue-500 hover:text-blue-700"
              onClick={() => setEditingMeta(false)}
            >
              完成
            </button>
          </div>
        </div>
      )}

      <div className="space-y-1">
        <VariableRefSelector
          value={field.sourceRef ?? ""}
          variables={upstreamVariables}
          onSelect={(ref) =>
            onUpdate({ ...field, sourceRef: ref, defaultValue: undefined })
          }
          onClear={() => onUpdate({ ...field, sourceRef: "" })}
        />

        {!hasRef && (
          <div>
            <input
              className="w-full rounded border border-slate-300 px-2 py-1 text-xs placeholder:text-slate-300"
              placeholder="输入默认值..."
              value={
                field.defaultValue != null ? String(field.defaultValue) : ""
              }
              onChange={(e) =>
                onUpdate({
                  ...field,
                  defaultValue: e.target.value || undefined,
                })
              }
            />
          </div>
        )}
      </div>
    </div>
  );
}

function OutputFieldCard({
  field,
  canUpdate,
  canDelete,
  onUpdate,
  onDelete,
}: {
  field: FieldSchema;
  canUpdate: boolean;
  canDelete: boolean;
  onUpdate: (updated: FieldSchema) => void;
  onDelete: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const isSystem = field.system === true;
  const editable = canUpdate && !isSystem;

  if (editing && editable) {
    return (
      <div className="rounded-lg border border-blue-200 bg-blue-50/50 px-3 py-2 space-y-2">
        <div className="flex gap-2">
          <input
            className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs"
            placeholder="字段名"
            defaultValue={field.key}
            onBlur={(e) => onUpdate({ ...field, key: e.target.value })}
          />
          <input
            className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs"
            placeholder="标签"
            defaultValue={field.label}
            onBlur={(e) => onUpdate({ ...field, label: e.target.value })}
          />
        </div>
        <div className="flex gap-2 items-center">
          <select
            className="rounded border border-slate-300 px-2 py-1 text-xs"
            defaultValue={field.type}
            onChange={(e) => onUpdate({ ...field, type: e.target.value })}
          >
            {FIELD_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <button
            className="ml-auto text-xs text-blue-500 hover:text-blue-700"
            onClick={() => setEditing(false)}
          >
            完成
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      className={cn(
        "group rounded-lg px-3 py-2 flex items-center gap-2",
        isSystem ? "bg-slate-100" : "bg-slate-50",
      )}
    >
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-slate-700">
            {field.label || field.key}
          </span>
          {isSystem && (
            <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
              系统
            </span>
          )}
        </div>
        <div className="text-xs text-slate-400">
          {field.key} · {field.type}
        </div>
      </div>
      <div className="hidden group-hover:flex items-center gap-1">
        {editable && (
          <button
            className="text-xs text-slate-400 hover:text-blue-500"
            onClick={() => setEditing(true)}
            title="编辑"
          >
            ✏️
          </button>
        )}
        {canDelete && !isSystem && (
          <button
            className="text-xs text-slate-400 hover:text-red-500"
            onClick={onDelete}
            title="删除"
          >
            🗑️
          </button>
        )}
      </div>
    </div>
  );
}

function NodeConfigTabs({
  nodeType,
  template,
  policy,
  inputSchema,
  outputSchema,
  userConfig,
  upstreamVariables = [],
  contextReferenceNodes = [],
  promptTemplateVariables = [],
  onConfigChange,
  onInputSchemaChange,
  onOutputSchemaChange,
}: NodeConfigTabsProps) {
  const resolvedNodeType =
    nodeType ??
    (template?.typeCode?.toUpperCase() as WorkflowNodeType | undefined);
  const isLlmNode = resolvedNodeType === "LLM";
  const isHttpNode = resolvedNodeType === "HTTP";
  const isToolNode = resolvedNodeType === "TOOL";
  const hasConfigTab =
    (template &&
      template.configFieldGroups &&
      template.configFieldGroups.length > 0) ||
    isLlmNode || isHttpNode || isToolNode;
  const availableTabs = hasConfigTab
    ? TABS
    : TABS.filter((t) => t.key !== "config");
  const [activeTab, setActiveTab] = useState<TabKey>(
    hasConfigTab ? "config" : "input",
  );

  const canAddInput = policy?.inputSchemaAdd !== false;
  const canAddOutput = policy?.outputSchemaAdd !== false;
  const canUpdateInput = policy?.inputSchemaUpdate !== false;
  const canUpdateOutput = policy?.outputSchemaUpdate !== false;
  const isKnowledgeNode = resolvedNodeType === "KNOWLEDGE";
  const hasKnowledgeQuery = inputSchema.some((field) => field.key === "query");
  const [showLlmAdvancedInputs, setShowLlmAdvancedInputs] = useState(false);
  const visibleConfigGroups =
    template?.configFieldGroups
      .map((group) => ({
        ...group,
        fields: group.fields.filter(
          (field) => !(isLlmNode && field.fieldKey === "contextRefNodes"),
        ),
      }))
      .filter((group) => group.fields.length > 0) ?? [];

  const handleAddField = (target: "input" | "output") => {
    if (target === "input" && isKnowledgeNode) {
      if (hasKnowledgeQuery) return;
      onInputSchemaChange?.([...inputSchema, { ...KNOWLEDGE_QUERY_FIELD }]);
      return;
    }

    const newField: FieldSchema = {
      key: `field_${Date.now()}`,
      label: "新字段",
      type: "string",
    };
    if (target === "input") onInputSchemaChange?.([...inputSchema, newField]);
    else onOutputSchemaChange?.([...outputSchema, newField]);
  };

  const handleUpdateInputField = (index: number, updated: FieldSchema) => {
    const next = [...inputSchema];
    next[index] = updated;
    onInputSchemaChange?.(next);
  };

  const handleUpdateOutputField = (index: number, updated: FieldSchema) => {
    const next = [...outputSchema];
    next[index] = updated;
    onOutputSchemaChange?.(next);
  };

  const handleDeleteField = (target: "input" | "output", index: number) => {
    if (target === "input")
      onInputSchemaChange?.(inputSchema.filter((_, i) => i !== index));
    else onOutputSchemaChange?.(outputSchema.filter((_, i) => i !== index));
  };

  return (
    <div className="flex flex-col">
      <div role="tablist" className="flex border-b border-slate-100 px-2 pt-2 gap-4">
        {availableTabs.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            className={cn(
              "pb-2 text-xs font-semibold transition-all relative",
              activeTab === tab.key
                ? "text-blue-600 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600 after:rounded-t-sm"
                : "text-slate-500 hover:text-slate-800",
            )}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="nowheel max-h-72 overflow-y-auto p-3">
        {activeTab === "input" &&
          (isLlmNode ? (
            <div className="space-y-3">
              <PromptTemplateEditor
                value={
                  typeof userConfig.userPromptTemplate === "string"
                    ? userConfig.userPromptTemplate
                    : DEFAULT_LLM_PROMPT_TEMPLATE
                }
                variables={promptTemplateVariables}
                onChange={(nextValue) =>
                  onConfigChange("userPromptTemplate", nextValue)
                }
              />

              <div className="rounded-lg border border-slate-200 bg-slate-50/70">
                <button
                  type="button"
                  className="flex w-full items-center justify-between px-3 py-2 text-left"
                  onClick={() => setShowLlmAdvancedInputs((value) => !value)}
                >
                  <div>
                    <div className="text-xs font-semibold text-slate-600">
                      高级映射
                    </div>
                    <div className="mt-0.5 text-[11px] text-slate-400">
                      兼容历史工作流或需要手动维护 inputSchema 时使用
                    </div>
                  </div>
                  <span className="text-xs text-slate-400">
                    {showLlmAdvancedInputs ? "收起" : "展开"}
                  </span>
                </button>

                {showLlmAdvancedInputs && (
                  <div className="space-y-2 border-t border-slate-200 p-3">
                    {inputSchema.length === 0 ? (
                      <p className="text-xs text-slate-400">暂无输入字段</p>
                    ) : (
                      inputSchema.map((field, i) => (
                        <InputFieldCard
                          key={field.key}
                          field={field}
                          canUpdate={canUpdateInput}
                          canDelete={canAddInput}
                          upstreamVariables={upstreamVariables}
                          onUpdate={(updated) =>
                            handleUpdateInputField(i, updated)
                          }
                          onDelete={() => handleDeleteField("input", i)}
                        />
                      ))
                    )}
                    {canAddInput && (
                      <button
                        className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
                        onClick={() => handleAddField("input")}
                      >
                        + 添加输入字段
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              {inputSchema.length === 0 ? (
                <p className="text-xs text-slate-400">暂无输入字段</p>
              ) : (
                inputSchema.map((field, i) => (
                  <InputFieldCard
                    key={field.key}
                    field={field}
                    canUpdate={canUpdateInput}
                    canDelete={canAddInput}
                    upstreamVariables={upstreamVariables}
                    onUpdate={(updated) => handleUpdateInputField(i, updated)}
                    onDelete={() => handleDeleteField("input", i)}
                  />
                ))
              )}
              {canAddInput && (!isKnowledgeNode || !hasKnowledgeQuery) && (
                <button
                  className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
                  onClick={() => handleAddField("input")}
                >
                  {isKnowledgeNode ? "+ 恢复查询词字段" : "+ 添加输入字段"}
                </button>
              )}
            </div>
          ))}

        {activeTab === "output" && (
          <div className="space-y-2">
            {isHttpNode ? (
              <HttpOutputSection
                outputSchema={outputSchema}
                userConfig={userConfig}
                onConfigChange={onConfigChange}
                onOutputSchemaChange={onOutputSchemaChange}
                canAddOutput={canAddOutput}
                canUpdateOutput={canUpdateOutput}
              />
            ) : isToolNode ? (
              outputSchema.length === 0 ? (
                <p className="text-xs text-slate-400">请在「配置」中选择工具后查看输出字段</p>
              ) : (
                outputSchema.map((field) => (
                  <div
                    key={field.key}
                    className="rounded-lg bg-slate-100 px-3 py-2 flex items-center gap-2"
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-medium text-slate-700">
                          {field.label || field.key}
                        </span>
                        <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">
                          系统
                        </span>
                      </div>
                      <div className="text-[10px] text-slate-400">
                        {field.key} · {field.type}
                      </div>
                    </div>
                  </div>
                ))
              )
            ) : outputSchema.length === 0 ? (
              <p className="text-xs text-slate-400">暂无输出字段</p>
            ) : (
              outputSchema.map((field, i) => (
                <OutputFieldCard
                  key={field.key}
                  field={field}
                  canUpdate={canUpdateOutput}
                  canDelete={canAddOutput}
                  onUpdate={(updated) => handleUpdateOutputField(i, updated)}
                  onDelete={() => handleDeleteField("output", i)}
                />
              ))
            )}
            {!isHttpNode && !isToolNode && canAddOutput && (
              <button
                className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition"
                onClick={() => handleAddField("output")}
              >
                + 添加输出字段
              </button>
            )}
          </div>
        )}

        {activeTab === "config" && hasConfigTab && (
          <div className="space-y-4">
            {visibleConfigGroups.map((group) => (
              <div key={group.groupName}>
                <h4 className="mb-2 text-xs font-semibold text-slate-500">
                  {group.groupName}
                </h4>
                <div className="space-y-3">
                  {group.fields.map((field) => (
                    <FieldRenderer
                      key={field.fieldKey}
                      field={field}
                      value={userConfig[field.fieldKey]}
                      onChange={onConfigChange}
                    />
                  ))}
                </div>
              </div>
            ))}
            {isLlmNode && (
              <ContextReferenceSection
                options={contextReferenceNodes}
                value={userConfig.contextRefNodes}
                onChange={(nodeIds) =>
                  onConfigChange("contextRefNodes", nodeIds)
                }
              />
            )}
            {isToolNode && (
              <ToolSection
                userConfig={userConfig}
                inputSchema={inputSchema}
                outputSchema={outputSchema}
                onConfigChange={onConfigChange}
                onInputSchemaChange={onInputSchemaChange}
                onOutputSchemaChange={onOutputSchemaChange}
                upstreamVariables={upstreamVariables}
              />
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default NodeConfigTabs;
