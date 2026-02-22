import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

const { default: AgentConfigPanel } = await import('../AgentConfigPanel')

describe('AgentConfigPanel', () => {
  const defaults = {
    agentName: '测试 Agent',
    agentDescription: '这是描述',
    agentIcon: '',
    collapsed: false,
    onToggle: vi.fn(),
    onChange: vi.fn(),
  }

  it('renders agent name input', () => {
    render(<AgentConfigPanel {...defaults} />)
    expect(screen.getByDisplayValue('测试 Agent')).toBeInTheDocument()
  })

  it('renders agent description textarea', () => {
    render(<AgentConfigPanel {...defaults} />)
    expect(screen.getByDisplayValue('这是描述')).toBeInTheDocument()
  })

  it('calls onChange when name changes', () => {
    const onChange = vi.fn()
    render(<AgentConfigPanel {...defaults} onChange={onChange} />)
    fireEvent.change(screen.getByDisplayValue('测试 Agent'), { target: { value: '新名称' } })
    expect(onChange).toHaveBeenCalledWith('agentName', '新名称')
  })

  it('calls onToggle when collapse button clicked', () => {
    const onToggle = vi.fn()
    render(<AgentConfigPanel {...defaults} onToggle={onToggle} />)
    fireEvent.click(screen.getByLabelText('收起面板'))
    expect(onToggle).toHaveBeenCalled()
  })

  it('renders nothing visible when collapsed', () => {
    render(<AgentConfigPanel {...defaults} collapsed={true} />)
    expect(screen.queryByDisplayValue('测试 Agent')).not.toBeInTheDocument()
  })
})
