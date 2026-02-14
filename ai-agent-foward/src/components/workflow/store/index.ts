import { create } from 'zustand';
import { createHistorySlice, HistorySlice } from './workflow/history-slice';
import { createLayoutSlice, LayoutSlice } from './workflow/layout-slice';
import { createPanelSlice, PanelSlice } from './workflow/panel-slice';
import { createWorkflowSlice, WorkflowSlice } from './workflow/workflow-slice';

export type WorkflowStore = WorkflowSlice & LayoutSlice & PanelSlice & HistorySlice;

export const useWorkflowStore = create<WorkflowStore>()((...args) => ({
  ...createWorkflowSlice(...args),
  ...createLayoutSlice(...args),
  ...createPanelSlice(...args),
  ...createHistorySlice(...args)
}));
