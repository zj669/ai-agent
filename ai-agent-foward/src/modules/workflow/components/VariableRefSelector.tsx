import { useState, useRef, useEffect } from 'react'
import { cn } from '../../../lib/utils'

export interface UpstreamVariable {
  nodeId: string
  nodeName: string
  nodeType: string
  fieldKey: string
  fieldLabel: string
  fieldType: string
  /** 完整引用路径，如 start.output.inputMessage */
  ref: string
}

interface VariableRefSelectorProps {
  /** 当前选中的引用路径 */
  value: string
  /** 可选的上游变量列表 */
  variables: UpstreamVariable[]
  /** 选中变量时的回调 */
  onSelect: (ref: string) => void
  /** 清除引用时的回调 */
  onClear: () => void
}

function VariableRefSelector({ value, variables, onSelect, onClear }: VariableRefSelectorProps) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // 点击外部关闭
  useEffect(() => {
    if (!open) return
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as HTMLElement)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  // 找到当前选中的变量信息
  const selected = variables.find((v) => v.ref === value)

  // 按节点分组
  const grouped = variables.reduce<Record<string, UpstreamVariable[]>>((acc, v) => {
    const key = v.nodeId
    if (!acc[key]) acc[key] = []
    acc[key].push(v)
    return acc
  }, {})

  return (
    <div ref={containerRef} className="relative">
      {value && selected ? (
        <div className="flex items-center gap-1 rounded border border-blue-200 bg-blue-50 px-2 py-1">
          <span className="text-[10px] text-blue-400">{selected.nodeName}</span>
          <span className="text-xs text-blue-600">{selected.fieldLabel || selected.fieldKey}</span>
          <button
            className="ml-auto text-blue-400 hover:text-red-500 text-xs"
            onClick={(e) => { e.stopPropagation(); onClear() }}
            title="清除引用"
          >×</button>
        </div>
      ) : value ? (
        <div className="flex items-center gap-1 rounded border border-amber-200 bg-amber-50 px-2 py-1">
          <span className="text-xs text-amber-600 truncate">{value}</span>
          <button
            className="ml-auto text-amber-400 hover:text-red-500 text-xs"
            onClick={(e) => { e.stopPropagation(); onClear() }}
            title="清除引用"
          >×</button>
        </div>
      ) : null}

      <button
        className={cn(
          'w-full rounded border border-dashed px-2 py-1 text-xs text-left transition',
          value
            ? 'border-slate-200 text-slate-400 hover:border-blue-300 hover:text-blue-500 mt-1'
            : 'border-slate-300 text-slate-400 hover:border-blue-400 hover:text-blue-500'
        )}
        onClick={(e) => { e.stopPropagation(); setOpen(!open) }}
      >
        {value ? '更换引用' : '🔗 引用变量'}
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-1 w-64 rounded-lg border border-slate-200 bg-white shadow-lg max-h-48 overflow-y-auto">
          {variables.length === 0 ? (
            <div className="px-3 py-2 text-xs text-slate-400">无可用的上游变量</div>
          ) : (
            Object.entries(grouped).map(([nodeId, vars]) => (
              <div key={nodeId}>
                <div className="sticky top-0 bg-slate-50 px-3 py-1 text-[10px] font-medium text-slate-500 border-b border-slate-100">
                  {vars[0].nodeName} ({vars[0].nodeType})
                </div>
                {vars.map((v) => (
                  <button
                    key={v.ref}
                    className={cn(
                      'w-full text-left px-3 py-1.5 text-xs hover:bg-blue-50 transition flex items-center gap-2',
                      v.ref === value && 'bg-blue-50 text-blue-600'
                    )}
                    onClick={(e) => {
                      e.stopPropagation()
                      onSelect(v.ref)
                      setOpen(false)
                    }}
                  >
                    <span className="text-slate-600">{v.fieldLabel || v.fieldKey}</span>
                    <span className="text-[10px] text-slate-400">{v.fieldType}</span>
                  </button>
                ))}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export default VariableRefSelector
