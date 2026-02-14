import type { StateCreator } from 'zustand';
import type { WorkflowStore } from '../index';

interface WorkflowPanelVisibility {
  config: boolean;
  debug: boolean;
  history: boolean;
  execution: boolean;
  logs: boolean;
  nodeLibrary: boolean;
  env: boolean;
  workflowConfig: boolean;
}

export interface PanelSlice {
  selectedNodeId: string | null;
  panelVisibility: WorkflowPanelVisibility;
  useLargeNodes: boolean;
  activePanel: keyof WorkflowPanelVisibility;
  setSelectedNodeId: (nodeId: string | null) => void;
  setPanelVisible: (panel: keyof WorkflowPanelVisibility, visible: boolean) => void;
  setUseLargeNodes: (enabled: boolean) => void;
  setActivePanel: (panel: keyof WorkflowPanelVisibility) => void;
  resetPanelState: () => void;
}

const defaultPanelVisibility: WorkflowPanelVisibility = {
  config: true,
  debug: false,
  history: false,
  execution: false,
  logs: false,
  nodeLibrary: true,
  env: false,
  workflowConfig: true
};

export const createPanelSlice: StateCreator<WorkflowStore, [], [], PanelSlice> = (set) => ({
  selectedNodeId: null,
  panelVisibility: { ...defaultPanelVisibility },
  useLargeNodes: true,
  activePanel: 'config',
  setSelectedNodeId: (selectedNodeId) => set({ selectedNodeId }),
  setPanelVisible: (panel, visible) =>
    set((state) => ({
      panelVisibility: {
        ...state.panelVisibility,
        [panel]: visible
      }
    })),
  setUseLargeNodes: (useLargeNodes) => set({ useLargeNodes }),
  setActivePanel: (activePanel) =>
    set((state) => ({
      activePanel,
      panelVisibility: {
        ...state.panelVisibility,
        [activePanel]: true
      }
    })),
  resetPanelState: () =>
    set({
      selectedNodeId: null,
      panelVisibility: { ...defaultPanelVisibility },
      activePanel: 'config'
    })
});
