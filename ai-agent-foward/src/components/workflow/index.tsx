import type { ReactNode } from 'react';
import './style.css';

export interface WorkflowModuleRootProps {
  children?: ReactNode;
  className?: string;
}

export function WorkflowModuleRoot({ children, className }: WorkflowModuleRootProps) {
  const rootClassName = className ? `workflow-module-root ${className}` : 'workflow-module-root';
  return <div className={rootClassName}>{children}</div>;
}

export { WorkflowCustomEdge } from './custom-edge';
export { WorkflowCustomConnectionLine } from './custom-connection-line';
export { useWorkflowStore } from './store';

export { WorkflowPanel } from './panel';
export { WorkflowHeader } from './operator/header';
export { WorkflowControl } from './operator/control';
export { WorkflowZoom } from './operator/zoom';
export { WorkflowDebugAndPreviewFeature } from './features/debug-and-preview';
export { WorkflowRunHistoryFeature } from './features/run-history';

export { WorkflowStartNode } from './nodes/start/node';
export { WorkflowStartNodePanel } from './nodes/start/panel';
export { defaultStartNodeData } from './nodes/start/default';
export { startNodeSchema } from './nodes/start/schema';

export { WorkflowEndNode } from './nodes/end/node';
export { WorkflowEndNodePanel } from './nodes/end/panel';
export { defaultEndNodeData } from './nodes/end/default';
export { endNodeSchema } from './nodes/end/schema';

export { WorkflowLlmNode } from './nodes/llm/node';
export { WorkflowLlmNodePanel } from './nodes/llm/panel';
export { defaultLlmNodeData } from './nodes/llm/default';
export { llmNodeSchema } from './nodes/llm/schema';

export { WorkflowToolNode } from './nodes/tool/node';
export { WorkflowToolNodePanel } from './nodes/tool/panel';
export { defaultToolNodeData } from './nodes/tool/default';
export { toolNodeSchema } from './nodes/tool/schema';

export { WorkflowIfElseNode } from './nodes/if-else/node';
export { WorkflowIfElseNodePanel } from './nodes/if-else/panel';
export { defaultIfElseNodeData } from './nodes/if-else/default';
export { ifElseNodeSchema } from './nodes/if-else/schema';

export * from './types';
export * from './constants';
