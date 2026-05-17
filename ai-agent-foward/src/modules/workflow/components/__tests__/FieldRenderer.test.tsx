import { render, screen, fireEvent } from '@testing-library/react'
import { vi } from 'vitest'
import type { ConfigFieldDTO } from '../../../../shared/api/adapters/metadataAdapter'

const { default: FieldRenderer } = await import('../FieldRenderer')

const createField = (overrides: Partial<ConfigFieldDTO>): ConfigFieldDTO => ({
  fieldId: 0,
  fieldKey: 'field',
  fieldLabel: '字段',
  fieldType: 'text',
  options: null,
  defaultValue: '',
  placeholder: '',
  description: '',
  validationRules: null,
  groupName: '基础配置',
  sortOrder: 0,
  overrideDefault: '',
  isRequired: 0,
  ...overrides,
})

describe('FieldRenderer', () => {
  it('renders text input for fieldType=text', () => {
    const onChange = vi.fn()
    render(
      <FieldRenderer
        field={createField({ fieldId: 1, fieldKey: 'model', fieldLabel: '模型', fieldType: 'text', placeholder: '请输入模型名', isRequired: 1 })}
        value=""
        onChange={onChange}
      />
    )
    expect(screen.getByLabelText('模型')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('模型'), { target: { value: 'gpt-4' } })
    expect(onChange).toHaveBeenCalledWith('model', 'gpt-4')
  })

  it('renders textarea for fieldType=textarea', () => {
    render(
      <FieldRenderer
        field={createField({ fieldId: 2, fieldKey: 'prompt', fieldLabel: '提示词', fieldType: 'textarea', placeholder: '输入提示词' })}
        value="hello"
        onChange={vi.fn()}
      />
    )
    expect(screen.getByLabelText('提示词')).toBeInTheDocument()
    expect(screen.getByDisplayValue('hello')).toBeInTheDocument()
  })

  it('renders select for fieldType=select with options', () => {
    render(
      <FieldRenderer
        field={createField({ fieldId: 3, fieldKey: 'method', fieldLabel: '方法', fieldType: 'select', options: [{ label: 'GET', value: 'GET' }, { label: 'POST', value: 'POST' }] })}
        value="GET"
        onChange={vi.fn()}
      />
    )
    expect(screen.getByLabelText('方法')).toBeInTheDocument()
    expect(screen.getByDisplayValue('GET')).toBeInTheDocument()
  })

  it('renders switch for fieldType=switch', () => {
    const onChange = vi.fn()
    render(
      <FieldRenderer
        field={createField({ fieldId: 4, fieldKey: 'debug', fieldLabel: '调试模式', fieldType: 'switch' })}
        value={false}
        onChange={onChange}
      />
    )
    expect(screen.getByLabelText('调试模式')).toBeInTheDocument()
  })

  it('renders number input for fieldType=number', () => {
    render(
      <FieldRenderer
        field={createField({ fieldId: 5, fieldKey: 'timeout', fieldLabel: '超时', fieldType: 'number', placeholder: '毫秒' })}
        value={5000}
        onChange={vi.fn()}
      />
    )
    expect(screen.getByLabelText('超时')).toBeInTheDocument()
    expect(screen.getByDisplayValue('5000')).toBeInTheDocument()
  })
})
