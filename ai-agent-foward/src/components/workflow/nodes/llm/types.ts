import type { WorkflowNodeData } from '../../types';

export interface LlmNodeConfig {
  model: string;
  prompt: string;
}

export interface LlmNodeData extends WorkflowNodeData {
  nodeType: 'llm';
  config: LlmNodeConfig;
}
