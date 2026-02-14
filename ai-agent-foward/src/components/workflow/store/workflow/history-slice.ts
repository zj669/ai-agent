import type { StateCreator } from 'zustand';
import { WORKFLOW_MAX_HISTORY } from '../../constants';
import type { WorkflowOperationType, WorkflowSnapshot } from '../../types';
import type { WorkflowStore } from '../index';

export interface HistorySlice {
  undoStack: WorkflowSnapshot[];
  redoStack: WorkflowSnapshot[];
  isRestoring: boolean;
  maxHistorySize: number;
  historyCursor: number;
  lastHistoryAction: WorkflowOperationType | null;
  pushSnapshot: (snapshot: WorkflowSnapshot) => void;
  takeUndoSnapshot: () => WorkflowSnapshot | null;
  takeRedoSnapshot: () => WorkflowSnapshot | null;
  pushRedoSnapshot: (snapshot: WorkflowSnapshot) => void;
  pushUndoSnapshot: (snapshot: WorkflowSnapshot) => void;
  setRestoring: (restoring: boolean) => void;
  clearHistory: () => void;
}

export const createHistorySlice: StateCreator<WorkflowStore, [], [], HistorySlice> = (set, get) => ({
  undoStack: [],
  redoStack: [],
  isRestoring: false,
  maxHistorySize: WORKFLOW_MAX_HISTORY,
  historyCursor: -1,
  lastHistoryAction: null,
  pushSnapshot: (snapshot) =>
    set((state) => {
      if (state.isRestoring) {
        return state;
      }

      const undoStack = [...state.undoStack, snapshot].slice(-state.maxHistorySize);
      return {
        undoStack,
        redoStack: [],
        historyCursor: undoStack.length - 1,
        lastHistoryAction: snapshot.actionType ?? null
      };
    }),
  takeUndoSnapshot: () => {
    const { undoStack } = get();
    if (undoStack.length === 0) return null;
    const snapshot = undoStack[undoStack.length - 1];
    set({ undoStack: undoStack.slice(0, -1), historyCursor: undoStack.length - 2 });
    return snapshot;
  },
  takeRedoSnapshot: () => {
    const { redoStack, undoStack } = get();
    if (redoStack.length === 0) return null;
    const snapshot = redoStack[redoStack.length - 1];
    set({ redoStack: redoStack.slice(0, -1), historyCursor: undoStack.length });
    return snapshot;
  },
  pushRedoSnapshot: (snapshot) =>
    set((state) => ({
      redoStack: [...state.redoStack, snapshot].slice(-state.maxHistorySize)
    })),
  pushUndoSnapshot: (snapshot) =>
    set((state) => ({
      undoStack: [...state.undoStack, snapshot].slice(-state.maxHistorySize),
      historyCursor: Math.min(state.undoStack.length, state.maxHistorySize - 1)
    })),
  setRestoring: (isRestoring) => set({ isRestoring }),
  clearHistory: () =>
    set({
      undoStack: [],
      redoStack: [],
      isRestoring: false,
      historyCursor: -1,
      lastHistoryAction: null
    })
});
