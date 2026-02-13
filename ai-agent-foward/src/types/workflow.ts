/**
 * 工作流相关类型定义
 */

// ========== 节点类型 ==========

export enum NodeType {
  START = 'START',
  END = 'END',
  LLM = 'LLM',
  HTTP = 'HTTP',
  CONDITION = 'CONDITION',
  TOOL = 'TOOL'
}

export enum EdgeType {
  DEPENDENCY = 'DEPENDENCY',
  CONDITIONAL = 'CONDITIONAL',
  DEFAULT = 'DEFAULT'
}

// ========== 执行状态 ==========

export enum ExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  PAUSED_FOR_REVIEW = 'PAUSED_FOR_REVIEW',
  SUCCEEDED = 'SUCCEEDED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export enum NodeExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  SUCCEEDED = 'SUCCEEDED',
  FAILED = 'FAILED',
  SKIPPED = 'SKIPPED'
}

export enum ExecutionMode {
  STANDARD = 'STANDARD',
  DEBUG = 'DEBUG',
  DRY_RUN = 'DRY_RUN'
}

// ========== 节点配置 ==========

export interface NodePosition {
  x: number;
  y: number;
}

export interface HumanReviewConfig {
  enabled: boolean;
  reviewers?: string[];
  description?: string;
}

export interface RetryPolicy {
  maxRetries: number;
  retryDelayMs: number;
  retryOnErrors?: string[];
}

export interface NodeConfig {
  properties: Record<string, any>;
  humanReviewConfig?: HumanReviewConfig;
  retryPolicy?: RetryPolicy;
  timeoutMs?: number;
}

// ========== 工作流图结构 ==========

export interface WorkflowNode {
  nodeId: string;
  name: string;
  type: NodeType;
  config?: NodeConfig;
  inputs?: Record<string, any>;
  outputs?: Record<string, string>;
  dependencies?: string[];
  successors?: string[];
  position?: NodePosition;
}

export interface WorkflowEdge {
  edgeId: string;
  source: string;
  target: string;
  condition?: string;
  edgeType: EdgeType;
}

export interface WorkflowGraph {
  graphId: string;
  version: string;
  description?: string;
  nodes: Record<string, WorkflowNode>;
  edges: Record<string, string[]>;
  edgeDetails: Record<string, WorkflowEdge[]>;
}

// ========== 执行相关 ==========

export interface StartExecutionRequest {
  agentId: number;
  userId: number;
  conversationId: string;
  versionId?: number;
  inputs: Record<string, any>;
  mode?: ExecutionMode;
}

export interface StopExecutionRequest {
  executionId: string;
}

export interface ExecutionDTO {
  executionId: string;
  agentId: number;
  userId: number;
  conversationId: string;
  status: ExecutionStatus;
  startTime: string;
  endTime?: string;
  nodeStatuses: Record<string, NodeExecutionStatus>;
}

export interface WorkflowNodeExecutionLog {
  executionId: string;
  nodeId: string;
  nodeType: NodeType;
  status: NodeExecutionStatus;
  startTime: string;
  endTime?: string;
  input?: string;
  output?: string;
  errorMessage?: string;
}

export interface WorkflowNodeExecutionLogDTO {
  nodeId: string;
  nodeName: string;
  nodeType: NodeType;
  status: NodeExecutionStatus;
  startTime: string;
  endTime?: string;
}

export interface ChatMessage {
  role: string;
  content: string;
  timestamp: number;
}

export interface ExecutionContextDTO {
  executionId: string;
  longTermMemories: string[];
  chatHistory: ChatMessage[];
  executionLog: string;
  globalVariables: Record<string, any>;
}

// ========== SSE 事件 ==========

export enum SSEEventType {
  CONNECTED = 'connected',
  START = 'start',
  UPDATE = 'update',
  FINISH = 'finish',
  ERROR = 'error',
  PING = 'ping'
}

export interface SSEConnectedEvent {
  executionId: string;
}

export interface SSEStartEvent {
  executionId: string;
  nodeId: string;
  nodeType: NodeType;
  timestamp: number;
}

export interface SSEUpdateEvent {
  executionId: string;
  nodeId: string;
  delta: string;
  timestamp: number;
}

export interface SSEFinishEvent {
  executionId: string;
  nodeId: string;
  status: NodeExecutionStatus;
  timestamp: number;
}

export interface SSEErrorEvent {
  executionId: string;
  message: string;
}

// ========== ReactFlow 相关 ==========

export interface ReactFlowNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: {
    label: string;
    nodeType: NodeType;
    config?: NodeConfig;
    inputs?: Record<string, any>;
    outputs?: Record<string, string>;
    status?: NodeExecutionStatus;
  };
}

export interface ReactFlowEdge {
  id: string;
  source: string;
  target: string;
  type?: string;
  label?: string;
  data?: {
    condition?: string;
    edgeType: EdgeType;
  };
}
