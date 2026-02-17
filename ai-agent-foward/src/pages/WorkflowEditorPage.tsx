import { useCallback, useMemo, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Modal, Space, Spin, message } from 'antd';
import { ArrowLeft, RefreshCw, Save } from 'lucide-react';
import {
  ReactFlow,
  Background,
  ReactFlowProvider,
  useReactFlow,
  type Connection,
  type Node
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useWorkflowEditor } from '../hooks/useWorkflowEditor';
import { WorkflowNode } from '../components/workflow/WorkflowNode';
import { NodePanel } from '../components/workflow/NodePanel';
import { NodePropertiesPanel } from '../components/workflow/NodePropertiesPanel';

const nodeTypes = {
  workflowNode: WorkflowNode
};

const NODE_DRAG_MIME = 'application/x-ai-agent-workflow-node';

const normalizeGraphId = (value?: string | number | null): string => String(value ?? '').trim();

const WorkflowEditorContent: React.FC<{ agentId: number }> = ({ agentId }) => {
  const navigate = useNavigate();
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { screenToFlowPosition } = useReactFlow();

  const {
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
    setSelectedNodeId,
    addNodeFromTemplate,
    onConnect,
    onNodesChange,
    onEdgesChange,
    updateNode,
    removeSelectedNode,
    save,
    reloadLatest,
    reloadMetadata
  } = useWorkflowEditor(agentId);

  const selectedTemplate = useMemo(() => {
    if (!selectedNode) return undefined;

    if (selectedNode.data.templateId) {
      return templates.find(
        (item) =>
          item.templateId === selectedNode.data.templateId ||
          item.id === selectedNode.data.templateId
      );
    }

    return templatesByType[selectedNode.data.rawNodeType || selectedNode.data.nodeType];
  }, [selectedNode, templates, templatesByType]);

  const handleAddNode = useCallback(
    (nodeType: string, templateId?: string) => {
      addNodeFromTemplate({ nodeType, templateId });
    },
    [addNodeFromTemplate]
  );

  const handleCanvasDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const handleCanvasDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      const payloadText =
        event.dataTransfer.getData(NODE_DRAG_MIME) || event.dataTransfer.getData('text/plain');
      if (!payloadText || !reactFlowWrapper.current) return;

      try {
        const payload = JSON.parse(payloadText) as { nodeType?: string; templateId?: string };
        if (!payload.nodeType) return;

        const position = screenToFlowPosition({
          x: event.clientX,
          y: event.clientY
        });

        addNodeFromTemplate({
          nodeType: payload.nodeType,
          templateId: payload.templateId,
          position
        });
      } catch {
        // ignore invalid payload
      }
    },
    [addNodeFromTemplate, screenToFlowPosition]
  );

  const handleSave = async () => {
    const result = await save();
    if (result.ok) {
      message.success('工作流保存成功');
      return;
    }

    if (result.conflict) {
      Modal.confirm({
        title: '检测到版本冲突',
        content: '当前编辑基于旧版本。是否刷新为最新版本后重试保存？',
        okText: '刷新最新版本',
        cancelText: '稍后再说',
        onOk: async () => {
          await reloadLatest();
          message.info('已刷新到最新版本，请确认变更后重试保存');
        }
      });
    }
  };

  const handleBack = () => {
    navigate('/agents');
  };

  const handleNodeClick = useCallback(
    (_: unknown, node: Node) => {
      setSelectedNodeId(node.id);
    },
    [setSelectedNodeId]
  );

  const isValidConnection = useCallback(
    (connection: Connection) => {
      const sourceId = normalizeGraphId(connection.source);
      const targetId = normalizeGraphId(connection.target);

      if (!sourceId || !targetId) {
        return false;
      }

      const sourceNode = nodes.find((node) => normalizeGraphId(node.id) === sourceId);
      const targetNode = nodes.find((node) => normalizeGraphId(node.id) === targetId);

      if (!sourceNode || !targetNode) {
        return false;
      }

      if (sourceNode.data.nodeType === 'END') {
        return false;
      }

      if (targetNode.data.nodeType === 'START') {
        return false;
      }

      return true;
    },
    [nodes]
  );

  const handlePaneClick = useCallback(() => {
    setSelectedNodeId(null);
  }, [setSelectedNodeId]);

  if (loading || metadataLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip={loading ? '加载工作流中...' : '加载节点元数据中...'} />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div
        style={{
          height: 60,
          borderBottom: '1px solid #f0f0f0',
          padding: '0 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff'
        }}
      >
        <Space>
          <Button icon={<ArrowLeft size={16} />} onClick={handleBack}>
            返回
          </Button>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{agent?.name || '工作流编辑器'}</div>
        </Space>

        <Space>
          <Button icon={<RefreshCw size={16} />} onClick={reloadLatest}>
            刷新
          </Button>
          <Button type="primary" icon={<Save size={16} />} onClick={handleSave} loading={saving}>
            保存
          </Button>
        </Space>
      </div>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <NodePanel
          templates={templates}
          metadataLoading={metadataLoading}
          metadataError={metadataError}
          onReloadMetadata={reloadMetadata}
          onAddNode={handleAddNode}
        />

        <div
          ref={reactFlowWrapper}
          style={{ flex: 1, background: '#fafafa' }}
          onDragOver={handleCanvasDragOver}
          onDrop={handleCanvasDrop}
        >
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            isValidConnection={isValidConnection}
            onNodeClick={handleNodeClick}
            onPaneClick={handlePaneClick}
            nodeTypes={nodeTypes}
            fitView
            minZoom={0.2}
            maxZoom={2}
            defaultEdgeOptions={{
              type: 'smoothstep',
              animated: true
            }}
          >
            <Background />
          </ReactFlow>
        </div>

        <NodePropertiesPanel
          node={selectedNode}
          template={selectedTemplate}
          hasUnknownType={Boolean(selectedNode?.data.hasUnknownType)}
          onChange={updateNode}
          onDelete={removeSelectedNode}
        />
      </div>
    </div>
  );
};

export const WorkflowEditorPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const agentId = id ? parseInt(id, 10) : undefined;

  if (!agentId || Number.isNaN(agentId)) {
    return (
      <div style={{ padding: 20 }}>
        <div>无效的 Agent ID</div>
      </div>
    );
  }

  return (
    <ReactFlowProvider>
      <WorkflowEditorContent agentId={agentId} />
    </ReactFlowProvider>
  );
};
