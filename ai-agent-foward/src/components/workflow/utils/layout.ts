import dagre from 'dagre';
import type { WorkflowCanvasEdge, WorkflowCanvasNode } from '../types';
import { WORKFLOW_NODE_HEIGHT, WORKFLOW_NODE_WIDTH } from '../constants';

export function applyAutoLayout(
  nodes: WorkflowCanvasNode[],
  edges: WorkflowCanvasEdge[],
  direction: 'TB' | 'LR' = 'TB'
): WorkflowCanvasNode[] {
  const graph = new dagre.graphlib.Graph();
  graph.setDefaultEdgeLabel(() => ({}));
  graph.setGraph({
    rankdir: direction,
    ranksep: 100,
    nodesep: 44
  });

  nodes.forEach((node) => {
    graph.setNode(node.id, {
      width: WORKFLOW_NODE_WIDTH,
      height: WORKFLOW_NODE_HEIGHT
    });
  });

  edges.forEach((edge) => {
    graph.setEdge(edge.source, edge.target);
  });

  dagre.layout(graph);

  return nodes.map((node) => {
    const layoutNode = graph.node(node.id);
    if (!layoutNode) return node;
    return {
      ...node,
      position: {
        x: layoutNode.x - WORKFLOW_NODE_WIDTH / 2,
        y: layoutNode.y - WORKFLOW_NODE_HEIGHT / 2
      }
    };
  });
}
