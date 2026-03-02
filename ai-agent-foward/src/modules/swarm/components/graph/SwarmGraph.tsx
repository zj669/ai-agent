import { useCallback, useEffect, useRef } from 'react'
import { ReactFlow, Background, Controls, MiniMap, type NodeTypes, type EdgeTypes, type ReactFlowInstance } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import GraphNodeComponent from './GraphNode'
import GraphEdgeComponent from './GraphEdge'
import { BEAM_STYLES } from './GraphBeam'
import { useSwarmGraph } from '../../hooks/useSwarmGraph'
import type { SwarmAgent, GraphEdge } from '../../types/swarm'

const nodeTypes: NodeTypes = {
  swarmNode: GraphNodeComponent,
}

const edgeTypes: EdgeTypes = {
  swarmEdge: GraphEdgeComponent,
}

interface Props {
  agents: SwarmAgent[]
  graphEdges?: GraphEdge[]
  onNodeClick?: (agentId: number) => void
}

export default function SwarmGraph({ agents, graphEdges = [], onNodeClick }: Props) {
  const { nodes, edges } = useSwarmGraph(agents, graphEdges)
  const rfInstance = useRef<ReactFlowInstance | null>(null)
  const prevNodeCount = useRef(nodes.length)

  const handleNodeClick = useCallback((_: React.MouseEvent, node: { id: string }) => {
    onNodeClick?.(Number(node.id))
  }, [onNodeClick])

  // 当节点数量变化时自动 fitView
  useEffect(() => {
    if (nodes.length !== prevNodeCount.current && rfInstance.current) {
      prevNodeCount.current = nodes.length
      setTimeout(() => rfInstance.current?.fitView({ duration: 300 }), 100)
    }
  }, [nodes.length])

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <style>{BEAM_STYLES}</style>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        onNodeClick={handleNodeClick}
        onInit={(instance) => { rfInstance.current = instance }}
        fitView
        proOptions={{ hideAttribution: true }}
        nodesDraggable
        nodesConnectable={false}
      >
        <Background />
        <Controls showInteractive={false} />
        <MiniMap
          nodeColor={(node) => node.data?.role === 'human' ? '#1677ff' : '#722ed1'}
          style={{ width: 100, height: 70 }}
        />
      </ReactFlow>
    </div>
  )
}
