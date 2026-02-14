import type { WorkflowCanvasEdge, WorkflowCanvasNode, WorkflowDraftPayload, WorkflowGraphPayload } from '../types';

interface BasicGraphPayload {
  nodes: WorkflowCanvasNode[];
  edges: WorkflowCanvasEdge[];
  timestamp?: number;
  version?: number;
}

export function toGraphJson(
  nodes: WorkflowCanvasNode[],
  edges: WorkflowCanvasEdge[],
  meta?: { timestamp?: number; version?: number }
): string {
  const payload: BasicGraphPayload = {
    nodes,
    edges,
    timestamp: meta?.timestamp,
    version: meta?.version
  };
  return JSON.stringify(payload);
}

export function fromGraphJson(graphJson: string): (WorkflowGraphPayload & Partial<WorkflowDraftPayload>) | null {
  try {
    const parsed = JSON.parse(graphJson) as BasicGraphPayload;
    if (!parsed || !Array.isArray(parsed.nodes) || !Array.isArray(parsed.edges)) {
      return null;
    }

    return {
      nodes: parsed.nodes,
      edges: parsed.edges,
      timestamp: parsed.timestamp,
      version: parsed.version
    };
  } catch {
    return null;
  }
}
