import { BaseEdge, getStraightPath, type EdgeProps } from '@xyflow/react'

interface EdgeData {
  count?: number
  [key: string]: unknown
}

export default function GraphEdge(props: EdgeProps) {
  const { sourceX, sourceY, targetX, targetY, data } = props
  const edgeData = data as EdgeData | undefined

  const [edgePath, labelX, labelY] = getStraightPath({
    sourceX, sourceY, targetX, targetY,
  })

  return (
    <>
      <BaseEdge
        {...props}
        path={edgePath}
        style={{ stroke: '#b1b1b7', strokeWidth: 2 }}
      />
      {edgeData?.count != null && edgeData.count > 0 && (
        <foreignObject
          x={labelX - 12} y={labelY - 10}
          width={24} height={20}
          style={{ overflow: 'visible' }}
        >
          <div style={{
            background: '#722ed1', color: '#fff',
            borderRadius: 10, fontSize: 10, fontWeight: 600,
            width: 24, height: 20, display: 'flex',
            alignItems: 'center', justifyContent: 'center',
          }}>
            {edgeData.count}
          </div>
        </foreignObject>
      )}
    </>
  )
}
