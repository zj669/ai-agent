import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type DragEvent,
} from "react";
import { useParams } from "react-router-dom";
import {
  ReactFlow,
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  MiniMap,
  Background,
  BackgroundVariant,
  type Connection,
  type Edge,
  type EdgeChange,
  type Node,
  type NodeChange,
  type ReactFlowInstance,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import {
  fetchWorkflowDetail,
  publishWorkflow,
  saveWorkflow,
  fetchNodeTemplates,
  type NodeTemplateDTO,
} from "../api/workflowService";
import { validateConnection } from "../validation/validateConnection";
import { validateWorkflowGraph } from "../validation/validateWorkflowGraph";
import WorkflowNode, {
  type WorkflowNodeData,
  type WorkflowNodeType,
  type SchemaPolicy,
  type FieldSchema,
  type Branch as WorkflowBranch,
} from "../components/WorkflowNode";
import type {
  Branch as UiConditionBranch,
  Condition as UiCondition,
  ConditionBranchConfig,
} from "../components/ConditionBranchEditor";
import { useEditorStore } from "../stores/useEditorStore";
import EditorHeader from "../components/EditorHeader";
import AgentConfigPanel from "../components/AgentConfigPanel";
import CanvasToolbar from "../components/CanvasToolbar";
import CustomEdge from "../components/CustomEdge";
import CustomConnectionLine from "../components/CustomConnectionLine";

const nodeTypes = { workflowNode: WorkflowNode };
const edgeTypes = { custom: CustomEdge };

const INITIAL_NODES: Node[] = [
  {
    id: "start",
    type: "workflowNode",
    position: { x: 50, y: 250 },
    data: { label: "开始节点", nodeType: "START" } satisfies WorkflowNodeData,
  },
  {
    id: "end",
    type: "workflowNode",
    position: { x: 600, y: 250 },
    data: { label: "结束节点", nodeType: "END" } satisfies WorkflowNodeData,
  },
];

const START_INPUT_FIELD: FieldSchema = {
  key: "inputMessage",
  type: "string",
  label: "用户输入",
  system: true,
  required: true,
  description: "用户发送的消息内容",
};

const START_OUTPUT_FIELDS: FieldSchema[] = [
  {
    key: "inputMessage",
    type: "string",
    label: "用户输入",
    system: true,
    description: "用户发送的消息，传递给下游节点",
  },
];

const KNOWLEDGE_QUERY_FIELD: FieldSchema = {
  key: "query",
  type: "string",
  label: "查询词",
  system: true,
  required: true,
  sourceRef: "",
  description: "请选择上游节点的输出变量作为查询词",
};

const KNOWLEDGE_OUTPUT_FIELD: FieldSchema = {
  key: "knowledge_list",
  type: "array",
  label: "知识列表",
  system: true,
};

const HTTP_RESPONSE_FIELD: FieldSchema = {
  key: "http_response",
  type: "object",
  label: "HTTP 响应",
  system: true,
  description: "HTTP 响应的完整内容",
};
const DEFAULT_LLM_PROMPT_TEMPLATE = "{{inputs.inputMessage}}";
const DEFAULT_BRANCH_PRIORITY = 2147483647;

const NO_VALUE_OPERATORS = new Set(["IS_EMPTY", "IS_NOT_EMPTY"]);

const OPERATOR_MAP: Record<string, string> = {
  EQUALS: "EQUALS",
  NOT_EQUALS: "NOT_EQUALS",
  CONTAINS: "CONTAINS",
  NOT_CONTAINS: "NOT_CONTAINS",
  IS_EMPTY: "IS_EMPTY",
  IS_NOT_EMPTY: "IS_NOT_EMPTY",
  STARTS_WITH: "STARTS_WITH",
  ENDS_WITH: "ENDS_WITH",
  GT: "GREATER_THAN",
  LT: "LESS_THAN",
  GTE: "GREATER_THAN_OR_EQUAL",
  LTE: "LESS_THAN_OR_EQUAL",
};

type BackendConditionItem = {
  leftOperand: string;
  operator: string;
  rightOperand?: unknown;
};

type BackendConditionGroup = {
  operator: "AND" | "OR";
  conditions: BackendConditionItem[];
};

type BackendConditionBranch = {
  priority: number;
  targetNodeId: string;
  description?: string;
  isDefault: boolean;
  conditionGroups?: BackendConditionGroup[];
};

function cloneFieldSchema(field: FieldSchema): FieldSchema {
  return { ...field };
}

function mergeTemplateSchema(
  existing: FieldSchema[] | undefined,
  templateSchema: FieldSchema[] | undefined,
): FieldSchema[] {
  if (!existing || existing.length === 0) {
    return (templateSchema ?? []).map(cloneFieldSchema);
  }

  const next = existing.map(cloneFieldSchema);
  const systemFields = (templateSchema ?? []).filter((field) => field.system);
  for (const field of systemFields) {
    if (!next.some((existingField) => existingField.key === field.key)) {
      next.push(cloneFieldSchema(field));
    }
  }
  return next;
}

function mergeRequiredFields(
  existing: FieldSchema[] | undefined,
  requiredFields: FieldSchema[],
): FieldSchema[] {
  const next = (existing ?? []).map(cloneFieldSchema);
  for (const requiredField of requiredFields) {
    const index = next.findIndex((field) => field.key === requiredField.key);
    if (index >= 0) {
      const preserved = next[index];
      next[index] = {
        ...preserved,
        ...requiredField,
        description: preserved.description ?? requiredField.description,
      };
    } else {
      next.push(cloneFieldSchema(requiredField));
    }
  }
  return next;
}

function normalizeKnowledgeInputSchema(
  existing: FieldSchema[] | undefined,
): FieldSchema[] {
  const fields = (existing ?? []).map(cloneFieldSchema);
  const queryIndex = fields.findIndex((field) => field.key === "query");

  if (queryIndex >= 0) {
    const existingQuery = fields[queryIndex];
    // 废弃的 start.output.query 引用已无效（START 节点不再输出 query），自动清空让用户重新选择
    const safeSourceRef =
      existingQuery.sourceRef === "start.output.query"
        ? ""
        : (existingQuery.sourceRef ?? "");
    fields[queryIndex] = {
      ...KNOWLEDGE_QUERY_FIELD,
      sourceRef: safeSourceRef,
    };
    return fields;
  }

  // 不存在 query 字段则新增（sourceRef 为空，由用户选择）
  return [...fields, { ...KNOWLEDGE_QUERY_FIELD }];
}

function normalizeContextRefNodes(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.filter(
    (item): item is string =>
      typeof item === "string" && item.trim().length > 0,
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function normalizeConditionLogic(value: unknown): "AND" | "OR" {
  return value === "OR" ? "OR" : "AND";
}

function normalizeConditionOperator(value: unknown): string {
  const raw = typeof value === "string" ? value : "EQUALS";
  return OPERATOR_MAP[raw] ?? raw;
}

function normalizeConditionReference(value: unknown): string {
  if (typeof value !== "string") return "";
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (trimmed.startsWith("inputs.") || trimmed.startsWith("nodes.")) {
    return trimmed;
  }
  if (trimmed === "start.output.inputMessage") {
    return "inputs.inputMessage";
  }

  const match = /^([^.]+)\.output\.(.+)$/.exec(trimmed);
  if (match) {
    return `nodes.${match[1]}.${match[2]}`;
  }

  return trimmed;
}

function branchLabelForIndex(isDefault: boolean, index: number): string {
  if (isDefault) return "否则";
  return index === 0 ? "如果" : "否则如果";
}

function conditionItemsToUiConditions(
  conditions: unknown,
): UiCondition[] {
  if (!Array.isArray(conditions)) return [];

  return conditions
    .map((item) => {
      if (!isRecord(item)) return null;
      const operator =
        typeof item.operator === "string" ? item.operator : "EQUALS";
      return {
        sourceRef:
          typeof item.leftOperand === "string" ? item.leftOperand : "",
        operator,
        value:
          typeof item.rightOperand === "string"
            ? item.rightOperand
            : item.rightOperand == null
              ? ""
              : String(item.rightOperand),
        valueType:
          typeof item.rightOperand === "string" &&
          (item.rightOperand.startsWith("inputs.") ||
            item.rightOperand.startsWith("nodes."))
            ? "ref"
            : "literal",
      } satisfies UiCondition;
    })
    .filter((item): item is UiCondition => item !== null);
}

function buildConditionConfigFromBackend(
  nodeId: string,
  userConfig: Record<string, unknown>,
  graphEdges: Edge[],
): ConditionBranchConfig | null {
  const branchesRaw = userConfig.branches;
  if (!Array.isArray(branchesRaw) || branchesRaw.length === 0) return null;

  const routingStrategy =
    userConfig.routingStrategy === "LLM" ? "LLM" : "EXPRESSION";
  let nonDefaultIndex = 0;
  const usedEdgeIds = new Set<string>();

  const branches = branchesRaw
    .map((raw) => {
      if (!isRecord(raw)) return null;
      const isDefault = raw.isDefault === true;
      const targetNodeId =
        typeof raw.targetNodeId === "string" ? raw.targetNodeId : "";
      const edge = graphEdges.find(
        (item) =>
          item.source === nodeId &&
          item.target === targetNodeId &&
          !usedEdgeIds.has(item.id),
      );
      if (edge) usedEdgeIds.add(edge.id);
      const branchIndex = nonDefaultIndex;
      if (!isDefault) nonDefaultIndex += 1;

      const groups = Array.isArray(raw.conditionGroups)
        ? raw.conditionGroups
        : [];
      const firstGroup = groups.find(isRecord);
      const conditions = firstGroup
        ? conditionItemsToUiConditions(firstGroup.conditions)
        : [];

      return {
        id:
          typeof edge?.sourceHandle === "string" && edge.sourceHandle
            ? edge.sourceHandle
            : `${isDefault ? "else" : "branch"}-${nodeId}-${branchIndex}`,
        name: branchLabelForIndex(isDefault, branchIndex),
        type: isDefault
          ? "else"
          : branchIndex === 0
            ? "if"
            : "elseif",
        priority:
          typeof raw.priority === "number"
            ? raw.priority
            : isDefault
              ? DEFAULT_BRANCH_PRIORITY
              : branchIndex,
        logic: firstGroup
          ? normalizeConditionLogic(firstGroup.operator)
          : "AND",
        conditions: isDefault
          ? []
          : conditions.length > 0
            ? conditions
            : [
                {
                  sourceRef: "",
                  operator: "EQUALS",
                  value: "",
                  valueType: "literal",
                },
              ],
        description:
          typeof raw.description === "string" ? raw.description : undefined,
      } satisfies UiConditionBranch;
    })
    .filter((item): item is UiConditionBranch => item !== null);

  return {
    routingStrategy,
    routingPrompt:
      typeof userConfig.routingPrompt === "string"
        ? userConfig.routingPrompt
        : undefined,
    llmConfigId:
      typeof userConfig.llmConfigId === "number"
        ? userConfig.llmConfigId
        : undefined,
    branches,
  };
}

function getConditionConfig(
  node: Node,
  graphEdges: Edge[],
): ConditionBranchConfig | null {
  const data = node.data as unknown as WorkflowNodeData;
  const userConfig = data.userConfig ?? {};
  const existing = userConfig.conditionConfig;
  if (isRecord(existing) && Array.isArray(existing.branches)) {
    return existing as unknown as ConditionBranchConfig;
  }
  return buildConditionConfigFromBackend(node.id, userConfig, graphEdges);
}

function getConditionBranchesForHandles(
  node: Node,
  graphEdges: Edge[],
): WorkflowBranch[] {
  const config = getConditionConfig(node, graphEdges);
  if (config?.branches?.length) {
    return config.branches.map((branch) => ({
      id: branch.id,
      name: branch.name,
    }));
  }

  const data = node.data as unknown as WorkflowNodeData;
  return data.branches ?? [];
}

function buildBackendConditionBranches(
  nodeId: string,
  config: ConditionBranchConfig,
  edges: Edge[],
): BackendConditionBranch[] {
  let priority = 0;

  return config.branches.map((branch) => {
    const targetNodeId =
      edges.find(
        (edge) => edge.source === nodeId && edge.sourceHandle === branch.id,
      )?.target ?? "";

    if (branch.type === "else") {
      return {
        priority: DEFAULT_BRANCH_PRIORITY,
        targetNodeId,
        description: branch.description,
        isDefault: true,
        conditionGroups: [],
      };
    }

    const result: BackendConditionBranch = {
      priority,
      targetNodeId,
      description: branch.description,
      isDefault: false,
    };
    if (config.routingStrategy !== "LLM") {
      result.conditionGroups = [
        {
          operator: normalizeConditionLogic(branch.logic),
          conditions: branch.conditions.map((condition) => {
            const operator = normalizeConditionOperator(condition.operator);
            const item: BackendConditionItem = {
              leftOperand: normalizeConditionReference(condition.sourceRef),
              operator,
            };
            if (!NO_VALUE_OPERATORS.has(operator)) {
              item.rightOperand =
                condition.valueType === "ref"
                  ? normalizeConditionReference(condition.value)
                  : condition.value;
            }
            return item;
          }),
        },
      ];
    }
    priority += 1;
    return result;
  });
}

function buildConditionUserConfig(
  node: Node,
  edges: Edge[],
): Record<string, unknown> {
  const data = node.data as unknown as WorkflowNodeData;
  const userConfig = { ...(data.userConfig ?? {}) };
  const conditionConfig = getConditionConfig(node, edges);
  if (!conditionConfig) return userConfig;

  const branches = buildBackendConditionBranches(
    node.id,
    conditionConfig,
    edges,
  );

  return {
    ...userConfig,
    routingStrategy: conditionConfig.routingStrategy,
    branches,
    llmConfigId: conditionConfig.llmConfigId,
    routingPrompt: conditionConfig.routingPrompt,
    conditionConfig,
  };
}

type ConditionValidationResult =
  | { ok: true }
  | { ok: false; message: string };

function validateConditionNodes(
  nodes: Node[],
  edges: Edge[],
): ConditionValidationResult {
  for (const node of nodes) {
    const data = node.data as unknown as WorkflowNodeData;
    if (data.nodeType !== "CONDITION") continue;

    const nodeLabel = data.label || node.id;
    const config = getConditionConfig(node, edges);
    if (!config || !Array.isArray(config.branches)) {
      return {
        ok: false,
        message: `条件节点「${nodeLabel}」缺少分支配置`,
      };
    }

    const elseBranches = config.branches.filter(
      (branch) => branch.type === "else",
    );
    if (elseBranches.length !== 1) {
      return {
        ok: false,
        message: `条件节点「${nodeLabel}」必须且只能有一个「否则」分支`,
      };
    }

    for (const branch of config.branches) {
      const outgoingEdges = edges.filter(
        (edge) =>
          edge.source === node.id && edge.sourceHandle === branch.id,
      );
      if (outgoingEdges.length === 0) {
        return {
          ok: false,
          message: `条件节点「${nodeLabel}」的分支「${branch.name}」尚未连接目标节点`,
        };
      }
      if (outgoingEdges.length > 1) {
        return {
          ok: false,
          message: `条件节点「${nodeLabel}」的分支「${branch.name}」只能连接一个目标节点`,
        };
      }

      if (
        config.routingStrategy !== "LLM" &&
        branch.type !== "else"
      ) {
        if (!Array.isArray(branch.conditions) || branch.conditions.length === 0) {
          return {
            ok: false,
            message: `条件节点「${nodeLabel}」的分支「${branch.name}」至少需要一个条件`,
          };
        }

        for (const condition of branch.conditions) {
          const operator = normalizeConditionOperator(condition.operator);
          if (!condition.sourceRef?.trim() || !condition.operator?.trim()) {
            return {
              ok: false,
              message: `条件节点「${nodeLabel}」的分支「${branch.name}」存在未填写完整的条件`,
            };
          }
          if (
            !NO_VALUE_OPERATORS.has(operator) &&
            !condition.value?.trim()
          ) {
            return {
              ok: false,
              message: `条件节点「${nodeLabel}」的分支「${branch.name}」缺少比较值`,
            };
          }
        }
      }
    }
  }

  return { ok: true };
}

function normalizeWorkflowNodes(
  nodes: Node[],
  edges: Edge[],
  templates: NodeTemplateDTO[],
): Node[] {
  const nodeTypeById = new Map(
    nodes.map(
      (node) => [node.id, (node.data as WorkflowNodeData).nodeType] as const,
    ),
  );

  return nodes.map((node) => {
    const data = node.data as WorkflowNodeData;
    const template = templates.find((item) => item.typeCode === data.nodeType);
    const initial = template?.initialSchema as
      | { inputSchema?: FieldSchema[]; outputSchema?: FieldSchema[] }
      | undefined;
    const defaultPolicy = template?.defaultSchemaPolicy as
      | SchemaPolicy
      | undefined;

    let inputSchema = mergeTemplateSchema(
      data.inputSchema,
      initial?.inputSchema,
    );
    let outputSchema = mergeTemplateSchema(
      data.outputSchema,
      initial?.outputSchema,
    );
    let userConfig = { ...(data.userConfig ?? {}) };

    if (data.nodeType === "START") {
      inputSchema = mergeRequiredFields(inputSchema, [START_INPUT_FIELD]);
      // START 节点输出层用定义强制替换，移除任何历史工作流中残留的多余字段（如旧的 query）
      outputSchema = START_OUTPUT_FIELDS.map(cloneFieldSchema);
    }

    if (data.nodeType === "END") {
      // END 节点允许用户自由添加输入引用和自定义输出字段
      // 保留已保存的字段（不常规节点直接返回）
      return {
        ...node,
        data: {
          ...data,
          inputSchema,
          outputSchema,
          policy: {
            inputSchemaAdd: true,
            outputSchemaAdd: true,
            inputSchemaUpdate: true,
            outputSchemaUpdate: true,
          },
          userConfig,
        } satisfies WorkflowNodeData,
      };
    }

    if (data.nodeType === "HTTP") {
      // 强制保留 http_response 字段，移除用户可能错误删除的系统字段
      outputSchema = mergeRequiredFields(outputSchema, [HTTP_RESPONSE_FIELD]);
    }

    if (data.nodeType === "KNOWLEDGE") {
      inputSchema = normalizeKnowledgeInputSchema(inputSchema);
      outputSchema = mergeRequiredFields(outputSchema, [
        KNOWLEDGE_OUTPUT_FIELD,
      ]);
    }

    if (data.nodeType === "LLM") {
      const hasExplicitContextRefNodes = Array.isArray(
        userConfig.contextRefNodes,
      );
      const hasExplicitUserPromptTemplate =
        typeof userConfig.userPromptTemplate === "string";
      const upstreamKnowledgeNodeIds = edges
        .filter((edge) => edge.target === node.id)
        .map((edge) => edge.source)
        .filter((sourceId) => nodeTypeById.get(sourceId) === "KNOWLEDGE");

      if (!hasExplicitUserPromptTemplate) {
        userConfig = {
          ...userConfig,
          userPromptTemplate: DEFAULT_LLM_PROMPT_TEMPLATE,
        };
      }

      if (hasExplicitContextRefNodes) {
        userConfig = {
          ...userConfig,
          contextRefNodes: normalizeContextRefNodes(userConfig.contextRefNodes),
        };
      } else if (upstreamKnowledgeNodeIds.length > 0) {
        userConfig = {
          ...userConfig,
          contextRefNodes: Array.from(new Set([...upstreamKnowledgeNodeIds])),
        };
      }
    }

    if (data.nodeType === "CONDITION") {
      const conditionConfig = getConditionConfig(node, edges);
      const branches = getConditionBranchesForHandles(node, edges);
      userConfig = conditionConfig
        ? {
            ...userConfig,
            conditionConfig,
          }
        : userConfig;
      return {
        ...node,
        data: {
          ...data,
          branches,
          inputSchema,
          outputSchema,
          policy: data.policy ?? defaultPolicy,
          userConfig,
        } satisfies WorkflowNodeData,
      };
    }

    return {
      ...node,
      data: {
        ...data,
        inputSchema,
        outputSchema,
        policy: data.policy ?? defaultPolicy,
        userConfig,
      } satisfies WorkflowNodeData,
    };
  });
}

function normalizeNodeType(input: unknown): WorkflowNodeType {
  const value = typeof input === "string" ? input.toUpperCase() : "";
  if (
    value === "START" ||
    value === "END" ||
    value === "LLM" ||
    value === "CONDITION" ||
    value === "TOOL" ||
    value === "HTTP" ||
    value === "KNOWLEDGE"
  ) {
    return value;
  }
  return "TOOL";
}

function mapGraphToFlowNodes(graph: Record<string, unknown>): Node[] {
  const rawNodes = Array.isArray(graph.nodes) ? graph.nodes : [];
  const graphEdges = mapGraphToFlowEdges(graph);
  return rawNodes
    .map((item, index) => {
      if (!item || typeof item !== "object") return null;
      const node = item as Record<string, unknown>;
      const id =
        typeof node.nodeId === "string"
          ? node.nodeId
          : typeof node.id === "string"
            ? node.id
            : `node-${index + 1}`;
      const name =
        typeof node.nodeName === "string"
          ? node.nodeName
          : typeof node.name === "string"
            ? node.name
            : `节点-${index + 1}`;
      const nodeType = normalizeNodeType(node.nodeType ?? node.type);
      let userConfig =
        node.userConfig && typeof node.userConfig === "object"
          ? (node.userConfig as Record<string, unknown>)
          : {};
      const policy =
        node.policy && typeof node.policy === "object"
          ? (node.policy as SchemaPolicy)
          : undefined;
      const inputSchema = Array.isArray(node.inputSchema)
        ? (node.inputSchema as FieldSchema[])
        : undefined;
      const outputSchema = Array.isArray(node.outputSchema)
        ? (node.outputSchema as FieldSchema[])
        : undefined;
      const pos = node.position as Record<string, unknown> | undefined;
      const x = typeof pos?.x === "number" ? pos.x : 250;
      const y = typeof pos?.y === "number" ? pos.y : index * 150 + 50;
      const flowNode = {
        id,
        type: "workflowNode",
        position: { x, y },
        data: {
          label: name,
          nodeType,
          userConfig,
          policy,
          inputSchema,
          outputSchema,
        } satisfies WorkflowNodeData,
      } as Node;

      if (nodeType === "CONDITION") {
        const conditionConfig = getConditionConfig(flowNode, graphEdges);
        if (conditionConfig) {
          userConfig = { ...userConfig, conditionConfig };
          return {
            ...flowNode,
            data: {
              ...(flowNode.data as WorkflowNodeData),
              branches: conditionConfig.branches.map((branch) => ({
                id: branch.id,
                name: branch.name,
              })),
              userConfig,
            } satisfies WorkflowNodeData,
          } as Node;
        }
      }

      return flowNode;
    })
    .filter((n): n is Node => n !== null);
}

function mapGraphToFlowEdges(graph: Record<string, unknown>): Edge[] {
  const rawEdges = Array.isArray(graph.edges) ? graph.edges : [];
  return rawEdges
    .map((item, index) => {
      if (!item || typeof item !== "object") return null;
      const edge = item as Record<string, unknown>;
      const source = typeof edge.source === "string" ? edge.source : "";
      const target = typeof edge.target === "string" ? edge.target : "";
      if (!source || !target) return null;
      return {
        id:
          typeof edge.edgeId === "string"
            ? edge.edgeId
            : typeof edge.id === "string"
              ? edge.id
              : `edge-${index + 1}`,
        source,
        target,
        sourceHandle:
          typeof edge.sourceHandle === "string" ? edge.sourceHandle : null,
        type: "custom",
      } as Edge;
    })
    .filter((e): e is Edge => e !== null);
}

function buildGraphPayload(nodes: Node[], edges: Edge[]) {
  const startNode = nodes.find(
    (n) => (n.data as unknown as WorkflowNodeData).nodeType === "START",
  );
  return {
    version: "1.0",
    startNodeId: startNode?.id ?? "start",
    nodes: nodes.map((node) => {
      const d = node.data as unknown as WorkflowNodeData;
      return {
        id: node.id,
        nodeId: node.id,
        type: d.nodeType,
        nodeName: d.label,
        nodeType: d.nodeType,
        position: { x: node.position.x, y: node.position.y },
        policy: d.policy,
        inputSchema: d.inputSchema,
        outputSchema: d.outputSchema,
        userConfig:
          d.nodeType === "CONDITION"
            ? buildConditionUserConfig(node, edges)
            : (d.userConfig ?? {}),
      };
    }),
    edges: edges.map((edge) => ({
      edgeId: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle ?? null,
      edgeType: "DEPENDENCY",
    })),
  };
}

function hasDirtyNodeChange(changes: NodeChange[]): boolean {
  return changes.some((change) =>
    ["add", "remove", "replace", "position"].includes(change.type),
  );
}

function hasDirtyEdgeChange(changes: EdgeChange[]): boolean {
  return changes.some((change) =>
    ["add", "remove", "replace"].includes(change.type),
  );
}

let nodeCounter = 0;

function WorkflowEditorPage() {
  const { agentId } = useParams();
  const [nodes, setNodes, onNodesChange] = useNodesState(INITIAL_NODES);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [loadMessage, setLoadMessage] = useState("");
  const [saveMessage, setSaveMessage] = useState("");
  const [publishMessage, setPublishMessage] = useState("");
  const [connectError, setConnectError] = useState("");
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [rfInstance, setRfInstance] = useState<ReactFlowInstance | null>(null);

  const store = useEditorStore();
  const numericAgentId = agentId ? Number(agentId) : NaN;

  useEffect(() => {
    if (!Number.isFinite(numericAgentId)) {
      setLoadMessage("Agent ID 无效，无法加载 workflow");
      return;
    }

    const loadAll = async () => {
      try {
        const [detail, templates] = await Promise.all([
          fetchWorkflowDetail(numericAgentId),
          fetchNodeTemplates(),
        ]);

        store.setNodeTemplates(templates);
        store.setAgentInfo({
          agentName: detail.name,
          agentDescription: detail.description ?? "",
          agentIcon: detail.icon ?? "",
          version: detail.version,
        });
        setLoadMessage("");

        if (detail.graph && typeof detail.graph === "object") {
          let nextNodes = mapGraphToFlowNodes(
            detail.graph as Record<string, unknown>,
          );
          const nextEdges = mapGraphToFlowEdges(
            detail.graph as Record<string, unknown>,
          );
          nextNodes = normalizeWorkflowNodes(nextNodes, nextEdges, templates);

          if (nextNodes.length > 0) setNodes(nextNodes);
          setEdges(nextEdges);
          store.markClean();
        }
      } catch {
        setLoadMessage("workflow 加载失败，请稍后重试");
      }
    };

    void loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numericAgentId, setNodes, setEdges]);

  const onConnect = useCallback(
    (connection: Connection) => {
      const validation = validateConnection({
        source: connection.source,
        target: connection.target,
      });
      if (!validation.ok) {
        setConnectError(validation.message);
        return;
      }
      const duplicated = edges.some(
        (e) =>
          e.source === connection.source &&
          e.target === connection.target &&
          (e.sourceHandle ?? null) === (connection.sourceHandle ?? null),
      );
      if (duplicated) {
        setConnectError("该连线已存在，请勿重复添加");
        return;
      }
      setEdges((eds) => addEdge({ ...connection, type: "custom" }, eds));
      store.markDirty();
      setConnectError("");
      setSaveMessage("");
      setPublishMessage("");
    },
    [edges, setEdges, store],
  );

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => {
      onNodesChange(changes);
      if (hasDirtyNodeChange(changes)) {
        store.markDirty();
        setSaveMessage("");
        setPublishMessage("");
      }
    },
    [onNodesChange, store],
  );

  const handleEdgesChange = useCallback(
    (changes: EdgeChange[]) => {
      onEdgesChange(changes);
      if (hasDirtyEdgeChange(changes)) {
        store.markDirty();
        setSaveMessage("");
        setPublishMessage("");
      }
    },
    [onEdgesChange, store],
  );

  const onDragOver = useCallback((event: DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onDrop = useCallback(
    (event: DragEvent) => {
      event.preventDefault();
      const nodeType = event.dataTransfer.getData(
        "application/workflow-node-type",
      ) as WorkflowNodeType;
      if (!nodeType) return;
      if (!rfInstance || !reactFlowWrapper.current) return;

      const bounds = reactFlowWrapper.current.getBoundingClientRect();
      const position = rfInstance.screenToFlowPosition({
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      });

      // 从 nodeTemplates 获取默认 schema 和 policy
      const tpl = store.nodeTemplates.find((t) => t.typeCode === nodeType);
      const initialSchema = tpl?.initialSchema as
        | { inputSchema?: FieldSchema[]; outputSchema?: FieldSchema[] }
        | undefined;
      const policy = tpl?.defaultSchemaPolicy as SchemaPolicy | undefined;

      nodeCounter += 1;
      const ts = Date.now();
      const ifBranchId = `branch-${ts}-0`;
      const elseBranchId = `else-${ts}`;
      const newNode: Node = {
        id: `${nodeType.toLowerCase()}-${Date.now()}-${nodeCounter}`,
        type: "workflowNode",
        position,
        data: (nodeType === "CONDITION"
          ? {
              label: `条件节点`,
              nodeType,
              branches: [
                { id: ifBranchId, name: "如果" },
                { id: elseBranchId, name: "否则" },
              ],
              policy,
              inputSchema: initialSchema?.inputSchema ?? [],
              outputSchema: initialSchema?.outputSchema ?? [],
              userConfig: {
                conditionConfig: {
                  routingStrategy: "EXPRESSION",
                  branches: [
                    {
                      id: ifBranchId,
                      name: "如果",
                      type: "if",
                      priority: 1,
                      logic: "AND",
                      conditions: [
                        {
                          sourceRef: "",
                          operator: "EQUALS",
                          value: "",
                          valueType: "literal",
                        },
                      ],
                    },
                    {
                      id: elseBranchId,
                      name: "否则",
                      type: "else",
                      priority: 0,
                      logic: "AND",
                      conditions: [],
                    },
                  ],
                },
              },
            }
          : nodeType === "KNOWLEDGE"
            ? {
                label: `KNOWLEDGE 节点`,
                nodeType,
                policy,
                // 强制清空 query 的 sourceRef，不使用数据库模板中可能存在的旧默认值
                inputSchema: [{ ...KNOWLEDGE_QUERY_FIELD }],
                outputSchema: initialSchema?.outputSchema ?? [],
              }
            : nodeType === "END"
              ? {
                  label: `结束节点`,
                  nodeType,
                  // END 节点完全开放，schema 为空，由用户自由添加
                  policy: {
                    inputSchemaAdd: true,
                    outputSchemaAdd: true,
                    inputSchemaUpdate: true,
                    outputSchemaUpdate: true,
                  },
                  inputSchema: [],
                  outputSchema: [],
                }
              : {
                  label: `${nodeType} 节点`,
                  nodeType,
                  policy,
                  inputSchema: initialSchema?.inputSchema ?? [],
                  outputSchema: initialSchema?.outputSchema ?? [],
                }) satisfies WorkflowNodeData,
      };
      setNodes((nds) => [...nds, newNode]);
      store.markDirty();
      setSaveMessage("");
      setPublishMessage("");
    },
    [rfInstance, setNodes, store],
  );

  const validationNodes = useMemo(
    () => nodes.map((n) => ({ id: n.id })),
    [nodes],
  );
  const validationEdges = useMemo(
    () => edges.map((e) => ({ source: e.source, target: e.target })),
    [edges],
  );

  const handleSave = async () => {
    setSaveMessage("");
    setPublishMessage("");
    if (
      !Number.isFinite(numericAgentId) ||
      store.version === null ||
      !store.agentName
    ) {
      setSaveMessage("缺少保存所需信息，请刷新页面重试");
      return;
    }
    const validation = validateWorkflowGraph({
      nodes: validationNodes,
      edges: validationEdges,
    });
    if (!validation.ok) {
      setSaveMessage(validation.message);
      return;
    }
    const conditionValidation = validateConditionNodes(nodes, edges);
    if (!conditionValidation.ok) {
      setSaveMessage(conditionValidation.message);
      return;
    }
    store.setOperationState("saving");
    try {
      const normalizedNodes = normalizeWorkflowNodes(
        nodes,
        edges,
        store.nodeTemplates,
      );
      const result = await saveWorkflow({
        agentId: numericAgentId,
        version: store.version,
        name: store.agentName,
        description: store.agentDescription,
        icon: store.agentIcon,
        graph: buildGraphPayload(normalizedNodes, edges),
      });
      setNodes(normalizedNodes);
      store.setAgentInfo({ version: result.version });
      store.markClean();
      setSaveMessage("保存成功");
    } catch {
      setSaveMessage("保存失败，请稍后重试");
    } finally {
      store.setOperationState("idle");
    }
  };

  const handlePublish = async () => {
    setPublishMessage("");
    setSaveMessage("");
    if (!Number.isFinite(numericAgentId)) {
      setPublishMessage("Agent ID 无效，无法发布");
      return;
    }
    const validation = validateWorkflowGraph({
      nodes: validationNodes,
      edges: validationEdges,
    });
    if (!validation.ok) {
      setPublishMessage(validation.message);
      return;
    }
    const conditionValidation = validateConditionNodes(nodes, edges);
    if (!conditionValidation.ok) {
      setPublishMessage(conditionValidation.message);
      return;
    }
    if (store.isDirty) {
      setPublishMessage("请先保存后再发布");
      return;
    }
    store.setOperationState("publishing");
    try {
      const result = await publishWorkflow(numericAgentId);
      store.setAgentInfo({ version: result.version });
      store.markClean();
      setPublishMessage("发布成功");
    } catch {
      setPublishMessage("发布失败，请稍后重试");
    } finally {
      store.setOperationState("idle");
    }
  };

  const errorBanner =
    saveMessage || publishMessage || connectError || loadMessage;

  return (
    <section className="flex h-screen flex-col bg-slate-50">
      <EditorHeader
        agentName={store.agentName}
        isDirty={store.isDirty}
        operationState={store.operationState}
        onSave={handleSave}
        onPublish={handlePublish}
      />

      {errorBanner && (
        <div className="border-b border-red-100 bg-red-50 px-4 py-1 text-sm text-red-600">
          {saveMessage || publishMessage || connectError || loadMessage}
        </div>
      )}

      <div className="flex flex-1 overflow-hidden">
        <AgentConfigPanel
          agentName={store.agentName}
          agentDescription={store.agentDescription}
          agentIcon={store.agentIcon}
          collapsed={store.panelCollapsed}
          onToggle={store.togglePanel}
          onChange={(field, value) => {
            store.setAgentInfo({ [field]: value });
            store.markDirty();
          }}
        />

        <div className="relative flex-1" ref={reactFlowWrapper}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={handleNodesChange}
            onEdgesChange={handleEdgesChange}
            onConnect={onConnect}
            onInit={setRfInstance}
            onDragOver={onDragOver}
            onDrop={onDrop}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            defaultEdgeOptions={{ type: "custom" }}
            connectionLineComponent={CustomConnectionLine}
            fitView
          >
            <Controls position="bottom-right" className="!shadow-md !border-slate-200" />
            <MiniMap position="top-right" className="!shadow-md !border-slate-200 !rounded-xl" maskColor="rgba(248, 250, 252, 0.7)" />
            <Background variant={BackgroundVariant.Dots} gap={24} size={1.5} color="#cbd5e1" />
          </ReactFlow>
          <CanvasToolbar />
        </div>
      </div>
    </section>
  );
}

export default WorkflowEditorPage;
