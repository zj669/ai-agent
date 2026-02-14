import { memo, useMemo } from 'react';
import { Handle, Position, NodeProps, Node } from '@xyflow/react';
import { Button, Tag, Tooltip } from 'antd';
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
  Settings,
  Play,
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
    color: '#10b981',
    bgColor: '#ecfdf5',
    borderColor: '#a7f3d0'
  },
  [NodeType.END]: {
    icon: StopCircle,
    label: '结束',
    color: '#ef4444',
    bgColor: '#fef2f2',
    borderColor: '#fecaca'
  },
  [NodeType.LLM]: {
    icon: MessageSquare,
    label: 'LLM',
    color: '#8b5cf6',
    bgColor: '#faf5ff',
    borderColor: '#e9d5ff'
  },
  [NodeType.HTTP]: {
    icon: Globe,
    label: 'HTTP',
    color: '#3b82f6',
    bgColor: '#eff6ff',
    borderColor: '#bfdbfe'
  },
  [NodeType.CONDITION]: {
    icon: GitBranch,
    label: '条件',
    color: '#f59e0b',
    bgColor: '#fffbeb',
    borderColor: '#fde68a'
  },
  [NodeType.TOOL]: {
    icon: Wrench,
    label: '工具',
    color: '#06b6d4',
    bgColor: '#ecfeff',
    borderColor: '#a5f3fc'
  }
};

// 状态配置
const STATUS_CONFIG = {
  [NodeExecutionStatus.RUNNING]: {
    icon: Loader2,
    color: '#3b82f6',
    label: '运行中',
    animate: true
  },
  [NodeExecutionStatus.SUCCEEDED]: {
    icon: CheckCircle2,
    color: '#10b981',
    label: '成功',
    animate: false
  },
  [NodeExecutionStatus.FAILED]: {
    icon: XCircle,
    color: '#ef4444',
    label: '失败',
    animate: false
  }
};

/**
 * 配置预览组件
 */
function ConfigPreview({ nodeType, config }: { nodeType: NodeType; config?: NodeConfig }) {
  if (!config?.properties) {
    return (
      <div className="text-xs text-gray-400 italic py-2">
        未配置
      </div>
    );
  }

  const props = config.properties;

  switch (nodeType) {
    case NodeType.LLM:
      return (
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">模型</span>
            <span className="text-gray-800 font-medium">{props.model || 'GPT-4o'}</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">温度</span>
            <span className="text-gray-800 font-medium">{props.temperature ?? 0.7}</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">最大令牌</span>
            <span className="text-gray-800 font-medium">{props.maxTokens || 2000}</span>
          </div>
          {props.systemPrompt && (
            <div className="text-xs mt-2 pt-2 border-t border-gray-100">
              <span className="text-gray-500">系统提示</span>
              <p className="text-gray-700 mt-1 line-clamp-2 leading-relaxed">
                {props.systemPrompt}
              </p>
            </div>
          )}
        </div>
      );

    case NodeType.HTTP:
      return (
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">方法</span>
            <Tag color={props.method === 'GET' ? 'blue' : 'green'} className="!text-xs !py-0">
              {props.method || 'GET'}
            </Tag>
          </div>
          <div className="text-xs">
            <span className="text-gray-500">URL</span>
            <p className="text-gray-800 font-mono text-[11px] mt-1 truncate">
              {props.url || '未设置'}
            </p>
          </div>
          {props.headers && Object.keys(props.headers).length > 0 && (
            <div className="flex justify-between text-xs">
              <span className="text-gray-500">请求头</span>
              <span className="text-gray-800">{Object.keys(props.headers).length} 项</span>
            </div>
          )}
        </div>
      );

    case NodeType.CONDITION:
      const branches = props.branches || [];
      return (
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">模式</span>
            <span className="text-gray-800 font-medium">
              {props.mode === 'LLM' ? 'LLM 判断' : '表达式'}
            </span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">分支数</span>
            <span className="text-gray-800 font-medium">{branches.length}</span>
          </div>
          {branches.length > 0 && (
            <div className="text-xs mt-2 pt-2 border-t border-gray-100 space-y-1">
              {branches.slice(0, 3).map((branch: any, idx: number) => (
                <div key={idx} className="flex items-start gap-1.5">
                  <span className="text-gray-400 mt-0.5">├─</span>
                  <span className="text-gray-700 flex-1 truncate">
                    {branch.condition || `分支 ${idx + 1}`}
                  </span>
                </div>
              ))}
              {branches.length > 3 && (
                <div className="text-gray-400 text-[11px]">
                  还有 {branches.length - 3} 个分支...
                </div>
              )}
            </div>
          )}
        </div>
      );

    case NodeType.TOOL:
      return (
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">工具名称</span>
            <span className="text-gray-800 font-medium">{props.toolName || '未配置'}</span>
          </div>
          {props.description && (
            <div className="text-xs text-gray-600 mt-2 pt-2 border-t border-gray-100">
              {props.description}
            </div>
          )}
        </div>
      );

    default:
      return null;
  }
}

/**
 * 大尺寸工作流节点组件
 */
