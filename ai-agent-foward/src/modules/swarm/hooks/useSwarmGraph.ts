import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { SwarmAgent, GraphEdge as SwarmGraphEdge } from '../types/swarm'

const LEVEL_GAP_Y = 120
const SIBLING_GAP_X = 180

interface TreeNode {
  agent: SwarmAgent
  children: TreeNode[]
}

function buildTree(agents: SwarmAgent[]): TreeNode[] {
  const map = new Map<number, TreeNode>()
  agents.forEach(a => map.set(a.id, { agent: a, children: [] }))

  const roots: TreeNode[] = []
  agents.forEach(a => {
    const node = map.get(a.id)!
    if (a.parentId && map.has(a.parentId)) {
      map.get(a.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/**
 * 布局逻辑：
 * - human 节点和它的直接子节点（初始 assistant）放在同一行（depth=0）
 * - 后续子 agent 才开始树形展开（depth=1, 2, ...）
 */
function layoutTree(roots: TreeNode[]): Map<number, { x: number; y: number }> {
  const positions = new Map<number, { x: number; y: number }>()
  let globalX = 0

  // 收集顶层节点：roots + human 的直接子节点提升为同级
  const topLevel: TreeNode[] = []

  for (const root of roots) {
    if (root.agent.role === 'human') {
      topLevel.push(root)
      // 把 human 的直接子节点也提升到顶层
      for (const child of root.children) {
        topLevel.push(child)
      }
      // 清空 human 的 children（已提升）
      root.children = []
    } else {
      topLevel.push(root)
    }
  }

  // 先布局顶层节点的子树（确定宽度）
  function layoutSubTree(node: TreeNode, depth: number) {
    if (node.children.length === 0) {
      positions.set(node.agent.id, { x: globalX, y: depth * LEVEL_GAP_Y })
      globalX += SIBLING_GAP_X
    } else {
      node.children.forEach(child => layoutSubTree(child, depth + 1))
      const childPositions = node.children.map(c => positions.get(c.agent.id)!)
      const avgX = childPositions.reduce((sum, p) => sum + p.x, 0) / childPositions.length
      positions.set(node.agent.id, { x: avgX, y: depth * LEVEL_GAP_Y })
    }
  }

  // 布局每个顶层节点及其子树
  for (const node of topLevel) {
    if (node.children.length === 0) {
      positions.set(node.agent.id, { x: globalX, y: 0 })
      globalX += SIBLING_GAP_X
    } else {
      // 先布局子树
      node.children.forEach(child => layoutSubTree(child, 1))
      const childPositions = node.children.map(c => positions.get(c.agent.id)!)
      const avgX = childPositions.reduce((sum, p) => sum + p.x, 0) / childPositions.length
      positions.set(node.agent.id, { x: avgX, y: 0 })
    }
  }

  return positions
}

export function useSwarmGraph(agents: SwarmAgent[], graphEdges: SwarmGraphEdge[] = []) {
  const { nodes, edges } = useMemo(() => {
    const tree = buildTree(agents)
    const positions = layoutTree(tree)

    const nodes: Node[] = agents.map(a => {
      const pos = positions.get(a.id) ?? { x: 0, y: 0 }
      return {
        id: String(a.id),
        type: 'swarmNode',
        position: pos,
        data: { role: a.role, status: a.status, agentId: a.id },
      }
    })

    // 连线：跳过 human→assistant 的 parent 关系（它们是同级），其余保留
    const humanAgent = agents.find(a => a.role === 'human')
    const edges: Edge[] = agents
      .filter(a => a.parentId)
      .filter(a => !(humanAgent && a.parentId === humanAgent.id))
      .map(a => {
        const edgeData = graphEdges.find(e => e.from === a.parentId && e.to === a.id)
        return {
          id: `e-${a.parentId}-${a.id}`,
          source: String(a.parentId),
          target: String(a.id),
          type: 'swarmEdge',
          data: { count: edgeData?.count ?? 0 },
        }
      })

    return { nodes, edges }
  }, [agents, graphEdges])

  return { nodes, edges }
}
