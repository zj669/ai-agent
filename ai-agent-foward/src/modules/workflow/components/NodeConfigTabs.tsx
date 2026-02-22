import { useState } from 'react'
import { cn } from '../../../lib/utils'
import type { NodeTemplateDTO } from '../../../shared/api/adapters/metadataAdapter'
import FieldRenderer from './FieldRenderer'

interface FieldSchema {
  key: string
  label: string
  type: string
  description?: string
  required?: boolean
  defaultValue?: unknown
  sourceRef?: string
}

interface NodeConfigTabsProps {
  template: NodeTemplateDTO
  inputSchema: FieldSchema[]
  outputSchema: FieldSchema[]
  userConfig: Record<string, unknown>
  onConfigChange: (key: string, value: unknown) => void
}

type TabKey = 'input' | 'output' | 'config'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'input', label: '输入' },
  { key: 'output', label: '输出' },
  { key: 'config', label: '配置' },
]

function NodeConfigTabs({ template, inputSchema, outputSchema, userConfig, onConfigChange }: NodeConfigTabsProps) {
  const [activeTab, setActiveTab] = useState<TabKey>('config')

  return (
    <div className="flex flex-col">
      <div role="tablist" className="flex border-b border-slate-200">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            className={cn(
              'px-3 py-1.5 text-xs font-medium transition',
              activeTab === tab.key
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-slate-500 hover:text-slate-700'
            )}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="max-h-60 overflow-y-auto p-3">
        {activeTab === 'input' && (
          <div className="space-y-2">
            {inputSchema.length === 0 ? (
              <p className="text-xs text-slate-400">暂无输入字段</p>
            ) : (
              inputSchema.map((field) => (
                <div key={field.key} className="rounded-lg bg-slate-50 px-3 py-2">
                  <div className="text-xs font-medium text-slate-700">{field.label}</div>
                  <div className="text-xs text-slate-400">{field.type}{field.sourceRef ? ` <- ${field.sourceRef}` : ''}</div>
                </div>
              ))
            )}
          </div>
        )}

        {activeTab === 'output' && (
          <div className="space-y-2">
            {outputSchema.length === 0 ? (
              <p className="text-xs text-slate-400">暂无输出字段</p>
            ) : (
              outputSchema.map((field) => (
                <div key={field.key} className="rounded-lg bg-slate-50 px-3 py-2">
                  <div className="text-xs font-medium text-slate-700">{field.label}</div>
                  <div className="text-xs text-slate-400">{field.type}</div>
                </div>
              ))
            )}
          </div>
        )}

        {activeTab === 'config' && (
          <div className="space-y-4">
            {template.configFieldGroups.map((group) => (
              <div key={group.groupName}>
                <h4 className="mb-2 text-xs font-semibold text-slate-500">{group.groupName}</h4>
                <div className="space-y-3">
                  {group.fields.map((field) => (
                    <FieldRenderer
                      key={field.fieldKey}
                      field={field}
                      value={userConfig[field.fieldKey]}
                      onChange={onConfigChange}
                    />
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