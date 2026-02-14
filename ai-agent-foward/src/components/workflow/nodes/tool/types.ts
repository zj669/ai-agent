import type { WorkflowNodeData } from '../../types';

export interface ToolNodeConfig {
  toolName: string;
  inputTemplate: string;
}

export interface ToolNodeData extends WorkflowNodeData {
  nodeType: 'tool';
  config: ToolNodeConfig;
}
