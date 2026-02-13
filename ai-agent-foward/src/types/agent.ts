export enum AgentStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED'
}

export interface AgentSummary {
  id: number;
  userId: number;
  name: string;
  description?: string;
  icon?: string;
  status: AgentStatus;
  publishedVersionId?: number;
  updateTime: string;
}

export interface AgentDetail {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  graphJson?: string;
  version: number;
  publishedVersionId?: number;
  status: number;
}

export interface CreateAgentRequest {
  name: string;
  description?: string;
  icon?: string;
}

export interface UpdateAgentRequest {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  graphJson?: string;
  version: number;
}

export interface PublishAgentRequest {
  id: number;
}

export interface RollbackAgentRequest {
  id: number;
  targetVersion: number;
}

export interface VersionHistory {
  agentId: number;
  versions: AgentVersion[];
}

export interface AgentVersion {
  versionId: number;
  version: number;
  graphJson: string;
  createdAt: string;
  isPublished: boolean;
}
