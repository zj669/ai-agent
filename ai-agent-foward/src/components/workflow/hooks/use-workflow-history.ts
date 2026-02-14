import { useCallback } from 'react';
import { useWorkflowStore } from '../store';
import type { WorkflowOperationType, WorkflowSnapshot } from '../types';

export function useWorkflowHistory() {
  const nodes = useWorkflowStore((state) => state.nodes);
  const edges = useWorkflowStore((state) => state.edges);
  const undoStack = useWorkflowStore((state) => state.undoStack);
  const redoStack = useWorkflowStore((state) => state.redoStack);
  const pushSnapshot = useWorkflowStore((state) => state.pushSnapshot);
  const takeUndoSnapshot = useWorkflowStore((state) => state.takeUndoSnapshot);
  const takeRedoSnapshot = useWorkflowStore((state) => state.takeRedoSnapshot);
  const pushRedoSnapshot = useWorkflowStore((state) => state.pushRedoSnapshot);
  const pushUndoSnapshot = useWorkflowStore((state) => state.pushUndoSnapshot);
  const setGraph = useWorkflowStore((state) => state.setGraph);
  const setRestoring = useWorkflowStore((state) => state.setRestoring);
  const setLastOperation = useWorkflowStore((state) => state.setLastOperation);

  const snapshot = useCallback(
    (actionType?: WorkflowOperationType) => {
      pushSnapshot({ nodes: [...nodes], edges: [...edges], actionType, timestamp: Date.now() });
    },
    [nodes, edges, pushSnapshot]
  );

  const undo = useCallback(() => {
    const previous = takeUndoSnapshot();
    if (!previous) return;

    const current: WorkflowSnapshot = {
      nodes: [...nodes],
      edges: [...edges],
      actionType: 'HISTORY_UNDO',
      timestamp: Date.now()
    };

    setRestoring(true);
    pushRedoSnapshot(current);
    setGraph({ nodes: previous.nodes, edges: previous.edges });
    setLastOperation('HISTORY_UNDO');
    setRestoring(false);
  }, [edges, nodes, pushRedoSnapshot, setGraph, setLastOperation, setRestoring, takeUndoSnapshot]);

  const redo = useCallback(() => {
    const next = takeRedoSnapshot();
    if (!next) return;

    const current: WorkflowSnapshot = {
      nodes: [...nodes],
      edges: [...edges],
      actionType: 'HISTORY_REDO',
      timestamp: Date.now()
    };

    setRestoring(true);
    pushUndoSnapshot(current);
    setGraph({ nodes: next.nodes, edges: next.edges });
    setLastOperation('HISTORY_REDO');
    setRestoring(false);
  }, [edges, nodes, pushUndoSnapshot, setGraph, setLastOperation, setRestoring, takeRedoSnapshot]);

  return {
    snapshot,
    undo,
    redo,
    canUndo: undoStack.length > 0,
    canRedo: redoStack.length > 0
  };
}