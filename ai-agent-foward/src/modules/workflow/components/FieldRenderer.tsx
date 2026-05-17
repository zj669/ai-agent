import { useEffect, useMemo, useRef, useState, type ChangeEvent, type KeyboardEvent } from 'react'
import { getLlmConfigs, type LlmConfig } from '../../llm-config/api/llmConfigService'
import { getKnowledgeDatasetList, type KnowledgeDataset } from '../../../shared/api/adapters/knowledgeAdapter'
import type { ConfigFieldDTO } from '../../../shared/api/adapters/metadataAdapter'
import type { PromptTemplateVariableOption } from './WorkflowNode'

interface FieldRendererProps {
  field: ConfigFieldDTO
  value: unknown
  onChange: (key: string, value: unknown) => void
  templateVariables?: PromptTemplateVariableOption[]
}

const inputClass = 'w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-sm text-slate-800 transition focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none'

/** 缓存已加载的 LLM 配置列表，避免每个节点重复请求 */
let llmConfigCache: LlmConfig[] | null = null

/** 缓存已加载的知识库列表 */
let knowledgeCache: KnowledgeDataset[] | null = null

function getTemplateTrigger(value: string, cursor: number) {
  const beforeCursor = value.slice(0, cursor)
  const start = beforeCursor.lastIndexOf('{{')
  if (start < 0) return null

  const lastClose = beforeCursor.lastIndexOf('}}')
  if (lastClose > start) return null

  return {
    start,
    query: beforeCursor.slice(start + 2).trim().toLowerCase()
  }
}

function templateVariableMatches(variable: PromptTemplateVariableOption, query: string): boolean {
  if (!query) return true
  return [variable.label, variable.detail, variable.template]
    .join(' ')
    .toLowerCase()
    .includes(query)
}

