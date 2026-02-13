import apiClient from './apiClient';
import type {
  Conversation,
  Message,
  PageResult
} from '../types/chat';

class ChatService {
  // Conversation Management
  async createConversation(userId: string, agentId: string): Promise<string> {
    const response = await apiClient.post<string>('/chat/conversations', null, {
      params: { userId, agentId }
    });
    return response.data;
  }

  async getConversations(
    userId: string,
    agentId: string,
    page: number = 1,
    size: number = 20
  ): Promise<PageResult<Conversation>> {
    const response = await apiClient.get<PageResult<Conversation>>('/chat/conversations', {
      params: { userId, agentId, page, size }
    });
    return response.data;
  }

  async deleteConversation(conversationId: string, userId: string): Promise<void> {
    await apiClient.delete(`/chat/conversations/${conversationId}`, {
      params: { userId }
    });
  }

  // Message Management
  async getMessages(
    conversationId: string,
    userId: string,
    page: number = 1,
    size: number = 50
  ): Promise<Message[]> {
    const response = await apiClient.get<Message[]>(
      `/chat/conversations/${conversationId}/messages`,
      {
        params: { userId, page, size }
      }
    );
    return response.data;
  }
}

export const chatService = new ChatService();
