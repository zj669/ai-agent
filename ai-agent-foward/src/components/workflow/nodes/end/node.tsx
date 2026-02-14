import type { NodeProps } from '@xyflow/react';
import { WorkflowBaseNode } from '../_base/node';
import type { EndNodeData } from './types';

export function WorkflowEndNode(props: NodeProps<EndNodeData>) {
  return <WorkflowBaseNode {...props} />;
}
