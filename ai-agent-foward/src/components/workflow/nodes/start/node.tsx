import type { NodeProps } from '@xyflow/react';
import { WorkflowBaseNode } from '../_base/node';
import type { StartNodeData } from './types';

export function WorkflowStartNode(props: NodeProps<StartNodeData>) {
  return <WorkflowBaseNode {...props} />;
}
