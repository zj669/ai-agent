import type { WorkflowNodeData } from '../../types';

export type IfElseMode = 'expression' | 'llm';

export interface IfElseNodeConfig {
  mode: IfElseMode;
  expression: string;
}

export interface IfElseNodeData extends WorkflowNodeData {
  nodeType: 'if-else';
  config: IfElseNodeConfig;
}
