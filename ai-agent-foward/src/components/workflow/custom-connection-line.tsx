import type { ConnectionLineComponentProps } from '@xyflow/react';

export function WorkflowCustomConnectionLine({ fromX, fromY, toX, toY }: ConnectionLineComponentProps) {
  return (
    <g>
      <path
        d={`M ${fromX},${fromY} L ${toX},${toY}`}
        fill="none"
        stroke="#5b7cfa"
        strokeWidth={2}
        strokeDasharray="4 4"
      />
    </g>
  );
}
