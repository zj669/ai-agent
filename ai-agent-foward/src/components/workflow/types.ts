import type { Edge, Node, Viewport } from '@xyflow/react';

export type WorkflowInteractionMode = 'select' | 'pan';
export type WorkflowLayoutDirection = 'TB' | 'LR';
export type WorkflowOperationType =
  | 'SET_GRAPH'
  | 'NODE_ADD'
  | 'NODE_UPDATE'
  | 'NODE_DELETE'
  | 'EDGE_CONNECT'
  | 'EDGE_UPDATE'
  | 'EDGE_DELETE'
  | 'LAYOUT_APPLY'
  | 'HISTORY_UNDO'
  | 'HISTORY_REDO'
  | 'DRAFT_IMPORT'
  | 'DRAFT_EXPORT'
  | 'DRAFT_SYNC'
  | 'RUNTIME_STATUS_UPDATE';

export interface WorkflowNodeData {
  label: string;
  nodeType: string;
  status?: string;
  config?: {
    properties?: Record<string, unknown>;
    [key: string]: unknown;
  };
  inputs?: Record<string, unknown>;
  outputs?: Record<string, string>;
}

export type WorkflowCanvasNode = Node<WorkflowNodeData>;
export type WorkflowCanvasEdge = Edge;

export interface WorkflowSnapshot {
  nodes: WorkflowCanvasNode[];
  edges: WorkflowCanvasEdge[];
  actionType?: WorkflowOperationType;
  timestamp?: number;
}

export interface WorkflowViewportState {
  viewport: Viewport;
  zoom: number;
}

export interface WorkflowGraphPayload {
  nodes: WorkflowCanvasNode[];
  edges: WorkflowCanvasEdge[];
}

export interface WorkflowDraftPayload {
  graphJson: string;
  timestamp?: number;
  version?: number;
}

export interface WorkflowValidationResult {
  valid: boolean;
  reason?: string;
}

export interface WorkflowExecutionState {
  isExecuting: boolean;
  executionId: string | null;
}

export interface WorkflowDraftSyncResult {
  ok: boolean;
  reason?: string;
  timestamp: number;
  version: number;
}
