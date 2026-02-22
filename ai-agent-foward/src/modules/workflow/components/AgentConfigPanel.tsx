import { cn } from '../../../lib/utils'

interface AgentConfigPanelProps {
  agentName: string
  agentDescription: string
  agentIcon: string
  collapsed: boolean
  onToggle: () => void
  onChange: (field: string, value: string) => void
}

const inputClass =
  'w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 transition focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none'

function AgentConfigPanel({
  agentName,
  agentDescription,
  agentIcon,
  collapsed,
  onToggle,
  onChange,
}: AgentConfigPanelProps) {
  if (collapsed) {
    return (
      <div className="flex w-12 shrink-0 flex-col items-center border-r border-slate-200 bg-slate-50/80 pt-3">
        <button
          type="button"
          aria-label="展开面板"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-200 hover:text-slate-600"
          onClick={onToggle}
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <path d="M6 3l5 5-5 5" />
          </svg>
        </button>
      </div>
    )
  }

  return (
    <aside className="flex w-80 shrink-0 flex-col border-r border-slate-200 bg-slate-50/80">
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h3 className="text-sm font-semibold text-slate-700">Agent 配置</h3>
        <button
          type="button"
          aria-label="收起面板"
          className="flex h-6 w-6 items-center justify-center rounded text-slate-400 transition hover:bg-slate-200 hover:text-slate-600"
          onClick={onToggle}
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 14 14"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <path d="M8 2L3 7l5 5" />
          </svg>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-5">
        <div className="flex flex-col items-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-2xl text-white shadow-md">
            {agentIcon || agentName.charAt(0) || '🤖'}
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-slate-600">名称</label>
          <input
            className={inputClass}
            value={agentName}
            onChange={(e) => onChange('agentName', e.target.value)}
            placeholder="Agent 名称"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-slate-600">描述</label>
          <textarea
            className={cn(inputClass, 'min-h-[80px] resize-y')}
            value={agentDescription}
            onChange={(e) => onChange('agentDescription', e.target.value)}
            placeholder="Agent 描述"
          />
        </div>
      </div>
    </aside>
  )
}

export default AgentConfigPanel