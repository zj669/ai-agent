import { memo, useMemo } from 'react';
import { Handle, Position, NodeProps, Node } from '@xyflow/react';
import {
  PlayCircle,
  StopCircle,
  MessageSquare,
  Globe,
  GitBranch,
  Wrench,
  Loader2,
  CheckCircle2,
  XCircle,
  MinusCircle,
  AlertCircle
} from 'lucide-react';
import { Form, Input, Select } from 'antd';
import { NodeType, NodeExecutionStatus, NodeConfig } from '../types/workflow';

interface WorkflowNodeData extends Record<string, unknown> {
  label: string;
  nodeType: NodeType;
  config?: NodeConfig;
  status?: NodeExecutionStatus;
  isExpanded?: boolean;
  onToggleExpand?: (nodeId: string) => void;
  onUpdateNodeData?: (nodeId: string, patch: Partial<WorkflowNodeData>) => void;
}

type WorkflowNodeType = Node<WorkflowNodeData>;

const NODE_CONFIG = {
  [NodeType.START]: {
    icon: PlayCircle,
    label: '开始',
    iconBg: 'bg-emerald-500',
    iconColor: 'text-white',
    badge: 'text-emerald-700 bg-emerald-100'
  },
  [NodeType.END]: {
    icon: StopCircle,
    label: '结束',
    iconBg: 'bg-rose-500',
    iconColor: 'text-white',
    badge: 'text-rose-700 bg-rose-100'
  },
  [NodeType.LLM]: {
    icon: MessageSquare,
    label: 'LLM',
    iconBg: 'bg-violet-500',
    iconColor: 'text-white',
    badge: 'text-violet-700 bg-violet-100'
  },
  [NodeType.HTTP]: {
    icon: Globe,
    label: 'HTTP',
    iconBg: 'bg-blue-500',
    iconColor: 'text-white',
    badge: 'text-blue-700 bg-blue-100'
  },
  [NodeType.CONDITION]: {
    icon: GitBranch,
    label: '条件',
    iconBg: 'bg-amber-500',
    iconColor: 'text-white',
    badge: 'text-amber-700 bg-amber-100'
  },
  [NodeType.TOOL]: {
    icon: Wrench,
    label: '工具',
    iconBg: 'bg-cyan-500',
    iconColor: 'text-white',
    badge: 'text-cyan-700 bg-cyan-100'
  }
};

const STATUS_CONFIG = {
  [NodeExecutionStatus.PENDING]: {
    icon: null,
    color: 'text-slate-400',
    ring: 'ring-0 ring-transparent',
    animate: false
  },
  [NodeExecutionStatus.RUNNING]: {
    icon: Loader2,
    color: 'text-blue-500',
    ring: 'ring-2 ring-blue-200',
    animate: true
  },
  [NodeExecutionStatus.SUCCEEDED]: {
    icon: CheckCircle2,
    color: 'text-emerald-500',
    ring: 'ring-2 ring-emerald-200',
    animate: false
  },
  [NodeExecutionStatus.FAILED]: {
    icon: XCircle,
    color: 'text-rose-500',
    ring: 'ring-2 ring-rose-200',
    animate: false
  },
  [NodeExecutionStatus.SKIPPED]: {
    icon: MinusCircle,
    color: 'text-slate-400',
    ring: 'ring-1 ring-slate-200',
    animate: false
  }
};

function getPrimaryOption(nodeType: NodeType) {
  if (nodeType === NodeType.LLM) {
    return {
      key: 'model',
      label: '模型',
      options: [
        { label: 'gpt-4o-mini', value: 'gpt-4o-mini' },
        { label: 'gpt-4o', value: 'gpt-4o' },
        { label: 'claude-sonnet-4-5', value: 'claude-sonnet-4-5' }
      ]
    };
  }
  if (nodeType === NodeType.START) {
    return {
      key: 'inputKey',
      label: '输入字段',
      options: [
        { label: 'query', value: 'query' },
        { label: 'task', value: 'task' },
        { label: 'message', value: 'message' }
      ]
    };
  }
  return {
    key: 'outputKey',
    label: '输出字段',
    options: [
      { label: 'result', value: 'result' },
      { label: 'answer', value: 'answer' },
      { label: 'summary', value: 'summary' }
    ]
  };
}

