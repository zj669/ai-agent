interface Condition {
  sourceRef: string
  operator: string
  value: string
}

interface ConditionGroup {
  conditions: Condition[]
}

interface Branch {
  name: string
  conditionGroups?: ConditionGroup[]
}

interface ConditionConfig {
  routingStrategy: 'EXPRESSION' | 'LLM'
  routingPrompt?: string
  branches: Branch[]
}

interface ConditionBranchEditorProps {
  config: ConditionConfig
  onChange: (config: ConditionConfig) => void
}

const OPERATORS = ['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'GT', 'LT', 'GTE', 'LTE', 'IS_EMPTY', 'IS_NOT_EMPTY']
const inputClass = 'rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-800 focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none'

function ConditionBranchEditor({ config, onChange }: ConditionBranchEditorProps) {
  const updateStrategy = (strategy: 'EXPRESSION' | 'LLM') => {
    onChange({ ...config, routingStrategy: strategy })
  }

  const addBranch = () => {
    const newBranch: Branch = config.routingStrategy === 'EXPRESSION'
      ? { name: `分支${config.branches.length + 1}`, conditionGroups: [{ conditions: [{ sourceRef: '', operator: 'EQUALS', value: '' }] }] }
      : { name: `分支${config.branches.length + 1}` }
    onChange({ ...config, branches: [...config.branches, newBranch] })
  }

  const updateBranchName = (index: number, name: string) => {
    const branches = config.branches.map((b, i) => i === index ? { ...b, name } : b)
    onChange({ ...config, branches })
  }

  const updateCondition = (branchIdx: number, groupIdx: number, condIdx: number, field: keyof Condition, value: string) => {
    const branches = config.branches.map((branch, bi) => {
      if (bi !== branchIdx || !branch.conditionGroups) return branch
      const groups = branch.conditionGroups.map((group, gi) => {
        if (gi !== groupIdx) return group
        const conditions = group.conditions.map((cond, ci) =>
          ci === condIdx ? { ...cond, [field]: value } : cond
        )
        return { ...group, conditions }
      })
      return { ...branch, conditionGroups: groups }
    })
    onChange({ ...config, branches })
  }

  const removeBranch = (index: number) => {
    onChange({ ...config, branches: config.branches.filter((_, i) => i !== index) })
  }

  return (
    <div className="space-y-3">
      <div className="space-y-1">
        <label className="text-xs font-medium text-slate-600">路由策略</label>
        <select className={`w-full ${inputClass}`} value={config.routingStrategy} onChange={(e) => updateStrategy(e.target.value as 'EXPRESSION' | 'LLM')}>
          <option value="EXPRESSION">表达式</option>
          <option value="LLM">LLM 语义</option>
        </select>
      </div>

      {config.routingStrategy === 'LLM' && (
        <div className="space-y-1">
          <label className="text-xs font-medium text-slate-600">路由提示词</label>
          <textarea
            className={`w-full min-h-[60px] resize-y ${inputClass}`}
            value={config.routingPrompt ?? ''}
            onChange={(e) => onChange({ ...config, routingPrompt: e.target.value })}
            placeholder="描述如何判断走哪个分支..."
          />
        </div>
      )}

      <div className="space-y-2">
        {config.branches.map((branch, branchIdx) => (
          <div key={branchIdx} className="rounded-lg border border-slate-200 bg-white p-2 space-y-2">
            <div className="flex items-center gap-2">
              <input
                className={`flex-1 ${inputClass}`}
                value={branch.name}
                onChange={(e) => updateBranchName(branchIdx, e.target.value)}
                placeholder="分支名称"
              />
              {config.branches.length > 1 && (
                <button type="button" className="text-xs text-red-400 hover:text-red-600" onClick={() => removeBranch(branchIdx)}>删除</button>
              )}
            </div>

            {config.routingStrategy === 'EXPRESSION' && branch.conditionGroups?.map((group, groupIdx) => (
              <div key={groupIdx} className="space-y-1 pl-2 border-l-2 border-slate-100">
                {group.conditions.map((cond, condIdx) => (
                  <div key={condIdx} className="flex items-center gap-1">
                    <input className={`flex-1 ${inputClass}`} value={cond.sourceRef} placeholder="变量引用" onChange={(e) => updateCondition(branchIdx, groupIdx, condIdx, 'sourceRef', e.target.value)} />
                    <select className={inputClass} value={cond.operator} onChange={(e) => updateCondition(branchIdx, groupIdx, condIdx, 'operator', e.target.value)}>
                      {OPERATORS.map((op) => <option key={op} value={op}>{op}</option>)}
                    </select>
                    <input className={`flex-1 ${inputClass}`} value={cond.value} placeholder="值" onChange={(e) => updateCondition(branchIdx, groupIdx, condIdx, 'value', e.target.value)} />
                  </div>
                ))}
              </div>
            ))}
          </div>
        ))}
      </div>

      <button type="button" className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-500 transition hover:border-blue-300 hover:text-blue-500" onClick={addBranch}>
        + 添加分支
      </button>
    </div>
  )
}

export default ConditionBranchEditor