export const WorkflowNodeLarge = memo(({ data, selected }: NodeProps<WorkflowNodeType>) => {
  const { label, nodeType, config, status } = data as WorkflowNodeData;

  const nodeConfig = useMemo(() => {
    return NODE_CONFIG[nodeType] || NODE_CONFIG[NodeType.START];
  }, [nodeType]);

  const statusConfig = useMemo(() => {
    return status ? STATUS_CONFIG[status] : null;
  }, [status]);

  const Icon = nodeConfig.icon;
  const StatusIcon = statusConfig?.icon;

  const showInputHandle = nodeType !== NodeType.START;
  const showOutputHandle = nodeType !== NodeType.END;

  // 条件节点的分支数量
  const branchCount = nodeType === NodeType.CONDITION
    ? (config?.properties?.branches?.length || 2)
    : 1;

  return (
    <div
      className={`
        workflow-node-large relative bg-white rounded-xl border
        transition-all duration-200
        ${selected 
          ? 'border-blue-400 shadow-lg ring-2 ring-blue-200' 
          : 'border-gray-200 shadow-sm hover:shadow-md'
        }
        ${status === NodeExecutionStatus.RUNNING ? 'animate-pulse-subtle' : ''}
      `}
      style={{ minWidth: '320px', maxWidth: '400px' }}
    >
      {/* 输入 Handle */}
      {showInputHandle && (
        <Handle
          type="target"
          position={Position.Left}
          className="!w-3 !h-3 !-left-[6px] !bg-blue-400 !border-2 !border-white hover:!scale-125 transition-transform"
        />
      )}

      {/* 节点头部 */}
      <div
        className="flex items-center justify-between px-4 py-3 rounded-t-xl border-b"
        style={{
          backgroundColor: nodeConfig.bgColor,
          borderColor: nodeConfig.borderColor
        }}
      >
        <div className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-lg flex items-center justify-center shadow-sm"
            style={{ backgroundColor: nodeConfig.color }}
          >
            <Icon className="w-[18px] h-[18px] text-white" />
          </div>
          <div>
            <div className="text-xs font-medium" style={{ color: nodeConfig.color }}>
              {nodeConfig.label}
            </div>
            <div className="text-sm font-semibold text-gray-800 mt-0.5">
              {label}
            </div>
          </div>
        </div>

        {/* 状态指示器 */}
        {statusConfig && StatusIcon && (
          <Tooltip title={statusConfig.label}>
            <div style={{ color: statusConfig.color }}>
              <StatusIcon
                className={`w-5 h-5 ${statusConfig.animate ? 'animate-spin' : ''}`}
              />
            </div>
          </Tooltip>
        )}

        {/* 人工审核标记 */}
        {config?.humanReviewConfig?.enabled && !statusConfig && (
          <Tooltip title="需要人工审核">
            <AlertCircle className="w-5 h-5 text-amber-500" />
          </Tooltip>
        )}
      </div>

      {/* 配置预览区 */}
      <div className="config-preview px-4 py-3 min-h-[100px]">
        <ConfigPreview nodeType={nodeType} config={config} />
      </div>

      {/* 操作按钮区 */}
      <div className="px-4 py-2.5 border-t border-gray-100 flex items-center gap-2">
        <Button
          size="small"
          icon={<Play className="w-3.5 h-3.5" />}
          className="!text-xs"
        >
          测试
        </Button>
        <Button
          size="small"
          icon={<Settings className="w-3.5 h-3.5" />}
          className="!text-xs"
        >
          配置
        </Button>
        {nodeType !== NodeType.START && nodeType !== NodeType.END && (
          <div className="ml-auto text-xs text-gray-400">
            拖动连接
          </div>
        )}
      </div>

      {/* 执行状态条 */}
      {status && status !== NodeExecutionStatus.PENDING && (
        <div
          className="h-1 rounded-b-xl transition-all duration-300"
          style={{
            backgroundColor: statusConfig?.color || '#e5e7eb'
          }}
        />
      )}

      {/* 输出 Handle */}
      {showOutputHandle && (
        <>
          {nodeType === NodeType.CONDITION ? (
            // 条件节点 - 多个输出
            <>
              {Array.from({ length: branchCount }).map((_, index) => {
                const totalWidth = 320; // 节点最小宽度
                const spacing = totalWidth / (branchCount + 1);
                const leftPosition = spacing * (index + 1);
                
                return (
                  <Handle
                    key={`branch-${index}`}
                    type="source"
                    position={Position.Right}
                    id={`branch-${index}`}
                    className="!w-3 !h-3 !-right-[6px] !bg-blue-400 !border-2 !border-white hover:!scale-125 transition-transform"
                    style={{ 
                      top: `${((index + 1) / (branchCount + 1)) * 100}%`,
                      transform: 'translateY(-50%)'
                    }}
                  />
                );
              })}
            </>
          ) : (
            // 普通节点 - 单个输出
            <Handle
              type="source"
              position={Position.Right}
              className="!w-3 !h-3 !-right-[6px] !bg-blue-400 !border-2 !border-white hover:!scale-125 transition-transform"
            />
          )}
        </>
      )}
    </div>
  );
});

WorkflowNodeLarge.displayName = 'WorkflowNodeLarge';

// 导出节点类型映射
export const largeNodeTypes = {
  start: WorkflowNodeLarge,
  end: WorkflowNodeLarge,
  llm: WorkflowNodeLarge,
  http: WorkflowNodeLarge,
  condition: WorkflowNodeLarge,
  tool: WorkflowNodeLarge,
  default: WorkflowNodeLarge
};
