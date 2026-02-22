import { createContext, useContext, type ReactNode } from 'react'
import type { WorkflowNodeType } from './WorkflowNode'

export type OnNodeAdd = (nodeType: WorkflowNodeType, sourceNodeId: string, sourceHandleId: string) => void
export type OnConfigChange = (nodeId: string, key: string, value: unknown) => void
export type OnSchemaChange = (nodeId: string, tab: 'input' | 'output', schema: { key: string; label: string; type: string; sourceRef?: string }[]) => void

interface EditorCallbacks {
  onNodeAdd: OnNodeAdd | null
  onConfigChange: OnConfigChange | null
  onSchemaChange: OnSchemaChange | null
}

const EditorCallbackContext = createContext<EditorCallbacks>({ onNodeAdd: null, onConfigChange: null, onSchemaChange: null })

export function EditorCallbackProvider({
  onNodeAdd,
  onConfigChange,
  onSchemaChange,
  children,
}: {
  onNodeAdd: OnNodeAdd
  onConfigChange: OnConfigChange
  onSchemaChange: OnSchemaChange
  children: ReactNode
}) {
  return (
    <EditorCallbackContext.Provider value={{ onNodeAdd, onConfigChange, onSchemaChange }}>
      {children}
    </EditorCallbackContext.Provider>
  )
}

export function useEditorCallbacks() {
  return useContext(EditorCallbackContext)
}
