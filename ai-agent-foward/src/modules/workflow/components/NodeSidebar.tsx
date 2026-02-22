import type { DragEvent } from 'react'
import type { WorkflowNodeType } from './WorkflowNode'

const DRAGGABLE_NODES: { type: WorkflowNodeType; label: string; icon: string }[] = [
  { type: 'LLM', label: 'LLM 节点', icon: '🤖' },
  { type: 'CONDITION', label: '条件节点', icon: '◇' },
  { type: 'TOOL', label: '工具节点', icon: '⚙' },
  { type: 'HTTP', label: 'HTTP 节点', icon: '↗' },
]

function NodeSidebar() {
  const onDragStart = (event: DragEvent, nodeType: WorkflowNodeType) => {
    event.dataTransfer.setData('application/workflow-node-type', nodeType)
    event.dataTransfer.effectAllowed = 'move'
  }

  return (
    <div className="w-48 shrink-0 border-r border-slate-200 bg-slate-50 p-3">
      <h3 className="mb-3 text-sm font-medium text-slate-700">节点面板</h3>
      <div className="space-y-2">
        {DRAGGABLE_NODES.map((item) => (
          <div
            key={item.type}
            className="cursor-grab rounded border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm hover:border-blue-300 hover:shadow"
            draggable
            onDragStart={(e) => onDragStart(e, item.type)}
          >
            <span className="mr-2">{item.icon}</span>
            {item.label}
          </div>
        ))}
      </div>
    </div>
  )
}

export default NodeSidebar
