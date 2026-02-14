import type { Viewport } from '@xyflow/react';
import type { WorkflowInteractionMode, WorkflowLayoutDirection } from './types';

export const WORKFLOW_NODE_WIDTH = 220;
export const WORKFLOW_NODE_HEIGHT = 104;
export const WORKFLOW_MAX_HISTORY = 50;

export const DEFAULT_WORKFLOW_INTERACTION_MODE: WorkflowInteractionMode = 'select';

export const DEFAULT_WORKFLOW_LAYOUT_DIRECTION: WorkflowLayoutDirection = 'TB';

export const DEFAULT_WORKFLOW_VIEWPORT: Viewport = {
  x: 0,
  y: 0,
  zoom: 1
};

export const WORKFLOW_DEFAULT_RUNNING_NODE_STATUSES: Record<string, string> = {};

export const WORKFLOW_GRID_SNAP: [number, number] = [16, 16];

export const WORKFLOW_DEFAULT_MAXIMIZED = false;
export const WORKFLOW_DEFAULT_READONLY = false;
