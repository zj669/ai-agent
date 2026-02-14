import type { WorkflowNodeData } from '../../types';

export interface StartNodeConfig {
  title: string;
}

export interface StartNodeData extends WorkflowNodeData {
  nodeType: 'start';
  config: StartNodeConfig;
}
