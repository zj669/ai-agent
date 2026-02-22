import { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Input, List, Select, Space, Typography, Alert, Spin, Empty } from 'antd'
import { SendOutlined, StopOutlined, PlusOutlined } from '@ant-design/icons'
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

const { Text, Title } = Typography
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

  if (!selectedAgentId) {
    return (
      <div style={{ padding: 24 }}>
        <Title level={4}>聊天</Title>
        {loadingAgents ? <Spin /> : (
          <div style={{ maxWidth: 400, marginTop: 16 }}>
            <Text>请选择一个 Agent 开始聊天：</Text>
            <Select
              style={{ width: '100%', marginTop: 8 }}
              placeholder="选择 Agent"
              options={agents.map((a) => ({ value: a.id, label: a.name }))}
              onChange={(v) => setSelectedAgentId(v)}
            />
            {agents.length === 0 && <Empty description="暂无可用 Agent" style={{ marginTop: 24 }} />}
          </div>
        )}
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 80px)' }}>
      {/* Left: conversations */}
      <div style={{ width: 260, borderRight: '1px solid #f0f0f0', display: 'flex', flexDirection: 'column', padding: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <Select
            size="small"
            value={selectedAgentId}
            style={{ flex: 1, marginRight: 8 }}
            options={agents.map((a) => ({ value: a.id, label: a.name }))}
            onChange={(v) => setSelectedAgentId(v)}
          />
          <Button size="small" icon={<PlusOutlined />} onClick={() => void handleCreateConversation()} />
        </div>
        {listError && <Alert type="error" message={listError} showIcon style={{ marginBottom: 8 }} />}
        <div style={{ flex: 1, overflow: 'auto' }}>
          <List
            size="small"
            dataSource={conversations}
            renderItem={(c) => (
              <List.Item
                onClick={() => setActiveConversationId(c.id)}
                style={{
                  cursor: 'pointer', padding: '8px 12px', borderRadius: 6,
                  background: activeConversationId === c.id ? '#e6f4ff' : undefined
                }}
              >
                <div style={{ width: '100%', overflow: 'hidden' }}>
                  <Text ellipsis style={{ fontSize: 13 }}>{c.title || c.id.slice(0, 8)}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: 11 }}>{formatTime(c.updatedAt)}</Text>
                </div>
              </List.Item>
            )}
          />
        </div>
      </div>

      {/* Right: messages */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 16 }}>
        <div style={{ flex: 1, overflow: 'auto', paddingBottom: 16 }}>
          {messages.length === 0 && <Empty description="发送消息开始对话" />}
          {messages.map((m) => (
            <div key={m.id} style={{
              display: 'flex', justifyContent: m.role === 'USER' ? 'flex-end' : 'flex-start',
              marginBottom: 12
            }}>
              <div style={{
                maxWidth: '70%', padding: '8px 14px', borderRadius: 8,
                background: m.role === 'USER' ? '#1677ff' : '#f5f5f5',
                color: m.role === 'USER' ? '#fff' : '#333'
              }}>
                {m.role === 'USER' ? (
                  <div style={{ whiteSpace: 'pre-wrap', fontSize: 14 }}>{m.content || '...'}</div>
                ) : (
                  <div className="markdown-body" style={{ fontSize: 14 }}>
                    <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                      {m.content || '...'}
                    </ReactMarkdown>
                    {m.status === 'STREAMING' && <Spin size="small" style={{ marginLeft: 4 }} />}
                  </div>
                )}
                <div style={{ fontSize: 11, marginTop: 4, opacity: 0.6, textAlign: 'right' }}>
                  {formatTime(m.createdAt)}
                </div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {streamError && <Alert type="error" message={streamError} showIcon closable onClose={() => setStreamError('')} style={{ marginBottom: 8 }} />}

        <div style={{ display: 'flex', gap: 8 }}>
          <Input
            value={input}
            placeholder="输入消息..."
            onChange={(e) => setInput(e.target.value)}
            onPressEnter={() => void handleSend()}
            disabled={isSending}
          />
          <Button type="primary" icon={<SendOutlined />} disabled={!canSend} onClick={() => void handleSend()}>
            发送
          </Button>
          <Button icon={<StopOutlined />} disabled={!isSending} onClick={() => void handleStop()}>
            中断
          </Button>
        </div>
      </div>
    </div>
  )
}

export default ChatPage
