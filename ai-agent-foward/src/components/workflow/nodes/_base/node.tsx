import type { NodeProps } from '@xyflow/react';
import type { WorkflowNodeData } from '../../types';
import { WorkflowNodeHandle } from './node-handle';

export function WorkflowBaseNode({ data, selected }: NodeProps<WorkflowNodeData>) {
  return (
    <div
      className="workflow-base-node"
      style={{
        borderColor: selected ? '#5b7cfa' : '#dbe1ea'
      }}
    >
      <WorkflowNodeHandle id="in" direction="in" />
      <div style={{ fontSize: 13, fontWeight: 600 }}>{data.label}</div>
      <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>{data.nodeType}</div>
      <WorkflowNodeHandle id="out" direction="out" />
    </div>
  );
}
