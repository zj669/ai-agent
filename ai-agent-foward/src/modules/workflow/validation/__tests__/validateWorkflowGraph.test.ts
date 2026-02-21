import { describe, expect, it } from 'vitest'
import { validateWorkflowGraph } from '../validateWorkflowGraph'

describe('validateWorkflowGraph', () => {
  it('无连线时返回 NO_EDGE', () => {
    const result = validateWorkflowGraph({
      nodes: [{ id: 'start' }, { id: 'end' }],
      edges: []
    })

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.code).toBe('NO_EDGE')
    }
  })

  it('边引用不存在节点时返回 INVALID_EDGE', () => {
    const result = validateWorkflowGraph({
      nodes: [{ id: 'start' }],
      edges: [{ source: 'start', target: 'end' }]
    })

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.code).toBe('INVALID_EDGE')
    }
  })

  it('边自连时返回 INVALID_EDGE', () => {
    const result = validateWorkflowGraph({
      nodes: [{ id: 'start' }],
      edges: [{ source: 'start', target: 'start' }]
    })

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.code).toBe('INVALID_EDGE')
      expect(result.message).toBe('不允许节点自连')
    }
  })

  it('图合法时校验通过', () => {
    const result = validateWorkflowGraph({
      nodes: [{ id: 'start' }, { id: 'end' }],
      edges: [{ source: 'start', target: 'end' }]
    })

    expect(result).toEqual({ ok: true })
  })
})
