import apiClient from './apiClient';
import type { ApiResponse } from '../types/auth';
import type { NodeTemplate } from '../types/execution';

const normalizeOptions = (options: any): Array<{ label: string; value: string | number | boolean }> => {
  if (Array.isArray(options)) {
    return options
      .map((option) => {
        if (option && typeof option === 'object') {
          const label = option.label ?? option.name ?? option.text ?? option.value;
          const value = option.value ?? option.key ?? option.id ?? option.label;
          if (value === undefined || value === null) return null;
          return { label: String(label ?? value), value };
        }

        if (option === undefined || option === null) return null;
        return { label: String(option), value: option };
      })
      .filter(Boolean) as Array<{ label: string; value: string | number | boolean }>;
  }

  if (options && typeof options === 'object') {
    return Object.entries(options).map(([value, label]) => ({
      label: String(label),
      value
    }));
  }

  return [];
};

const normalizeTemplate = (raw: any): NodeTemplate => {
  const templateId = raw?.templateId ?? raw?.id;
  const type = String(raw?.type ?? raw?.typeCode ?? '').toUpperCase();

  const configFieldGroups = Array.isArray(raw?.configFieldGroups)
    ? raw.configFieldGroups.map((group: any) => ({
        groupKey: group?.groupKey,
        groupName: group?.groupName,
        fields: Array.isArray(group?.fields)
          ? group.fields.map((field: any) => ({
              key: field?.key ?? field?.fieldKey ?? '',
              label: field?.label ?? field?.fieldLabel,
              description: field?.description,
              type: field?.type ?? field?.fieldType,
              required:
                typeof field?.required === 'boolean'
                  ? field.required
                  : field?.isRequired === 1 || field?.isRequired === true,
              defaultValue: field?.defaultValue,
              placeholder: field?.placeholder,
              options: normalizeOptions(field?.options),
              enumValues: Array.isArray(field?.enumValues)
                ? field.enumValues
                : Array.isArray(field?.options)
                  ? field.options
                  : undefined
            }))
          : []
      }))
    : [];

  const requiredFromGroups = configFieldGroups
    .flatMap((group) => group.fields || [])
    .filter((field) => field.required && field.key)
    .map((field) => field.key);

  return {
    id: templateId ? String(templateId) : undefined,
    templateId: templateId ? String(templateId) : undefined,
    name: raw?.name ?? type,
    type,
    description: raw?.description,
    icon: raw?.icon,
    category: raw?.category,
    sortOrder: raw?.sortOrder,
    configSchema: {
      type: 'object',
      required: Array.from(new Set(requiredFromGroups)),
      properties: {}
    },
    configFieldGroups,
    inputSchema: Array.isArray(raw?.inputSchema)
      ? raw.inputSchema
      : Array.isArray(raw?.initialSchema?.inputSchema)
        ? raw.initialSchema.inputSchema
        : [],
    outputSchema: Array.isArray(raw?.outputSchema)
      ? raw.outputSchema
      : Array.isArray(raw?.initialSchema?.outputSchema)
        ? raw.initialSchema.outputSchema
        : [],
    defaultConfig: {
      properties:
        raw?.defaultConfig?.properties ||
        raw?.defaultSchemaPolicy?.properties ||
        raw?.defaultSchemaPolicy ||
        {}
    }
  };
};

class MetadataService {
  async getNodeTemplates(): Promise<NodeTemplate[]> {
    const response = await apiClient.get<ApiResponse<any[]>>('/meta/node-types');
    const list = Array.isArray(response.data.data) ? response.data.data : [];

    return list
      .map(normalizeTemplate)
      .filter((template) => Boolean(template.type));
  }
}

export const metadataService = new MetadataService();
