export interface WorkflowRunHistoryItem {
  id: string;
  status: 'pending' | 'running' | 'succeeded' | 'failed';
}

export type WorkflowRunHistory = WorkflowRunHistoryItem[];

export function WorkflowRunHistoryFeature() {
  return null;
}
