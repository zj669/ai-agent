import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface ConfigFieldDTO {
  fieldId: number
  fieldKey: string
  fieldLabel: string
  fieldType: string
  options: unknown
  defaultValue: string
  placeholder: string
  description: string
  validationRules: unknown
  groupName: string
  sortOrder: number
  overrideDefault: string
  isRequired: number
}

export interface ConfigFieldGroupDTO {
  groupName: string
  fields: ConfigFieldDTO[]
}

export interface NodeTemplateDTO {
  id: number
  typeCode: string
  name: string
  description: string
  icon: string
  category: string
  sortOrder: number
  defaultSchemaPolicy: unknown
  initialSchema: unknown
  configFieldGroups: ConfigFieldGroupDTO[]
}

export async function fetchNodeTemplates(
  client: ApiClientLike = apiClient
): Promise<NodeTemplateDTO[]> {
  const response = await client.get<ApiResponse<NodeTemplateDTO[]>>('/api/meta/node-templates')
  return unwrapResponse(response)
}
