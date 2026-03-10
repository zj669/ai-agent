import { useCallback, useEffect, useState } from 'react'
import { getLlmConfigs, type LlmConfig } from '../../llm-config/api/llmConfigService'

/* ========== Types ========== */

export interface Condition {
  sourceRef: string
  operator: string
  value: string
  valueType: 'literal' | 'ref'
}

export interface Branch {
  id: string
  name: string
  type: 'if' | 'elseif' | 'else'
  priority: number
  logic: 'AND' | 'OR'
  conditions: Condition[]
  description?: string
}

export interface ConditionConfig {
  routingStrategy: 'EXPRESSION' | 'LLM'
  routingPrompt?: string
  /** 关联的 LLM 配置 ID（用于 LLM 语义路由） */
  llmConfigId?: number
  branches: Branch[]
}

interface ConditionBranchEditorProps {
  config: ConditionConfig
  onChange: (config: ConditionConfig) => void
  /** 上游节点的输出变量，用于下拉选择 */
  availableVars?: { label: string; value: string }[]
}

/* ========== Constants ========== */

const OPERATORS: { value: string; label: string }[] = [
  { value: 'EQUALS', label: '等于' },
  { value: 'NOT_EQUALS', label: '不等于' },
  { value: 'CONTAINS', label: '包含' },
  { value: 'NOT_CONTAINS', label: '不包含' },
  { value: 'GT', label: '大于' },
  { value: 'LT', label: '小于' },
  { value: 'GTE', label: '大于等于' },
  { value: 'LTE', label: '小于等于' },
  { value: 'IS_EMPTY', label: '为空' },
  { value: 'IS_NOT_EMPTY', label: '不为空' },
  { value: 'STARTS_WITH', label: '开头是' },
  { value: 'ENDS_WITH', label: '结尾是' },
]

const BRANCH_LABELS: Record<string, string> = { if: '如果', elseif: '否则如果', else: '否则' }

const sel = 'h-7 rounded border border-slate-200 bg-white px-2 text-xs text-slate-700 focus:border-blue-400 focus:outline-none'
const inp = 'h-7 rounded border border-slate-200 bg-white px-2 text-xs text-slate-700 focus:border-blue-400 focus:outline-none'

/* ========== Helpers ========== */

function emptyCondition(): Condition {
  return { sourceRef: '', operator: 'EQUALS', value: '', valueType: 'literal' }
}

function newBranch(index: number, type: 'if' | 'elseif'): Branch {
  return {
    id: `branch-${Date.now()}-${index}`,
    name: type === 'if' ? '如果' : '否则如果',
    type,
    priority: index + 1,
    logic: 'AND',
    conditions: [emptyCondition()],
  }
}

/* ========== Sub-components ========== */

interface ConditionRowProps {
  cond: Condition
  index: number
  total: number
  logic: 'AND' | 'OR'
  availableVars: { label: string; value: string }[]
  onUpdate: (index: number, field: keyof Condition, value: string) => void
  onRemove: (index: number) => void
}

function ConditionRow({ cond, index, total, logic, availableVars, onUpdate, onRemove }: ConditionRowProps) {
  const noValue = cond.operator === 'IS_EMPTY' || cond.operator === 'IS_NOT_EMPTY'

  return (
    <div className="flex items-center gap-1.5">
      {/* Logic label (且/或) or empty space for first row */}
      <div className="w-6 shrink-0 text-center">
        {index > 0 && (
          <span className="text-[10px] font-medium text-slate-400">{logic === 'AND' ? '且' : '或'}</span>
        )}
      </div>

      {/* Variable ref */}
      {availableVars.length > 0 ? (
        <select className={`flex-1 min-w-0 ${sel}`} value={cond.sourceRef} onChange={(e) => onUpdate(index, 'sourceRef', e.target.value)}>
          <option value="">请选择</option>
          {availableVars.map((v) => <option key={v.value} value={v.value}>{v.label}</option>)}
        </select>
      ) : (
        <input className={`flex-1 min-w-0 ${inp}`} value={cond.sourceRef} placeholder="引用变量" onChange={(e) => onUpdate(index, 'sourceRef', e.target.value)} />
      )}

      {/* Operator */}
      <select className={`w-[90px] shrink-0 ${sel}`} value={cond.operator} onChange={(e) => onUpdate(index, 'operator', e.target.value)}>
        {OPERATORS.map((op) => <option key={op.value} value={op.value}>{op.label}</option>)}
      </select>

      {/* Value type toggle */}
      {!noValue && (
        <button
          type="button"
          className="shrink-0 rounded border border-slate-200 bg-slate-50 px-1.5 h-7 text-[10px] text-slate-500 hover:bg-slate-100"
          onClick={() => onUpdate(index, 'valueType', cond.valueType === 'literal' ? 'ref' : 'literal')}
        >
          {cond.valueType === 'ref' ? '引用' : '值'}
        </button>
      )}

      {/* Compare value */}
      {!noValue && (
        cond.valueType === 'ref' && availableVars.length > 0 ? (
          <select className={`flex-1 min-w-0 ${sel}`} value={cond.value} onChange={(e) => onUpdate(index, 'value', e.target.value)}>
            <option value="">请选择</option>
            {availableVars.map((v) => <option key={v.value} value={v.value}>{v.label}</option>)}
          </select>
        ) : (
          <input className={`flex-1 min-w-0 ${inp}`} value={cond.value} placeholder="请选择" onChange={(e) => onUpdate(index, 'value', e.target.value)} />
        )
      )}

      {/* Delete button */}
      {total > 1 && (
        <button type="button" className="shrink-0 h-7 w-7 flex items-center justify-center rounded text-slate-300 hover:text-red-500 hover:bg-red-50" onClick={() => onRemove(index)}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12" /></svg>
        </button>
      )}
    </div>
  )
}

