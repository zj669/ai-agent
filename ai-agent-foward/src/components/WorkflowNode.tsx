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
import { NodeType, NodeExecutionStatus, NodeConfig } from '../types/workflow';

interface WorkflowNodeData extends Record<string, unknown> {
  label: string;
  nodeType: NodeType;
  config?: NodeConfig;
  status?: NodeExecutionStatus;
}

type WorkflowNodeType = Node<WorkflowNodeData>;

// 节点类型配置
const NODE_CONFIG = {
  [NodeType.START]: {
    icon: PlayCircle,
    label: '开始',
    gradient: 'from-emerald-400 to-green-500',
    bgColor: 'bg-emerald-50',
    borderColor: 'border-emerald-200',
    textColor: 'text-emerald-700',
    ringColor: 'ring-emerald-400'
  },
  [NodeType.END]: {
    icon: StopCircle,
    label: '结束',
    gradient: 'from-rose-400 to-red-500',
    bgColor: 'bg-rose-50',
    borderColor: 'border-rose-200',
    textColor: 'text-rose-700',
    ringColor: 'ring-rose-400'
  },
  [NodeType.LLM]: {
    icon: MessageSquare,
    label: 'LLM',
    gradient: 'from-violet-400 to-purple-500',
    bgColor: 'bg-violet-50',
    borderColor: 'border-violet-200',
    textColor: 'text-violet-700',
    ringColor: 'ring-violet-400'
  },
  [NodeType.HTTP]: {
    icon: Globe,
    label: 'HTTP',
    gradient: 'from-blue-400 to-indigo-500',
    bgColor: 'bg-blue-50',
    borderColor: 'border-blue-200',
    textColor: 'text-blue-700',
    ringColor: 'ring-blue-400'
  },
  [NodeType.CONDITION]: {
    icon: GitBranch,
    label: '条件',
    gradient: 'from-amber-400 to-orange-500',
    bgColor: 'bg-amber-50',
    borderColor: 'border-amber-200',
    textColor: 'text-amber-700',
    ringColor: 'ring-amber-400'
  },
  [NodeType.TOOL]: {
    icon: Wrench,
    label: '工具',
    gradient: 'from-cyan-400 to-teal-500',
    bgColor: 'bg-cyan-50',
    borderColor: 'border-cyan-200',
    textColor: 'text-cyan-700',
    ringColor: 'ring-cyan-400'
  }
};

// 状态配置
const STATUS_CONFIG = {
  [NodeExecutionStatus.PENDING]: {
    icon: null,
    color: 'text-gray-400',
    bgColor: 'bg-gray-100',
    animate: false
  },
  [NodeExecutionStatus.RUNNING]: {
    icon: Loader2,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    animate: true
  },
  [NodeExecutionStatus.SUCCEEDED]: {
    icon: CheckCircle2,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    animate: false
  },
  [NodeExecutionStatus.FAILED]: {
    icon: XCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    animate: false
  },
  [NodeExecutionStatus.SKIPPED]: {
    icon: MinusCircle,
    color: 'text-gray-400',
    bgColor: 'bg-gray-100',
    animate: false
  }
};

/**
 * 工作流节点组件 - 现代化设计
 */
