import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

const { default: CanvasToolbar } = await import('../CanvasToolbar')

describe('CanvasToolbar', () => {
  it('renders 5 draggable node types', () => {
    render(<CanvasToolbar />)
    expect(screen.getByText('LLM')).toBeInTheDocument()
    expect(screen.getByText('条件')).toBeInTheDocument()
    expect(screen.getByText('工具')).toBeInTheDocument()
    expect(screen.getByText('HTTP')).toBeInTheDocument()
    expect(screen.getByText('知识库')).toBeInTheDocument()
  })

  it('sets drag data on dragStart', () => {
    render(<CanvasToolbar />)
    const llmItem = screen.getByText('LLM').closest('[draggable]')!
    const setData = vi.fn()
    fireEvent.dragStart(llmItem, { dataTransfer: { setData, effectAllowed: '' } })
    expect(setData).toHaveBeenCalledWith('application/workflow-node-type', 'LLM')
  })
})
