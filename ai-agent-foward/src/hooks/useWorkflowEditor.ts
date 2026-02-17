import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  type Connection,
  type EdgeChange,
  type NodeChange
} from '@xyflow/react';
import { message } from 'antd';
import { workflowService } from '../services/workflowService';
import type { NodeTemplate } from '../types/execution';
import type { AgentDetail } from '../types/agent';
import {
  type WorkflowCanvasEdge,
  type WorkflowCanvasNode,
  type WorkflowGraphDTO,
  type WorkflowGraphNodeDTO,
  type WorkflowNodeType,
  type WorkflowValidationIssue,
  WORKFLOW_NODE_TYPE_WHITELIST
} from '../types/workflow';

const createDagId = () => `dag-${crypto.randomUUID()}`;

const createNodeId = () => `node-${crypto.randomUUID()}`;

const createEdgeId = () => `edge-${crypto.randomUUID()}`;

const normalizeGraphId = (value?: string | number | null): string => String(value ?? '').trim();

const normalizeNodeType = (raw?: string | null): { normalized: WorkflowNodeType | 'UNKNOWN'; raw: string } => {
  const upper = String(raw || '').toUpperCase();
  if (WORKFLOW_NODE_TYPE_WHITELIST.includes(upper as WorkflowNodeType)) {
    return { normalized: upper as WorkflowNodeType, raw: upper };
  }

  return {
    normalized: 'UNKNOWN',
    raw: upper || 'UNKNOWN'
  };
};

const getTemplateRequiredFields = (template?: NodeTemplate): string[] => {
  if (!template) return [];

  const requiredFromSchema = Array.isArray(template.configSchema?.required)
    ? template.configSchema?.required
    : [];

  const requiredFromGroups = (template.configFieldGroups || [])
    .flatMap((group) => group.fields || [])
    .filter((field) => field.required)
    .map((field) => field.key)
    .filter(Boolean);

  return Array.from(new Set([...(requiredFromSchema || []), ...requiredFromGroups]));
};

const NODE_DEFAULT_CONFIG_FALLBACK: Record<string, Record<string, any>> = {
  LLM: { prompt: '' },
  CONDITION: { expression: '' },
  TOOL: { toolName: '' },
  START: {},
  END: {},
  HTTP: {}
};

const buildDefaultConfig = (nodeType: string, template?: NodeTemplate) => {
  const result: Record<string, any> = {
    ...(NODE_DEFAULT_CONFIG_FALLBACK[nodeType] || {}),
    ...(template?.defaultConfig?.properties || {})
  };

  (template?.configFieldGroups || []).forEach((group) => {
    (group.fields || []).forEach((field) => {
      if (field.key && field.defaultValue !== undefined && result[field.key] === undefined) {
        result[field.key] = field.defaultValue;
      }
    });
  });

  return result;
};
const mapGraphNodeToCanvasNode = (
  dto: WorkflowGraphNodeDTO,
  templatesByType: Record<string, NodeTemplate>
): WorkflowCanvasNode => {
  const normalized = normalizeNodeType(dto.nodeType);
  const template =
    templatesByType[normalized.raw] ||
    (dto.templateId ? Object.values(templatesByType).find((item) => item.templateId === String(dto.templateId)) : undefined);

  const normalizedNodeId = normalizeGraphId(dto.nodeId);

  return {
    id: normalizedNodeId,
    type: 'workflowNode',
    position: {
      x: dto.position?.x ?? 100,
      y: dto.position?.y ?? 100
    },
    data: {
      nodeId: normalizedNodeId,
      nodeName: dto.nodeName || normalized.raw || normalizedNodeId,
      nodeType: normalized.normalized,
      rawNodeType: normalized.raw,
      hasUnknownType: normalized.normalized === 'UNKNOWN',
      userConfig: dto.userConfig || {},
      templateId: dto.templateId ? String(dto.templateId) : template?.templateId,
      inputSchema: dto.inputSchema,
      outputSchema: dto.outputSchema
    }
  };
};

const toCanvasEdge = (dto: WorkflowGraphDTO['edges'][number]): WorkflowCanvasEdge => ({
  id: normalizeGraphId(dto.edgeId) || createEdgeId(),
  source: normalizeGraphId(dto.source),
  target: normalizeGraphId(dto.target),
  label: dto.label,
  data: {
    condition: dto.condition,
    edgeType: dto.edgeType || 'DEPENDENCY'
  }
});

