import type { ConfigFieldDTO } from '../../../shared/api/adapters/metadataAdapter'

interface FieldRendererProps {
  field: ConfigFieldDTO
  value: unknown
  onChange: (key: string, value: unknown) => void
}

const inputClass = 'w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-sm text-slate-800 transition focus:border-blue-300 focus:ring-1 focus:ring-blue-200 focus:outline-none'

function FieldRenderer({ field, value, onChange }: FieldRendererProps) {
  const id = `field-${field.fieldKey}`

  switch (field.fieldType) {
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
