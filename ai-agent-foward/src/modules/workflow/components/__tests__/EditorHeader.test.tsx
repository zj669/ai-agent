import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

const { default: EditorHeader } = await import('../EditorHeader')

function renderHeader(props = {}) {
  const defaults = {
    agentName: '测试 Agent',
    isDirty: false,
    operationState: 'idle' as const,
    onSave: vi.fn(),
    onPublish: vi.fn(),
    ...props,
  }
  return { ...render(<MemoryRouter><EditorHeader {...defaults} /></MemoryRouter>), props: defaults }
}

describe('EditorHeader', () => {
  it('displays agent name', () => {
    renderHeader()
    expect(screen.getByText('测试 Agent')).toBeInTheDocument()
  })

  it('shows 已保存 when not dirty', () => {
    renderHeader({ isDirty: false })
    expect(screen.getByText('已保存')).toBeInTheDocument()
  })

  it('shows 未保存 when dirty', () => {
    renderHeader({ isDirty: true })
    expect(screen.getByText('未保存')).toBeInTheDocument()
  })

  it('shows 保存中... when saving', () => {
    renderHeader({ operationState: 'saving' })
    const matches = screen.getAllByText('保存中...')
    expect(matches.length).toBeGreaterThanOrEqual(1)
  })

  it('calls onSave when save button clicked', () => {
    const onSave = vi.fn()
    renderHeader({ onSave })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(onSave).toHaveBeenCalledTimes(1)
  })

  it('calls onPublish when publish button clicked', () => {
    const onPublish = vi.fn()
    renderHeader({ onPublish })
    fireEvent.click(screen.getByRole('button', { name: '发布' }))
    expect(onPublish).toHaveBeenCalledTimes(1)
  })

  it('disables buttons when saving', () => {
    renderHeader({ operationState: 'saving' })
    expect(screen.getByRole('button', { name: /保存/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: '发布' })).toBeDisabled()
  })

  it('has a back link to /agents', () => {
    renderHeader()
    expect(screen.getByRole('link', { name: /返回/ })).toHaveAttribute('href', '/agents')
  })
})
