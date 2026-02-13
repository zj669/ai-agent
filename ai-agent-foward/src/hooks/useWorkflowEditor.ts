import { useState, useCallback, useRef, useEffect, DragEvent } from 'react';
import { useNodesState, useEdgesState, addEdge, Connection, ReactFlowInstance } from '@xyflow/react';
import { message } from 'antd';
import { workflowService } from '../services/workflowService';
import {
  WorkflowGraph,
  WorkflowNode,
  WorkflowEdge,
  ReactFlowNode,
  ReactFlowEdge,
  NodeType,
  EdgeType,
  StartExecutionRequest,
  NodeExecutionStatus,
  SSEConnectedEvent,
  SSEStartEvent,
  SSEUpdateEvent,
  SSEFinishEvent,
  SSEErrorEvent
} from '../types/workflow';

// 历史记录状态类型
interface HistoryState {
  nodes: ReactFlowNode[];
  edges: ReactFlowEdge[];
}

// 最大历史记录数
const MAX_HISTORY_SIZE = 50;

/**
 * 工作流编辑器 Hook
 */
export function useWorkflowEditor(initialGraph?: WorkflowGraph) {
  // ReactFlow 状态
  const [nodes, setNodes, onNodesChange] = useNodesState<ReactFlowNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<ReactFlowEdge>([]);

  // 历史记录状态（撤销/重做）
  const [undoStack, setUndoStack] = useState<HistoryState[]>([]);
  const [redoStack, setRedoStack] = useState<HistoryState[]>([]);
  const isUndoRedoAction = useRef(false);

  // 执行状态
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionId, setExecutionId] = useState<string | null>(null);
  const [executionLogs, setExecutionLogs] = useState<string[]>([]);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 初始化图
  useEffect(() => {
    if (initialGraph) {
      const { nodes: rfNodes, edges: rfEdges } = convertToReactFlow(initialGraph);
      setNodes(rfNodes);
      setEdges(rfEdges);
    }
  }, [initialGraph, setNodes, setEdges]);

  /**
   * 保存当前状态到历史记录
   */
  const saveToHistory = useCallback(() => {
    if (isUndoRedoAction.current) {
      isUndoRedoAction.current = false;
      return;
    }
    setUndoStack((prev) => {
      const newStack = [...prev, { nodes: [...nodes], edges: [...edges] }];
      // 限制历史记录大小
      if (newStack.length > MAX_HISTORY_SIZE) {
        return newStack.slice(-MAX_HISTORY_SIZE);
      }
      return newStack;
    });
    // 新操作清空重做栈
    setRedoStack([]);
  }, [nodes, edges]);

  /**
   * 撤销操作
   */
  const undo = useCallback(() => {
    if (undoStack.length === 0) return;

    const previousState = undoStack[undoStack.length - 1];

    // 保存当前状态到重做栈
    setRedoStack((prev) => [...prev, { nodes: [...nodes], edges: [...edges] }]);

    // 恢复上一个状态
    isUndoRedoAction.current = true;
    setNodes(previousState.nodes);
    setEdges(previousState.edges);

    // 从撤销栈移除
    setUndoStack((prev) => prev.slice(0, -1));
  }, [undoStack, nodes, edges, setNodes, setEdges]);

  /**
   * 重做操作
   */
  const redo = useCallback(() => {
    if (redoStack.length === 0) return;

    const nextState = redoStack[redoStack.length - 1];

    // 保存当前状态到撤销栈
    setUndoStack((prev) => [...prev, { nodes: [...nodes], edges: [...edges] }]);

    // 恢复下一个状态
    isUndoRedoAction.current = true;
    setNodes(nextState.nodes);
    setEdges(nextState.edges);

    // 从重做栈移除
    setRedoStack((prev) => prev.slice(0, -1));
  }, [redoStack, nodes, edges, setNodes, setEdges]);

  // 撤销/重做状态
  const canUndo = undoStack.length > 0;
  const canRedo = redoStack.length > 0;

  /**
   * 将 WorkflowGraph 转换为 ReactFlow 格式
   */
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
    Object.entries(graph.edgeDetails).forEach(([sourceId, edgeList]) => {
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

  /**
   * 将 ReactFlow 格式转换为 WorkflowGraph
   */
  const convertToWorkflowGraph = useCallback((): WorkflowGraph => {
    const workflowNodes: Record<string, WorkflowNode> = {};
    const edgesMap: Record<string, string[]> = {};
    const edgeDetailsMap: Record<string, WorkflowEdge[]> = {};

    // 转换节点
    nodes.forEach((node) => {
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

    // 转换边
    edges.forEach((edge) => {
      // 更新 edges map
      if (!edgesMap[edge.source]) {
        edgesMap[edge.source] = [];
      }
      edgesMap[edge.source].push(edge.target);

      // 更新 edgeDetails map
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

  /**
   * 添加节点
   */
  const addNode = useCallback(
    (type: NodeType, position: { x: number; y: number }) => {
      saveToHistory();
      const newNode: ReactFlowNode = {
        id: `node_${Date.now()}`,
        type: getReactFlowNodeType(type),
        position,
        data: {
          label: getDefaultNodeName(type),
          nodeType: type,
          config: { properties: {} },
          inputs: {},
          outputs: {}
        }
      };

      setNodes((nds) => [...nds, newNode]);
      message.success(`已添加 ${getDefaultNodeName(type)} 节点`);
    },
    [setNodes, saveToHistory]
  );

  /**
   * 拖拽经过画布时的处理
   */
  const onDragOver = useCallback((event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  /**
   * 拖拽放置节点
   */
  const onDrop = useCallback(
    (event: DragEvent<HTMLDivElement>, reactFlowInstance: ReactFlowInstance) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');
      if (!type) return;

      // 使用 screenToFlowPosition 转换坐标
      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX,
        y: event.clientY
      });

      addNode(type as NodeType, position);
    },
    [addNode]
  );

  /**
   * 复制节点
   */
  const duplicateNode = useCallback(
    (nodeId: string) => {
      const nodeToDuplicate = nodes.find((n) => n.id === nodeId);
      if (!nodeToDuplicate) {
        message.error('未找到要复制的节点');
        return;
      }

      saveToHistory();

      const newNode: ReactFlowNode = {
        id: `node_${Date.now()}`,
        type: nodeToDuplicate.type,
        position: {
          x: nodeToDuplicate.position.x + 50,
          y: nodeToDuplicate.position.y + 50
        },
        data: {
          ...nodeToDuplicate.data,
          label: `${nodeToDuplicate.data.label} (副本)`
        }
      };

      setNodes((nds) => [...nds, newNode]);
      message.success('节点已复制');
    },
    [nodes, setNodes, saveToHistory]
  );

  /**
   * 删除节点
   */
  const deleteNode = useCallback(
    (nodeId: string) => {
      saveToHistory();
      setNodes((nds) => nds.filter((n) => n.id !== nodeId));
      setEdges((eds) => eds.filter((e) => e.source !== nodeId && e.target !== nodeId));
      message.success('节点已删除');
    },
    [setNodes, setEdges, saveToHistory]
  );

  /**
   * 更新节点数据
   */
  const updateNodeData = useCallback(
    (nodeId: string, data: Partial<ReactFlowNode['data']>) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === nodeId
            ? {
                ...node,
                data: { ...node.data, ...data }
              }
            : node
        )
      );
    },
    [setNodes]
  );

  /**
   * 连接节点
   */
  const onConnect = useCallback(
    (connection: Connection) => {
      saveToHistory();
      const newEdge: ReactFlowEdge = {
        id: `edge_${Date.now()}`,
        source: connection.source!,
        target: connection.target!,
        type: 'default',
        data: {
          edgeType: EdgeType.DEPENDENCY
        }
      };

      setEdges((eds) => addEdge(newEdge, eds) as ReactFlowEdge[]);
    },
    [setEdges, saveToHistory]
  );

  /**
   * 删除边
   */
  const deleteEdge = useCallback(
    (edgeId: string) => {
      saveToHistory();
      setEdges((eds) => eds.filter((e) => e.id !== edgeId));
      message.success('连接已删除');
    },
    [setEdges, saveToHistory]
  );

  /**
   * 启动工作流执行
   */
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
              `[${new Date(data.timestamp).toLocaleTimeString()}] 节点 ${data.nodeId} 执行完成: ${
                data.status
              }`
            ]);
            updateNodeData(data.nodeId, { status: data.status });

            // 如果是最后一个节点,结束执行
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
          onPing: () => {
            // 心跳,不需要处理
          }
        });

        abortControllerRef.current = controller;
      } catch (error: any) {
        setIsExecuting(false);
        message.error(`启动失败: ${error.message}`);
      }
    },
    [isExecuting, updateNodeData]
  );

  /**
   * 停止工作流执行
   */
  const stopExecution = useCallback(async () => {
    if (!executionId) {
      message.warning('没有正在执行的工作流');
      return;
    }

    try {
      // 取消 SSE 连接
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }

      // 调用后端停止接口
      await workflowService.stopExecution({ executionId });

      setIsExecuting(false);
      setExecutionId(null);
      message.success('工作流已停止');
    } catch (error: any) {
      message.error(`停止失败: ${error.message}`);
    }
  }, [executionId]);

  /**
   * 加载工作流图到画布
   */
  const loadGraph = useCallback(
    (graph: WorkflowGraph) => {
      const { nodes: rfNodes, edges: rfEdges } = convertToReactFlow(graph);
      setNodes(rfNodes);
      setEdges(rfEdges);
    },
    [convertToReactFlow, setNodes, setEdges]
  );

  /**
   * 清空画布
   */
  const clearGraph = useCallback(() => {
    setNodes([]);
    setEdges([]);
  }, [setNodes, setEdges]);

  /**
   * 清理执行状态
   */
  const clearExecutionStatus = useCallback(() => {
    setNodes((nds) =>
      nds.map((node) => ({
        ...node,
        data: { ...node.data, status: undefined }
      }))
    );
    setExecutionLogs([]);
    setExecutionId(null);
  }, [setNodes]);

  return {
    // ReactFlow 状态
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,

    // 拖拽支持
    onDragOver,
    onDrop,

    // 节点操作
    addNode,
    deleteNode,
    updateNodeData,
    deleteEdge,
    duplicateNode,

    // 撤销/重做
    undo,
    redo,
    canUndo,
    canRedo,

    // 图操作
    loadGraph,
    clearGraph,
    convertToWorkflowGraph,

    // 执行控制
    isExecuting,
    executionId,
    executionLogs,
    startExecution,
    stopExecution,
    clearExecutionStatus
  };
}

// ========== 辅助函数 ==========

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
      return 'conditional';
    case EdgeType.DEFAULT:
      return 'default';
    default:
      return 'default';
  }
}

function getDefaultNodeName(nodeType: NodeType): string {
  switch (nodeType) {
    case NodeType.START:
      return '开始';
    case NodeType.END:
      return '结束';
    case NodeType.LLM:
      return 'LLM 节点';
    case NodeType.HTTP:
      return 'HTTP 请求';
    case NodeType.CONDITION:
      return '条件分支';
    case NodeType.TOOL:
      return '工具调用';
    default:
      return '未知节点';
  }
}
