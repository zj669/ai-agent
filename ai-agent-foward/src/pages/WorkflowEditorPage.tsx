import { useState, useCallback, useEffect, useRef, KeyboardEvent, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useReactFlow,
  ReactFlowProvider,
  BackgroundVariant,
  MarkerType,
  type NodeMouseHandler
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import '../styles/workflow-enhanced.css';
import { Button, message, Modal, Input, Form, Select } from 'antd';
import { Play, Square, Save, ArrowLeft, Check, Loader2 } from 'lucide-react';
import { useWorkflowEditor } from '../hooks/useWorkflowEditor';
import { nodeTypes } from '../components/WorkflowNode';
import { workflowEdgeTypes } from '../components/WorkflowEdge';
import { NodeQuickAddRail } from '../components/NodeQuickAddRail';
import { WorkflowConfigPanel } from '../components/WorkflowConfigPanel';
import { useWorkflowStore } from '../components/workflow/store';
import { NodeType, ExecutionMode, WorkflowGraph, ReactFlowNode, NodeExecutionStatus } from '../types/workflow';
import { AgentDetail } from '../types/agent';
import { agentService } from '../services/agentService';
import { useAuthStore } from '../stores/authStore';

function WorkflowEditorInner() {
  const { agentId } = useParams<{ agentId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { screenToFlowPosition, fitView } = useReactFlow();
  const setSelectedNode = useWorkflowStore((state) => state.setSelectedNode);
  const selectedNodeId = useWorkflowStore((state) => state.selectedNodeId);

  const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
  const [loadingAgent, setLoadingAgent] = useState(false);
  const [workflowName, setWorkflowName] = useState('未命名工作流');
  const [workflowDescription, setWorkflowDescription] = useState('');
  const [saveStatus, setSaveStatus] = useState<'saved' | 'unsaved' | 'saving'>('saved');
  const [showExecutionModal, setShowExecutionModal] = useState(false);
  const [executionForm] = Form.useForm();

  const {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,
    addNode,
    loadGraph,
    convertToWorkflowGraph,
    isExecuting,
    startExecution,
    stopExecution,
    updateNodeData,
    clearExecutionStatus
  } = useWorkflowEditor();

  const enhancedNodes = useMemo(
    () =>
      (nodes as ReactFlowNode[]).map((node) => ({
        ...node,
        data: {
          ...node.data,
          isExpanded: selectedNodeId === node.id,
          onToggleExpand: (nodeId: string) => {
            setSelectedNode(selectedNodeId === nodeId ? null : nodeId);
          },
          onUpdateNodeData: (nodeId: string, patch: Partial<ReactFlowNode['data']>) => {
            updateNodeData(nodeId, patch);
          }
        }
      })),
    [nodes, selectedNodeId, setSelectedNode, updateNodeData]
  );

  useEffect(() => {
    if (!agentId) return;

    let disposed = false;
    const fetchAgent = async () => {
      setLoadingAgent(true);
      try {
        const detail = await agentService.getAgent(parseInt(agentId));
        if (disposed) return;

        setAgentDetail(detail);
        setWorkflowName(detail.name || '未命名工作流');
        setWorkflowDescription(detail.description || '');

        if (detail.graphJson) {
          const graph: WorkflowGraph = JSON.parse(detail.graphJson);
          if (graph.nodes && Object.keys(graph.nodes).length > 0) {
            loadGraph(graph);
          }
        }
      } catch {
        if (!disposed) {
          message.error('加载 Agent 失败');
        }
      } finally {
        if (!disposed) {
          setLoadingAgent(false);
          setSaveStatus('saved');
        }
      }
    };

    fetchAgent();
    return () => {
      disposed = true;
    };
  }, [agentId, loadGraph]);

  useEffect(() => {
    if (!loadingAgent && agentDetail) {
      setSaveStatus('unsaved');
    }
  }, [nodes, edges, workflowName, workflowDescription, loadingAgent, agentDetail]);

  const handleAddNodeFromDock = useCallback(
    (type: NodeType) => {
      const wrapperRect = reactFlowWrapper.current?.getBoundingClientRect();
      if (!wrapperRect) {
        addNode(type, { x: 320, y: 260 });
        return;
      }

      const centerPosition = screenToFlowPosition({
        x: wrapperRect.left + wrapperRect.width / 2,
        y: wrapperRect.top + wrapperRect.height / 2
      });
      addNode(type, centerPosition);
    },
    [addNode, screenToFlowPosition]
  );

  const handleSave = useCallback(async () => {
    if (!agentId || !agentDetail) {
      message.error('Agent 数据未加载');
      return;
    }

    setSaveStatus('saving');
    try {
      const graph = convertToWorkflowGraph();
      const graphJson = JSON.stringify(graph);

      await agentService.updateAgent({
        id: parseInt(agentId),
        name: workflowName,
        description: workflowDescription,
        icon: agentDetail.icon,
        graphJson,
        version: agentDetail.version
      });

      setAgentDetail((prev) =>
        prev
          ? {
              ...prev,
              version: prev.version + 1,
              graphJson,
              name: workflowName,
              description: workflowDescription
            }
          : prev
      );
      setSaveStatus('saved');
      message.success('工作流已保存');
    } catch (error: any) {
      setSaveStatus('unsaved');
      message.error(`保存失败: ${error.response?.data?.message || error.message}`);
    }
  }, [agentId, agentDetail, workflowName, workflowDescription, convertToWorkflowGraph]);

  const handleLoad = useCallback(() => {
    if (!agentDetail?.graphJson) {
      message.warning('没有可加载的工作流草稿');
      return;
    }

    try {
      const graph: WorkflowGraph = JSON.parse(agentDetail.graphJson);
      loadGraph(graph);
      clearExecutionStatus();
      setSelectedNode(null);
      message.success('工作流已加载');
    } catch {
      message.error('加载失败：工作流 JSON 无效');
    }
  }, [agentDetail?.graphJson, clearExecutionStatus, loadGraph, setSelectedNode]);

  const handleOpenExecutionModal = useCallback(() => {
    if (nodes.length === 0) {
      message.warning('请先添加节点');
      return;
    }
    executionForm.setFieldsValue({
      conversationId: `conv_${Date.now()}`,
      mode: ExecutionMode.STANDARD
    });
    setShowExecutionModal(true);
  }, [nodes, executionForm]);

  const handleExecute = useCallback(async () => {
    try {
      const values = await executionForm.validateFields();
      if (!agentId || !user) {
        message.error('缺少必要参数');
        return;
      }

      await startExecution({
        agentId: parseInt(agentId),
        userId: user.id,
        conversationId: values.conversationId,
        inputs: values.inputs ? JSON.parse(values.inputs) : {},
        mode: values.mode
      });

      setShowExecutionModal(false);
      message.success('工作流已启动');
    } catch (error: any) {
      message.error(`启动失败: ${error.message}`);
    }
  }, [agentId, user, executionForm, startExecution]);

  const handleMockRun = useCallback(() => {
    if (nodes.length === 0) {
      message.warning('请先添加节点');
      return;
    }

    clearExecutionStatus();
    const queue = [...nodes];
    const failIndex = queue.length > 1 ? queue.length - 2 : -1;

    queue.forEach((node, index) => {
      setTimeout(() => {
        updateNodeData(node.id, { status: NodeExecutionStatus.RUNNING });
      }, index * 600);

      setTimeout(() => {
        const status = index === failIndex ? NodeExecutionStatus.FAILED : NodeExecutionStatus.SUCCEEDED;
        updateNodeData(node.id, { status });
      }, index * 600 + 420);
    });

    message.success('已模拟运行状态流转（含成功/失败）');
  }, [clearExecutionStatus, nodes, updateNodeData]);

  const handleStop = useCallback(async () => {
    await stopExecution();
    message.success('工作流已停止');
  }, [stopExecution]);

  const handleNodeClick: NodeMouseHandler = useCallback(
    (_event, node) => {
      setSelectedNode(node.id);
    },
    [setSelectedNode]
  );

  const handlePaneClick = useCallback(() => {
    setSelectedNode(null);
  }, [setSelectedNode]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent<HTMLDivElement>) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 's') {
        event.preventDefault();
        handleSave();
      }
    },
    [handleSave]
  );

  const renderSaveStatus = () => {
    if (saveStatus === 'saving') {
      return (
        <span className="flex items-center gap-1 text-xs text-slate-500">
          <Loader2 className="w-3 h-3 animate-spin" />
          保存中
        </span>
      );
    }
    if (saveStatus === 'saved') {
      return (
        <span className="flex items-center gap-1 text-xs text-emerald-600">
          <Check className="w-3 h-3" />
          已保存
        </span>
      );
    }
    return <span className="text-xs text-amber-600">未保存</span>;
  };

  return (
    <div className="h-full min-h-full" onKeyDown={handleKeyDown} tabIndex={0}>
      <div className="workflow-topbar flex h-14 items-center justify-between px-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/agents')}
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <Input
            value={workflowName}
            onChange={(e) => setWorkflowName(e.target.value)}
            className="w-56"
            placeholder="工作流名称"
          />
          {renderSaveStatus()}
        </div>

        <div className="flex items-center gap-2">
          <Button onClick={() => fitView({ padding: 0.2 })}>适应画布</Button>
          <Button onClick={handleLoad}>加载</Button>
          <Button onClick={handleMockRun}>模拟运行</Button>
          {!isExecuting ? (
            <Button type="primary" icon={<Play className="w-4 h-4" />} onClick={handleOpenExecutionModal}>
              运行
            </Button>
          ) : (
            <Button danger icon={<Square className="w-4 h-4" />} onClick={handleStop}>
              停止
            </Button>
          )}
          <Button icon={<Save className="w-4 h-4" />} onClick={handleSave}>
            保存
          </Button>
        </div>
      </div>

      <div ref={reactFlowWrapper} className="workflow-canvas-shell relative h-[calc(100%-56px)] w-full">
        <ReactFlow
          nodes={enhancedNodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={handleNodeClick}
          onPaneClick={handlePaneClick}
          nodeTypes={nodeTypes}
          edgeTypes={workflowEdgeTypes}
          fitView
          fitViewOptions={{ padding: 0.2 }}
          panOnScroll
          defaultEdgeOptions={{
            type: 'smoothstep',
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: '#c4b5fd',
              width: 16,
              height: 16
            },
            style: {
              stroke: '#c4b5fd',
              strokeWidth: 2.2
            }
          }}
          connectionLineStyle={{ strokeWidth: 2.2, stroke: '#c4b5fd' }}
        >
          <Background variant={BackgroundVariant.Dots} gap={12} size={1} color="#d4d4d8" />
          <Controls className="!rounded-xl !border !border-slate-200 !shadow-md" />
          <MiniMap
            pannable
            zoomable
            maskColor="rgba(15, 23, 42, 0.06)"
            className="!rounded-xl !border !border-slate-200 !bg-white/90 !shadow-md"
          />
        </ReactFlow>

        <WorkflowConfigPanel
          workflowName={workflowName}
          workflowDescription={workflowDescription}
          onWorkflowNameChange={setWorkflowName}
          onWorkflowDescriptionChange={setWorkflowDescription}
          nodeCount={nodes.length}
          edgeCount={edges.length}
        />

        <NodeQuickAddRail onAddNode={handleAddNodeFromDock} />
      </div>

      <Modal
        title="执行工作流"
        open={showExecutionModal}
        onOk={handleExecute}
        onCancel={() => setShowExecutionModal(false)}
        okText="开始执行"
        cancelText="取消"
      >
        <Form form={executionForm} layout="vertical">
          <Form.Item
            label="会话 ID"
            name="conversationId"
            rules={[{ required: true, message: '请输入会话 ID' }]}
          >
            <Input placeholder="conv_123" />
          </Form.Item>

          <Form.Item label="输入参数 (JSON)" name="inputs">
            <Input.TextArea rows={4} placeholder='{"query": "你好"}' />
          </Form.Item>

          <Form.Item label="执行模式" name="mode">
            <Select
              options={[
                { value: ExecutionMode.STANDARD, label: '标准模式' },
                { value: ExecutionMode.DEBUG, label: '调试模式' },
                { value: ExecutionMode.DRY_RUN, label: '试运行' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export function WorkflowEditorPage() {
  return (
    <ReactFlowProvider>
      <WorkflowEditorInner />
    </ReactFlowProvider>
  );
}
