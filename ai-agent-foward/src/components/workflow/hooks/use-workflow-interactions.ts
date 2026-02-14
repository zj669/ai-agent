import { useCallback } from 'react';
import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  type Connection,
  type EdgeChange,
  type NodeChange
} from '@xyflow/react';
import { useWorkflowStore } from '../store';
import type { WorkflowCanvasEdge, WorkflowCanvasNode, WorkflowViewportState } from '../types';
import { validateConnection } from '../utils/validation';
import { useWorkflowHistory } from './use-workflow-history';

export function useWorkflowInteractions() {
  const nodes = useWorkflowStore((state) => state.nodes);
  const edges = useWorkflowStore((state) => state.edges);
  const isReadonly = useWorkflowStore((state) => state.isReadonly);
  const isExecuting = useWorkflowStore((state) => state.isExecuting);
  const setNodes = useWorkflowStore((state) => state.setNodes);
  const setEdges = useWorkflowStore((state) => state.setEdges);
  const setGraph = useWorkflowStore((state) => state.setGraph);
  const setLastOperation = useWorkflowStore((state) => state.setLastOperation);
  const interactionMode = useWorkflowStore((state) => state.interactionMode);
  const setInteractionMode = useWorkflowStore((state) => state.setInteractionMode);
  const viewport = useWorkflowStore((state) => state.viewport);
  const zoom = useWorkflowStore((state) => state.zoom);
  const setViewportState = useWorkflowStore((state) => state.setViewportState);
  const { snapshot } = useWorkflowHistory();

  const onNodesChange = useCallback(
    (changes: NodeChange<WorkflowCanvasNode>[]) => {
      if (isReadonly || isExecuting) return;

      const shouldSnapshot = changes.some((change) => {
        if (change.type === 'remove') return true;
        if (change.type === 'position') {
          return (change as { dragging?: boolean }).dragging === false;
        }
        return false;
      });

      if (shouldSnapshot) {
        const hasRemove = changes.some((change) => change.type === 'remove');
        snapshot(hasRemove ? 'NODE_DELETE' : 'NODE_UPDATE');
      }

      setNodes(applyNodeChanges(changes, nodes));
    },
    [isExecuting, isReadonly, nodes, setNodes, snapshot]
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange<WorkflowCanvasEdge>[]) => {
      if (isReadonly || isExecuting) return;
      const shouldSnapshot = changes.some((change) => change.type === 'remove');
      if (shouldSnapshot) {
        snapshot('EDGE_DELETE');
      }

      setEdges(applyEdgeChanges(changes, edges));
    },
    [edges, isExecuting, isReadonly, setEdges, snapshot]
  );

  const onConnect = useCallback(
    (connection: Connection) => {
      if (isReadonly || isExecuting) return;
      const validationResult = validateConnection(connection);
      if (!validationResult.valid || !connection.source || !connection.target) {
        return;
      }

      snapshot('EDGE_CONNECT');
      setEdges(
        addEdge(
          {
            ...connection,
            id: `edge_${Date.now()}`
          },
          edges
        )
      );
      setLastOperation('EDGE_CONNECT');
    },
    [edges, isExecuting, isReadonly, setEdges, setLastOperation, snapshot]
  );

  const updateViewport = useCallback(
    (state: WorkflowViewportState) => {
      setViewportState(state);
    },
    [setViewportState]
  );

  const clearGraph = useCallback(() => {
    if (isReadonly || isExecuting) return;
    snapshot('SET_GRAPH');
    setGraph({ nodes: [], edges: [] });
  }, [isExecuting, isReadonly, setGraph, snapshot]);

  return {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,
    clearGraph,
    interactionMode,
    setInteractionMode,
    viewport,
    zoom,
    setViewportState: updateViewport
  };
}