const mapCanvasNodeToGraphNode = (node: WorkflowCanvasNode): WorkflowGraphNodeDTO => {
  const nodeType = node.data.nodeType === 'UNKNOWN' ? node.data.rawNodeType || 'UNKNOWN' : node.data.nodeType;

  const normalizedNodeId = normalizeGraphId(node.data.nodeId) || normalizeGraphId(node.id);

  return {
    nodeId: normalizedNodeId,
    nodeName: node.data.nodeName,
    nodeType,
    userConfig: node.data.userConfig || {},
    inputSchema: node.data.inputSchema || [],
    outputSchema: node.data.outputSchema || [],
    position: {
      x: node.position.x,
      y: node.position.y
    },
    templateId: node.data.templateId
  };
};

const buildSaveGraph = (nodes: WorkflowCanvasNode[], edges: WorkflowCanvasEdge[], dagId: string, agentId: number): WorkflowGraphDTO => {
  const startNode = nodes.find((node) => node.data.nodeType === 'START');

  return {
    dagId: dagId || createDagId(),
    version: '1.0.0',
    description: `agent-${agentId}-workflow`,
    startNodeId: startNode?.id,
    nodes: nodes.map(mapCanvasNodeToGraphNode),
    edges: edges.map((edge) => ({
      edgeId: normalizeGraphId(edge.id) || createEdgeId(),
      source: normalizeGraphId(edge.source),
      target: normalizeGraphId(edge.target),
      label: typeof edge.label === 'string' ? edge.label : undefined,
      condition: edge.data?.condition,
      edgeType: edge.data?.edgeType || 'DEPENDENCY'
    }))
  };
};

const toTemplatesByType = (templateList: NodeTemplate[]) =>
  templateList.reduce<Record<string, NodeTemplate>>((acc, template) => {
    if (template.type) {
      acc[template.type.toUpperCase()] = template;
    }
    return acc;
  }, {});

interface SaveResult {
  ok: boolean;
  conflict?: boolean;
  message?: string;
}

