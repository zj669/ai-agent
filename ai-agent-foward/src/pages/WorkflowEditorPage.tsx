import { useState, useCallback, useEffect, useRef, DragEvent, KeyboardEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Panel,
  useReactFlow,
  ReactFlowProvider,
  BackgroundVariant
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { Button, message, Modal, Input, Form, Select, Dropdown, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import {
  Play,
  Square,
  Save,
  FileDown,
  FileUp,
  Trash2,
  ArrowLeft,
  Undo2,
  Redo2,
  ZoomIn,
  ZoomOut,
  Maximize2,
  MoreHorizontal,
  Check,
  Loader2,
  MousePointer2
} from 'lucide-react';
import { useWorkflowEditor } from '../hooks/useWorkflowEditor';
import { nodeTypes } from '../components/WorkflowNode';
import { NodePanel } from '../components/NodePanel';
import { NodePropertiesPanel } from '../components/NodePropertiesPanel';
import { ExecutionLogPanel } from '../components/ExecutionLogPanel';
import { NodeType, ExecutionMode, WorkflowGraph, ReactFlowNode, ExecutionContextDTO } from '../types/workflow';
import { AgentDetail } from '../types/agent';
import { agentService } from '../services/agentService';
import { useAuthStore } from '../stores/authStore';
import { workflowService } from '../services/workflowService';

/**
 * 工作流编辑器内部组件 - 需要在 ReactFlowProvider 内部使用
 */
function WorkflowEditorInner() {
  const { agentId } = useParams<{ agentId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { screenToFlowPosition, zoomIn, zoomOut, fitView, setViewport, getViewport } = useReactFlow();

  // Agent 数据
  const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
  const [loadingAgent, setLoadingAgent] = useState(false);
  const [workflowName, setWorkflowName] = useState('未命名工作流');
  const [isEditingName, setIsEditingName] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'saved' | 'unsaved' | 'saving'>('saved');

  // 工作流编辑器状态
  const {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,
    addNode,
    deleteNode,
    updateNodeData,
    loadGraph,
    clearGraph,
    convertToWorkflowGraph,
    isExecuting,
    executionId,
    executionLogs,
    startExecution,
    stopExecution
  } = useWorkflowEditor();

  // UI 状态
  const [selectedNode, setSelectedNode] = useState<ReactFlowNode | null>(null);
  const [nodePanelCollapsed, setNodePanelCollapsed] = useState(false);
  const [showLogPanel, setShowLogPanel] = useState(false);
  const [showExecutionModal, setShowExecutionModal] = useState(false);
  const [executionForm] = Form.useForm();
  const [zoom, setZoom] = useState(100);
  const [executionContext, setExecutionContext] = useState<ExecutionContextDTO | null>(null);

  // 加载 Agent 数据和工作流图
  useEffect(() => {
    if (!agentId) return;
    const fetchAgent = async () => {
      setLoadingAgent(true);
      try {
        const detail = await agentService.getAgent(parseInt(agentId));
        setAgentDetail(detail);
        setWorkflowName(detail.name || '未命名工作流');
        if (detail.graphJson) {
          try {
            const graph: WorkflowGraph = JSON.parse(detail.graphJson);
            if (graph.nodes && Object.keys(graph.nodes).length > 0) {
              loadGraph(graph);
            }
          } catch (e) {
            console.warn('无法解析工作流图:', e);
          }
        }
      } catch (error: any) {
        message.error('加载 Agent 失败');
      } finally {
        setLoadingAgent(false);
      }
    };
    fetchAgent();
  }, [agentId, loadGraph]);

  // 监听节点/边变化，标记为未保存
  useEffect(() => {
    if (!loadingAgent && agentDetail) {
      setSaveStatus('unsaved');
    }
  }, [nodes, edges]);

  // 更新缩放比例
  useEffect(() => {
    const viewport = getViewport();
    setZoom(Math.round(viewport.zoom * 100));
  }, [getViewport]);

  // 拉取执行上下文快照（变量检查）
  useEffect(() => {
    if (!executionId) {
      setExecutionContext(null);
      return;
    }
    let disposed = false;
    const fetchContext = async () => {
      try {
        const context = await workflowService.getExecutionContext(executionId);
        if (!disposed) {
          setExecutionContext(context);
        }
      } catch {
        // 保持静默，避免影响执行主流程
      }
    };
    fetchContext();
    const timer = window.setInterval(fetchContext, 3000);
    return () => {
      disposed = true;
      window.clearInterval(timer);
    };
  }, [executionId]);

  // 处理拖拽放置
  const handleDragOver = useCallback((event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const handleDrop = useCallback(
    (event: DragEvent<HTMLDivElement>) => {
      event.preventDefault();

      const nodeType = event.dataTransfer.getData('application/reactflow') as NodeType;
      if (!nodeType) return;

      // 将屏幕坐标转换为画布坐标
      const position = screenToFlowPosition({
        x: event.clientX,
        y: event.clientY
      });

      addNode(nodeType, position);
    },
    [screenToFlowPosition, addNode]
  );

  // 节点点击事件
  const handleNodeClick = useCallback((_event: React.MouseEvent, node: any) => {
    setSelectedNode(node as ReactFlowNode);
  }, []);

  // 画布点击事件（取消选中）
  const handlePaneClick = useCallback(() => {
    setSelectedNode(null);
  }, []);

  // 关闭属性面板
  const handleClosePropertiesPanel = useCallback(() => {
    setSelectedNode(null);
  }, []);

  // 保存工作流
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
        description: agentDetail.description,
        icon: agentDetail.icon,
        graphJson,
        version: agentDetail.version
      });

      setAgentDetail((prev) =>
        prev ? { ...prev, version: prev.version + 1, graphJson, name: workflowName } : prev
      );
      setSaveStatus('saved');
      message.success('工作流已保存');
    } catch (error: any) {
      setSaveStatus('unsaved');
      message.error(`保存失败: ${error.response?.data?.message || error.message}`);
    }
  }, [agentId, agentDetail, workflowName, convertToWorkflowGraph]);

  // 快捷键处理
  const handleKeyDown = useCallback(
    (event: KeyboardEvent<HTMLDivElement>) => {
      // Ctrl+S 保存
      if ((event.ctrlKey || event.metaKey) && event.key === 's') {
        event.preventDefault();
        handleSave();
      }
      // Delete 删除选中节点
      if (event.key === 'Delete' && selectedNode) {
        deleteNode(selectedNode.id);
        setSelectedNode(null);
      }
    },
    [handleSave, selectedNode, deleteNode]
  );

  // 缩放控制
  const handleZoomIn = useCallback(() => {
    zoomIn();
    setTimeout(() => setZoom(Math.round(getViewport().zoom * 100)), 50);
  }, [zoomIn, getViewport]);

  const handleZoomOut = useCallback(() => {
    zoomOut();
    setTimeout(() => setZoom(Math.round(getViewport().zoom * 100)), 50);
  }, [zoomOut, getViewport]);

  const handleFitView = useCallback(() => {
    fitView({ padding: 0.2 });
    setTimeout(() => setZoom(Math.round(getViewport().zoom * 100)), 50);
  }, [fitView, getViewport]);

  const handleResetZoom = useCallback(() => {
    const viewport = getViewport();
    setViewport({ ...viewport, zoom: 1 });
    setZoom(100);
  }, [getViewport, setViewport]);

  // 导出工作流
  const handleExport = useCallback(() => {
    try {
      const graph = convertToWorkflowGraph();
      const dataStr = JSON.stringify(graph, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(dataBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${workflowName}_${Date.now()}.json`;
      link.click();
      URL.revokeObjectURL(url);
      message.success('工作流已导出');
    } catch (error: any) {
      message.error(`导出失败: ${error.message}`);
    }
  }, [convertToWorkflowGraph, workflowName]);

  // 导入工作流
  const handleImport = useCallback(() => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'application/json';
    input.onchange = async (e: any) => {
      try {
        const file = e.target.files[0];
        const text = await file.text();
        const graph: WorkflowGraph = JSON.parse(text);
        loadGraph(graph);
        message.success('工作流已导入');
      } catch (error: any) {
        message.error(`导入失败: ${error.message}`);
      }
    };
    input.click();
  }, [loadGraph]);

  // 清空画布
  const handleClear = useCallback(() => {
    Modal.confirm({
      title: '确认清空',
      content: '确定要清空整个画布吗？此操作不可恢复。',
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => {
        clearGraph();
        setSelectedNode(null);
        message.success('画布已清空');
      }
    });
  }, [clearGraph]);

  // 打开执行对话框
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

  // 执行工作流
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
      setShowLogPanel(true);
      message.success('工作流已启动');
    } catch (error: any) {
      message.error(`启动失败: ${error.message}`);
    }
  }, [agentId, user, executionForm, startExecution]);

  // 停止执行
  const handleStop = useCallback(async () => {
    await stopExecution();
    message.success('工作流已停止');
  }, [stopExecution]);

  // 单步调试节点（使用 DEBUG 模式）
  const handleStepRunNode = useCallback(
    async (nodeId: string) => {
      if (!agentId || !user) {
        message.error('缺少必要参数');
        return;
      }
      const node = nodes.find((n) => n.id === nodeId);
      await startExecution({
        agentId: parseInt(agentId),
        userId: user.id,
        conversationId: `step_${nodeId}_${Date.now()}`,
        inputs: {
          __stepNodeId: nodeId
        },
        mode: ExecutionMode.DEBUG
      });
      setShowLogPanel(true);
      message.success(`已启动节点调试: ${node?.data.label || nodeId}`);
    },
    [agentId, user, nodes, startExecution]
  );

  const nodeScopedLogs = selectedNode
    ? executionLogs.filter((log) => log.includes(selectedNode.id) || log.includes(selectedNode.data.label))
    : [];

  // 复制节点
  const handleDuplicateNode = useCallback(
    (nodeId: string) => {
      const node = nodes.find((n) => n.id === nodeId);
      if (node) {
        const newPosition = {
          x: node.position.x + 50,
          y: node.position.y + 50
        };
        addNode(node.data.nodeType, newPosition);
      }
    },
    [nodes, addNode]
  );

  // 更多操作下拉菜单
  const moreMenuItems: MenuProps['items'] = [
    {
      key: 'import',
      label: '导入工作流',
      icon: <FileUp className="w-4 h-4" />,
      onClick: handleImport
    },
    {
      key: 'export',
      label: '导出工作流',
      icon: <FileDown className="w-4 h-4" />,
      onClick: handleExport
    },
    { type: 'divider' },
    {
      key: 'clear',
      label: '清空画布',
      icon: <Trash2 className="w-4 h-4" />,
      danger: true,
      onClick: handleClear
    }
  ];

  // 渲染保存状态
  const renderSaveStatus = () => {
    switch (saveStatus) {
      case 'saving':
        return (
          <span className="flex items-center gap-1 text-xs text-slate-400">
            <Loader2 className="w-3 h-3 animate-spin" />
            保存中...
          </span>
        );
      case 'saved':
        return (
          <span className="flex items-center gap-1 text-xs text-emerald-400">
            <Check className="w-3 h-3" />
            已保存
          </span>
        );
      case 'unsaved':
        return <span className="text-xs text-amber-400">未保存</span>;
    }
  };

  return (
    <div
      className="workflow-editor-shell h-full min-h-full flex flex-col"
      onKeyDown={handleKeyDown}
      tabIndex={0}
    >
      {/* 顶部工具栏 */}
      <div className="workflow-toolbar h-14 px-4 flex items-center justify-between flex-shrink-0">
        {/* 左侧：返回 + 名称 + 保存状态 */}
        <div className="flex items-center gap-3">
          <Tooltip title="返回">
            <button
              onClick={() => navigate('/agents')}
              className="workflow-toolbar-btn p-2 rounded-lg"
            >
              <ArrowLeft className="w-5 h-5 text-slate-200" />
            </button>
          </Tooltip>

          <div className="h-6 w-px bg-slate-700/80" />

          {isEditingName ? (
            <Input
              value={workflowName}
              onChange={(e) => setWorkflowName(e.target.value)}
              onBlur={() => setIsEditingName(false)}
              onPressEnter={() => setIsEditingName(false)}
              autoFocus
              className="w-48 font-semibold"
              size="small"
            />
          ) : (
            <button
              onClick={() => setIsEditingName(true)}
              className="font-semibold text-slate-100 hover:text-cyan-300 transition-colors"
            >
              {workflowName}
            </button>
          )}

          {renderSaveStatus()}

          {executionId && (
            <span className="px-2 py-1 text-xs bg-cyan-500/20 text-cyan-300 rounded-full border border-cyan-400/40">
              执行中
            </span>
          )}
        </div>

        {/* 中间：撤销/重做 + 缩放控制 */}
        <div className="flex items-center gap-1 bg-slate-900/70 border border-slate-700 rounded-lg p-1">
          <Tooltip title="撤销 (Ctrl+Z)">
            <button className="p-2 hover:bg-slate-800 rounded-md transition-colors text-slate-500 cursor-not-allowed">
              <Undo2 className="w-4 h-4" />
            </button>
          </Tooltip>
          <Tooltip title="重做 (Ctrl+Shift+Z)">
            <button className="p-2 hover:bg-slate-800 rounded-md transition-colors text-slate-500 cursor-not-allowed">
              <Redo2 className="w-4 h-4" />
            </button>
          </Tooltip>

          <div className="h-4 w-px bg-slate-700 mx-1" />

          <Tooltip title="缩小">
            <button
              onClick={handleZoomOut}
              className="workflow-toolbar-btn p-2 rounded-md text-slate-300"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
          </Tooltip>
          <button
            onClick={handleResetZoom}
            className="workflow-toolbar-btn px-2 py-1 text-xs font-medium text-slate-300 rounded-md min-w-[48px]"
          >
            {zoom}%
          </button>
          <Tooltip title="放大">
            <button
              onClick={handleZoomIn}
              className="workflow-toolbar-btn p-2 rounded-md text-slate-300"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
          </Tooltip>
          <Tooltip title="适应画布">
            <button
              onClick={handleFitView}
              className="workflow-toolbar-btn p-2 rounded-md text-slate-300"
            >
              <Maximize2 className="w-4 h-4" />
            </button>
          </Tooltip>
        </div>

        {/* 右侧：运行/停止 + 保存 + 更多 */}
        <div className="flex items-center gap-2">
          {!isExecuting ? (
            <Button
              type="primary"
              icon={<Play className="w-4 h-4" />}
              onClick={handleOpenExecutionModal}
              className="!bg-emerald-500 hover:!bg-emerald-400 !border-emerald-500"
            >
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

          <Dropdown menu={{ items: moreMenuItems }} trigger={['click']}>
            <Button icon={<MoreHorizontal className="w-4 h-4" />} />
          </Dropdown>
        </div>
      </div>

      {/* 主内容区 - 三栏布局 */}
      <div className="flex-1 flex overflow-hidden">
        {/* 左侧：节点面板 */}
        <NodePanel
          onAddNode={(type) => addNode(type, { x: 400, y: 300 })}
          collapsed={nodePanelCollapsed}
          onCollapsedChange={setNodePanelCollapsed}
        />

        {/* 中间：ReactFlow 画布 */}
        <div
          ref={reactFlowWrapper}
          className="workflow-canvas-wrap flex-1 relative"
          onDragOver={handleDragOver}
          onDrop={handleDrop}
        >
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={handleNodeClick}
            onPaneClick={handlePaneClick}
            nodeTypes={nodeTypes}
            fitView
            fitViewOptions={{ padding: 0.2 }}
            defaultEdgeOptions={{
              type: 'smoothstep',
              style: { strokeWidth: 2, stroke: '#94a3b8' }
            }}
            connectionLineStyle={{ strokeWidth: 2, stroke: '#3b82f6' }}
            className="workflow-canvas"
          >
            <Background variant={BackgroundVariant.Dots} gap={24} size={1.4} color="#334155" />
            <Controls
              showZoom={false}
              showFitView={false}
              showInteractive={false}
              className="!shadow-lg !rounded-lg"
            />
            <MiniMap
              nodeColor={(node) => {
                switch (node.data?.nodeType) {
                  case NodeType.START:
                    return '#10b981';
                  case NodeType.END:
                    return '#ef4444';
                  case NodeType.LLM:
                    return '#8b5cf6';
                  case NodeType.HTTP:
                    return '#3b82f6';
                  case NodeType.CONDITION:
                    return '#f59e0b';
                  case NodeType.TOOL:
                    return '#06b6d4';
                  default:
                    return '#6b7280';
                }
              }}
              maskColor="rgba(14, 19, 30, 0.24)"
              className="!shadow-lg !rounded-lg"
            />

            {/* 空状态提示 */}
            {nodes.length === 0 && (
              <Panel position="top-center" className="!top-1/3">
                <div className="workflow-empty-state rounded-xl shadow-lg px-8 py-6 text-center border">
                  <div className="w-16 h-16 mx-auto mb-4 bg-slate-800 rounded-full flex items-center justify-center">
                    <MousePointer2 className="w-8 h-8 text-slate-300" />
                  </div>
                  <h3 className="text-lg font-semibold text-slate-100 mb-2">开始构建工作流</h3>
                  <p className="text-slate-400 text-sm max-w-xs">
                    从左侧面板拖拽节点到画布，或点击节点直接添加
                  </p>
                </div>
              </Panel>
            )}
          </ReactFlow>

          {/* 执行日志面板 */}
          {showLogPanel && (
            <div className="absolute right-4 bottom-4 z-10">
              <ExecutionLogPanel
                logs={executionLogs}
                isExecuting={isExecuting}
                onClose={() => setShowLogPanel(false)}
              />
            </div>
          )}
        </div>

        {/* 右侧：属性面板 */}
        <NodePropertiesPanel
          node={selectedNode}
          onClose={handleClosePropertiesPanel}
          onUpdate={updateNodeData}
          onDelete={(nodeId) => {
            deleteNode(nodeId);
            setSelectedNode(null);
          }}
          onDuplicate={handleDuplicateNode}
          onStepRun={handleStepRunNode}
          nodeExecutionLogs={nodeScopedLogs}
          executionContext={executionContext}
          isExecuting={isExecuting}
        />
      </div>

      {/* 底部状态栏 */}
      <div className="workflow-statusbar h-8 px-4 flex items-center justify-between text-xs flex-shrink-0">
        <div className="flex items-center gap-4">
          <span>节点: {nodes.length}</span>
          <span>连接: {edges.length}</span>
        </div>
        <div className="flex items-center gap-4">
          <span>缩放: {zoom}%</span>
          {selectedNode && <span>选中: {selectedNode.data.label}</span>}
        </div>
      </div>

      {/* 执行对话框 */}
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

/**
 * 工作流编辑器页面 - 包装 ReactFlowProvider
 */
export function WorkflowEditorPage() {
  return (
    <ReactFlowProvider>
      <WorkflowEditorInner />
    </ReactFlowProvider>
  );
}
