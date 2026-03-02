import { useState } from 'react'
import { cn } from '../../../lib/utils'
import type { NodeTemplateDTO } from '../../../shared/api/adapters/metadataAdapter'
import type { SchemaPolicy, FieldSchema } from './WorkflowNode'
import VariableRefSelector, { type UpstreamVariable } from './VariableRefSelector'
import FieldRenderer from './FieldRenderer'

interface NodeConfigTabsProps {
  template: NodeTemplateDTO | null
  policy?: SchemaPolicy
  inputSchema: FieldSchema[]
  outputSchema: FieldSchema[]
  userConfig: Record<string, unknown>
  upstreamVariables?: UpstreamVariable[]
  onConfigChange: (key: string, value: unknown) => void
  onInputSchemaChange?: (schema: FieldSchema[]) => void
  onOutputSchemaChange?: (schema: FieldSchema[]) => void
}

type TabKey = 'input' | 'output' | 'config'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'input', label: '输入' },
  { key: 'output', label: '输出' },
  { key: 'config', label: '配置' },
]

const FIELD_TYPES = ['string', 'number', 'boolean', 'array', 'object']

function InputFieldCard({
  field,
  canUpdate,
  canDelete,
  upstreamVariables,
  onUpdate,
  onDelete,
}: {
  field: FieldSchema
  canUpdate: boolean
  canDelete: boolean
  upstreamVariables: UpstreamVariable[]
  onUpdate: (updated: FieldSchema) => void
  onDelete: () => void
}) {
  const [editingMeta, setEditingMeta] = useState(false)
  const isSystem = field.system === true
  const editable = canUpdate && !isSystem
  const hasRef = !!field.sourceRef

  return (
    <div className={cn('rounded-lg px-3 py-2 space-y-1.5', isSystem ? 'bg-slate-100' : 'bg-slate-50')}>
      <div className="group flex items-center gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-medium text-slate-700">{field.label || field.key}</span>
            {isSystem && <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">系统</span>}
            {field.required && <span className="text-red-400 text-[10px]">*</span>}
            <span className="text-[10px] text-slate-400">{field.type}</span>
          </div>
        </div>
        <div className="hidden group-hover:flex items-center gap-1">
          {editable && (
            <button className="text-xs text-slate-400 hover:text-blue-500" onClick={() => setEditingMeta(!editingMeta)} title="编辑字段定义">✏️</button>
          )}
          {canDelete && !isSystem && (
            <button className="text-xs text-slate-400 hover:text-red-500" onClick={onDelete} title="删除">🗑️</button>
          )}
        </div>
      </div>

      {editingMeta && editable && (
        <div className="rounded border border-blue-200 bg-blue-50/50 px-2 py-1.5 space-y-1.5">
          <div className="flex gap-2">
            <input className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs" placeholder="字段名" defaultValue={field.key} onBlur={(e) => onUpdate({ ...field, key: e.target.value })} />
            <input className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs" placeholder="标签" defaultValue={field.label} onBlur={(e) => onUpdate({ ...field, label: e.target.value })} />
          </div>
          <div className="flex gap-2 items-center">
            <select className="rounded border border-slate-300 px-2 py-1 text-xs" defaultValue={field.type} onChange={(e) => onUpdate({ ...field, type: e.target.value })}>
              {FIELD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <label className="flex items-center gap-1 text-xs text-slate-500">
              <input type="checkbox" defaultChecked={field.required ?? false} onChange={(e) => onUpdate({ ...field, required: e.target.checked })} />
              必填
            </label>
            <button className="ml-auto text-xs text-blue-500 hover:text-blue-700" onClick={() => setEditingMeta(false)}>完成</button>
          </div>
        </div>
      )}

      <div className="space-y-1">
        <VariableRefSelector
          value={field.sourceRef ?? ''}
          variables={upstreamVariables}
          onSelect={(ref) => onUpdate({ ...field, sourceRef: ref, defaultValue: undefined })}
          onClear={() => onUpdate({ ...field, sourceRef: '' })}
        />

        {!hasRef && (
          <div>
            <input
              className="w-full rounded border border-slate-300 px-2 py-1 text-xs placeholder:text-slate-300"
              placeholder="输入默认值..."
              value={field.defaultValue != null ? String(field.defaultValue) : ''}
              onChange={(e) => onUpdate({ ...field, defaultValue: e.target.value || undefined })}
            />
          </div>
        )}
      </div>
    </div>
  )
}

function OutputFieldCard({
  field,
  canUpdate,
  canDelete,
  onUpdate,
  onDelete,
}: {
  field: FieldSchema
  canUpdate: boolean
  canDelete: boolean
  onUpdate: (updated: FieldSchema) => void
  onDelete: () => void
}) {
  const [editing, setEditing] = useState(false)
  const isSystem = field.system === true
  const editable = canUpdate && !isSystem

  if (editing && editable) {
    return (
      <div className="rounded-lg border border-blue-200 bg-blue-50/50 px-3 py-2 space-y-2">
        <div className="flex gap-2">
          <input className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs" placeholder="字段名" defaultValue={field.key} onBlur={(e) => onUpdate({ ...field, key: e.target.value })} />
          <input className="flex-1 rounded border border-slate-300 px-2 py-1 text-xs" placeholder="标签" defaultValue={field.label} onBlur={(e) => onUpdate({ ...field, label: e.target.value })} />
        </div>
        <div className="flex gap-2 items-center">
          <select className="rounded border border-slate-300 px-2 py-1 text-xs" defaultValue={field.type} onChange={(e) => onUpdate({ ...field, type: e.target.value })}>
            {FIELD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <button className="ml-auto text-xs text-blue-500 hover:text-blue-700" onClick={() => setEditing(false)}>完成</button>
        </div>
      </div>
    )
  }

  return (
    <div className={cn('group rounded-lg px-3 py-2 flex items-center gap-2', isSystem ? 'bg-slate-100' : 'bg-slate-50')}>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-slate-700">{field.label || field.key}</span>
          {isSystem && <span className="rounded bg-slate-200 px-1 py-0.5 text-[10px] text-slate-500">系统</span>}
        </div>
        <div className="text-xs text-slate-400">{field.key} · {field.type}</div>
      </div>
      <div className="hidden group-hover:flex items-center gap-1">
        {editable && <button className="text-xs text-slate-400 hover:text-blue-500" onClick={() => setEditing(true)} title="编辑">✏️</button>}
        {canDelete && !isSystem && <button className="text-xs text-slate-400 hover:text-red-500" onClick={onDelete} title="删除">🗑️</button>}
      </div>
    </div>
  )
}

function NodeConfigTabs({
  template,
  policy,
  inputSchema,
  outputSchema,
  userConfig,
  upstreamVariables = [],
  onConfigChange,
  onInputSchemaChange,
  onOutputSchemaChange,
}: NodeConfigTabsProps) {
  const hasConfigTab = template && template.configFieldGroups && template.configFieldGroups.length > 0
  const availableTabs = hasConfigTab ? TABS : TABS.filter((t) => t.key !== 'config')
  const [activeTab, setActiveTab] = useState<TabKey>(hasConfigTab ? 'config' : 'input')

  const canAddInput = policy?.inputSchemaAdd !== false
  const canAddOutput = policy?.outputSchemaAdd !== false
  const canUpdateInput = policy?.inputSchemaUpdate !== false
  const canUpdateOutput = policy?.outputSchemaUpdate !== false

  const handleAddField = (target: 'input' | 'output') => {
    const newField: FieldSchema = { key: `field_${Date.now()}`, label: '新字段', type: 'string' }
    if (target === 'input') onInputSchemaChange?.([...inputSchema, newField])
    else onOutputSchemaChange?.([...outputSchema, newField])
  }

  const handleUpdateInputField = (index: number, updated: FieldSchema) => {
    const next = [...inputSchema]
    next[index] = updated
    onInputSchemaChange?.(next)
  }

  const handleUpdateOutputField = (index: number, updated: FieldSchema) => {
    const next = [...outputSchema]
    next[index] = updated
    onOutputSchemaChange?.(next)
  }

  const handleDeleteField = (target: 'input' | 'output', index: number) => {
    if (target === 'input') onInputSchemaChange?.(inputSchema.filter((_, i) => i !== index))
    else onOutputSchemaChange?.(outputSchema.filter((_, i) => i !== index))
  }

  return (
    <div className="flex flex-col">
      <div role="tablist" className="flex border-b border-slate-200">
        {availableTabs.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            className={cn('px-3 py-1.5 text-xs font-medium transition', activeTab === tab.key ? 'border-b-2 border-blue-500 text-blue-600' : 'text-slate-500 hover:text-slate-700')}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="nowheel max-h-72 overflow-y-auto p-3">
        {activeTab === 'input' && (
          <div className="space-y-2">
            {inputSchema.length === 0 ? <p className="text-xs text-slate-400">暂无输入字段</p> : inputSchema.map((field, i) => (
              <InputFieldCard
                key={field.key}
                field={field}
                canUpdate={canUpdateInput}
                canDelete={canAddInput}
                upstreamVariables={upstreamVariables}
                onUpdate={(updated) => handleUpdateInputField(i, updated)}
                onDelete={() => handleDeleteField('input', i)}
              />
            ))}
            {canAddInput && <button className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition" onClick={() => handleAddField('input')}>+ 添加输入字段</button>}
          </div>
        )}

        {activeTab === 'output' && (
          <div className="space-y-2">
            {outputSchema.length === 0 ? <p className="text-xs text-slate-400">暂无输出字段</p> : outputSchema.map((field, i) => (
              <OutputFieldCard
                key={field.key}
                field={field}
                canUpdate={canUpdateOutput}
                canDelete={canAddOutput}
                onUpdate={(updated) => handleUpdateOutputField(i, updated)}
                onDelete={() => handleDeleteField('output', i)}
              />
            ))}
            {canAddOutput && <button className="w-full rounded-lg border border-dashed border-slate-300 py-1.5 text-xs text-slate-400 hover:border-blue-400 hover:text-blue-500 transition" onClick={() => handleAddField('output')}>+ 添加输出字段</button>}
          </div>
        )}

        {activeTab === 'config' && template && (
          <div className="space-y-4">
            {template.configFieldGroups.map((group) => (
              <div key={group.groupName}>
                <h4 className="mb-2 text-xs font-semibold text-slate-500">{group.groupName}</h4>
                <div className="space-y-3">
                  {group.fields.map((field) => (
                    <FieldRenderer key={field.fieldKey} field={field} value={userConfig[field.fieldKey]} onChange={onConfigChange} />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default NodeConfigTabs
