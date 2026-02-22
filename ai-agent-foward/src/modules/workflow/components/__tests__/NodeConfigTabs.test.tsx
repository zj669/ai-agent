import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('../FieldRenderer', () => ({
  default: ({ field, value }: any) => (
    <div data-testid={`field-${field.fieldKey}`}>{field.fieldLabel}: {String(value ?? '')}</div>
  ),
}))

const { default: NodeConfigTabs } = await import('../NodeConfigTabs')

const mockTemplate = {
  id: 1,
  typeCode: 'LLM',
  name: 'LLM',
  configFieldGroups: [
    {
      groupName: '基础配置',
      fields: [
        { fieldId: 1, fieldKey: 'model', fieldLabel: '模型', fieldType: 'text' },
        { fieldId: 2, fieldKey: 'systemPrompt', fieldLabel: '系统提示词', fieldType: 'textarea' },
      ],
    },
  ],
} as any

describe('NodeConfigTabs', () => {
  it('renders 3 tabs: 输入, 输出, 配置', () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />
    )
    expect(screen.getByRole('tab', { name: '输入' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '输出' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '配置' })).toBeInTheDocument()
  })

  it('shows config tab content by default', () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ model: 'gpt-4' }}
        onConfigChange={vi.fn()}
      />
    )
    expect(screen.getByTestId('field-model')).toBeInTheDocument()
    expect(screen.getByTestId('field-systemPrompt')).toBeInTheDocument()
  })

  it('switches to input tab', () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[{ key: 'user_input', label: '用户输入', type: 'string' }]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />
    )
    fireEvent.click(screen.getByRole('tab', { name: '输入' }))
    expect(screen.getByText('用户输入')).toBeInTheDocument()
  })

  it('switches to output tab', () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[{ key: 'result', label: '输出结果', type: 'string' }]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />
    )
    fireEvent.click(screen.getByRole('tab', { name: '输出' }))
    expect(screen.getByText('输出结果')).toBeInTheDocument()
  })

  it('shows group name for config fields', () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />
    )
    expect(screen.getByText('基础配置')).toBeInTheDocument()
  })
})