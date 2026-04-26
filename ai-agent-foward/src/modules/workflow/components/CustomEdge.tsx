import { memo, useState, useCallback } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, Position, useReactFlow, type EdgeProps } from '@xyflow/react'
import { useEditorStore } from '../stores/useEditorStore'

function CustomEdge({ id, sourceX, sourceY, targetX, targetY }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition: Position.Right,
    targetX,
    targetY,
    targetPosition: Position.Left,
    curvature: 0.25,
  })

  const { setEdges } = useReactFlow()
  const [hovered, setHovered] = useState(false)

  const onDelete = useCallback(
    (e: React.MouseEvent) => {
      e.stopPropagation()
      e.preventDefault()
      setEdges((eds) => eds.filter((edge) => edge.id !== id))
      useEditorStore.getState().markDirty()
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
          stroke: hovered ? '#3b82f6' : '#cbd5e1',
          strokeWidth: hovered ? 3 : 2,
          transition: 'stroke 0.2s, stroke-width 0.2s',
        }}
      />
      {/* 覆盖一层透明宽路径来捕获 hover */}
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={30}
        style={{ pointerEvents: 'stroke' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      />
      
      {/* The EdgeLabelRenderer is outside the SVG coordinates, so we map the label via absolute positioning */}
      <EdgeLabelRenderer>
        <div
          className="absolute z-10 pointer-events-none nodrag nopan flex items-center justify-center transition-opacity duration-200"
          style={{
            transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
            opacity: hovered ? 1 : 0,
            pointerEvents: hovered ? 'all' : 'none',
          }}
          onMouseEnter={() => setHovered(true)}
          onMouseLeave={() => setHovered(false)}
        >
          <button
            type="button"
            className="flex items-center justify-center h-6 w-6 rounded-full bg-white border border-slate-200 shadow-md text-slate-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors"
            title="删除连线"
            onClick={onDelete}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </EdgeLabelRenderer>
    </>
  )
}

export default memo(CustomEdge)
