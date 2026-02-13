import { create } from 'zustand';
import { Conversation, Message } from '../types/chat';

interface ChatState {
  conversations: Conversation[];
  currentConversationId: string | null;
  messages: Message[];
  isLoading: boolean;
  isSending: boolean;

  // Actions
  setConversations: (conversations: Conversation[]) => void;
  setCurrentConversation: (conversationId: string | null) => void;
  setMessages: (messages: Message[]) => void;
  addMessage: (message: Message) => void;
  updateMessage: (messageId: string, updates: Partial<Message>) => void;
  setLoading: (isLoading: boolean) => void;
  setSending: (isSending: boolean) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  conversations: [],
  currentConversationId: null,
  messages: [],
  isLoading: false,
  isSending: false,

  setConversations: (conversations) => set({ conversations }),

  setCurrentConversation: (conversationId) => set({ currentConversationId: conversationId }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) =>
    set((state) => ({
      messages: [...state.messages, message]
    })),

  updateMessage: (messageId, updates) =>
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === messageId ? { ...msg, ...updates } : msg
      )
    })),

  setLoading: (isLoading) => set({ isLoading }),

  setSending: (isSending) => set({ isSending }),

  clearMessages: () => set({ messages: [] })
}));
