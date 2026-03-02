import { memo, useState, useCallback } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, Position, useReactFlow, type EdgeProps } from '@xyflow/react'

function CustomEdge({ id, sourceX, sourceY, targetX, targetY }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX: sourceX - 8,
    sourceY,
    sourcePosition: Position.Right,
    targetX: targetX + 8,
    targetY,
    targetPosition: Position.Left,
    curvature: 0.16,
  })

  const { setEdges } = useReactFlow()
  const [hovered, setHovered] = useState(false)

  const onDelete = useCallback(
    (e: React.MouseEvent) => {
      e.stopPropagation()
      e.preventDefault()
      setEdges((eds) => eds.filter((edge) => edge.id !== id))
    },
    [id, setEdges],
  )

  return (
    <>
      <BaseEdge
        id={id}
        path={edgePath}
        interactionWidth={20}
        style={{
          stroke: hovered ? '#f43f5e' : '#2970FF',
          strokeWidth: 2,
        }}
      />
      {/* 覆盖一层透明宽路径来捕获 hover，放在 BaseEdge 之后确保在最上层 */}
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={30}
        style={{ pointerEvents: 'stroke' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      />
      {hovered && (
        <EdgeLabelRenderer>
          <button
            type="button"
            className="nodrag nopan"
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
              width: 22,
              height: 22,
              borderRadius: '50%',
              background: '#f43f5e',
              color: '#fff',
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 12,
              lineHeight: 1,
              boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
              pointerEvents: 'all',
              zIndex: 10,
            }}
            title="删除连线"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            onClick={onDelete}
          >
            ✕
          </button>
        </EdgeLabelRenderer>
      )}
    </>
  )
}

export default memo(CustomEdge)
