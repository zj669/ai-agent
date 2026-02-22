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
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4">
      <div className="flex items-center gap-3">
        <Link
          to="/agents"
          aria-label="返回"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 10H5M5 10l5-5M5 10l5 5" />
          </svg>
        </Link>
        <div className="h-5 w-px bg-slate-200" />
        <span className="text-sm font-semibold text-slate-800">{agentName}</span>
      </div>

      <div className="flex items-center gap-3">
        <span className={cn('text-xs', isDirty ? 'text-amber-500' : 'text-slate-400')}>
          {operationState === 'saving' ? '保存中...' : operationState === 'publishing' ? '发布中...' : isDirty ? '未保存' : '已保存'}
        </span>
        <button
          type="button"
          className="rounded-lg bg-slate-900 px-4 py-1.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          onClick={onSave}
          disabled={busy}
        >
          {operationState === 'saving' ? '保存中...' : '保存'}
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-300 px-4 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
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
