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
    <div className="absolute bottom-4 left-1/2 z-10 flex -translate-x-1/2 items-center gap-2 rounded-xl border border-slate-200 bg-white/90 px-3 py-2 shadow-lg backdrop-blur">
      {TOOLBAR_ITEMS.map((item) => (
        <div
          key={item.type}
          className={cn(
            'flex cursor-grab items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-medium transition hover:shadow-md active:cursor-grabbing',
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