/* ========== Branch Card ========== */

interface BranchCardProps {
  branch: Branch
  branchIndex: number
  totalBranches: number
  availableVars: { label: string; value: string }[]
  onUpdate: (index: number, branch: Branch) => void
  onRemove: (index: number) => void
}

function BranchCard({ branch, branchIndex, totalBranches, availableVars, onUpdate, onRemove }: BranchCardProps) {
  const isElse = branch.type === 'else'
  const label = BRANCH_LABELS[branch.type] ?? branch.name

  const updateCondition = useCallback((condIdx: number, field: keyof Condition, value: string) => {
    const conditions = branch.conditions.map((c, i) => i === condIdx ? { ...c, [field]: value } : c)
    onUpdate(branchIndex, { ...branch, conditions })
  }, [branch, branchIndex, onUpdate])

  const removeCondition = useCallback((condIdx: number) => {
    const conditions = branch.conditions.filter((_, i) => i !== condIdx)
    onUpdate(branchIndex, { ...branch, conditions: conditions.length > 0 ? conditions : [emptyCondition()] })
  }, [branch, branchIndex, onUpdate])

  const addCondition = useCallback(() => {
    onUpdate(branchIndex, { ...branch, conditions: [...branch.conditions, emptyCondition()] })
  }, [branch, branchIndex, onUpdate])

  const toggleLogic = useCallback(() => {
    onUpdate(branchIndex, { ...branch, logic: branch.logic === 'AND' ? 'OR' : 'AND' })
  }, [branch, branchIndex, onUpdate])

  if (isElse) {
    return (
      <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-white px-3 py-2.5">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-700">{label}</span>
        </div>
        <div className="h-2.5 w-2.5 rounded-full bg-blue-500" />
      </div>
    )
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-700">{label}</span>
          <span className="text-[10px] text-slate-400">优先级{branch.priority}</span>
        </div>
        <div className="flex items-center gap-1">
          {branch.conditions.length > 1 && (
            <button
              type="button"
              className="rounded px-1.5 py-0.5 text-[10px] text-slate-400 hover:bg-slate-100"
              onClick={toggleLogic}
            >
              {branch.logic === 'AND' ? '切换为 或' : '切换为 且'}
            </button>
          )}
          {totalBranches > 2 && branch.type === 'elseif' && (
            <button type="button" className="rounded p-1 text-slate-300 hover:text-red-500 hover:bg-red-50" onClick={() => onRemove(branchIndex)}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12" /></svg>
            </button>
          )}
        </div>
      </div>

      {/* Condition rows */}
      <div className="px-3 py-2 space-y-1.5">
        <div className="grid grid-cols-[auto_1fr_auto_auto_1fr_auto] gap-x-1 text-[10px] text-slate-400 px-0 mb-0.5">
          <div className="w-6" />
          <div>引用变量</div>
          <div className="w-[90px]">选择条件</div>
          <div />
          <div>比较值</div>
          <div className="w-7" />
        </div>
        {branch.conditions.map((cond, condIdx) => (
          <ConditionRow
            key={condIdx}
            cond={cond}
            index={condIdx}
            total={branch.conditions.length}
            logic={branch.logic}
            availableVars={availableVars}
            onUpdate={updateCondition}
            onRemove={removeCondition}
          />
        ))}
      </div>

      {/* Add condition */}
      <div className="px-3 pb-2">
        <button
          type="button"
          className="flex items-center gap-1 text-xs text-blue-500 hover:text-blue-600"
          onClick={addCondition}
        >
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 8v8M8 12h8" /></svg>
          新增
        </button>
      </div>
    </div>
  )
}

/* ========== LLM Branch Card ========== */

interface LlmBranchCardProps {
  branch: Branch
  branchIndex: number
  totalBranches: number
  onUpdate: (index: number, branch: Branch) => void
  onRemove: (index: number) => void
}

