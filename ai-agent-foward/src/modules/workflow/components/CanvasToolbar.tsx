import type { DragEvent } from 'react'
import type { WorkflowNodeType } from './WorkflowNode'
import { cn } from '../../../lib/utils'

const TOOLBAR_ITEMS: { type: WorkflowNodeType; label: string; icon: string; color: string }[] = [
  { type: 'LLM', label: 'LLM', icon: '🧠', color: 'bg-blue-50 border-blue-200 text-blue-700' },
  { type: 'CONDITION', label: '条件', icon: '🔀', color: 'bg-amber-50 border-amber-200 text-amber-700' },
  { type: 'TOOL', label: '工具', icon: '🔧', color: 'bg-emerald-50 border-emerald-200 text-emerald-700' },
  { type: 'HTTP', label: 'HTTP', icon: '🌐', color: 'bg-violet-50 border-violet-200 text-violet-700' },
  { type: 'KNOWLEDGE', label: '知识库', icon: '📚', color: 'bg-teal-50 border-teal-200 text-teal-700' },
]

function CanvasToolbar() {
  const onDragStart = (event: DragEvent, nodeType: WorkflowNodeType) => {
    event.dataTransfer.setData('application/workflow-node-type', nodeType)
    event.dataTransfer.effectAllowed = 'move'
  }

  return (
    <div className="absolute bottom-6 left-1/2 z-10 flex -translate-x-1/2 items-center gap-1.5 rounded-2xl border border-slate-200/60 bg-white/80 px-2 py-1.5 shadow-lg shadow-slate-200/50 backdrop-blur-md">
      {TOOLBAR_ITEMS.map((item) => (
        <div
          key={item.type}
          className={cn(
            'flex cursor-grab items-center gap-1.5 rounded-xl border border-transparent px-3 py-1.5 text-xs font-semibold transition-all hover:scale-105 active:scale-95 active:cursor-grabbing',
            item.color
          )}
          draggable
          onDragStart={(e) => onDragStart(e, item.type)}
        >
          <span>{item.icon}</span>
          <span>{item.label}</span>
        </div>
      ))}
    </div>
  )
}

export default CanvasToolbar
