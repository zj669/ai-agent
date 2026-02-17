import type { Edge, Node } from '@xyflow/react';

export type WorkflowNodeType = 'START' | 'END' | 'LLM' | 'HTTP' | 'CONDITION' | 'TOOL';

export type WorkflowNormalizedNodeType = WorkflowNodeType | 'UNKNOWN';

export type WorkflowEdgeType = 'DEPENDENCY' | 'CONDITIONAL' | 'DEFAULT';

export const WORKFLOW_NODE_TYPE_WHITELIST: WorkflowNodeType[] = [
  'START',
  'END',
  'LLM',
  'HTTP',
  'CONDITION',
  'TOOL'
];

export interface WorkflowNodeData {
  nodeId: string;
  nodeName: string;
  nodeType: WorkflowNormalizedNodeType;
  userConfig: Record<string, any>;
  templateId?: string;
  inputSchema?: Array<Record<string, any>>;
  outputSchema?: Array<Record<string, any>>;
  rawNodeType?: string;
  hasUnknownType?: boolean;
}

export type WorkflowCanvasNode = Node<WorkflowNodeData>;

export type WorkflowCanvasEdge = Edge<{
  condition?: string;
  edgeType: WorkflowEdgeType;
}>;

export interface WorkflowGraphNodeDTO {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  inputSchema?: Array<Record<string, any>>;
  outputSchema?: Array<Record<string, any>>;
  userConfig?: Record<string, any>;
  position?: {
    x: number;
    y: number;
  };
  templateId?: string;
}

export interface WorkflowGraphEdgeDTO {
  edgeId: string;
  source: string;
  target: string;
  label?: string;
  condition?: string;
  edgeType?: WorkflowEdgeType;
}

export interface WorkflowGraphDTO {
  dagId: string;
  version?: string;
  description?: string;
  startNodeId?: string;
  nodes: WorkflowGraphNodeDTO[];
  edges: WorkflowGraphEdgeDTO[];
}

export interface WorkflowValidationIssue {
  key: string;
  message: string;
}
