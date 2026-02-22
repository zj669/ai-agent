import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

const { default: ConditionBranchEditor } = await import('../ConditionBranchEditor')

describe('ConditionBranchEditor', () => {
  const defaultConfig = {
    routingStrategy: 'EXPRESSION' as const,
    branches: [
      { name: '分支1', conditionGroups: [{ conditions: [{ sourceRef: '', operator: 'EQUALS', value: '' }] }] },
    ],
  }

  it('renders routing strategy selector', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('表达式')).toBeInTheDocument()
  })

  it('renders branch names', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('分支1')).toBeInTheDocument()
  })

  it('renders add branch button', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByText('+ 添加分支')).toBeInTheDocument()
  })

  it('adds a branch when button clicked', () => {
    const onChange = vi.fn()
    render(<ConditionBranchEditor config={defaultConfig} onChange={onChange} />)
    fireEvent.click(screen.getByText('+ 添加分支'))
    expect(onChange).toHaveBeenCalled()
    const newConfig = onChange.mock.calls[0][0]
    expect(newConfig.branches).toHaveLength(2)
  })

  it('shows LLM prompt field when strategy is LLM', () => {
    const llmConfig = { routingStrategy: 'LLM' as const, routingPrompt: '判断意图', branches: [{ name: '分支1' }] }
    render(<ConditionBranchEditor config={llmConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('判断意图')).toBeInTheDocument()
  })

  it('shows operator select in EXPRESSION mode', () => {
    render(<ConditionBranchEditor config={defaultConfig} onChange={vi.fn()} />)
    expect(screen.getByDisplayValue('EQUALS')).toBeInTheDocument()
  })
})
