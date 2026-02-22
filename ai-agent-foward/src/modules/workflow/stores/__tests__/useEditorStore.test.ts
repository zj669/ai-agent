import { act, renderHook } from '@testing-library/react'

const { useEditorStore } = await import('../useEditorStore')

describe('useEditorStore', () => {
  beforeEach(() => {
    act(() => useEditorStore.getState().reset())
  })

  it('initial state has empty values', () => {
    const state = useEditorStore.getState()
    expect(state.agentName).toBe('')
    expect(state.version).toBeNull()
    expect(state.isDirty).toBe(false)
    expect(state.expandedNodeId).toBe('')
    expect(state.nodeTemplates).toEqual([])
  })

  it('setAgentInfo updates agent fields', () => {
    act(() => useEditorStore.getState().setAgentInfo({ agentName: 'Test', version: 3 }))
    const state = useEditorStore.getState()
    expect(state.agentName).toBe('Test')
    expect(state.version).toBe(3)
  })

  it('markDirty and markClean toggle isDirty', () => {
    act(() => useEditorStore.getState().markDirty())
    expect(useEditorStore.getState().isDirty).toBe(true)
    act(() => useEditorStore.getState().markClean())
    expect(useEditorStore.getState().isDirty).toBe(false)
  })

  it('toggleNodeExpand expands and collapses', () => {
    act(() => useEditorStore.getState().toggleNodeExpand('node-1'))
    expect(useEditorStore.getState().expandedNodeId).toBe('node-1')
    act(() => useEditorStore.getState().toggleNodeExpand('node-1'))
    expect(useEditorStore.getState().expandedNodeId).toBe('')
  })

  it('toggleNodeExpand switches to different node', () => {
    act(() => useEditorStore.getState().toggleNodeExpand('node-1'))
    act(() => useEditorStore.getState().toggleNodeExpand('node-2'))
    expect(useEditorStore.getState().expandedNodeId).toBe('node-2')
  })

  it('setNodeTemplates stores templates', () => {
    const templates = [{ id: 1, typeCode: 'LLM', name: 'LLM', configFieldGroups: [] }]
    act(() => useEditorStore.getState().setNodeTemplates(templates as any))
    expect(useEditorStore.getState().nodeTemplates).toEqual(templates)
  })
})
