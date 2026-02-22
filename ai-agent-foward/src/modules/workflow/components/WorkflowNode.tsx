import { cn } from '../../../lib/utils'
import { useEditorStore } from '../stores/useEditorStore'
import { NodeTargetHandle, NodeSourceHandle } from './NodeHandle'
import NodeConfigTabs from './NodeConfigTabs'

export type WorkflowNodeType = 'START' | 'END' | 'LLM' | 'CONDITION' | 'TOOL' | 'HTTP'

export type Branch = { id: string; name: string }

export type WorkflowNodeData = {
  label: string
  nodeType: WorkflowNodeType
  branches?: Branch[]
  inputSchema?: { key: string; label: string; type: string; sourceRef?: string }[]
  outputSchema?: { key: string; label: string; type: string }[]
  userConfig?: Record<string, unknown>
}

const NODE_STYLES: Record<WorkflowNodeType, { bg: string; border: string; icon: string; accent: string }> = {
  START: { bg: 'bg-green-50', border: 'border-green-300', icon: '▶', accent: 'text-green-600' },
  END: { bg: 'bg-red-50', border: 'border-red-300', icon: '■', accent: 'text-red-600' },
  LLM: { bg: 'bg-blue-50', border: 'border-blue-300', icon: '🧠', accent: 'text-blue-600' },
  CONDITION: { bg: 'bg-amber-50', border: 'border-amber-300', icon: '🔀', accent: 'text-amber-600' },
  TOOL: { bg: 'bg-emerald-50', border: 'border-emerald-300', icon: '🔧', accent: 'text-emerald-600' },
  HTTP: { bg: 'bg-violet-50', border: 'border-violet-300', icon: '🌐', accent: 'text-violet-600' },
}

const NODE_TYPE_LABELS: Record<WorkflowNodeType, string> = {
  START: '开始', END: '结束', LLM: 'LLM', CONDITION: '条件', TOOL: '工具', HTTP: 'HTTP',
}

interface WorkflowNodeProps {
  id: string
  data: unknown
  selected: boolean
}

function WorkflowNode({ id, data, selected }: WorkflowNodeProps) {
  const nodeData = data as WorkflowNodeData
  const style = NODE_STYLES[nodeData.nodeType] ?? NODE_STYLES.TOOL
  const isStart = nodeData.nodeType === 'START'
  const isEnd = nodeData.nodeType === 'END'
  const isCondition = nodeData.nodeType === 'CONDITION'
  const canExpand = !isStart && !isEnd

  const expandedNodeId = useEditorStore((s) => s.expandedNodeId)
  const toggleNodeExpand = useEditorStore((s) => s.toggleNodeExpand)
  const nodeTemplates = useEditorStore((s) => s.nodeTemplates)

  const isExpanded = expandedNodeId === id
  const template = nodeTemplates.find((t) => t.typeCode === nodeData.nodeType)

  return (
    <div
      className={cn(
        'rounded-xl border-2 shadow-sm transition-all',
        style.bg, style.border,
        selected && 'ring-2 ring-blue-500 ring-offset-1',
        isExpanded ? 'min-w-[320px]' : 'min-w-[160px]'
      )}
    >
      {!isStart && <NodeTargetHandle handleId="target" />}

      <div className="flex items-center gap-2 px-3 py-2.5">
        <span className="text-base">{style.icon}</span>
        <div className="flex-1 min-w-0">
          <div className={cn('text-[10px] font-medium', style.accent)}>{NODE_TYPE_LABELS[nodeData.nodeType]}</div>
          <div className="truncate text-sm font-medium text-slate-800">{nodeData.label}</div>
        </div>
        {canExpand && (
          <button
            type="button"
            aria-label="展开配置"
            className="flex h-5 w-5 items-center justify-center rounded text-slate-400 transition hover:bg-slate-200 hover:text-slate-600"
            onClick={(e) => { e.stopPropagation(); toggleNodeExpand(id) }}
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              {isExpanded ? <path d="M3 7.5L6 4.5L9 7.5" /> : <path d="M3 4.5L6 7.5L9 4.5" />}
            </svg>
          </button>
        )}
      </div>

      {isExpanded && canExpand && template && (
        <div className="border-t border-slate-200">
          <NodeConfigTabs
            template={template}
            inputSchema={nodeData.inputSchema ?? []}
            outputSchema={nodeData.outputSchema ?? []}
            userConfig={nodeData.userConfig ?? {}}
            onConfigChange={() => {}}
          />
        </div>
      )}

      {isCondition && (
        <div className="flex flex-col gap-1 border-t border-slate-200 py-2 px-3">
          {(nodeData.branches ?? []).map((branch) => (
            <div key={branch.id} className="relative flex items-center justify-between py-1">
              <span className="text-xs text-slate-600">{branch.name}</span>
              <NodeSourceHandle handleId={branch.id} className="!relative !top-0 !right-0 !translate-x-0 !translate-y-0" />
            </div>
          ))}
        </div>
      )}

      {!isEnd && !isCondition && <NodeSourceHandle handleId="source" />}
    </div>
  )
}

export default WorkflowNode
