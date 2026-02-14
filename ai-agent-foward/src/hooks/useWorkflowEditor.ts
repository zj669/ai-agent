import { useState, useCallback, useRef, useEffect } from 'react';
import { addEdge, type Connection, type EdgeChange, type NodeChange } from '@xyflow/react';
import { message } from 'antd';
import { workflowService } from '../services/workflowService';
import { metadataService } from '../services/metadataService';
import { useWorkflowInteractions } from '../components/workflow/hooks/use-workflow-interactions';
import { useNodesInteractions } from '../components/workflow/hooks/use-nodes-interactions';
import { useEdgesInteractions } from '../components/workflow/hooks/use-edges-interactions';
import { useWorkflowHistory } from '../components/workflow/hooks/use-workflow-history';
import { useNodesSyncDraft } from '../components/workflow/hooks/use-nodes-sync-draft';
import { applyAutoLayout } from '../components/workflow/utils';
import {
  WorkflowGraph,
  WorkflowNode,
  WorkflowEdge,
  ReactFlowNode,
  ReactFlowEdge,
  NodeType,
  NodeTemplate,
  EdgeType,
  StartExecutionRequest,
  NodeExecutionStatus,
  SSEConnectedEvent,
  SSEStartEvent,
  SSEUpdateEvent,
  SSEFinishEvent,
  SSEErrorEvent
} from '../types/workflow';