export const WorkflowNode = memo(({ id, data, selected }: NodeProps<WorkflowNodeType>) => {
  const { label, nodeType, config, status, isExpanded, onToggleExpand, onUpdateNodeData } = data as WorkflowNodeData;

  const nodeConfig = useMemo(() => NODE_CONFIG[nodeType] || NODE_CONFIG[NodeType.START], [nodeType]);
  const statusConfig = useMemo(() => STATUS_CONFIG[status || NodeExecutionStatus.PENDING], [status]);
  const primaryOption = useMemo(() => getPrimaryOption(nodeType), [nodeType]);

  const Icon = nodeConfig.icon;
  const StatusIcon = statusConfig.icon;

  const showInputHandle = nodeType !== NodeType.START;
  const showOutputHandle = nodeType !== NodeType.END;
  const properties = config?.properties || {};

  const primaryValue =
    properties[primaryOption.key] ||
    (nodeType === NodeType.START ? 'query' : nodeType === NodeType.LLM ? 'gpt-4o-mini' : 'result');

  return (
    <div
      className={[
        'relative min-w-[260px] max-w-[340px] rounded-2xl border border-gray-200 bg-white shadow-md transition-all duration-150',
        selected ? 'border-violet-300 ring-2 ring-violet-300' : '',
        statusConfig.ring
      ].join(' ')}
    >
      {showInputHandle && (
        <Handle
          type="target"
          position={Position.Top}
          className="!h-[10px] !w-[10px] !-top-[5px] !border-2 !border-white !bg-violet-300"
        />
      )}

      <button
        type="button"
        onClick={() => onToggleExpand?.(id)}
        className="flex w-full items-center gap-2 rounded-t-2xl border-b border-gray-100 bg-white px-3 py-2.5 text-left"
      >
        <div className={`h-7 w-7 rounded-xl ${nodeConfig.iconBg} ${nodeConfig.iconColor} flex items-center justify-center`}>
          <Icon className="h-4 w-4" />
        </div>
        <span className="text-sm font-semibold text-gray-800">{label}</span>
        <span className={`ml-auto rounded-full px-2 py-0.5 text-[11px] ${nodeConfig.badge}`}>{nodeConfig.label}</span>
        {StatusIcon && <StatusIcon className={`h-4 w-4 ${statusConfig.color} ${statusConfig.animate ? 'animate-spin' : ''}`} />}
        {config?.humanReviewConfig?.enabled && <AlertCircle className="h-4 w-4 text-amber-500" />}
      </button>

      <div className="space-y-2.5 px-3 py-3">
        <Input
          readOnly
          value={String(primaryValue)}
          className="!rounded-xl"
          size="small"
        />
        <Input
          readOnly
          value={String(properties.description || `配置 ${nodeConfig.label} 节点参数`)}
          className="!rounded-xl"
          size="small"
        />
      </div>

      {isExpanded && (
        <div className="border-t border-violet-100 bg-violet-50/60 px-3 py-3">
          <Form layout="vertical" size="small">
            <Form.Item label="节点名称" className="!mb-2">
              <Input
                value={label}
                onChange={(event) => onUpdateNodeData?.(id, { label: event.target.value })}
              />
            </Form.Item>

            <Form.Item label={primaryOption.label} className="!mb-2">
              <Select
                value={primaryValue as string}
                options={primaryOption.options}
                onChange={(value) =>
                  onUpdateNodeData?.(id, {
                    config: {
                      ...(config || { properties: {} }),
                      properties: {
                        ...properties,
                        [primaryOption.key]: value
                      }
                    }
                  })
                }
              />
            </Form.Item>

            <Form.Item label="描述" className="!mb-0">
              <Input.TextArea
                rows={2}
                value={String(properties.description || '')}
                onChange={(event) =>
                  onUpdateNodeData?.(id, {
                    config: {
                      ...(config || { properties: {} }),
                      properties: {
                        ...properties,
                        description: event.target.value
                      }
                    }
                  })
                }
              />
            </Form.Item>
          </Form>
        </div>
      )}

      {showOutputHandle && (
        <Handle
          type="source"
          position={Position.Bottom}
          className="!h-[10px] !w-[10px] !-bottom-[5px] !border-2 !border-white !bg-violet-300"
        />
      )}
    </div>
  );
});

WorkflowNode.displayName = 'WorkflowNode';

export const nodeTypes = {
  start: WorkflowNode,
  end: WorkflowNode,
  llm: WorkflowNode,
  http: WorkflowNode,
  condition: WorkflowNode,
  tool: WorkflowNode,
  default: WorkflowNode
};
