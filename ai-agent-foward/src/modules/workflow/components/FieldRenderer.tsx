import { useEffect, useState } from 'react'
import { getLlmConfigs, type LlmConfig } from '../../llm-config/api/llmConfigService'
import { getKnowledgeDatasetList, type KnowledgeDataset } from '../../../shared/api/adapters/knowledgeAdapter'
import type { ConfigFieldDTO } from '../../../shared/api/adapters/metadataAdapter'

interface FieldRendererProps {
  field: ConfigFieldDTO
  value: unknown
  onChange: (key: string, value: unknown) => void
}

const inputClass = 'w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-sm text-slate-800 transition focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none'

/** 缓存已加载的 LLM 配置列表，避免每个节点重复请求 */
let llmConfigCache: LlmConfig[] | null = null

/** 缓存已加载的知识库列表 */
let knowledgeCache: KnowledgeDataset[] | null = null

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

function FieldRenderer({ field, value, onChange }: FieldRendererProps) {
  const id = `field-${field.fieldKey}`
  const type = field.fieldType?.toLowerCase() ?? 'text'

  switch (type) {
    case 'textarea':
      return (
        <div className="space-y-1">
          <label htmlFor={id} className="text-xs font-medium text-slate-600">{field.fieldLabel}</label>
          <textarea
            id={id}
            className={`${inputClass} min-h-[80px] resize-y`}
            placeholder={field.placeholder ?? ''}
            value={typeof value === 'string' ? value : ''}
            onChange={(e) => onChange(field.fieldKey, e.target.value)}
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
          <input
            id={id}
            type="text"
            className={inputClass}
            placeholder={field.placeholder ?? ''}
            value={typeof value === 'string' ? value : ''}
            onChange={(e) => onChange(field.fieldKey, e.target.value)}
          />
        </div>
      )
  }
}

export default FieldRenderer
