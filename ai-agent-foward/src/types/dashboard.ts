import type { ApiResponse } from './auth';

export interface DashboardStats {
  agentCount: number;
  workflowCount: number;
  conversationCount: number;
  knowledgeDatasetCount: number;
  totalExecutions: number;
  successfulExecutions: number;
  failedExecutions: number;
  avgResponseTime: number;
}

export interface ExecutionTrend {
  date: string;
  total: number;
  successful: number;
  failed: number;
}

export interface DashboardData {
  stats: DashboardStats;
  executionTrend: ExecutionTrend[];
  recentExecutions: RecentExecution[];
}

export interface RecentExecution {
  executionId: string;
  agentName: string;
  status: 'SUCCEEDED' | 'FAILED' | 'RUNNING';
  startTime: string;
  endTime?: string;
  duration?: number;
}
