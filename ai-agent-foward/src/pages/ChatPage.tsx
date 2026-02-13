import { useEffect, useRef, useState } from 'react';
import { Card, List, Input, Button, Empty, Spin, Modal, Select } from 'antd';
import { SendOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useChat } from '../hooks/useChat';
import { MessageItem } from '../components/MessageItem';
import { useAuthStore } from '../stores/authStore';
import { agentService } from '../services/agentService';
import { AgentSummary } from '../types/agent';

const { TextArea } = Input;

export const ChatPage: React.FC = () => {
  const { user } = useAuthStore();
  const [inputValue, setInputValue] = useState('');
  const [agents, setAgents] = useState<AgentSummary[]>([]);
  const [selectedAgentId, setSelectedAgentId] = useState<number | undefined>();
  const [loadingAgents, setLoadingAgents] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const {
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
  } = useChat(selectedAgentId);

  // Load agents
  useEffect(() => {
    const fetchAgents = async () => {
      setLoadingAgents(true);
      try {
        const data = await agentService.listAgents();
        setAgents(data);
        if (data.length > 0) {
          setSelectedAgentId(data[0].id);
        }
      } catch (error) {
        console.error('Failed to load agents:', error);
      } finally {
        setLoadingAgents(false);
      }
    };
    fetchAgents();
  }, []);

  // Load conversations when agent changes
  useEffect(() => {
    if (selectedAgentId && user) {
      loadConversations(user.id.toString());
    }
  }, [selectedAgentId, user]);

  // Load messages when conversation changes
  useEffect(() => {
    if (currentConversationId) {
      loadMessages(currentConversationId);
    }
  }, [currentConversationId]);

  // Auto scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    if (!inputValue.trim() || isSending || !user) return;

    let conversationId = currentConversationId;

    // Create new conversation if none selected
    if (!conversationId) {
      conversationId = await createConversation(user.id.toString());
      if (!conversationId) return;
      setCurrentConversation(conversationId);
    }

    await sendMessage(inputValue, conversationId);
    setInputValue('');
  };

  const handleNewChat = async () => {
    if (!user) return;

    const conversationId = await createConversation(user.id.toString());
    if (conversationId) {
      setCurrentConversation(conversationId);
      clearMessages();
    }
  };

  const handleDeleteConversation = (conversationId: string) => {
    if (!user) return;

    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个会话吗？此操作不可恢复。',
      okText: '确认',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deleteConversation(conversationId, user.id.toString())
    });
  };

  if (loadingAgents) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (agents.length === 0) {
    return (
      <Empty
        description="暂无可用的 Agent，请先创建 Agent"
        style={{ marginTop: 100 }}
      />
    );
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 150px)', gap: 16 }}>
      {/* Conversation List */}
      <Card
        title="会话列表"
        style={{ width: 300, overflow: 'auto' }}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={handleNewChat}
          >
            新建
          </Button>
        }
      >
        <div style={{ marginBottom: 16 }}>
          <Select
            style={{ width: '100%' }}
            placeholder="选择 Agent"
            value={selectedAgentId}
            onChange={setSelectedAgentId}
            options={agents.map((agent) => ({
              label: agent.name,
              value: agent.id
            }))}
          />
        </div>

        <List
          dataSource={conversations}
          loading={isLoading}
          renderItem={(conversation) => (
            <List.Item
              style={{
                cursor: 'pointer',
                backgroundColor:
                  currentConversationId === conversation.id ? '#e6f7ff' : 'transparent',
                padding: '8px 12px',
                borderRadius: 4
              }}
              onClick={() => setCurrentConversation(conversation.id)}
              actions={[
                <Button
                  type="text"
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteConversation(conversation.id);
                  }}
                />
              ]}
            >
              <List.Item.Meta
                title={conversation.title || '新会话'}
                description={new Date(conversation.createdAt).toLocaleString()}
              />
            </List.Item>
          )}
        />
      </Card>

      {/* Chat Area */}
      <Card
        title="对话"
        style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
        bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 0 }}
      >
        {/* Messages */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: 24,
            backgroundColor: '#fafafa'
          }}
        >
          {messages.length === 0 ? (
            <Empty description="开始新的对话" style={{ marginTop: 100 }} />
          ) : (
            messages.map((message) => (
              <MessageItem key={message.id} message={message} />
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div style={{ padding: 16, borderTop: '1px solid #f0f0f0' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <TextArea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder="输入消息... (Shift+Enter 换行)"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={isSending}
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
              loading={isSending}
              disabled={!inputValue.trim()}
            >
              发送
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
};
