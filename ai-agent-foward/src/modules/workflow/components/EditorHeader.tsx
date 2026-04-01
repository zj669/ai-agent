import { Link } from 'react-router-dom'
import { cn } from '../../../lib/utils'

interface EditorHeaderProps {
  agentName: string
  isDirty: boolean
  operationState: 'idle' | 'saving' | 'publishing'
  onSave: () => void
  onPublish: () => void
}

function EditorHeader({ agentName, isDirty, operationState, onSave, onPublish }: EditorHeaderProps) {
  const busy = operationState !== 'idle'

  return (
    <header className="flex h-12 shrink-0 items-center justify-between border-b border-slate-200/80 bg-white/80 backdrop-blur-md px-4 absolute top-0 left-0 right-0 z-10">
      <div className="flex items-center gap-3">
        <Link
          to="/agents"
          aria-label="返回"
          className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
        >
          <svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 10H5M5 10l5-5M5 10l5 5" />
          </svg>
        </Link>
        <div className="h-4 w-px bg-slate-200" />
        <span className="text-xs font-semibold text-slate-900">{agentName}</span>
      </div>

      <div className="flex items-center gap-3">
        <span className={cn('text-[11px] font-medium mr-1', isDirty ? 'text-amber-500' : 'text-slate-400')}>
          {operationState === 'saving' ? '保存中...' : operationState === 'publishing' ? '发布中...' : isDirty ? '未保存' : '已保存'}
        </span>
        <button
          type="button"
          className="rounded-lg bg-blue-600 px-3 py-1.5 text-[11px] font-semibold text-white transition hover:bg-blue-700 shadow-sm disabled:cursor-not-allowed disabled:bg-blue-300"
          onClick={onSave}
          disabled={busy}
        >
          {operationState === 'saving' ? '保存中...' : '保存'}
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-[11px] font-semibold text-slate-700 transition hover:bg-slate-50 shadow-sm disabled:cursor-not-allowed disabled:text-slate-300"
          onClick={onPublish}
          disabled={busy}
        >
          {operationState === 'publishing' ? '发布中...' : '发布'}
        </button>
      </div>
    </header>
  )
}

export default EditorHeader