export const useWorkflowEditor = (agentId?: number) => {
  const [agent, setAgent] = useState<AgentDetail | null>(null);
  const [version, setVersion] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dagId, setDagId] = useState('');
  const [nodes, setNodes] = useState<WorkflowCanvasNode[]>([]);
  const [edges, setEdges] = useState<WorkflowCanvasEdge[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [templates, setTemplates] = useState<NodeTemplate[]>([]);
  const [metadataLoading, setMetadataLoading] = useState(false);
  const [metadataError, setMetadataError] = useState<string | null>(null);

  const templatesByType = useMemo(() => {
    return templates.reduce<Record<string, NodeTemplate>>((acc, template) => {
      if (template.type) {
        acc[template.type.toUpperCase()] = template;
      }
      return acc;
    }, {});
  }, [templates]);

  const templatesById = useMemo(() => {
    return templates.reduce<Record<string, NodeTemplate>>((acc, template) => {
      const id = template.templateId || template.id;
      if (id) {
        acc[String(id)] = template;
      }
      return acc;
    }, {});
  }, [templates]);

  const loadWorkflow = useCallback(async () => {
    if (!agentId) return;

    setLoading(true);
    setMetadataLoading(true);
    setMetadataError(null);

    try {
      const detail = await workflowService.loadWorkflow(agentId);
      const metadataByType = toTemplatesByType(detail.metadata);

      setAgent(detail.agent);
      setVersion(detail.version);
      setTemplates(detail.metadata);
      setDagId(detail.graphJson.dagId);
      setNodes(detail.graphJson.nodes.map((node) => mapGraphNodeToCanvasNode(node, metadataByType)));
      setEdges(detail.graphJson.edges.map(toCanvasEdge));
      setSelectedNodeId(null);
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '加载工作流失败';
      setMetadataError(errorMessage);
      message.error(errorMessage);
    } finally {
      setLoading(false);
      setMetadataLoading(false);
    }
  }, [agentId]);

  useEffect(() => {
    loadWorkflow();
  }, [loadWorkflow]);

  const addNode = useCallback(
    (nodeType: string, position?: { x: number; y: number }, templateId?: string) => {
      const normalized = normalizeNodeType(nodeType);
      const template =
        (templateId ? templatesById[templateId] : undefined) ||
        templatesByType[normalized.raw];

      const id = createNodeId();
      const nameBase = template?.name || normalized.raw || 'NODE';
      const nodeName = `${nameBase}_${nodes.length + 1}`;
      const fallbackPosition = {
        x: 140 + ((nodes.length % 5) * 80),
        y: 120 + (Math.floor(nodes.length / 5) * 80)
      };
      const nodePosition = position || fallbackPosition;

      setNodes((prev) => [
        ...prev,
        {
          id,
          type: 'workflowNode',
          position: nodePosition,
          data: {
            nodeId: id,
            nodeName,
            nodeType: normalized.normalized,
            rawNodeType: normalized.raw,
            hasUnknownType: normalized.normalized === 'UNKNOWN',
            templateId: template?.templateId || template?.id,
            userConfig: buildDefaultConfig(normalized.raw, template),
            inputSchema: template?.inputSchema || [],
            outputSchema: template?.outputSchema || []
          }
        }
      ]);

      setSelectedNodeId(id);
    },
    [nodes.length, templatesById, templatesByType]
  );

  const addNodeFromTemplate = useCallback(
    (payload: { nodeType: string; templateId?: string; position?: { x: number; y: number } }) => {
      addNode(payload.nodeType, payload.position, payload.templateId);
    },
    [addNode]
  );

  const onConnect = useCallback((connection: Connection) => {
    const source = normalizeGraphId(connection.source);
    const target = normalizeGraphId(connection.target);

    if (!source || !target) {
      return;
    }

    setEdges((prev) =>
      addEdge(
        {
          ...connection,
          source,
          target,
          id: createEdgeId(),
          type: 'default',
          data: { edgeType: 'DEPENDENCY' }
        },
        prev
      )
    );
  }, []);

  const updateNode = useCallback((nodeId: string, patch: Partial<WorkflowCanvasNode['data']>) => {
    setNodes((prev) =>
      prev.map((node) => {
        if (node.id !== nodeId) return node;

        return {
          ...node,
          data: {
            ...node.data,
            ...patch,
            userConfig: {
              ...node.data.userConfig,
              ...(patch.userConfig || {})
            }
          }
        };
      })
    );
  }, []);

  const removeSelectedNode = useCallback(() => {
    if (!selectedNodeId) return;

    setNodes((prev) => prev.filter((node) => node.id !== selectedNodeId));
    setEdges((prev) => prev.filter((edge) => edge.source !== selectedNodeId && edge.target !== selectedNodeId));
    setSelectedNodeId(null);
  }, [selectedNodeId]);

  const selectedNode = useMemo(
    () => nodes.find((node) => node.id === selectedNodeId) || null,
    [nodes, selectedNodeId]
  );

  const validate = useCallback((): WorkflowValidationIssue[] => {
    const issues: WorkflowValidationIssue[] = [];

    const startNodes = nodes.filter((node) => node.data.nodeType === 'START');
    const endNodes = nodes.filter((node) => node.data.nodeType === 'END');

    if (startNodes.length !== 1) {
      issues.push({
        key: 'start-node',
        message: '必须且只能有一个 START 节点'
      });
    }

    if (endNodes.length < 1) {
      issues.push({
        key: 'end-node',
        message: '至少需要一个 END 节点'
      });
    }

    const incomingCount = new Map<string, number>();
    const outgoingCount = new Map<string, number>();

    nodes.forEach((node) => {
      incomingCount.set(node.id, 0);
      outgoingCount.set(node.id, 0);
    });

    edges.forEach((edge) => {
      outgoingCount.set(edge.source, (outgoingCount.get(edge.source) || 0) + 1);
      incomingCount.set(edge.target, (incomingCount.get(edge.target) || 0) + 1);
    });

    nodes.forEach((node) => {
      if (node.data.nodeType === 'UNKNOWN') {
        issues.push({
          key: `unknown-${node.id}`,
          message: `节点 ${node.data.nodeName} 存在未知类型 ${node.data.rawNodeType || 'UNKNOWN'}，禁止保存`
        });
        return;
      }

      if (node.data.nodeType !== 'START' && (incomingCount.get(node.id) || 0) === 0) {
        issues.push({
          key: `incoming-${node.id}`,
          message: `节点 ${node.data.nodeName} 缺少入边`
        });
      }

      if (node.data.nodeType !== 'END' && (outgoingCount.get(node.id) || 0) === 0) {
        issues.push({
          key: `outgoing-${node.id}`,
          message: `节点 ${node.data.nodeName} 缺少出边`
        });
      }

      const template =
        (node.data.templateId ? templatesById[node.data.templateId] : undefined) ||
        templatesByType[node.data.rawNodeType || node.data.nodeType];

      const requiredFields = getTemplateRequiredFields(template);
      requiredFields.forEach((fieldKey) => {
        const field = template?.configFieldGroups
          ?.flatMap((group) => group.fields || [])
          .find((item) => item.key === fieldKey);
        const value = node.data.userConfig?.[fieldKey];

        if (value === undefined || value === null || String(value).trim() === '') {
          issues.push({
            key: `required-${node.id}-${fieldKey}`,
            message: `节点 ${node.data.nodeName} 缺少必填配置：${field?.label || fieldKey}`
          });
        }
      });
    });

    return issues;
  }, [edges, nodes, templatesById, templatesByType]);

  const serialize = useCallback((): WorkflowGraphDTO => {
    if (!agentId) {
      return buildSaveGraph(nodes, edges, dagId, 0);
    }

    return buildSaveGraph(nodes, edges, dagId, agentId);
  }, [agentId, dagId, edges, nodes]);

  const save = useCallback(async (): Promise<SaveResult> => {
    if (!agentId) {
      message.error('Agent 信息缺失，无法保存');
      return { ok: false, message: 'agent-missing' };
    }

    if (version == null) {
      message.error('版本信息缺失，无法保存');
      return { ok: false, message: 'version-missing' };
    }

    const issues = validate();
    if (issues.length > 0) {
      message.error(`校验失败：${issues[0].message}`);
      return { ok: false, message: issues[0].message };
    }

    setSaving(true);
    try {
      const result = await workflowService.saveWorkflow(agentId, serialize(), version);
      if (result.conflict) {
        message.error('检测到版本冲突，请刷新最新版本后重试');
        return { ok: false, conflict: true, message: 'version-conflict' };
      }

      await loadWorkflow();
      return { ok: true, message: 'saved' };
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '保存失败';
      const lowerMessage = String(errorMessage).toLowerCase();
      if (lowerMessage.includes('version') || lowerMessage.includes('optimistic') || lowerMessage.includes('冲突')) {
        message.error('检测到版本冲突，请刷新最新版本后重试');
        return { ok: false, conflict: true, message: errorMessage };
      }

      message.error(errorMessage);
      return { ok: false, message: errorMessage };
    } finally {
      setSaving(false);
    }
  }, [agentId, loadWorkflow, serialize, validate, version]);

  const onNodesChange = useCallback((changes: NodeChange<WorkflowCanvasNode>[]) => {
    setNodes((prev) => applyNodeChanges(changes, prev));
  }, []);

  const onEdgesChange = useCallback((changes: EdgeChange<WorkflowCanvasEdge>[]) => {
    setEdges((prev) => applyEdgeChanges(changes, prev));
  }, []);

  const reloadLatest = useCallback(async () => {
    await loadWorkflow();
  }, [loadWorkflow]);

  const reloadMetadata = useCallback(async () => {
    await loadWorkflow();
  }, [loadWorkflow]);

  return {
    agent,
    loading,
    saving,
    metadataLoading,
    metadataError,
    templates,
    templatesByType,
    nodes,
    edges,
    selectedNode,
    selectedNodeId,
    setSelectedNodeId,
    addNode,
    addNodeFromTemplate,
    onConnect,
    onNodesChange,
    onEdgesChange,
    updateNode,
    removeSelectedNode,
    validate,
    serialize,
    save,
    reloadLatest,
    reloadMetadata,
    normalizeNodeType,
    mapGraphNodeToCanvasNode,
    mapCanvasNodeToGraphNode,
    buildSaveGraph
  };
};
