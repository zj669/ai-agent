import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

const mockStore = {
  expandedNodeId: '',
  toggleNodeExpand: vi.fn(),
  nodeTemplates: [
    { id: 1, typeCode: 'LLM', name: 'LLM', icon: '🧠', configFieldGroups: [] },
    { id: 2, typeCode: 'START', name: '开始', icon: '▶', configFieldGroups: [] },
  ],
}

vi.mock('../../stores/useEditorStore', () => ({
  useEditorStore: (selector: (s: typeof mockStore) => unknown) => selector(mockStore),
}))

vi.mock('../NodeConfigTabs', () => ({
  default: () => <div data-testid="node-config-tabs">Config Tabs</div>,
}))

vi.mock('@xyflow/react', () => ({
  Handle: (props: Record<string, unknown>) => (
    <div data-testid={`handle-${props.id ?? props.type}`} data-type={props.type} data-position={props.position} />
  ),
  Position: { Left: 'left', Right: 'right', Top: 'top', Bottom: 'bottom' },
}))

const { default: WorkflowNode } = await import('../WorkflowNode')

describe('WorkflowNode', () => {
  it('renders collapsed node with icon and label', () => {
    render(<WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />)
    expect(screen.getByText('LLM 节点')).toBeInTheDocument()
    expect(screen.getByText('LLM')).toBeInTheDocument()
  })

  it('shows expand arrow for non-START/END nodes', () => {
    render(<WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />)
    expect(screen.getByLabelText('展开配置')).toBeInTheDocument()
  })

  it('does NOT show expand arrow for START node', () => {
    render(<WorkflowNode id="start" data={{ label: '开始', nodeType: 'START' }} selected={false} />)
    expect(screen.queryByLabelText('展开配置')).not.toBeInTheDocument()
  })

  it('calls toggleNodeExpand when arrow clicked', () => {
    render(<WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />)
    fireEvent.click(screen.getByLabelText('展开配置'))
    expect(mockStore.toggleNodeExpand).toHaveBeenCalledWith('llm-1')
  })

  it('shows NodeConfigTabs when expanded', () => {
    mockStore.expandedNodeId = 'llm-1'
    render(<WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />)
    expect(screen.getByTestId('node-config-tabs')).toBeInTheDocument()
    mockStore.expandedNodeId = ''
  })

  it('renders left target handle and right source handle for LLM node', () => {
    const { getByTestId } = render(
      <WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />
    )
    expect(getByTestId('handle-target').dataset.position).toBe('left')
    expect(getByTestId('handle-source').dataset.position).toBe('right')
  })

  it('CONDITION node renders multiple source handles from branches', () => {
    const data = {
      label: '条件节点',
      nodeType: 'CONDITION',
      branches: [
        { id: 'branch-0', name: '如果' },
        { id: 'else', name: '否则' },
      ],
    }
    const { getByTestId } = render(
      <WorkflowNode id="cond-1" data={data} selected={false} />
    )
    expect(getByTestId('handle-target')).toBeInTheDocument()
    expect(getByTestId('handle-branch-0')).toBeInTheDocument()
    expect(getByTestId('handle-else')).toBeInTheDocument()
  })
})
