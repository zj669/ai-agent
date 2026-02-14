import type { WorkflowNodeData } from '../../types';

export interface EndNodeConfig {
  title: string;
}

export interface EndNodeData extends WorkflowNodeData {
  nodeType: 'end';
  config: EndNodeConfig;
}
