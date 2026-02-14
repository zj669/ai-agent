import { useState } from 'react';
import { Input, Collapse, Tooltip } from 'antd';
import {
  Search,
  PlayCircle,
  StopCircle,
  MessageSquare,
  Globe,
  GitBranch,
  Wrench,
  Sparkles,
  Workflow,
  Zap,
  ChevronLeft,
  ChevronRight,
  GripVertical
} from 'lucide-react';
import { NodeType } from '../types/workflow';

interface NodePanelProps {
  onAddNode?: (type: NodeType) => void;
  collapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
}

// 节点分组配置
const NODE_GROUPS = [
  {
    key: 'basic',
    label: '基础节点',
    icon: Workflow,
    nodes: [
      {
        type: NodeType.START,
        label: '开始',
        description: '工作流的起始点',
        icon: PlayCircle,
        gradient: 'from-emerald-400 to-green-500',
        bgHover: 'hover:bg-emerald-50'
      },
      {
        type: NodeType.END,
        label: '结束',
        description: '工作流的终止点',
        icon: StopCircle,
        gradient: 'from-rose-400 to-red-500',
        bgHover: 'hover:bg-rose-50'
      }
    ]
  },
  {
    key: 'ai',
    label: 'AI 节点',
    icon: Sparkles,
    nodes: [
      {
        type: NodeType.LLM,
        label: 'LLM',
        description: '调用大语言模型',
        icon: MessageSquare,
        gradient: 'from-violet-400 to-purple-500',
        bgHover: 'hover:bg-violet-50'
      }
    ]
  },
  {
    key: 'logic',
    label: '逻辑节点',
    icon: GitBranch,
    nodes: [
      {
        type: NodeType.CONDITION,
        label: '条件分支',
        description: '根据条件选择执行路径',
        icon: GitBranch,
        gradient: 'from-amber-400 to-orange-500',
        bgHover: 'hover:bg-amber-50'
      }
    ]
  },
  {
    key: 'integration',
    label: '集成节点',
    icon: Zap,
    nodes: [
      {
        type: NodeType.HTTP,
        label: 'HTTP 请求',
        description: '发送 HTTP API 请求',
        icon: Globe,
        gradient: 'from-blue-400 to-indigo-500',
        bgHover: 'hover:bg-blue-50'
      },
      {
        type: NodeType.TOOL,
        label: '工具调用',
        description: '调用外部工具或函数',
        icon: Wrench,
        gradient: 'from-cyan-400 to-teal-500',
        bgHover: 'hover:bg-cyan-50'
      }
    ]
  }
];

// 所有节点的扁平列表（用于搜索）
const ALL_NODES = NODE_GROUPS.flatMap(group => group.nodes);

/**
 * 节点面板组件 - 支持点击添加节点
 */
