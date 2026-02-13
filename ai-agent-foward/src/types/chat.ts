import type { ApiResponse } from './auth';

export enum MessageRole {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT',
  SYSTEM = 'SYSTEM'
}

export enum MessageStatus {
  PENDING = 'PENDING',
  STREAMING = 'STREAMING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export interface ThoughtStep {
  step: number;
  description: string;
  result?: string;
}

export interface Citation {
  source: string;
  content: string;
  score?: number;
}

export interface Message {
  id: string;
  conversationId: string;
  role: MessageRole;
  content: string;
  thoughtProcess?: ThoughtStep[];
  citations?: Citation[];
  status: MessageStatus;
  createdAt: string;
  metadata?: Record<string, any>;
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConversationRequest {
  userId: string;
  agentId: string;
}

export interface SendMessageRequest {
  agentId: number;
  conversationId: string;
  userMessage: string;
}

export interface PageResult<T> {
  total: number;
  pages: number;
  list: T[];
}

export interface SSEPayload {
  eventType: string;
  executionId: string;
  nodeId?: string;
  content?: string;
  data?: any;
}
