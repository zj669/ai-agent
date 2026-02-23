import { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Input, Avatar, Typography, Spin, Empty, Divider, Badge, Alert } from 'antd'
import {
  RobotOutlined,
  UserOutlined,
  SendOutlined,
  PlusOutlined,
  StopOutlined,
  SearchOutlined,
  MessageOutlined
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import {
  createChatConversation,
  fetchConversationList,
  fetchConversationMessages,
  startChatStream,
  stopChatExecution,
  type ChatConversation,
  type ChatMessage
} from '../api/chatService'
import { fetchAgentList, type AgentListItem } from '../../agent/api/agentService'

const { Text } = Typography
const { TextArea } = Input
const USER_ID = 1

function makeLocalId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return iso
  }
}

/* ---- typing dots keyframes (injected once) ---- */
const TYPING_STYLE_ID = 'chat-typing-dots-style'
if (typeof document !== 'undefined' && !document.getElementById(TYPING_STYLE_ID)) {
  const style = document.createElement('style')
  style.id = TYPING_STYLE_ID
  style.textContent = `
@keyframes typingBounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-4px); }
}
.typing-dots { display: inline-flex; gap: 3px; align-items: center; padding: 4px 0; }
.typing-dots span {
  width: 6px; height: 6px; border-radius: 50%; background: #999;
  animation: typingBounce 1.2s infinite ease-in-out;
}
.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }
`
  document.head.appendChild(style)
}

function TypingDots() {
  return (
    <div className="typing-dots">
      <span /><span /><span />
    </div>
  )
}