export function NodePanel({ onAddNode, collapsed = false, onCollapsedChange }: NodePanelProps) {
  const [searchText, setSearchText] = useState('');
  const [activeKeys, setActiveKeys] = useState<string[]>(['basic', 'ai', 'logic', 'integration']);

  // 过滤节点
  const filteredGroups = searchText
    ? NODE_GROUPS.map(group => ({
        ...group,
        nodes: group.nodes.filter(
          node =>
            node.label.toLowerCase().includes(searchText.toLowerCase()) ||
            node.description.toLowerCase().includes(searchText.toLowerCase())
        )
      })).filter(group => group.nodes.length > 0)
    : NODE_GROUPS;

  // 渲染节点卡片
  const renderNodeCard = (node: (typeof ALL_NODES)[0]) => {
    const Icon = node.icon;

    return (
      <Tooltip
        key={node.type}
        title={node.description}
        placement="right"
        mouseEnterDelay={0.5}
      >
        <div
          onClick={() => onAddNode?.(node.type)}
          className={`
            node-card-enhanced group flex items-center gap-3 px-3 py-2.5 rounded-xl cursor-pointer
            transition-all duration-300
          `}
        >
          {/* 添加手柄 */}
          <div className="opacity-0 group-hover:opacity-50 transition-opacity duration-300">
            <GripVertical className="w-3 h-3 text-gray-500" />
          </div>

          {/* 节点图标 */}
          <div
            className={`
              node-icon-enhanced flex-shrink-0 w-9 h-9 rounded-xl flex items-center justify-center
              bg-gradient-to-br ${node.gradient}
            `}
          >
            <Icon className="w-[18px] h-[18px] text-white" />
          </div>

          {/* 节点信息 */}
          <div className="flex-1 min-w-0 relative z-10">
            <div className="font-semibold text-gray-800 text-sm mb-0.5">{node.label}</div>
            <div className="text-xs text-gray-500 truncate leading-tight">{node.description}</div>
          </div>

          {/* 添加按钮提示 */}
          <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-300">
            <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center">
              <span className="text-white text-xs font-bold">+</span>
            </div>
          </div>
        </div>
      </Tooltip>
    );
  };

  // 折叠状态下的渲染
  if (collapsed) {
    return (
      <div className="workflow-nodepanel-collapsed h-full border-r flex flex-col items-center py-4 w-14">
        {/* 展开按钮 */}
        <button
          onClick={() => onCollapsedChange?.(false)}
          className="workflow-toolbar-btn p-2 rounded-lg transition-colors mb-4"
        >
          <ChevronRight className="w-5 h-5 text-slate-300" />
        </button>

        {/* 节点图标列表 */}
        <div className="flex flex-col gap-2">
          {ALL_NODES.map(node => {
            const Icon = node.icon;
            return (
              <Tooltip key={node.type} title={node.label} placement="right">
                <div
                  onClick={() => onAddNode?.(node.type)}
                  className={`
                    w-10 h-10 rounded-lg flex items-center justify-center cursor-pointer
                    bg-gradient-to-br ${node.gradient} shadow-sm
                    hover:shadow-md hover:scale-105 transition-all duration-200 active:scale-95
                  `}
                >
                  <Icon className="w-5 h-5 text-white" />
                </div>
              </Tooltip>
            );
          })}
        </div>
      </div>
    );
  }

  // 构建 Collapse items
  const collapseItems = filteredGroups.map(group => {
    const GroupIcon = group.icon;
    return {
      key: group.key,
      label: (
        <div className="flex items-center gap-2">
          <GroupIcon className="w-4 h-4 text-gray-500" />
          <span className="font-medium text-gray-700">{group.label}</span>
          <span className="text-xs text-gray-400 ml-auto">{group.nodes.length}</span>
        </div>
      ),
      children: (
        <div className="space-y-2 pb-2">
          {group.nodes.map(renderNodeCard)}
        </div>
      )
    };
  });

  return (
    <div className="workflow-nodepanel h-full border-r flex flex-col w-72">
      {/* 头部 */}
      <div className="flex-shrink-0 px-4 py-3 border-b border-slate-200 bg-white">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-slate-800">节点库</h3>
          <button
            onClick={() => onCollapsedChange?.(true)}
            className="workflow-toolbar-btn p-1.5 rounded-lg transition-colors"
          >
            <ChevronLeft className="w-4 h-4 text-slate-500" />
          </button>
        </div>

        {/* 搜索框 */}
        <Input
          placeholder="搜索节点..."
          prefix={<Search className="w-4 h-4 text-gray-400" />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          allowClear
          className="rounded-lg"
        />
      </div>

      {/* 节点列表 */}
      <div className="flex-1 overflow-y-auto px-3 py-3">
        {searchText ? (
          // 搜索结果 - 扁平显示
          <div className="space-y-2">
            {filteredGroups.flatMap(group => group.nodes).map(renderNodeCard)}
            {filteredGroups.length === 0 && (
              <div className="text-center text-gray-400 py-8">
                <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">未找到匹配的节点</p>
              </div>
            )}
          </div>
        ) : (
          // 分组显示
          <Collapse
            activeKey={activeKeys}
            onChange={(keys) => setActiveKeys(keys as string[])}
            items={collapseItems}
            bordered={false}
            expandIconPosition="end"
            className="bg-transparent node-panel-collapse"
            ghost
          />
        )}
      </div>

      {/* 底部提示 */}
      <div className="flex-shrink-0 px-4 py-3 border-t border-slate-200 bg-white">
        <p className="text-xs text-slate-500 text-center">
          点击节点添加到画布
        </p>
      </div>
    </div>
  );
}
