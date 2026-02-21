import { validateConnection } from './validateConnection'

export type WorkflowValidationNode = {
  id: string
}

export type WorkflowValidationEdge = {
  source: string
  target: string
}

export type ValidateWorkflowGraphResult =
  | { ok: true }
  | { ok: false; code: 'NO_EDGE' | 'INVALID_EDGE'; message: string }

export function validateWorkflowGraph(input: {
  nodes: WorkflowValidationNode[]
  edges: WorkflowValidationEdge[]
}): ValidateWorkflowGraphResult {
  if (input.edges.length === 0) {
    return {
      ok: false,
      code: 'NO_EDGE',
      message: '至少添加一条连线后再保存'
    }
  }

  const nodeIds = new Set(input.nodes.map((node) => node.id))

  for (const edge of input.edges) {
    if (!nodeIds.has(edge.source) || !nodeIds.has(edge.target)) {
      return {
        ok: false,
        code: 'INVALID_EDGE',
        message: '存在无效连线，请检查后重试'
      }
    }

    const result = validateConnection({ source: edge.source, target: edge.target })
    if (!result.ok) {
      return {
        ok: false,
        code: 'INVALID_EDGE',
        message: result.message
      }
    }
  }

  return { ok: true }
}
