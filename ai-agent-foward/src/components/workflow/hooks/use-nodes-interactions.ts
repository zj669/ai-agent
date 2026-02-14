import { useCallback } from 'react';
import type { XYPosition } from '@xyflow/react';
import { useWorkflowStore } from '../store';
import type { WorkflowCanvasNode, WorkflowNodeData } from '../types';
import { useWorkflowHistory } from './use-workflow-history';

function getDefaultNodeLabel(nodeType: string): string {
  switch (nodeType) {
    case 'START':
      return '开始';
    case 'END':
      return '结束';
    case 'LLM':
      return 'LLM 节点';
    case 'HTTP':
      return 'HTTP 请求';
    case 'CONDITION':
      return '条件分支';
    case 'TOOL':
      return '工具调用';
    default:
      return nodeType;
  }
}

export function useNodesInteractions() {
  const nodes = useWorkflowStore((state) => state.nodes);
  const isReadonly = useWorkflowStore((state) => state.isReadonly);
  const isExecuting = useWorkflowStore((state) => state.isExecuting);
  const addNodeToStore = useWorkflowStore((state) => state.addNode);
  const updateNode = useWorkflowStore((state) => state.updateNode);
  const deleteNode = useWorkflowStore((state) => state.deleteNode);
  const { snapshot } = useWorkflowHistory();

  const addNode = useCallback(
    (nodeType: string, position: XYPosition) => {
      if (isReadonly || isExecuting) return null;

      snapshot('NODE_ADD');
      const newNode: WorkflowCanvasNode = {
        id: `node_${Date.now()}`,
        type: 'default',
        position,
        data: {
          label: getDefaultNodeLabel(nodeType),
          nodeType,
          config: { properties: {} },
          inputs: {},
          outputs: {}
        }
      };

      addNodeToStore(newNode);
      return newNode;
    },
    [addNodeToStore, isExecuting, isReadonly, snapshot]
  );

  const duplicateNode = useCallback(
    (nodeId: string) => {
      if (isReadonly || isExecuting) return null;
      const node = nodes.find((item) => item.id === nodeId);
      if (!node) return null;

      snapshot('NODE_ADD');
      const duplicatedNode: WorkflowCanvasNode = {
        ...node,
        id: `node_${Date.now()}`,
        position: {
          x: node.position.x + 50,
          y: node.position.y + 50
        },
        data: {
          ...node.data,
          label: `${node.data.label} (副本)`
        }
      };

      addNodeToStore(duplicatedNode);
      return duplicatedNode;
    },
    [addNodeToStore, isExecuting, isReadonly, nodes, snapshot]
  );

  const removeNode = useCallback(
    (nodeId: string) => {
      if (isReadonly || isExecuting) return;
      snapshot('NODE_DELETE');
      deleteNode(nodeId);
    },
    [deleteNode, isExecuting, isReadonly, snapshot]
  );

  const updateNodeData = useCallback(
    (nodeId: string, patch: Partial<WorkflowNodeData>) => {
      if (isReadonly || isExecuting) return;
      snapshot('NODE_UPDATE');
      updateNode(nodeId, (node) => ({
        ...node,
        data: {
          ...node.data,
          ...patch
        }
      }));
    },
    [isExecuting, isReadonly, updateNode]
  );

  return {
    addNode,
    duplicateNode,
    removeNode,
    updateNodeData
  };
}
