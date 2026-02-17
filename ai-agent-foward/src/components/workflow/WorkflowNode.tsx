import { Handle, Position } from '@xyflow/react';
import type { WorkflowNodeData } from '../../types/workflow';

interface WorkflowNodeProps {
  data: WorkflowNodeData;
  selected?: boolean;
}

const NODE_COLORS: Record<string, string> = {
  START: '#52c41a',
  END: '#fa541c',
  LLM: '#1677ff',
  CONDITION: '#722ed1',
  TOOL: '#13c2c2',
  HTTP: '#faad14',
  UNKNOWN: '#8c8c8c'
};

export const WorkflowNode: React.FC<WorkflowNodeProps> = ({ data, selected }) => {
  const color = NODE_COLORS[data.nodeType] || '#1677ff';

  const showTarget = data.nodeType !== 'START';
  const showSource = data.nodeType !== 'END';

  return (
    <div
      style={{
        minWidth: 180,
        border: `2px solid ${selected ? color : '#d9d9d9'}`,
        borderRadius: 12,
        background: '#fff',
        boxShadow: selected ? `0 0 0 4px ${color}22` : '0 2px 8px rgba(0, 0, 0, 0.08)'
      }}
    >
      {showTarget && <Handle type="target" position={Position.Top} />}

      <div
        style={{
          padding: '8px 10px',
          borderBottom: '1px solid #f0f0f0',
          background: `${color}12`,
          borderTopLeftRadius: 10,
          borderTopRightRadius: 10,
          fontSize: 12,
          color,
          fontWeight: 600
        }}
      >
        {data.nodeType}
      </div>

      <div style={{ padding: '10px 12px' }}>
        <div style={{ fontWeight: 600, color: '#1f1f1f', marginBottom: 6 }}>{data.nodeName}</div>
        <div style={{ fontSize: 12, color: '#8c8c8c' }}>
          {Object.keys(data.userConfig || {}).length > 0
            ? `${Object.keys(data.userConfig || {}).length} 个配置项`
            : '未配置'}
        </div>
      </div>

      {showSource && <Handle type="source" position={Position.Bottom} />}
    </div>
  );
};
