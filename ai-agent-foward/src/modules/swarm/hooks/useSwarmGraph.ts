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

function layoutTree(roots: TreeNode[]): Map<number, { x: number; y: number }> {
  const positions = new Map<number, { x: number; y: number }>()
  let globalX = 0

  function layout(node: TreeNode, depth: number) {
    if (node.children.length === 0) {
      positions.set(node.agent.id, { x: globalX, y: depth * LEVEL_GAP_Y })
      globalX += SIBLING_GAP_X
    } else {
      node.children.forEach(child => layout(child, depth + 1))
      const childPositions = node.children.map(c => positions.get(c.agent.id)!)
      const avgX = childPositions.reduce((sum, p) => sum + p.x, 0) / childPositions.length
      positions.set(node.agent.id, { x: avgX, y: depth * LEVEL_GAP_Y })
    }
  }

  roots.forEach(root => layout(root, 0))
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

    const edges: Edge[] = agents
      .filter(a => a.parentId)
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
