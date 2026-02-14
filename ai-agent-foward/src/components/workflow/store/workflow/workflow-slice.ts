import type { StateCreator } from 'zustand';
import { WORKFLOW_DEFAULT_READONLY, WORKFLOW_DEFAULT_RUNNING_NODE_STATUSES } from '../../constants';
import type {
  WorkflowCanvasEdge,
  WorkflowCanvasNode,
  WorkflowExecutionState,
  WorkflowGraphPayload,
  WorkflowOperationType
} from '../../types';
import type { WorkflowStore } from '../index';

export interface WorkflowSlice {
  nodes: WorkflowCanvasNode[];
  edges: WorkflowCanvasEdge[];
  isReadonly: boolean;
  isExecuting: boolean;
  executionId: string | null;
  executionLogs: string[];
  runningNodeStatusMap: Record<string, string>;
  selectedNodeId: string | null;
  draftVersion: number;
  lastDraftSyncAt: number | null;
  lastOperation: WorkflowOperationType | null;
  setNodes: (nodes: WorkflowCanvasNode[]) => void;
  setEdges: (edges: WorkflowCanvasEdge[]) => void;
  setGraph: (payload: WorkflowGraphPayload) => void;
  addNode: (node: WorkflowCanvasNode) => void;
  updateNode: (nodeId: string, updater: (node: WorkflowCanvasNode) => WorkflowCanvasNode) => void;
  deleteNode: (nodeId: string) => void;
  addEdge: (edge: WorkflowCanvasEdge) => void;
  updateEdge: (edgeId: string, updater: (edge: WorkflowCanvasEdge) => WorkflowCanvasEdge) => void;
  deleteEdge: (edgeId: string) => void;
  upsertNode: (node: WorkflowCanvasNode) => void;
  removeNode: (nodeId: string) => void;
  setReadonly: (readonly: boolean) => void;
  setExecutionState: (state: WorkflowExecutionState) => void;
  setRunningNodeStatus: (nodeId: string, status: string) => void;
  setRunningNodeStatusMap: (map: Record<string, string>) => void;
  clearRunningNodeStatusMap: () => void;
  setSelectedNode: (nodeId: string | null) => void;
  setDraftSyncMeta: (meta: { version: number; timestamp: number | null }) => void;
  appendExecutionLog: (log: string) => void;
  clearExecutionLogs: () => void;
  setLastOperation: (operation: WorkflowOperationType | null) => void;
}

export const createWorkflowSlice: StateCreator<WorkflowStore, [], [], WorkflowSlice> = (set) => ({
  nodes: [],
  edges: [],
  isReadonly: WORKFLOW_DEFAULT_READONLY,
  isExecuting: false,
  executionId: null,
  executionLogs: [],
  runningNodeStatusMap: { ...WORKFLOW_DEFAULT_RUNNING_NODE_STATUSES },
  selectedNodeId: null,
  draftVersion: 1,
  lastDraftSyncAt: null,
  lastOperation: null,
  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),
  setGraph: ({ nodes, edges }) => set({ nodes, edges, lastOperation: 'SET_GRAPH' }),
  addNode: (node) =>
    set((state) => ({
      nodes: [...state.nodes, node],
      selectedNodeId: node.id,
      lastOperation: 'NODE_ADD'
    })),
  updateNode: (nodeId, updater) =>
    set((state) => ({
      nodes: state.nodes.map((node) => (node.id === nodeId ? updater(node) : node)),
      lastOperation: 'NODE_UPDATE'
    })),
  deleteNode: (nodeId) =>
    set((state) => ({
      nodes: state.nodes.filter((node) => node.id !== nodeId),
      edges: state.edges.filter((edge) => edge.source !== nodeId && edge.target !== nodeId),
      selectedNodeId: state.selectedNodeId === nodeId ? null : state.selectedNodeId,
      lastOperation: 'NODE_DELETE'
    })),
  addEdge: (edge) =>
    set((state) => ({
      edges: [...state.edges, edge],
      lastOperation: 'EDGE_CONNECT'
    })),
  updateEdge: (edgeId, updater) =>
    set((state) => ({
      edges: state.edges.map((edge) => (edge.id === edgeId ? updater(edge) : edge)),
      lastOperation: 'EDGE_UPDATE'
    })),
  deleteEdge: (edgeId) =>
    set((state) => ({
      edges: state.edges.filter((edge) => edge.id !== edgeId),
      lastOperation: 'EDGE_DELETE'
    })),
  upsertNode: (nextNode) =>
    set((state) => {
      const exists = state.nodes.some((node) => node.id === nextNode.id);
      return {
        nodes: exists ? state.nodes.map((node) => (node.id === nextNode.id ? nextNode : node)) : [...state.nodes, nextNode],
        selectedNodeId: nextNode.id,
        lastOperation: exists ? 'NODE_UPDATE' : 'NODE_ADD'
      };
    }),
  removeNode: (nodeId) =>
    set((state) => ({
      nodes: state.nodes.filter((node) => node.id !== nodeId),
      edges: state.edges.filter((edge) => edge.source !== nodeId && edge.target !== nodeId),
      selectedNodeId: state.selectedNodeId === nodeId ? null : state.selectedNodeId,
      lastOperation: 'NODE_DELETE'
    })),
  setReadonly: (isReadonly) => set({ isReadonly }),
  setExecutionState: ({ isExecuting, executionId }) =>
    set({
      isExecuting,
      executionId,
      lastOperation: 'RUNTIME_STATUS_UPDATE'
    }),
  setRunningNodeStatus: (nodeId, status) =>
    set((state) => ({
      runningNodeStatusMap: {
        ...state.runningNodeStatusMap,
        [nodeId]: status
      }
    })),
  setRunningNodeStatusMap: (runningNodeStatusMap) => set({ runningNodeStatusMap }),
  clearRunningNodeStatusMap: () => set({ runningNodeStatusMap: {} }),
  setSelectedNode: (selectedNodeId) => set({ selectedNodeId }),
  setDraftSyncMeta: ({ version, timestamp }) =>
    set({
      draftVersion: version,
      lastDraftSyncAt: timestamp,
      lastOperation: 'DRAFT_SYNC'
    }),
  appendExecutionLog: (log) =>
    set((state) => ({
      executionLogs: [...state.executionLogs, log]
    })),
  clearExecutionLogs: () => set({ executionLogs: [] }),
  setLastOperation: (lastOperation) => set({ lastOperation })
});