function TemplateTextControl({
  id,
  multiline,
  value,
  placeholder,
  variables,
  onChange
}: {
  id: string
  multiline?: boolean
  value: string
  placeholder?: string
  variables: PromptTemplateVariableOption[]
  onChange: (value: string) => void
}) {
  const inputRef = useRef<HTMLInputElement | HTMLTextAreaElement>(null)
  const selectionRef = useRef({ start: value.length, end: value.length })
  const [trigger, setTrigger] = useState<{ start: number; query: string } | null>(null)

  const filteredVariables = useMemo(
    () => variables.filter((variable) => templateVariableMatches(variable, trigger?.query ?? '')),
    [trigger?.query, variables]
  )

  const groupedVariables = useMemo(
    () =>
      filteredVariables.reduce<Record<string, PromptTemplateVariableOption[]>>((acc, variable) => {
        const key = '祖先节点输出'
        if (!acc[key]) acc[key] = []
        acc[key].push(variable)
        return acc
      }, {}),
    [filteredVariables]
  )

  const syncSelection = () => {
    const input = inputRef.current
    if (!input) return
    const cursor = input.selectionStart ?? input.value.length
    selectionRef.current = {
      start: cursor,
      end: input.selectionEnd ?? cursor
    }
    setTrigger(getTemplateTrigger(input.value, cursor))
  }

  const handleChange = (nextValue: string, cursor: number) => {
    selectionRef.current = { start: cursor, end: cursor }
    onChange(nextValue)
    setTrigger(getTemplateTrigger(nextValue, cursor))
  }

  const insertTemplate = (template: string) => {
    const input = inputRef.current
    if (!input || !trigger) return
    const { start } = trigger
    const end = selectionRef.current.end
    const nextValue = `${value.slice(0, start)}${template}${value.slice(end)}`
    onChange(nextValue)
    setTrigger(null)

    requestAnimationFrame(() => {
      input.focus()
      const cursor = start + template.length
      input.setSelectionRange(cursor, cursor)
      selectionRef.current = { start: cursor, end: cursor }
    })
  }

  const commonProps = {
    id,
    ref: inputRef,
    className: multiline ? `${inputClass} min-h-[80px] resize-y` : inputClass,
    placeholder,
    value,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      handleChange(
        event.target.value,
        event.target.selectionStart ?? event.target.value.length
      ),
    onSelect: syncSelection,
    onClick: syncSelection,
    onKeyUp: syncSelection,
    onFocus: syncSelection,
    onKeyDown: (event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (event.key === 'Escape') {
        setTrigger(null)
      }
      const acceptsTemplate =
        trigger &&
        filteredVariables.length > 0 &&
        (event.key === 'Tab' || (event.key === 'Enter' && !multiline))
      if (acceptsTemplate) {
        event.preventDefault()
        insertTemplate(filteredVariables[0].template)
      }
    }
  }

  return (
    <div className="relative">
      {multiline ? <textarea {...commonProps} /> : <input {...commonProps} type="text" />}

      {trigger && (
        <div className="absolute left-0 top-full z-50 mt-1 max-h-56 w-72 overflow-y-auto rounded-md border border-slate-200 bg-white shadow-lg">
          {filteredVariables.length === 0 ? (
            <div className="px-3 py-2 text-xs text-slate-400">无匹配的可引用变量</div>
          ) : (
            Object.entries(groupedVariables).map(([title, items]) => (
              <div key={title}>
                <div className="sticky top-0 border-b border-slate-100 bg-slate-50 px-3 py-1 text-[10px] font-medium text-slate-500">
                  {title}
                </div>
                {items.map((item) => (
                  <button
                    key={item.template}
                    type="button"
                    className="flex w-full flex-col px-3 py-1.5 text-left text-xs transition hover:bg-blue-50"
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => insertTemplate(item.template)}
                  >
                    <span className="text-slate-700">{item.label}</span>
                    <span className="text-[10px] text-slate-400">{item.detail}</span>
                  </button>
                ))}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

function LlmConfigSelect({ value, onChange, fieldKey, placeholder }: { value: unknown; onChange: (key: string, value: unknown) => void; fieldKey: string; placeholder?: string }) {
  const [configs, setConfigs] = useState<LlmConfig[]>(llmConfigCache ?? [])
  const [loading, setLoading] = useState(!llmConfigCache)

  useEffect(() => {
    if (llmConfigCache) return
    setLoading(true)
    getLlmConfigs()
      .then((data) => {
        llmConfigCache = data
        setConfigs(data)
      })
      .finally(() => setLoading(false))
  }, [])

  const currentValue = value != null ? String(value) : ''

  return (
    <select
      className={inputClass}
      value={currentValue}
      onChange={(e) => onChange(fieldKey, e.target.value === '' ? '' : Number(e.target.value))}
      disabled={loading}
    >
      <option value="">{loading ? '加载中...' : (placeholder ?? '请选择模型配置')}</option>
      {configs.filter((c) => c.status === 1).map((c) => (
        <option key={c.id} value={c.id}>
          {c.name} ({c.provider} / {c.model})
        </option>
      ))}
    </select>
  )
}

function KnowledgeSelect({ value, onChange, fieldKey, placeholder }: { value: unknown; onChange: (key: string, value: unknown) => void; fieldKey: string; placeholder?: string }) {
  const [datasets, setDatasets] = useState<KnowledgeDataset[]>(knowledgeCache ?? [])
  const [loading, setLoading] = useState(!knowledgeCache)

  useEffect(() => {
    if (knowledgeCache) return
    setLoading(true)
    getKnowledgeDatasetList()
      .then((data) => {
        knowledgeCache = data
        setDatasets(data)
      })
      .finally(() => setLoading(false))
  }, [])

  const currentValue = value != null ? String(value) : ''

  return (
    <select
      className={inputClass}
      value={currentValue}
      onChange={(e) => onChange(fieldKey, e.target.value)}
      disabled={loading}
    >
      <option value="">{loading ? '加载中...' : (placeholder ?? '请选择知识库')}</option>
      {datasets.map((d) => (
        <option key={d.datasetId} value={d.datasetId}>
          {d.name} ({d.documentCount} 文档)
        </option>
      ))}
    </select>
  )
}

function FieldRenderer({ field, value, onChange, templateVariables = [] }: FieldRendererProps) {
  const id = `field-${field.fieldKey}`
  const type = field.fieldType?.toLowerCase() ?? 'text'

  switch (type) {
    case 'textarea':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <TemplateTextControl
            id={id}
            multiline
            placeholder={field.placeholder ?? ''}
            value={typeof value === 'string' ? value : ''}
            variables={templateVariables}
            onChange={(nextValue) => onChange(field.fieldKey, nextValue)}
          />
        </div>
      )

    case 'select': {
      const options = Array.isArray(field.options) ? field.options as { label: string; value: string }[] : []
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <select
            id={id}
            className={inputClass}
            value={typeof value === 'string' ? value : ''}
            onChange={(e) => onChange(field.fieldKey, e.target.value)}
          >
            {options.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      )
    }

    case 'switch':
      return (
        <div className="flex items-center justify-between">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <input
            id={id}
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-blue-600"
            checked={!!value}
            onChange={(e) => onChange(field.fieldKey, e.target.checked)}
          />
        </div>
      )

    case 'number':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <input
            id={id}
            type="number"
            className={inputClass}
            placeholder={field.placeholder ?? ''}
            value={value != null ? String(value) : ''}
            onChange={(e) => onChange(field.fieldKey, e.target.value === '' ? '' : Number(e.target.value))}
          />
        </div>
      )

    case 'llm_config_select':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <LlmConfigSelect value={value} onChange={onChange} fieldKey={field.fieldKey} placeholder={field.placeholder ?? undefined} />
        </div>
      )

    case 'boolean':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <select
            id={id}
            className={inputClass}
            value={value === true || value === 'true' ? 'true' : 'false'}
            onChange={(e) => onChange(field.fieldKey, e.target.value === 'true')}
          >
            <option value="false">否</option>
            <option value="true">是</option>
          </select>
        </div>
      )

    case 'knowledge_select':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <KnowledgeSelect value={value} onChange={onChange} fieldKey={field.fieldKey} placeholder={field.placeholder ?? undefined} />
        </div>
      )

    default: // 'text' and fallback
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <TemplateTextControl
            id={id}
            placeholder={field.placeholder ?? ''}
            value={typeof value === 'string' ? value : ''}
            variables={templateVariables}
            onChange={(nextValue) => onChange(field.fieldKey, nextValue)}
          />
        </div>
      )
  }
}

export default FieldRenderer
