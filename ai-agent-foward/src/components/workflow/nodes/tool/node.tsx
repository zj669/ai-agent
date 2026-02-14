import type { NodeProps } from '@xyflow/react';
import { WorkflowBaseNode } from '../_base/node';
import type { ToolNodeData } from './types';

export function WorkflowToolNode(props: NodeProps<ToolNodeData>) {
  return <WorkflowBaseNode {...props} />;
}
