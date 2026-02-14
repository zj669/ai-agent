import { useCallback } from 'react';
import { useWorkflowStore } from '../store';
import type { WorkflowDraftSyncResult, WorkflowGraphPayload } from '../types';
import { fromGraphJson, toGraphJson } from '../utils/graph-transformer';
import { useWorkflowHistory } from './use-workflow-history';

export function useNodesSyncDraft() {
  const nodes = useWorkflowStore((state) => state.nodes);
  const edges = useWorkflowStore((state) => state.edges);
  const draftVersion = useWorkflowStore((state) => state.draftVersion);
  const setGraph = useWorkflowStore((state) => state.setGraph);
  const setLastOperation = useWorkflowStore((state) => state.setLastOperation);
  const clearHistory = useWorkflowStore((state) => state.clearHistory);
  const setDraftSyncMeta = useWorkflowStore((state) => state.setDraftSyncMeta);
  const { snapshot } = useWorkflowHistory();

  const exportDraft = useCallback(() => {
    const timestamp = Date.now();
    const graphJson = toGraphJson(nodes, edges, { timestamp, version: draftVersion });
    setLastOperation('DRAFT_EXPORT');
    return graphJson;
  }, [draftVersion, edges, nodes, setLastOperation]);

  const importDraft = useCallback(
    (graphJson: string): WorkflowGraphPayload | null => {
      const payload = fromGraphJson(graphJson);
      if (!payload) {
        return null;
      }

      const nextVersion = payload.version ?? draftVersion + 1;
      const nextTimestamp = payload.timestamp ?? Date.now();

      snapshot('DRAFT_IMPORT');
      setGraph({ nodes: payload.nodes, edges: payload.edges });
      setDraftSyncMeta({ version: nextVersion, timestamp: nextTimestamp });
      setLastOperation('DRAFT_IMPORT');

      return { nodes: payload.nodes, edges: payload.edges };
    },
    [draftVersion, setDraftSyncMeta, setGraph, setLastOperation, snapshot]
  );

  const loadGraph = useCallback(
    (payload: WorkflowGraphPayload) => {
      setGraph(payload);
      clearHistory();
      setDraftSyncMeta({ version: draftVersion + 1, timestamp: Date.now() });
      setLastOperation('SET_GRAPH');
    },
    [clearHistory, draftVersion, setDraftSyncMeta, setGraph, setLastOperation]
  );

  const syncDraft = useCallback((): WorkflowDraftSyncResult => {
    const timestamp = Date.now();
    const version = draftVersion + 1;
    const exported = toGraphJson(nodes, edges, { timestamp, version });
    const imported = importDraft(exported);

    if (!imported) {
      return {
        ok: false,
        reason: 'invalid draft payload',
        timestamp,
        version
      };
    }

    setDraftSyncMeta({ version, timestamp });
    setLastOperation('DRAFT_SYNC');

    return {
      ok: true,
      timestamp,
      version
    };
  }, [draftVersion, edges, importDraft, nodes, setDraftSyncMeta, setLastOperation]);

  return {
    exportDraft,
    importDraft,
    loadGraph,
    syncDraft
  };
}
