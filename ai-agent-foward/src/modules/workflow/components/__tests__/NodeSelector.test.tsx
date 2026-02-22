import { render, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'
import NodeSelector from '../NodeSelector'

describe('NodeSelector', () => {
  const onSelect = vi.fn()
  const onClose = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all 4 node types', () => {
    const { getAllByRole, getByText } = render(
      <NodeSelector onSelect={onSelect} onClose={onClose} />,
    )

    expect(getAllByRole('menuitem')).toHaveLength(4)
    expect(getByText('LLM')).toBeInTheDocument()
    expect(getByText('条件')).toBeInTheDocument()
    expect(getByText('工具')).toBeInTheDocument()
    expect(getByText('HTTP')).toBeInTheDocument()
  })

  it('has role="menu" on the container', () => {
    const { getByRole } = render(
      <NodeSelector onSelect={onSelect} onClose={onClose} />,
    )

    expect(getByRole('menu')).toBeInTheDocument()
  })

  it('calls onSelect with correct nodeType and onClose when clicking a button', () => {
    const { getByText } = render(
      <NodeSelector onSelect={onSelect} onClose={onClose} />,
    )

    fireEvent.click(getByText('LLM'))
    expect(onSelect).toHaveBeenCalledWith('LLM')
    expect(onClose).toHaveBeenCalledTimes(1)

    vi.clearAllMocks()

    fireEvent.click(getByText('条件'))
    expect(onSelect).toHaveBeenCalledWith('CONDITION')
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('renders colored icon containers for each node type', () => {
    const { getByText } = render(
      <NodeSelector onSelect={onSelect} onClose={onClose} />,
    )

    expect(getByText('🧠')).toBeInTheDocument()
    expect(getByText('🔀')).toBeInTheDocument()
    expect(getByText('🔧')).toBeInTheDocument()
    expect(getByText('🌐')).toBeInTheDocument()
  })
})
