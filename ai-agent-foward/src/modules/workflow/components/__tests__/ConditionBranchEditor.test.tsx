import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

const { default: ConditionBranchEditor } = await import('../ConditionBranchEditor')

describe('ConditionBranchEditor', () => {
  const defaultConfig = {
    routingStrategy: 'EXPRESSION' as const,
    branches: [
      {
        id: 'branch-if-1',
        name: '如果',
        type: 'if' as const,
        priority: 1,
        logic: 'AND' as const,
        conditions: [{ sourceRef: '', operator: 'EQUALS', value: '', valueType: 'literal' as const }],
      },
      {
        id: 'branch-else-1',
        name: '否则',
        type: 'else' as const,
        priority: 2,
        logic: 'AND' as const,
        conditions: [],
      },
    ],
  }

  it('renders routing strategy selector', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('表达式')).toBeInTheDocument()
  })

  it('renders branch names', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByText('如果')).toBeInTheDocument()
  })

  it('renders add branch button', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByText('新增分支')).toBeInTheDocument()
  })

  it('adds a branch when button clicked', () => {
    const onChange = vi.fn()
    render(<ConditionBranchEditor config={defaultConfig} onChange={onChange} />)
    fireEvent.click(screen.getByText('新增分支'))
    expect(onChange).toHaveBeenCalled()
    const newConfig = onChange.mock.calls[0][0]
    expect(newConfig.branches).toHaveLength(3)
  })

  it('shows LLM prompt field when strategy is LLM', () => {
    const llmConfig = {
      routingStrategy: 'LLM' as const,
      routingPrompt: '判断意图',
      branches: [
        { id: 'branch-if-1', name: '如果', type: 'if' as const, priority: 1, logic: 'AND' as const, conditions: [{ sourceRef: '', operator: 'EQUALS', value: '', valueType: 'literal' as const }] },
        { id: 'branch-else-1', name: '否则', type: 'else' as const, priority: 2, logic: 'AND' as const, conditions: [] },
      ],
    }
    render(<ConditionBranchEditor config={llmConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('判断意图')).toBeInTheDocument()
  })

  it('shows operator select in EXPRESSION mode', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('等于')).toBeInTheDocument()
  })
})
