import type { StateCreator } from 'zustand';
import {
  DEFAULT_WORKFLOW_INTERACTION_MODE,
  DEFAULT_WORKFLOW_LAYOUT_DIRECTION,
  DEFAULT_WORKFLOW_VIEWPORT,
  WORKFLOW_DEFAULT_MAXIMIZED
} from '../../constants';
import type {
  WorkflowInteractionMode,
  WorkflowLayoutDirection,
  WorkflowOperationType,
  WorkflowViewportState
} from '../../types';
import type { WorkflowStore } from '../index';

export interface LayoutSlice extends WorkflowViewportState {
  layoutDirection: WorkflowLayoutDirection;
  interactionMode: WorkflowInteractionMode;
  isCanvasDragActive: boolean;
  fitViewRequested: boolean;
  isMaximized: boolean;
  setViewport: (viewport: WorkflowViewportState['viewport']) => void;
  setZoom: (zoom: number) => void;
  setViewportState: (state: WorkflowViewportState) => void;
  setLayoutDirection: (direction: WorkflowLayoutDirection) => void;
  setInteractionMode: (mode: WorkflowInteractionMode) => void;
  setCanvasDragActive: (active: boolean) => void;
  setMaximized: (maximized: boolean) => void;
  toggleMaximized: () => void;
  requestFitView: () => void;
  consumeFitViewRequest: () => void;
  markLayoutOperation: (operation?: WorkflowOperationType) => void;
}

export const createLayoutSlice: StateCreator<WorkflowStore, [], [], LayoutSlice> = (set) => ({
  viewport: DEFAULT_WORKFLOW_VIEWPORT,
  zoom: DEFAULT_WORKFLOW_VIEWPORT.zoom,
  interactionMode: DEFAULT_WORKFLOW_INTERACTION_MODE,
  layoutDirection: DEFAULT_WORKFLOW_LAYOUT_DIRECTION,
  isCanvasDragActive: false,
  fitViewRequested: false,
  isMaximized: WORKFLOW_DEFAULT_MAXIMIZED,
  setViewport: (viewport) =>
    set({
      viewport,
      zoom: viewport.zoom
    }),
  setZoom: (zoom) =>
    set((state) => ({
      zoom,
      viewport: {
        ...state.viewport,
        zoom
      }
    })),
  setViewportState: ({ viewport, zoom }) =>
    set({
      viewport,
      zoom
    }),
  setLayoutDirection: (layoutDirection) => set({ layoutDirection }),
  setInteractionMode: (interactionMode) => set({ interactionMode }),
  setCanvasDragActive: (isCanvasDragActive) => set({ isCanvasDragActive }),
  setMaximized: (isMaximized) => set({ isMaximized }),
  toggleMaximized: () => set((state) => ({ isMaximized: !state.isMaximized })),
  requestFitView: () => set({ fitViewRequested: true, lastOperation: 'LAYOUT_APPLY' }),
  consumeFitViewRequest: () => set({ fitViewRequested: false }),
  markLayoutOperation: (operation = 'LAYOUT_APPLY') => set({ lastOperation: operation })
});