export function useWorkflowEditor(initialGraph?: WorkflowGraph) {
  const {
    nodes,
    edges,
    setNodes,
    setEdges,
    onNodesChange: onNodesChangeBase,
    onEdgesChange: onEdgesChangeBase,
    onConnect: onConnectBase,
    interactionMode,
    setInteractionMode
  } = useWorkflowInteractions();
  const { addNode, removeNode, updateNodeData } = useNodesInteractions();
  const { deleteEdge } = useEdgesInteractions();
  const { snapshot, undo, redo, canUndo, canRedo } = useWorkflowHistory();
  const { exportDraft } = useNodesSyncDraft();

  const [isExecuting, setIsExecuting] = useState(false);
  const [executionId, setExecutionId] = useState<string | null>(null);
  const [executionLogs, setExecutionLogs] = useState<string[]>([]);
  const [nodeTemplates, setNodeTemplates] = useState<NodeTemplate[]>([]);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    let disposed = false;
    const fetchTemplates = async () => {
      try {
        const templates = await metadataService.getNodeTemplates();
        if (!disposed) {
          setNodeTemplates(templates);
        }
      } catch {
        if (!disposed) {
          message.warning('节点模板加载失败，使用默认配置');
        }
      }
    };

    fetchTemplates();

    return () => {
      disposed = true;
    };
  }, []);

  const convertToReactFlow = useCallback((graph: WorkflowGraph) => {
    const rfNodes: ReactFlowNode[] = Object.values(graph.nodes).map((node) => ({
      id: node.nodeId,
      type: getReactFlowNodeType(node.type),
      position: node.position || { x: 0, y: 0 },
      data: {
        label: node.name,
        nodeType: node.type,
        config: node.config,
        inputs: node.inputs,
        outputs: node.outputs
      }
    }));

    const rfEdges: ReactFlowEdge[] = [];
    Object.values(graph.edgeDetails).forEach((edgeList) => {
      edgeList.forEach((edge) => {
        rfEdges.push({
          id: edge.edgeId,
          source: edge.source,
          target: edge.target,
          type: getReactFlowEdgeType(edge.edgeType),
          label: edge.condition,
          data: {
            condition: edge.condition,
            edgeType: edge.edgeType
          }
        });
      });
    });

    return { nodes: rfNodes, edges: rfEdges };
  }, []);

  useEffect(() => {
    if (!initialGraph) return;
    const { nodes: rfNodes, edges: rfEdges } = convertToReactFlow(initialGraph);
    setNodes(rfNodes as any);
    setEdges(rfEdges as any);
  }, [initialGraph, convertToReactFlow, setNodes, setEdges]);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => {
      const shouldSnapshot = changes.some((change) => {
        if (change.type === 'position') {
          return (change as { dragging?: boolean }).dragging === false;
        }
        return change.type === 'remove';
      });
      if (shouldSnapshot) {
        snapshot();
      }
      onNodesChangeBase(changes as any);
    },
    [onNodesChangeBase, snapshot]
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) => {
      if (changes.some((change) => change.type === 'remove')) {
        snapshot();
      }
      onEdgesChangeBase(changes as any);
    },
    [onEdgesChangeBase, snapshot]
  );

  const onConnect = useCallback(
    (connection: Connection) => {
      snapshot();
      if (!connection.source || !connection.target) {
        return;
      }

      setEdges(
        addEdge(
          {
            ...connection,
            id: `edge_${Date.now()}`,
            type: 'workflow',
            data: {
              edgeType: EdgeType.DEPENDENCY
            }
          },
          edges as any
        ) as any
      );
    },
    [edges, setEdges, snapshot]
  );

  const convertToWorkflowGraph = useCallback((): WorkflowGraph => {
    const workflowNodes: Record<string, WorkflowNode> = {};
    const edgesMap: Record<string, string[]> = {};
    const edgeDetailsMap: Record<string, WorkflowEdge[]> = {};

    (nodes as any as ReactFlowNode[]).forEach((node) => {
      workflowNodes[node.id] = {
        nodeId: node.id,
        name: node.data.label,
        type: node.data.nodeType,
        config: node.data.config,
        inputs: node.data.inputs,
        outputs: node.data.outputs,
        position: node.position
      };
    });

    (edges as any as ReactFlowEdge[]).forEach((edge) => {
      if (!edgesMap[edge.source]) {
        edgesMap[edge.source] = [];
      }
      edgesMap[edge.source].push(edge.target);

      if (!edgeDetailsMap[edge.source]) {
        edgeDetailsMap[edge.source] = [];
      }
      edgeDetailsMap[edge.source].push({
        edgeId: edge.id,
        source: edge.source,
        target: edge.target,
        condition: edge.data?.condition,
        edgeType: edge.data?.edgeType || EdgeType.DEPENDENCY
      });
    });

    return {
      graphId: initialGraph?.graphId || 'new-graph',
      version: initialGraph?.version || '1.0',
      description: initialGraph?.description,
      nodes: workflowNodes,
      edges: edgesMap,
      edgeDetails: edgeDetailsMap
    };
  }, [nodes, edges, initialGraph]);

  const addNodeByType = useCallback(
    (type: NodeType, position: { x: number; y: number }) => {
      snapshot();
      const matchedTemplate = nodeTemplates.find((template) => template.type === type);
      const createdNode = addNode(type, position);

      if (createdNode && matchedTemplate?.defaultConfig?.properties) {
        updateNodeData(createdNode.id, {
          label: matchedTemplate.name || createdNode.data.label,
          config: {
            ...(createdNode.data.config || { properties: {} }),
            properties: {
              ...(createdNode.data.config?.properties || {}),
              ...matchedTemplate.defaultConfig.properties
            }
          }
        });
      }
    },
    [addNode, nodeTemplates, snapshot, updateNodeData]
  );

  const duplicateNode = useCallback(
    (nodeId: string) => {
      const nodeToDuplicate = (nodes as any as ReactFlowNode[]).find((node) => node.id === nodeId);
      if (!nodeToDuplicate) {
        message.error('未找到要复制的节点');
        return;
      }
      snapshot();
      addNodeByType(nodeToDuplicate.data.nodeType, {
        x: nodeToDuplicate.position.x + 50,
        y: nodeToDuplicate.position.y + 50
      });
      message.success('节点已复制');
    },
    [nodes, addNodeByType, snapshot]
  );

  const deleteNode = useCallback(
    (nodeId: string) => {
      snapshot();
      removeNode(nodeId);
      message.success('节点已删除');
    },
    [removeNode, snapshot]
  );

  const deleteEdgeById = useCallback(
    (edgeId: string) => {
      snapshot();
      deleteEdge(edgeId);
      message.success('连接已删除');
    },
    [deleteEdge, snapshot]
  );

  const loadGraph = useCallback(
    (graph: WorkflowGraph) => {
      const { nodes: rfNodes, edges: rfEdges } = convertToReactFlow(graph);
      setNodes(rfNodes as any);
      setEdges(rfEdges as any);
    },
    [convertToReactFlow, setNodes, setEdges]
  );

  const clearGraph = useCallback(() => {
    snapshot();
    setNodes([]);
    setEdges([]);
  }, [setEdges, setNodes, snapshot]);

  const clearExecutionStatus = useCallback(() => {
    setNodes(
      (nodes as any as ReactFlowNode[]).map((node) => ({
        ...node,
        data: {
          ...node.data,
          status: undefined
        }
      })) as any
    );
    setExecutionLogs([]);
    setExecutionId(null);
  }, [nodes, setNodes]);

  const autoLayout = useCallback(
    (direction: 'TB' | 'LR' = 'TB') => {
      if (nodes.length === 0) return;
      snapshot();
      setNodes(applyAutoLayout(nodes as any, edges as any, direction));
    },
    [edges, nodes, setNodes, snapshot]
  );

  const startExecution = useCallback(
    async (request: StartExecutionRequest) => {
      if (isExecuting) {
        message.warning('工作流正在执行中');
        return;
      }

      setIsExecuting(true);
      setExecutionLogs([]);

      try {
        const controller = await workflowService.startExecution(request, {
          onConnected: (data: SSEConnectedEvent) => {
            setExecutionId(data.executionId);
            setExecutionLogs((logs) => [...logs, `[连接成功] 执行ID: ${data.executionId}`]);
          },
          onStart: (data: SSEStartEvent) => {
            setExecutionLogs((logs) => [
              ...logs,
              `[${new Date(data.timestamp).toLocaleTimeString()}] 节点 ${data.nodeId} 开始执行`
            ]);
            updateNodeData(data.nodeId, { status: NodeExecutionStatus.RUNNING });
          },
          onUpdate: (data: SSEUpdateEvent) => {
            setExecutionLogs((logs) => [
              ...logs,
              `[${new Date(data.timestamp).toLocaleTimeString()}] ${data.nodeId}: ${data.delta}`
            ]);
          },
          onFinish: (data: SSEFinishEvent) => {
            setExecutionLogs((logs) => [
              ...logs,
              `[${new Date(data.timestamp).toLocaleTimeString()}] 节点 ${data.nodeId} 执行完成: ${data.status}`
            ]);
            updateNodeData(data.nodeId, { status: data.status });
            if (data.status === NodeExecutionStatus.SUCCEEDED) {
              setIsExecuting(false);
              message.success('工作流执行完成');
            }
          },
          onError: (data: SSEErrorEvent) => {
            setExecutionLogs((logs) => [...logs, `[错误] ${data.message}`]);
            setIsExecuting(false);
            message.error(`执行失败: ${data.message}`);
          },
          onPing: () => undefined
        });

        abortControllerRef.current = controller;
      } catch (error: any) {
        setIsExecuting(false);
        message.error(`启动失败: ${error.message}`);
      }
    },
    [isExecuting, updateNodeData]
  );

  const stopExecution = useCallback(async () => {
    if (!executionId) {
      message.warning('没有正在执行的工作流');
      return;
    }

    try {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }

      await workflowService.stopExecution({ executionId });
      setIsExecuting(false);
      setExecutionId(null);
      message.success('工作流已停止');
    } catch (error: any) {
      message.error(`停止失败: ${error.message}`);
    }
  }, [executionId]);

  return {
    nodes: nodes as any as ReactFlowNode[],
    edges: edges as any as ReactFlowEdge[],
    onNodesChange,
    onEdgesChange,
    onConnect,
    addNode: addNodeByType,
    deleteNode,
    updateNodeData,
    deleteEdge: deleteEdgeById,
    duplicateNode,
    undo,
    redo,
    canUndo,
    canRedo,
    loadGraph,
    clearGraph,
    convertToWorkflowGraph,
    exportDraft,
    interactionMode,
    setInteractionMode,
    isExecuting,
    executionId,
    executionLogs,
    startExecution,
    stopExecution,
    nodeTemplates,
    clearExecutionStatus,
    autoLayout
  };
}

function getReactFlowNodeType(nodeType: NodeType): string {
  switch (nodeType) {
    case NodeType.START:
      return 'start';
    case NodeType.END:
      return 'end';
    case NodeType.LLM:
      return 'llm';
    case NodeType.HTTP:
      return 'http';
    case NodeType.CONDITION:
      return 'condition';
    case NodeType.TOOL:
      return 'tool';
    default:
      return 'default';
  }
}

function getReactFlowEdgeType(edgeType: EdgeType): string {
  switch (edgeType) {
    case EdgeType.CONDITIONAL:
    case EdgeType.DEFAULT:
    default:
      return 'workflow';
  }
}

