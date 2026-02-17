import { Button, Card, Empty, Space, Spin, Typography } from 'antd';
import type { NodeTemplate } from '../../types/execution';

const NODE_DRAG_MIME = 'application/x-ai-agent-workflow-node';

interface NodePanelProps {
  templates: NodeTemplate[];
  metadataLoading?: boolean;
  metadataError?: string | null;
  onReloadMetadata?: () => void;
  onAddNode: (nodeType: string, templateId?: string) => void;
}

const groupTemplates = (templates: NodeTemplate[]) => {
  const sorted = [...templates]
    .filter((template) => template.type?.toUpperCase() !== 'START')
    .sort((a, b) => {
      const sortOrderDiff = (a.sortOrder ?? 9999) - (b.sortOrder ?? 9999);
      if (sortOrderDiff !== 0) return sortOrderDiff;
      return a.name.localeCompare(b.name);
    });

  return sorted.reduce<Record<string, NodeTemplate[]>>((acc, template) => {
    const category = template.category || '其他';
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(template);
    return acc;
  }, {});
};

export const NodePanel: React.FC<NodePanelProps> = ({
  templates,
  metadataLoading,
  metadataError,
  onReloadMetadata,
  onAddNode
}) => {
  const grouped = groupTemplates(templates);
  const categories = Object.keys(grouped);

  const buildNodePayload = (item: NodeTemplate) =>
    JSON.stringify({
      nodeType: item.type,
      templateId: item.templateId || item.id
    });

  const handleDragStart = (event: React.DragEvent, item: NodeTemplate) => {
    const payload = buildNodePayload(item);
    event.dataTransfer.setData(NODE_DRAG_MIME, payload);
    event.dataTransfer.setData('text/plain', payload);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div style={{ width: 280, borderRight: '1px solid #f0f0f0', padding: 12, overflowY: 'auto' }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>节点库</div>

      {metadataLoading ? (
        <div style={{ paddingTop: 16, textAlign: 'center' }}>
          <Spin tip="加载元数据中..." />
        </div>
      ) : null}

      {!metadataLoading && metadataError ? (
        <div style={{ marginBottom: 12 }}>
          <Typography.Text type="danger">{metadataError}</Typography.Text>
          <div style={{ marginTop: 8 }}>
            <Button size="small" onClick={onReloadMetadata}>
              重试
            </Button>
          </div>
        </div>
      ) : null}

      {!metadataLoading && !metadataError && categories.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可用节点模板" />
      ) : null}

      {!metadataLoading && !metadataError && categories.length > 0 ? (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          {categories.map((category) => (
            <div key={category}>
              <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>{category}</div>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                {grouped[category].map((item) => (
                  <Card
                    key={`${item.type}-${item.templateId || item.id || item.name}`}
                    size="small"
                    style={{ borderRadius: 10 }}
                    styles={{ body: { padding: 10 } }}
                    hoverable
                    draggable
                    onDragStart={(event) => handleDragStart(event, item)}
                  >
                    <div style={{ marginBottom: 6, fontWeight: 600 }}>{item.name}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 10 }}>
                      {item.description || item.type}
                    </div>
                    <Button block onClick={() => onAddNode(item.type, item.templateId || item.id)}>
                      添加
                    </Button>
                  </Card>
                ))}
              </Space>
            </div>
          ))}
        </Space>
      ) : null}
    </div>
  );
};
