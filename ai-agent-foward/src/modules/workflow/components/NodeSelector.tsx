import { cn } from '../../../lib/utils'
import type { WorkflowNodeType } from './WorkflowNode'

interface NodeSelectorProps {
  onSelect: (nodeType: WorkflowNodeType) => void
  onClose: () => void
}

const SELECTABLE_NODES: { type: WorkflowNodeType; label: string; icon: string; iconBg: string }[] = [
  { type: 'LLM',       label: 'LLM',  icon: '🧠', iconBg: 'bg-indigo-500' },
  { type: 'CONDITION',  label: '条件', icon: '🔀', iconBg: 'bg-cyan-500' },
  { type: 'TOOL',       label: '工具', icon: '🔧', iconBg: 'bg-blue-500' },
  { type: 'HTTP',       label: 'HTTP', icon: '🌐', iconBg: 'bg-violet-500' },
  { type: 'KNOWLEDGE',  label: '知识库', icon: '📚', iconBg: 'bg-teal-500' },
]

function NodeSelector({ onSelect, onClose }: NodeSelectorProps) {
  return (
    <div
      role="menu"
      className="absolute left-full top-1/2 -translate-y-1/2 ml-2 z-50 flex flex-col gap-1 rounded-xl bg-white p-2 shadow-lg border border-slate-200"
      onClick={(e) => e.stopPropagation()}
    >
      {SELECTABLE_NODES.map((node) => (
        <button
          key={node.type}
          role="menuitem"
          type="button"
          className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-100"
          onClick={() => {
            onSelect(node.type)
            onClose()
          }}
        >
          <span className={cn('flex h-6 w-6 items-center justify-center rounded-lg text-sm text-white shadow-sm', node.iconBg)}>
            {node.icon}
          </span>
          <span>{node.label}</span>
        </button>
      ))}
    </div>
  )
}

export default NodeSelector
