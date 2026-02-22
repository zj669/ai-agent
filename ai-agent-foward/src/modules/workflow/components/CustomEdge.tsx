import { memo } from 'react'
import { BaseEdge, getBezierPath, Position, type EdgeProps } from '@xyflow/react'

function CustomEdge({ id, sourceX, sourceY, targetX, targetY }: EdgeProps) {
  const [edgePath] = getBezierPath({
    sourceX: sourceX - 8,
    sourceY,
    sourcePosition: Position.Right,
    targetX: targetX + 8,
    targetY,
    targetPosition: Position.Left,
    curvature: 0.16,
  })

  return (
    <BaseEdge
      id={id}
      path={edgePath}
      style={{ stroke: '#2970FF', strokeWidth: 2 }}
    />
  )
}

export default memo(CustomEdge)
