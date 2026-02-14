import { useCallback } from 'react';
import { addEdge, type Connection } from '@xyflow/react';
import { useWorkflowStore } from '../store';
import { validateConnection } from '../utils/validation';
import { useWorkflowHistory } from './use-workflow-history';

export function useEdgesInteractions() {
  const edges = useWorkflowStore((state) => state.edges);
  const isReadonly = useWorkflowStore((state) => state.isReadonly);
  const isExecuting = useWorkflowStore((state) => state.isExecuting);
  const setEdges = useWorkflowStore((state) => state.setEdges);
  const updateEdge = useWorkflowStore((state) => state.updateEdge);
  const deleteEdgeById = useWorkflowStore((state) => state.deleteEdge);
  const setLastOperation = useWorkflowStore((state) => state.setLastOperation);
  const { snapshot } = useWorkflowHistory();

  const isValidConnection = useCallback(
    (connection: Connection) => {
      const result = validateConnection(connection);
      if (!result.valid) return false;
      return !edges.some((edge) => edge.source === connection.source && edge.target === connection.target);
    },
    [edges]
  );

  const createEdge = useCallback(
    (connection: Connection) => {
      if (isReadonly || isExecuting) return;
      if (!isValidConnection(connection)) return;

      snapshot('EDGE_CONNECT');
      const nextEdges = addEdge(
        {
          ...connection,
          id: `edge_${Date.now()}`
        },
        edges
      );
      setEdges(nextEdges);
      setLastOperation('EDGE_CONNECT');
    },
    [edges, isExecuting, isReadonly, isValidConnection, setEdges, setLastOperation, snapshot]
  );

  const deleteEdge = useCallback(
    (edgeId: string) => {
      if (isReadonly || isExecuting) return;
      snapshot('EDGE_DELETE');
      deleteEdgeById(edgeId);
    },
    [deleteEdgeById, isExecuting, isReadonly, snapshot]
  );

  const updateEdgeCondition = useCallback(
    (edgeId: string, condition?: string) => {
      if (isReadonly || isExecuting) return;
      snapshot('EDGE_UPDATE');
      updateEdge(edgeId, (edge) => ({
        ...edge,
        data: {
          ...(edge.data as Record<string, unknown>),
          condition
        }
      }));
      setLastOperation('EDGE_UPDATE');
    },
    [isExecuting, isReadonly, setLastOperation, snapshot, updateEdge]
  );

  return {
    createEdge,
    deleteEdge,
    updateEdgeCondition,
    isValidConnection
  };
}
