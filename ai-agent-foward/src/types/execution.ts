/**
 * 工作流执行相关类型定义（精简版，仅用于聊天功能）
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

// ========== 节点模板 ==========

export interface NodeTemplate {
  id: string;
  name: string;
  type: string;
  description?: string;
  icon?: string;
  defaultConfig?: {
    properties?: Record<string, any>;
  };
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
