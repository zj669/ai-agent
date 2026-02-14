import { BaseEdge, EdgeLabelRenderer, EdgeProps, getSmoothStepPath } from '@xyflow/react';
import { EdgeType } from '../types/workflow';

interface WorkflowEdgeData {
  condition?: string;
  edgeType?: EdgeType;
}

export function WorkflowEdge({
  id,
  sourceX,
  sourceY,
  sourcePosition,
  targetX,
  targetY,
  targetPosition,
  markerEnd,
  selected,
  label,
  data
}: EdgeProps) {
  const edgeData = (data || {}) as WorkflowEdgeData;
  const edgeLabel = typeof label === 'string' ? label : edgeData.condition || '';

  const baseStroke = '#c4b5fd';
  const highlightStroke = '#8b5cf6';

  const [edgePath, labelX, labelY] = getSmoothStepPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
    borderRadius: 20,
    offset: 20
  });

  return (
    <>
      <BaseEdge
        id={`${id}-glow`}
        path={edgePath}
        style={{
          stroke: selected ? highlightStroke : baseStroke,
          strokeOpacity: selected ? 0.35 : 0.18,
          strokeWidth: selected ? 8 : 6,
          strokeLinecap: 'round'
        }}
      />
      <BaseEdge
        id={id}
        path={edgePath}
        markerEnd={markerEnd}
        style={{
          stroke: selected ? highlightStroke : baseStroke,
          strokeWidth: selected ? 3 : 2.2,
          strokeLinecap: 'round',
          transition: 'all 160ms ease'
        }}
      />

      {edgeLabel && (
        <EdgeLabelRenderer>
          <div
            className="pointer-events-none absolute text-[11px] text-violet-700 px-2 py-1 rounded-lg border border-violet-200 bg-white/95 shadow-sm"
            style={{
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`
            }}
          >
            {edgeLabel}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  );
}

export const workflowEdgeTypes = {
  workflow: WorkflowEdge,
  default: WorkflowEdge,
  conditional: WorkflowEdge,
  smoothstep: WorkflowEdge
};
