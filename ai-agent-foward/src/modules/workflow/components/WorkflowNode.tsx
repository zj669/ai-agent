import { useCallback, useEffect, useMemo, useRef } from "react";
import { Handle, Position, useReactFlow } from "@xyflow/react";
import { cn } from "../../../lib/utils";
import { useEditorStore } from "../stores/useEditorStore";
import { NodeTargetHandle, NodeSourceHandle } from "./NodeHandle";
import NodeConfigTabs from "./NodeConfigTabs";
import ConditionBranchEditor from "./ConditionBranchEditor";
import type { UpstreamVariable } from "./VariableRefSelector";
import type {
  ConditionConfig,
  Branch as ConditionBranch,
} from "./ConditionBranchEditor";

export type WorkflowNodeType =
  | "START"
  | "END"
  | "LLM"
  | "CONDITION"
  | "TOOL"
  | "HTTP"
  | "KNOWLEDGE";

export type Branch = { id: string; name: string };

export interface SchemaPolicy {
  inputSchemaAdd?: boolean;
  outputSchemaAdd?: boolean;
  inputSchemaUpdate?: boolean;
  outputSchemaUpdate?: boolean;
}

export interface FieldSchema {
  key: string;
  label: string;
  type: string;
  description?: string;
  required?: boolean;
  defaultValue?: unknown;
  sourceRef?: string;
  system?: boolean;
}

export interface ReferenceNodeOption {
  nodeId: string;
  nodeName: string;
  nodeType: WorkflowNodeType;
}

export interface PromptTemplateVariableOption {
  label: string;
  detail: string;
  template: string;
  category: "inputs" | "node";
}

export type WorkflowNodeData = {
  label: string;
  nodeType: WorkflowNodeType;
  branches?: Branch[];
  policy?: SchemaPolicy;
  inputSchema?: FieldSchema[];
  outputSchema?: FieldSchema[];
  userConfig?: Record<string, unknown>;
};

const NODE_STYLES: Record<
  WorkflowNodeType,
  { bg: string; border: string; icon: string; accent: string }
> = {
  START: {
    bg: "bg-green-50",
    border: "border-green-300",
    icon: "▶",
    accent: "text-green-600",
  },
  END: {
    bg: "bg-red-50",
    border: "border-red-300",
    icon: "■",
    accent: "text-red-600",
  },
  LLM: {
    bg: "bg-blue-50",
    border: "border-blue-300",
    icon: "🧠",
    accent: "text-blue-600",
  },
  CONDITION: {
    bg: "bg-amber-50",
    border: "border-amber-300",
    icon: "🔀",
    accent: "text-amber-600",
  },
  TOOL: {
    bg: "bg-emerald-50",
    border: "border-emerald-300",
    icon: "🔧",
    accent: "text-emerald-600",
  },
  HTTP: {
    bg: "bg-violet-50",
    border: "border-violet-300",
    icon: "🌐",
    accent: "text-violet-600",
  },
  KNOWLEDGE: {
    bg: "bg-teal-50",
    border: "border-teal-300",
    icon: "📚",
    accent: "text-teal-600",
  },
};

const NODE_TYPE_LABELS: Record<WorkflowNodeType, string> = {
  START: "开始",
  END: "结束",
  LLM: "LLM",
  CONDITION: "条件",
  TOOL: "工具",
  HTTP: "HTTP",
  KNOWLEDGE: "知识库",
};

interface WorkflowNodeProps {
  id: string;
  data: unknown;
  selected: boolean;
}

