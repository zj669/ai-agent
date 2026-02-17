import { agentService } from './agentService';
import { metadataService } from './metadataService';
import type { AgentDetail } from '../types/agent';
import type { NodeTemplate } from '../types/execution';
import type { WorkflowGraphDTO } from '../types/workflow';

const createDagId = () => `dag-${crypto.randomUUID()}`;
const createNodeId = () => `node-${crypto.randomUUID()}`;
const createEdgeId = () => `edge-${crypto.randomUUID()}`;

const createDefaultGraph = (): WorkflowGraphDTO => {
  const startId = createNodeId();
  const endId = createNodeId();

  return {
    dagId: createDagId(),
    version: '1.0.0',
    description: 'workflow draft',
    startNodeId: startId,
    nodes: [
      {
        nodeId: startId,
        nodeName: 'Start',
        nodeType: 'START',
        userConfig: {},
        inputSchema: [],
        outputSchema: [],
        position: { x: 220, y: 120 }
      },
      {
        nodeId: endId,
        nodeName: 'End',
        nodeType: 'END',
        userConfig: {},
        inputSchema: [],
        outputSchema: [],
        position: { x: 220, y: 320 }
      }
    ],
    edges: [
      {
        edgeId: createEdgeId(),
        source: startId,
        target: endId,
        edgeType: 'DEPENDENCY'
      }
    ]
  };
};

const parseGraph = (graphJson?: string): WorkflowGraphDTO => {
  if (!graphJson) return createDefaultGraph();

  try {
    const parsed = JSON.parse(graphJson) as Partial<WorkflowGraphDTO>;
    if (!parsed.nodes || !parsed.edges || !parsed.dagId) {
      return createDefaultGraph();
    }

    return {
      dagId: parsed.dagId,
      version: parsed.version || '1.0.0',
      description: parsed.description,
      startNodeId: parsed.startNodeId,
      nodes: parsed.nodes,
      edges: parsed.edges
    };
  } catch {
    return createDefaultGraph();
  }
};

export interface LoadWorkflowResult {
  graphJson: WorkflowGraphDTO;
  metadata: NodeTemplate[];
  version: number;
  agent: AgentDetail;
}

export interface SaveWorkflowResult {
  success: boolean;
  newVersion?: number;
  conflict?: boolean;
}

class WorkflowService {
  async loadWorkflow(agentId: number): Promise<LoadWorkflowResult> {
    const [agent, metadata] = await Promise.all([
      agentService.getAgent(agentId),
      metadataService.getNodeTemplates()
    ]);

    return {
      graphJson: parseGraph(agent.graphJson),
      metadata,
      version: agent.version,
      agent
    };
  }

  async saveWorkflow(agentId: number, graphJson: WorkflowGraphDTO, version: number): Promise<SaveWorkflowResult> {
    try {
      const latest = await agentService.getAgent(agentId);
      await agentService.updateAgent({
        id: agentId,
        name: latest.name,
        description: latest.description,
        icon: latest.icon,
        version,
        graphJson: JSON.stringify(graphJson)
      });

      const refreshed = await agentService.getAgent(agentId);
      return {
        success: true,
        newVersion: refreshed.version
      };
    } catch (error: any) {
      const errorMessage = String(error?.response?.data?.message || error?.message || '保存失败').toLowerCase();
      if (errorMessage.includes('version') || errorMessage.includes('optimistic') || errorMessage.includes('冲突')) {
        return { success: false, conflict: true };
      }
      throw error;
    }
  }
}

export const workflowService = new WorkflowService();
