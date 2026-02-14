import type { NodeProps } from '@xyflow/react';
import { WorkflowBaseNode } from '../_base/node';
import type { LlmNodeData } from './types';

export function WorkflowLlmNode(props: NodeProps<LlmNodeData>) {
  return <WorkflowBaseNode {...props} />;
}