function WorkflowNode({ id, data, selected }: WorkflowNodeProps) {
  const nodeData = data as WorkflowNodeData;
  const style = NODE_STYLES[nodeData.nodeType] ?? NODE_STYLES.TOOL;
  const isStart = nodeData.nodeType === "START";
  const isEnd = nodeData.nodeType === "END";
  const isCondition = nodeData.nodeType === "CONDITION";
  const canExpand = true;
  const hasSchema =
    (nodeData.inputSchema && nodeData.inputSchema.length > 0) ||
    (nodeData.outputSchema && nodeData.outputSchema.length > 0) ||
    nodeData.policy;

  const expandedNodeId = useEditorStore((s) => s.expandedNodeId);
  const toggleNodeExpand = useEditorStore((s) => s.toggleNodeExpand);
  const nodeTemplates = useEditorStore((s) => s.nodeTemplates);
  const markDirty = useEditorStore((s) => s.markDirty);

  const isExpanded = expandedNodeId === id;
  const template = nodeTemplates.find((t) => t.typeCode === nodeData.nodeType);
  const { setNodes, setEdges, getEdges, getNodes } = useReactFlow();

  const canDelete = !isStart && !isEnd;

  // 计算上游节点的输出变量（用于输入字段的引用选择）
  const upstreamVariables = useMemo<UpstreamVariable[]>(() => {
    if (!isExpanded) return [];
    const edges = getEdges();
    const nodes = getNodes();
    // 找到所有指向当前节点的边的 source 节点
    const upstreamNodeIds = new Set(
      edges.filter((e) => e.target === id).map((e) => e.source),
    );
    const vars: UpstreamVariable[] = [];
    for (const n of nodes) {
      if (!upstreamNodeIds.has(n.id)) continue;
      const nd = n.data as unknown as WorkflowNodeData;
      const outputs = nd.outputSchema ?? [];
      for (const field of outputs) {
        vars.push({
          nodeId: n.id,
          nodeName: nd.label,
          nodeType: nd.nodeType,
          fieldKey: field.key,
          fieldLabel: field.label ?? field.key,
          fieldType: field.type,
          ref: `${n.id}.output.${field.key}`,
        });
      }
    }
    return vars;
  }, [id, isExpanded, getEdges, getNodes]);

  const contextReferenceNodes = useMemo<ReferenceNodeOption[]>(() => {
    if (!isExpanded) return [];

    const edges = getEdges();
    const nodes = getNodes();
    const incomingMap = new Map<string, string[]>();

    for (const edge of edges) {
      const sources = incomingMap.get(edge.target) ?? [];
      sources.push(edge.source);
      incomingMap.set(edge.target, sources);
    }

    const visited = new Set<string>();
    const stack = [...(incomingMap.get(id) ?? [])];

    while (stack.length > 0) {
      const nodeId = stack.pop();
      if (!nodeId || visited.has(nodeId)) continue;
      visited.add(nodeId);
      const parents = incomingMap.get(nodeId) ?? [];
      for (const parentId of parents) {
        if (!visited.has(parentId)) {
          stack.push(parentId);
        }
      }
    }

    return nodes
      .filter((node) => visited.has(node.id) && node.id !== id)
      .map((node) => {
        const data = node.data as unknown as WorkflowNodeData;
        return {
          nodeId: node.id,
          nodeName: data.label,
          nodeType: data.nodeType,
        };
      });
  }, [id, isExpanded, getEdges, getNodes]);

  const promptTemplateVariables = useMemo<
    PromptTemplateVariableOption[]
  >(() => {
    if (!isExpanded) return [];

    const edges = getEdges();
    const nodes = getNodes();
    const nodeMap = new Map(
      nodes.map((node) => [node.id, node.data as unknown as WorkflowNodeData]),
    );
    const incomingMap = new Map<string, string[]>();

    for (const edge of edges) {
      const sources = incomingMap.get(edge.target) ?? [];
      sources.push(edge.source);
      incomingMap.set(edge.target, sources);
    }

    const visited = new Set<string>();
    const stack = [...(incomingMap.get(id) ?? [])];

    while (stack.length > 0) {
      const nodeId = stack.pop();
      if (!nodeId || visited.has(nodeId)) continue;
      visited.add(nodeId);
      const parents = incomingMap.get(nodeId) ?? [];
      for (const parentId of parents) {
        if (!visited.has(parentId)) {
          stack.push(parentId);
        }
      }
    }

    const variables: PromptTemplateVariableOption[] = [];
    const startNode = nodes.find(
      (node) => (node.data as WorkflowNodeData).nodeType === "START",
    );

    if (startNode) {
      const startData = startNode.data as WorkflowNodeData;
      for (const field of startData.outputSchema ?? []) {
        variables.push({
          label: field.label ?? field.key,
          detail: `inputs.${field.key}`,
          template: `{{inputs.${field.key}}}`,
          category: "inputs",
        });
      }
    }

    for (const ancestorId of visited) {
      const ancestorData = nodeMap.get(ancestorId);
      if (!ancestorData) continue;
      for (const field of ancestorData.outputSchema ?? []) {
        variables.push({
          label: `${ancestorData.label} · ${field.label ?? field.key}`,
          detail: `${ancestorId}.output.${field.key}`,
          template: `{{${ancestorId}.output.${field.key}}}`,
          category: "node",
        });
      }
    }

    return variables;
  }, [id, isExpanded, getEdges, getNodes]);

  const handleDelete = useCallback(
    (e: React.MouseEvent) => {
      e.stopPropagation();
      setEdges(
        getEdges().filter((edge) => edge.source !== id && edge.target !== id),
      );
      setNodes((nds) => nds.filter((n) => n.id !== id));
    },
    [id, setNodes, setEdges, getEdges],
  );

  const handleConfigChange = useCallback(
    (key: string, value: unknown) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as unknown as WorkflowNodeData;
          return {
            ...n,
            data: {
              ...d,
              userConfig: { ...(d.userConfig ?? {}), [key]: value },
            },
          };
        }),
      );
      markDirty();
    },
    [id, markDirty, setNodes],
  );

  const handleInputSchemaChange = useCallback(
    (schema: FieldSchema[]) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as unknown as WorkflowNodeData;
          return { ...n, data: { ...d, inputSchema: schema } };
        }),
      );
      markDirty();
    },
    [id, markDirty, setNodes],
  );

  const handleOutputSchemaChange = useCallback(
    (schema: FieldSchema[]) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as unknown as WorkflowNodeData;
          return { ...n, data: { ...d, outputSchema: schema } };
        }),
      );
      markDirty();
    },
    [id, markDirty, setNodes],
  );

  const handleConditionConfigChange = useCallback(
    (condConfig: ConditionConfig) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as unknown as WorkflowNodeData;
          // Sync branches for handle rendering
          const branches: Branch[] = condConfig.branches.map(
            (b: ConditionBranch) => ({
              id: b.id,
              name: b.name,
            }),
          );
          return {
            ...n,
            data: {
              ...d,
              branches,
              userConfig: {
                ...(d.userConfig ?? {}),
                conditionConfig: condConfig,
              },
            },
          };
        }),
      );
      markDirty();
    },
    [id, markDirty, setNodes],
  );

  // Build condition config from userConfig or defaults
  const conditionConfig: ConditionConfig | null = isCondition
    ? ((nodeData.userConfig?.conditionConfig as ConditionConfig) ?? {
        routingStrategy: "EXPRESSION",
        branches:
          (nodeData.branches ?? []).length > 0
            ? (nodeData.branches ?? []).map((b, i, arr) => ({
                id: b.id,
                name: b.name,
                type: (i === 0
                  ? "if"
                  : i === arr.length - 1
                    ? "else"
                    : "elseif") as "if" | "elseif" | "else",
                priority: i + 1,
                logic: "AND" as const,
                conditions: [
                  {
                    sourceRef: "",
                    operator: "EQUALS",
                    value: "",
                    valueType: "literal" as const,
                  },
                ],
              }))
            : [
                {
                  id: `branch-default-if`,
                  name: "如果",
                  type: "if" as const,
                  priority: 1,
                  logic: "AND" as const,
                  conditions: [
                    {
                      sourceRef: "",
                      operator: "EQUALS",
                      value: "",
                      valueType: "literal" as const,
                    },
                  ],
                },
                {
                  id: `branch-default-else`,
                  name: "否则",
                  type: "else" as const,
                  priority: 2,
                  logic: "AND" as const,
                  conditions: [],
                },
              ],
      })
    : null;

  // Sync default branches to nodeData on first render if missing
  const didInitBranches = useRef(false);
  useEffect(() => {
    if (
      isCondition &&
      conditionConfig &&
      (!nodeData.branches || nodeData.branches.length === 0) &&
      !didInitBranches.current
    ) {
      didInitBranches.current = true;
      handleConditionConfigChange(conditionConfig);
    }
  }, [
    isCondition,
    conditionConfig,
    nodeData.branches,
    handleConditionConfigChange,
  ]);

  return (
    <div
      className={cn(
        "rounded-xl border-2 shadow-sm transition-all",
        style.bg,
        style.border,
        selected && "ring-2 ring-blue-500 ring-offset-1",
        isExpanded ? "min-w-[320px]" : "min-w-[160px]",
      )}
    >
      {!isStart && <NodeTargetHandle handleId="target" />}

      <div className="flex items-center gap-2 px-3 py-2.5">
        <span className="text-base">{style.icon}</span>
        <div className="flex-1 min-w-0">
          <div className={cn("text-[10px] font-medium", style.accent)}>
            {NODE_TYPE_LABELS[nodeData.nodeType]}
          </div>
          <div className="truncate text-sm font-medium text-slate-800">
            {nodeData.label}
          </div>
        </div>
        {canExpand && (
          <button
            type="button"
            aria-label="展开配置"
            className="flex h-5 w-5 items-center justify-center rounded text-slate-400 transition hover:bg-slate-200 hover:text-slate-600"
            onClick={(e) => {
              e.stopPropagation();
              toggleNodeExpand(id);
            }}
          >
            <svg
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            >
              {isExpanded ? (
                <path d="M3 7.5L6 4.5L9 7.5" />
              ) : (
                <path d="M3 4.5L6 7.5L9 4.5" />
              )}
            </svg>
          </button>
        )}
        {canDelete && (
          <button
            type="button"
            aria-label="删除节点"
            className="flex h-5 w-5 items-center justify-center rounded text-slate-300 transition hover:bg-red-50 hover:text-red-500"
            onClick={handleDelete}
          >
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            >
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {isExpanded && canExpand && (
        <div className="border-t border-slate-200">
          {isCondition && conditionConfig ? (
            <div className="nowheel p-3 max-h-80 overflow-y-auto">
              <ConditionBranchEditor
                config={conditionConfig}
                onChange={handleConditionConfigChange}
              />
            </div>
          ) : template ? (
            <NodeConfigTabs
              nodeType={nodeData.nodeType}
              template={template}
              policy={nodeData.policy}
              inputSchema={nodeData.inputSchema ?? []}
              outputSchema={nodeData.outputSchema ?? []}
              userConfig={nodeData.userConfig ?? {}}
              upstreamVariables={upstreamVariables}
              contextReferenceNodes={contextReferenceNodes}
              promptTemplateVariables={promptTemplateVariables}
              onConfigChange={handleConfigChange}
              onInputSchemaChange={handleInputSchemaChange}
              onOutputSchemaChange={handleOutputSchemaChange}
            />
          ) : hasSchema ? (
            <NodeConfigTabs
              nodeType={nodeData.nodeType}
              template={null}
              policy={nodeData.policy}
              inputSchema={nodeData.inputSchema ?? []}
              outputSchema={nodeData.outputSchema ?? []}
              userConfig={nodeData.userConfig ?? {}}
              upstreamVariables={upstreamVariables}
              contextReferenceNodes={contextReferenceNodes}
              promptTemplateVariables={promptTemplateVariables}
              onConfigChange={handleConfigChange}
              onInputSchemaChange={handleInputSchemaChange}
              onOutputSchemaChange={handleOutputSchemaChange}
            />
          ) : null}
        </div>
      )}

      {isCondition && (
        <div className="border-t border-slate-200">
          {(nodeData.branches ?? []).map((branch) => (
            <div
              key={branch.id}
              className="relative flex items-center pl-3 pr-5 py-2 border-b border-slate-100 last:border-b-0"
            >
              <div className="flex items-center gap-1.5 flex-1 min-w-0">
                <div
                  className={cn(
                    "h-2 w-2 rounded-full shrink-0",
                    branch.name === "否则" ? "bg-slate-400" : "bg-amber-500",
                  )}
                />
                <span className="text-xs text-slate-600 truncate">
                  {branch.name}
                </span>
              </div>
              <Handle
                type="source"
                position={Position.Right}
                id={branch.id}
                className="!h-3 !w-3 !rounded-full !border-2 !border-amber-400 !bg-white"
              />
            </div>
          ))}
        </div>
      )}

      {!isEnd && !isCondition && <NodeSourceHandle handleId="source" />}
    </div>
  );
}

export default WorkflowNode;