function LlmBranchCard({ branch, branchIndex, totalBranches, onUpdate, onRemove }: LlmBranchCardProps) {
  const isElse = branch.type === 'else'
  const label = BRANCH_LABELS[branch.type] ?? branch.name

  if (isElse) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white px-3 py-2.5">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-700">{label}</span>
          <span className="text-[10px] text-slate-400">默认分支</span>
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white">
      <div className="flex items-center justify-between px-3 py-2 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-700">{label}</span>
          <span className="text-[10px] text-slate-400">优先级{branch.priority}</span>
        </div>
        {totalBranches > 2 && branch.type === 'elseif' && (
          <button type="button" className="rounded p-1 text-slate-300 hover:text-red-500 hover:bg-red-50" onClick={() => onRemove(branchIndex)}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12" /></svg>
          </button>
        )}
      </div>
      <div className="px-3 py-2">
        <textarea
          className="w-full min-h-[48px] resize-y rounded border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700 focus:border-blue-400 focus:outline-none"
          value={branch.description ?? ''}
          onChange={(e) => onUpdate(branchIndex, { ...branch, description: e.target.value })}
          placeholder="描述该分支的触发条件，如：用户表达了购买意向"
        />
      </div>
    </div>
  )
}

/* ========== Main Component ========== */

function ConditionBranchEditor({ config, onChange, availableVars = [] }: ConditionBranchEditorProps) {
  const updateStrategy = (strategy: 'EXPRESSION' | 'LLM') => {
    onChange({ ...config, routingStrategy: strategy })
  }

  const updateBranch = useCallback((index: number, branch: Branch) => {
    const branches = config.branches.map((b, i) => i === index ? branch : b)
    onChange({ ...config, branches })
  }, [config, onChange])

  const removeBranch = useCallback((index: number) => {
    const branches = config.branches.filter((_, i) => i !== index)
    // Re-calculate priorities
    let priority = 1
    const updated = branches.map((b) => {
      if (b.type === 'else') return b
      return { ...b, priority: priority++ }
    })
    onChange({ ...config, branches: updated })
  }, [config, onChange])

  const addBranch = useCallback(() => {
    const elseBranch = config.branches.find((b) => b.type === 'else')
    const nonElse = config.branches.filter((b) => b.type !== 'else')
    const newIdx = nonElse.length
    const branch = newBranch(newIdx, 'elseif')
    const updated = [...nonElse, branch, ...(elseBranch ? [elseBranch] : [])]
    onChange({ ...config, branches: updated })
  }, [config, onChange])

  // LLM 配置列表（仅在 LLM 模式下懒加载）
  const [llmConfigs, setLlmConfigs] = useState<LlmConfig[]>([])
  useEffect(() => {
    if (config.routingStrategy !== 'LLM') return
    void getLlmConfigs()
      .then(setLlmConfigs)
      .catch(() => setLlmConfigs([]))
  }, [config.routingStrategy])

  return (
    <div className="space-y-3">
      {/* Strategy selector */}
      <div className="space-y-1">
        <label className="text-xs font-medium text-slate-600">路由策略</label>
        <select className={`w-full ${sel}`} value={config.routingStrategy} onChange={(e) => updateStrategy(e.target.value as 'EXPRESSION' | 'LLM')}>
          <option value="EXPRESSION">表达式</option>
          <option value="LLM">LLM 语义</option>
        </select>
      </div>

      {config.routingStrategy === 'LLM' && (
        <div className="space-y-2">
          {/* 模型选择 */}
          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-600">选择 LLM 模型</label>
            <select
              className={`w-full ${sel}`}
              value={config.llmConfigId ?? ''}
              onChange={(e) => {
                const val = e.target.value
                onChange({ ...config, llmConfigId: val ? Number(val) : undefined })
              }}
            >
              <option value="">请选择模型配置</option>
              {llmConfigs.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.provider})
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-600">路由提示词（可选）</label>
            <textarea
              className={`w-full min-h-[48px] resize-y rounded border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700 focus:border-blue-400 focus:outline-none`}
              value={config.routingPrompt ?? ''}
              onChange={(e) => onChange({ ...config, routingPrompt: e.target.value })}
              placeholder="可选：提供额外的路由判断上下文..."
            />
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-600">条件分支</span>
            <button
              type="button"
              className="flex items-center gap-1 text-xs text-blue-500 hover:text-blue-600"
              onClick={addBranch}
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 8v8M8 12h8" /></svg>
              新增分支
            </button>
          </div>

          <div className="space-y-2">
            {config.branches.map((branch, idx) => (
              <LlmBranchCard
                key={branch.id}
                branch={branch}
                branchIndex={idx}
                totalBranches={config.branches.length}
                onUpdate={updateBranch}
                onRemove={removeBranch}
              />
            ))}
          </div>
        </div>
      )}

      {config.routingStrategy === 'EXPRESSION' && (
        <div className="space-y-2">
          {/* Header */}
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-600">条件分支</span>
            <button
              type="button"
              className="flex items-center gap-1 text-xs text-blue-500 hover:text-blue-600"
              onClick={addBranch}
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 8v8M8 12h8" /></svg>
              新增分支
            </button>
          </div>

          {/* Branch cards */}
          <div className="space-y-2">
            {config.branches.map((branch, idx) => (
              <BranchCard
                key={branch.id}
                branch={branch}
                branchIndex={idx}
                totalBranches={config.branches.length}
                availableVars={availableVars}
                onUpdate={updateBranch}
                onRemove={removeBranch}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default ConditionBranchEditor
export type { ConditionConfig as ConditionBranchConfig }
