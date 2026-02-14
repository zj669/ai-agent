import { useEffect, useRef } from 'react';
import { message as antdMessage } from 'antd';
import { chatService } from '../services/chatService';
import { executionService } from '../services/executionService';
import { useChatStore } from '../stores/chatStore';
import { useAuthStore } from '../stores/authStore';
import { Message, MessageRole, MessageStatus } from '../types/chat';
import { NodeExecutionStatus } from '../types/execution';

export const useChat = (agentId?: number) => {
  const {
    conversations,
    currentConversationId,
    messages,
    isLoading,
    isSending,
    setConversations,
    setCurrentConversation,
    setMessages,
    addMessage,
    updateMessage,
    setLoading,
    setSending,
    clearMessages
  } = useChatStore();

  const { user } = useAuthStore();
  const abortControllerRef = useRef<AbortController | null>(null);

  // Load conversations
  const loadConversations = async (userId: string) => {
    if (!agentId) return;

    setLoading(true);
    try {
      const result = await chatService.getConversations(userId, agentId.toString());
      setConversations(result.list || []);
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '加载会话列表失败');
    } finally {
      setLoading(false);
    }
  };

  // Load messages
  const loadMessages = async (conversationId: string) => {
    if (!user) return;
    setLoading(true);
    try {
      const msgs = await chatService.getMessages(conversationId, user.id.toString());
      setMessages(msgs);
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '加载消息失败');
    } finally {
      setLoading(false);
    }
  };

  // Create conversation
  const createConversation = async (userId: string) => {
    if (!agentId) return null;

    try {
      const conversationId = await chatService.createConversation(userId, agentId.toString());
      await loadConversations(userId);
      return conversationId;
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '创建会话失败');
      return null;
    }
  };

  // Send message - uses executionService.startExecution for SSE streaming
  const sendMessage = async (content: string, conversationId: string) => {
    if (!agentId || !content.trim() || !user) return;

    // Add user message to local state
    const userMessage: Message = {
      id: `temp-${Date.now()}`,
      conversationId,
      role: MessageRole.USER,
      content: content.trim(),
      status: MessageStatus.COMPLETED,
      createdAt: new Date().toISOString()
    };
    addMessage(userMessage);

    // Add assistant message placeholder
    const assistantMessageId = `temp-assistant-${Date.now()}`;
    const assistantMessage: Message = {
      id: assistantMessageId,
      conversationId,
      role: MessageRole.ASSISTANT,
      content: '',
      status: MessageStatus.STREAMING,
      createdAt: new Date().toISOString()
    };
    addMessage(assistantMessage);

    setSending(true);

    // Cancel previous SSE connection if exists
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // Accumulate streaming content
    let accumulatedContent = '';

    try {
      // Start workflow execution via SSE (the backend handles saving messages)
      const controller = await executionService.startExecution(
        {
          agentId,
          userId: user.id,
          conversationId,
          inputs: { query: content.trim() },
          mode: 'STANDARD'
        },
        {
          onConnected: () => {
            // Connection established
          },
          onStart: () => {
            // Node started executing
          },
          onUpdate: (data) => {
            // Streaming text chunk from LLM node
            accumulatedContent += data.delta;
            updateMessage(assistantMessageId, {
              content: accumulatedContent,
              status: MessageStatus.STREAMING
            });
          },
          onFinish: (data) => {
            // Check if END node finished (workflow complete)
            if (data.status === NodeExecutionStatus.SUCCEEDED) {
              updateMessage(assistantMessageId, {
                content: accumulatedContent || '(工作流执行完成)',
                status: MessageStatus.COMPLETED
              });
              setSending(false);
            }
          },
          onError: (data) => {
            console.error('[Chat] SSE Error:', data.message);
            updateMessage(assistantMessageId, {
              content: accumulatedContent || '抱歉，消息处理失败，请重试。',
              status: MessageStatus.FAILED
            });
            setSending(false);
            antdMessage.error(data.message || '消息发送失败');
          }
        }
      );

      abortControllerRef.current = controller;
    } catch (error: any) {
      console.error('[Chat] Failed to start execution:', error);
      updateMessage(assistantMessageId, {
        content: '抱歉，消息发送失败，请重试。',
        status: MessageStatus.FAILED
      });
      setSending(false);
      antdMessage.error('消息发送失败');
    }
  };

  // Delete conversation
  const deleteConversation = async (conversationId: string, userId: string) => {
    try {
      await chatService.deleteConversation(conversationId, userId);
      await loadConversations(userId);
      if (currentConversationId === conversationId) {
        setCurrentConversation(null);
        clearMessages();
      }
      antdMessage.success('删除成功');
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '删除失败');
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  return {
    conversations,
    currentConversationId,
    messages,
    isLoading,
    isSending,
    loadConversations,
    loadMessages,
    createConversation,
    sendMessage,
    deleteConversation,
    setCurrentConversation,
    clearMessages
  };
};
