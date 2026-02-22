import { create } from 'zustand'

// Temporary type until metadataAdapter is ready (Task 1)
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
  configFieldGroups: { groupName: string; fields: unknown[] }[]
}

interface EditorState {
  agentName: string
  agentDescription: string
  agentIcon: string
  version: number | null
  isDirty: boolean
  expandedNodeId: string
  operationState: 'idle' | 'saving' | 'publishing'
  nodeTemplates: NodeTemplateDTO[]
  panelCollapsed: boolean

  setAgentInfo: (info: {
    agentName?: string
    agentDescription?: string
    agentIcon?: string
    version?: number | null
  }) => void
  markDirty: () => void
  markClean: () => void
  toggleNodeExpand: (nodeId: string) => void
  setOperationState: (state: 'idle' | 'saving' | 'publishing') => void
  setNodeTemplates: (templates: NodeTemplateDTO[]) => void
  togglePanel: () => void
  reset: () => void
}

const initialState = {
  agentName: '',
  agentDescription: '',
  agentIcon: '',
  version: null as number | null,
  isDirty: false,
  expandedNodeId: '',
  operationState: 'idle' as const,
  nodeTemplates: [] as NodeTemplateDTO[],
  panelCollapsed: false,
}

export const useEditorStore = create<EditorState>((set) => ({
  ...initialState,
  setAgentInfo: (info) => set((s) => ({ ...s, ...info })),
  markDirty: () => set({ isDirty: true }),
  markClean: () => set({ isDirty: false }),
  toggleNodeExpand: (nodeId) =>
    set((s) => ({ expandedNodeId: s.expandedNodeId === nodeId ? '' : nodeId })),
  setOperationState: (operationState) => set({ operationState }),
  setNodeTemplates: (nodeTemplates) => set({ nodeTemplates }),
  togglePanel: () => set((s) => ({ panelCollapsed: !s.panelCollapsed })),
  reset: () => set(initialState),
}))
