import type { NodeProps } from '@xyflow/react';
import { WorkflowBaseNode } from '../_base/node';
import type { IfElseNodeData } from './types';

export function WorkflowIfElseNode(props: NodeProps<IfElseNodeData>) {
  return <WorkflowBaseNode {...props} />;
}