export const WorkflowNode = memo(({ data, selected }: NodeProps<WorkflowNodeType>) => {
  const { label, nodeType, config, status } = data as WorkflowNodeData;

  // 获取节点配置
  const nodeConfig = useMemo(() => {
    return NODE_CONFIG[nodeType] || NODE_CONFIG[NodeType.START];
  }, [nodeType]);

  // 获取状态配置
  const statusConfig = useMemo(() => {
    return status ? STATUS_CONFIG[status] : null;
  }, [status]);

  const Icon = nodeConfig.icon;
  const StatusIcon = statusConfig?.icon;

  // 是否显示输入 Handle
  const showInputHandle = nodeType !== NodeType.START;

  // 是否显示输出 Handle
  const showOutputHandle = nodeType !== NodeType.END;

  // 条件节点的分支数量
  const branchCount = nodeType === NodeType.CONDITION
    ? (config?.properties?.branches?.length || 2)
    : 1;

  // 获取配置预览文本
  const getConfigPreview = () => {
    if (!config?.properties) return null;

    switch (nodeType) {
      case NodeType.LLM:
        return config.properties.model || 'GPT-4o';
      case NodeType.HTTP:
        return config.properties.method || 'GET';
      case NodeType.CONDITION:
        return config.properties.mode === 'LLM' ? 'LLM 判断' : '表达式';
      case NodeType.TOOL:
        return config.properties.toolName || '未配置';
      default:
        return null;
    }
  };

  const configPreview = getConfigPreview();

  return (
    <div
      className={`
        relative min-w-[200px] max-w-[280px] rounded-xl
        bg-white border-2 shadow-lg
        ${selected ? `${nodeConfig.borderColor} ${nodeConfig.ringColor} ring-2 ring-offset-2` : 'border-gray-100'}
        ${status === NodeExecutionStatus.RUNNING ? 'animate-pulse-subtle' : ''}
        transition-all duration-200 hover:shadow-xl
      `}
    >
      {/* 输入 Handle */}
      {showInputHandle && (
        <Handle
          type="target"
          position={Position.Top}
          className={`
            !w-4 !h-4 !-top-2 !bg-gray-300 !border-2 !border-white
            hover:!bg-gray-500 hover:!scale-125 transition-all duration-200
          `}
        />
      )}

      {/* 节点头部 */}
      <div
        className={`
          flex items-center gap-2 px-3 py-2 rounded-t-[10px]
          bg-gradient-to-r ${nodeConfig.gradient}
        `}
      >
        {/* 节点图标 */}
        <div className="w-6 h-6 rounded-md bg-white/20 flex items-center justify-center">
          <Icon className="w-4 h-4 text-white" />
        </div>

        {/* 节点类型标签 */}
        <span className="text-xs font-medium text-white/90">{nodeConfig.label}</span>

        {/* 状态指示器 */}
        {statusConfig && StatusIcon && (
          <div className={`ml-auto ${statusConfig.color}`}>
            <StatusIcon
              className={`w-4 h-4 ${statusConfig.animate ? 'animate-spin' : ''}`}
            />
          </div>
        )}

        {/* 人工审核标记 */}
        {config?.humanReviewConfig?.enabled && (
          <div className="ml-auto">
            <AlertCircle className="w-4 h-4 text-white/80" />
          </div>
        )}
      </div>

      {/* 节点主体 */}
      <div className="px-3 py-3">
        {/* 节点名称 */}
        <div className="font-medium text-gray-800 text-sm truncate">{label}</div>

        {/* 配置预览 */}
        {configPreview && (
          <div className={`mt-1 text-xs ${nodeConfig.textColor} opacity-70`}>
            {configPreview}
          </div>
        )}

        {/* 节点描述 */}
        {config?.properties?.description && (
          <div className="mt-2 text-xs text-gray-400 line-clamp-2">
            {config.properties.description}
          </div>
        )}
      </div>

      {/* 执行状态条 */}
      {status && status !== NodeExecutionStatus.PENDING && (
        <div
          className={`
            h-1 rounded-b-xl transition-all duration-300
            ${status === NodeExecutionStatus.RUNNING ? 'bg-blue-400 animate-pulse' : ''}
            ${status === NodeExecutionStatus.SUCCEEDED ? 'bg-green-400' : ''}
            ${status === NodeExecutionStatus.FAILED ? 'bg-red-400' : ''}
            ${status === NodeExecutionStatus.SKIPPED ? 'bg-gray-300' : ''}
          `}
        />
      )}

      {/* 输出 Handle */}
      {showOutputHandle && (
        <>
          {nodeType === NodeType.CONDITION ? (
            // 条件节点 - 多个输出 Handle
            <>
              {Array.from({ length: branchCount }).map((_, index) => {
                const offset = (index - (branchCount - 1) / 2) * 40;
                return (
                  <Handle
                    key={`branch-${index}`}
                    type="source"
                    position={Position.Bottom}
                    id={`branch-${index}`}
                    className={`
                      !w-4 !h-4 !-bottom-2 !bg-amber-400 !border-2 !border-white
                      hover:!bg-amber-600 hover:!scale-125 transition-all duration-200
                    `}
                    style={{ left: `calc(50% + ${offset}px)` }}
                  />
                );
              })}
            </>
          ) : (
            // 普通节点 - 单个输出 Handle
            <Handle
              type="source"
              position={Position.Bottom}
              className={`
                !w-4 !h-4 !-bottom-2 !bg-gray-300 !border-2 !border-white
                hover:!bg-gray-500 hover:!scale-125 transition-all duration-200
              `}
            />
          )}
        </>
      )}
    </div>
  );
});

WorkflowNode.displayName = 'WorkflowNode';

// 导出节点类型映射（用于 ReactFlow 注册）
export const nodeTypes = {
  start: WorkflowNode,
  end: WorkflowNode,
  llm: WorkflowNode,
  http: WorkflowNode,
  condition: WorkflowNode,
  tool: WorkflowNode,
  default: WorkflowNode
};