function ChatPage() {
  const [agents, setAgents] = useState<AgentListItem[]>([])
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null)
  const [conversations, setConversations] = useState<ChatConversation[]>([])
  const [activeConversationId, setActiveConversationId] = useState<string>('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [listError, setListError] = useState('')
  const [streamError, setStreamError] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [loadingAgents, setLoadingAgents] = useState(true)
  const [agentSearch, setAgentSearch] = useState('')

  const abortControllerRef = useRef<AbortController | null>(null)
  const executionIdRef = useRef('')
  const isStoppedRef = useRef(false)
  const hasStreamErrorRef = useRef(false)
  const pendingStopRef = useRef(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load agent list
  useEffect(() => {
    setLoadingAgents(true)
    void fetchAgentList()
      .then((data) => {
        const published = data.filter((a) => a.status === 'PUBLISHED' || a.status === '1')
        setAgents(published.length > 0 ? published : data)
      })
      .catch(() => setAgents([]))
      .finally(() => setLoadingAgents(false))
  }, [])

  // Load conversations when agent selected
  useEffect(() => {
    if (!selectedAgentId) {
      setConversations([])
      setActiveConversationId('')
      setMessages([])
      return
    }
    void fetchConversationList(USER_ID, selectedAgentId)
      .then((data) => { setConversations(data); setListError('') })
      .catch(() => { setConversations([]); setListError('会话列表加载失败') })
  }, [selectedAgentId])

  // Load messages when conversation selected
  useEffect(() => {
    if (!activeConversationId) { setMessages([]); return }
    void fetchConversationMessages(USER_ID, activeConversationId)
      .then((data) => { setMessages(data); setStreamError('') })
      .catch(() => { setMessages([]); setStreamError('消息加载失败') })
  }, [activeConversationId])

  // Auto-scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  // Cleanup
  useEffect(() => { return () => { abortControllerRef.current?.abort() } }, [])

  const canSend = useMemo(() => input.trim().length > 0 && !isSending && !!selectedAgentId, [input, isSending, selectedAgentId])

  const selectedAgent = useMemo(() => agents.find((a) => a.id === selectedAgentId), [agents, selectedAgentId])

  const filteredAgents = useMemo(() => {
    if (!agentSearch.trim()) return agents
    const kw = agentSearch.trim().toLowerCase()
    return agents.filter((a) => a.name.toLowerCase().includes(kw) || a.description?.toLowerCase().includes(kw))
  }, [agents, agentSearch])

  const appendDelta = (id: string, delta: string) => {
    setMessages((cur) => cur.map((m) => m.id === id ? { ...m, content: m.content + delta, status: 'STREAMING' } : m))
  }

  const finishMsg = (id: string, status: 'COMPLETED' | 'FAILED') => {
    setMessages((cur) => cur.map((m) => m.id === id ? { ...m, status } : m))
  }

  const resetRuntime = (ctrl?: AbortController) => {
    if (ctrl && abortControllerRef.current !== ctrl) return
    abortControllerRef.current = null
    executionIdRef.current = ''
    pendingStopRef.current = false
    isStoppedRef.current = false
    hasStreamErrorRef.current = false
  }

  const submitStop = async () => {
    const eid = executionIdRef.current
    if (!eid) return
    try { await stopChatExecution(eid) } catch { setStreamError('中断请求提交失败') }
  }

  const handleCreateConversation = async () => {
    if (!selectedAgentId) return
    setListError('')
    try {
      const id = await createChatConversation(USER_ID, selectedAgentId)
      const list = await fetchConversationList(USER_ID, selectedAgentId)
      setConversations(list)
      setActiveConversationId(id)
    } catch { setListError('创建会话失败') }
  }

  const handleSend = async () => {
    const content = input.trim()
    if (!content || isSending || !selectedAgentId) return
    setStreamError(''); setIsSending(true); setInput('')

    let convId = activeConversationId
    if (!convId) {
      try {
        convId = await createChatConversation(USER_ID, selectedAgentId)
        const list = await fetchConversationList(USER_ID, selectedAgentId)
        setConversations(list); setActiveConversationId(convId)
      } catch { setStreamError('创建会话失败'); setIsSending(false); return }
    }

    const userMsg: ChatMessage = { id: makeLocalId('u'), role: 'USER', content, status: 'COMPLETED', createdAt: new Date().toISOString() }
    const aId = makeLocalId('a')
    const assistantMsg: ChatMessage = { id: aId, role: 'ASSISTANT', content: '', status: 'STREAMING', createdAt: new Date().toISOString() }
    setMessages((cur) => [...cur, userMsg, assistantMsg])

    const ctrl = new AbortController()
    abortControllerRef.current = ctrl
    executionIdRef.current = ''; pendingStopRef.current = false; isStoppedRef.current = false; hasStreamErrorRef.current = false

    try {
      await startChatStream(
        { userId: USER_ID, agentId: selectedAgentId, conversationId: convId, content },
        {
          onConnected: (id) => { executionIdRef.current = id; if (pendingStopRef.current) void submitStop() },
          onDelta: (d) => { if (!isStoppedRef.current && !hasStreamErrorRef.current) appendDelta(aId, d) },
          onFinish: () => { if (!isStoppedRef.current && !hasStreamErrorRef.current) finishMsg(aId, 'COMPLETED') },
          onError: (msg) => { hasStreamErrorRef.current = true; finishMsg(aId, 'FAILED'); setStreamError(msg) }
        },
        ctrl.signal
      )
      if (!isStoppedRef.current && !hasStreamErrorRef.current) finishMsg(aId, 'COMPLETED')
    } catch {
      if (!isStoppedRef.current) { finishMsg(aId, 'FAILED'); setStreamError('流式消息发送失败') }
    } finally { setIsSending(false); resetRuntime(ctrl) }
  }

  const handleStop = async () => {
    if (!isSending) return
    isStoppedRef.current = true; pendingStopRef.current = true
    abortControllerRef.current?.abort()
    await submitStop()
    setMessages((cur) => cur.map((m) => m.status === 'STREAMING' ? { ...m, status: 'FAILED' } : m))
    setStreamError((c) => c || '已手动中断'); setIsSending(false)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void handleSend()
    }
  }

  /* ---- RENDER ---- */
  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 64px - 48px)', overflow: 'hidden' }}>
      {/* ===== Left Panel ===== */}
      <div style={{
        width: 280, background: '#fff', borderRight: '1px solid #f0f0f0',
        display: 'flex', flexDirection: 'column', flexShrink: 0
      }}>
        {/* Agent search */}
        <div style={{ padding: '16px 16px 8px' }}>
          <Input.Search
            placeholder="搜索 Agent..."
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            allowClear
            value={agentSearch}
            onChange={(e) => setAgentSearch(e.target.value)}
            style={{ marginBottom: 8 }}
          />
        </div>

        {/* Agent list */}
        <div style={{ flex: '0 0 auto', maxHeight: 220, overflowY: 'auto', padding: '0 8px' }}>
          {loadingAgents ? (
            <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
          ) : filteredAgents.length === 0 ? (
            <Empty description="暂无可用 Agent" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ margin: '16px 0' }} />
          ) : (
            filteredAgents.map((agent) => {
              const isSelected = agent.id === selectedAgentId
              return (
                <div
                  key={agent.id}
                  onClick={() => setSelectedAgentId(agent.id)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '10px 12px', marginBottom: 4, borderRadius: 8, cursor: 'pointer',
                    borderLeft: isSelected ? '3px solid #1677ff' : '3px solid transparent',
                    background: isSelected ? '#e6f4ff' : 'transparent',
                    transition: 'all 0.2s'
                  }}
                >
                  <Avatar size={36} icon={<RobotOutlined />} style={{ background: isSelected ? '#1677ff' : '#d9d9d9', flexShrink: 0 }} />
                  <div style={{ overflow: 'hidden', flex: 1 }}>
                    <Text strong ellipsis style={{ fontSize: 14, display: 'block' }}>{agent.name}</Text>
                    {agent.description && (
                      <Text type="secondary" ellipsis style={{ fontSize: 12 }}>{agent.description}</Text>
                    )}
                  </div>
                </div>
              )
            })
          )}
        </div>

        <Divider style={{ margin: '8px 0' }} />

        {/* Conversation list */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '0 8px' }}>
          {listError && <Alert type="error" message={listError} showIcon style={{ margin: '0 8px 8px' }} />}
          {selectedAgentId && conversations.length === 0 && !listError && (
            <Empty description="暂无对话" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ marginTop: 24 }} />
          )}
          {conversations.map((c) => {
            const isActive = activeConversationId === c.id
            return (
              <div
                key={c.id}
                onClick={() => setActiveConversationId(c.id)}
                style={{
                  padding: '10px 12px', marginBottom: 4, borderRadius: 8, cursor: 'pointer',
                  background: isActive ? '#e6f4ff' : 'transparent',
                  transition: 'background 0.2s'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <MessageOutlined style={{ color: isActive ? '#1677ff' : '#bfbfbf', fontSize: 14 }} />
                  <Text ellipsis style={{ fontSize: 13, flex: 1, color: isActive ? '#1677ff' : undefined }}>
                    {c.title || c.id.slice(0, 8)}
                  </Text>
                </div>
                <Text type="secondary" style={{ fontSize: 11, marginLeft: 20 }}>{formatTime(c.updatedAt)}</Text>
              </div>
            )
          })}
        </div>

        {/* New conversation button */}
        <div style={{ padding: 12 }}>
          <Button
            type="dashed"
            icon={<PlusOutlined />}
            block
            disabled={!selectedAgentId}
            onClick={() => void handleCreateConversation()}
          >
            新建对话
          </Button>
        </div>
      </div>

      {/* ===== Right Panel ===== */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#fafafa', minWidth: 0 }}>
        {!selectedAgentId ? (
          /* Empty state */
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <RobotOutlined style={{ fontSize: 72, color: '#d9d9d9' }} />
            <Text type="secondary" style={{ fontSize: 16, marginTop: 16 }}>选择一个 Agent 开始对话</Text>
          </div>
        ) : (
          <>
            {/* Top bar */}
            <div style={{
              height: 56, padding: '0 20px', display: 'flex', alignItems: 'center', gap: 10,
              background: '#fff', borderBottom: '1px solid #f0f0f0', flexShrink: 0
            }}>
              <Avatar size={32} icon={<RobotOutlined />} style={{ background: '#1677ff' }} />
              <Text strong style={{ fontSize: 15 }}>{selectedAgent?.name ?? 'Agent'}</Text>
              <Badge status="processing" text={<Text type="secondary" style={{ fontSize: 12 }}>在线</Text>} />
            </div>

            {/* Message area */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px' }}>
              {messages.length === 0 && (
                <div style={{ textAlign: 'center', paddingTop: 80 }}>
                  <Empty description="发送消息开始对话" />
                </div>
              )}
              {messages.map((m) => {
                const isUser = m.role === 'USER'
                return (
                  <div key={m.id} style={{
                    display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start',
                    marginBottom: 16, gap: 8
                  }}>
                    {!isUser && (
                      <Avatar size={32} icon={<RobotOutlined />} style={{ background: '#1677ff', flexShrink: 0, marginTop: 2 }} />
                    )}
                    <div style={{ maxWidth: '70%' }}>
                      <div style={{
                        padding: '10px 14px',
                        borderRadius: isUser ? '12px 12px 2px 12px' : '12px 12px 12px 2px',
                        background: isUser ? '#1677ff' : '#f5f5f5',
                        color: isUser ? '#fff' : '#333',
                        wordBreak: 'break-word'
                      }}>
                        {isUser ? (
                          <div style={{ whiteSpace: 'pre-wrap', fontSize: 14, lineHeight: 1.6 }}>{m.content || '...'}</div>
                        ) : (
                          <div className="markdown-body" style={{ fontSize: 14, lineHeight: 1.6 }}>
                            {m.content ? (
                              <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                                {m.content}
                              </ReactMarkdown>
                            ) : m.status === 'STREAMING' ? (
                              <TypingDots />
                            ) : (
                              '...'
                            )}
                            {m.status === 'STREAMING' && m.content && <TypingDots />}
                          </div>
                        )}
                      </div>
                      <div style={{
                        fontSize: 11, marginTop: 4, color: '#999',
                        textAlign: isUser ? 'right' : 'left'
                      }}>
                        {formatTime(m.createdAt)}
                      </div>
                    </div>
                    {isUser && (
                      <Avatar size={32} icon={<UserOutlined />} style={{ background: '#87d068', flexShrink: 0, marginTop: 2 }} />
                    )}
                  </div>
                )
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Input area */}
            <div style={{
              padding: '12px 20px 16px', background: '#fff', borderTop: '1px solid #f0f0f0', flexShrink: 0
            }}>
              {streamError && (
                <Alert type="error" message={streamError} showIcon closable
                  onClose={() => setStreamError('')} style={{ marginBottom: 8 }} />
              )}
              <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                <TextArea
                  value={input}
                  placeholder="输入消息..."
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={isSending}
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  style={{ flex: 1, borderRadius: 8 }}
                />
                {isSending ? (
                  <Button danger icon={<StopOutlined />} onClick={() => void handleStop()}>
                    停止
                  </Button>
                ) : (
                  <Button type="primary" icon={<SendOutlined />} disabled={!canSend} onClick={() => void handleSend()}>
                    发送
                  </Button>
                )}
              </div>
              <Text type="secondary" style={{ fontSize: 11, marginTop: 4, display: 'block' }}>
                Enter 发送, Shift+Enter 换行
              </Text>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default ChatPage
