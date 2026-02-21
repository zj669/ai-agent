import { useEffect, useMemo, useRef, useState } from 'react'
import {
  createChatConversation,
  fetchConversationList,
  fetchConversationMessages,
  startChatStream,
  stopChatExecution,
  type ChatConversation,
  type ChatMessage
} from '../api/chatService'

function makeLocalId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function ChatPage() {
  const [userId, setUserId] = useState(1)
  const [agentId, setAgentId] = useState(1)

  const [conversations, setConversations] = useState<ChatConversation[]>([])
  const [activeConversationId, setActiveConversationId] = useState<string>('')
  const [messages, setMessages] = useState<ChatMessage[]>([])

  const [input, setInput] = useState('')
  const [listError, setListError] = useState('')
  const [streamError, setStreamError] = useState('')
  const [isSending, setIsSending] = useState(false)

  const abortControllerRef = useRef<AbortController | null>(null)
  const executionIdRef = useRef('')
  const isStoppedRef = useRef(false)
  const hasStreamErrorRef = useRef(false)
  const pendingStopRef = useRef(false)

  useEffect(() => {
    void fetchConversationList(userId, agentId)
      .then((data) => {
        setConversations(data)
        setListError('')
      })
      .catch(() => {
        setConversations([])
        setListError('会话列表加载失败')
      })
  }, [userId, agentId])

  useEffect(() => {
    if (!activeConversationId) {
      setMessages([])
      return
    }

    void fetchConversationMessages(userId, activeConversationId)
      .then((data) => {
        setMessages(data)
        setStreamError('')
      })
      .catch(() => {
        setMessages([])
        setStreamError('消息加载失败')
      })
  }, [userId, activeConversationId])

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort()
    }
  }, [])

  const canSend = useMemo(() => {
    return input.trim().length > 0 && !isSending
  }, [input, isSending])

  const appendAssistantDelta = (targetMessageId: string, delta: string) => {
    setMessages((current) =>
      current.map((message) => {
        if (message.id !== targetMessageId) {
          return message
        }

        return {
          ...message,
          content: `${message.content}${delta}`,
          status: 'STREAMING'
        }
      })
    )
  }

  const finishAssistantMessage = (targetMessageId: string, status: 'COMPLETED' | 'FAILED') => {
    setMessages((current) =>
      current.map((message) => {
        if (message.id !== targetMessageId) {
          return message
        }

        return {
          ...message,
          status
        }
      })
    )
  }

  const markLastStreamingAsFailed = () => {
    setMessages((current) => {
      const lastStreamingIndex = [...current].reverse().findIndex((message) => message.status === 'STREAMING')
      if (lastStreamingIndex < 0) {
        return current
      }

      const index = current.length - 1 - lastStreamingIndex
      return current.map((message, messageIndex) => {
        if (messageIndex !== index) {
          return message
        }

        return {
          ...message,
          status: 'FAILED'
        }
      })
    })
  }

  const resetStreamRuntime = (controller?: AbortController) => {
    if (controller && abortControllerRef.current !== controller) {
      return
    }

    abortControllerRef.current = null
    executionIdRef.current = ''
    pendingStopRef.current = false
    isStoppedRef.current = false
    hasStreamErrorRef.current = false
  }

  const submitStopRequest = async () => {
    const executionId = executionIdRef.current
    if (!executionId) {
      return
    }

    try {
      await stopChatExecution(executionId)
    } catch {
      setStreamError('中断请求提交失败')
    }
  }

  const handleCreateConversation = async () => {
    setListError('')

    try {
      const id = await createChatConversation(userId, agentId)
      const list = await fetchConversationList(userId, agentId)
      setConversations(list)
      setActiveConversationId(id)
    } catch {
      setListError('创建会话失败')
    }
  }

  const handleSend = async () => {
    const content = input.trim()
    if (!content || isSending) {
      return
    }

    setStreamError('')
    setIsSending(true)
    setInput('')

    let conversationId = activeConversationId
    if (!conversationId) {
      try {
        conversationId = await createChatConversation(userId, agentId)
        const list = await fetchConversationList(userId, agentId)
        setConversations(list)
        setActiveConversationId(conversationId)
      } catch {
        setStreamError('创建会话失败')
        setIsSending(false)
        return
      }
    }

    const userMessage: ChatMessage = {
      id: makeLocalId('user'),
      role: 'USER',
      content,
      status: 'COMPLETED',
      createdAt: new Date().toISOString()
    }

    const assistantMessageId = makeLocalId('assistant')
    const assistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'ASSISTANT',
      content: '',
      status: 'STREAMING',
      createdAt: new Date().toISOString()
    }

    setMessages((current) => [...current, userMessage, assistantMessage])

    const controller = new AbortController()
    abortControllerRef.current = controller
    executionIdRef.current = ''
    pendingStopRef.current = false
    isStoppedRef.current = false
    hasStreamErrorRef.current = false

    try {
      await startChatStream(
        {
          userId,
          agentId,
          conversationId,
          content
        },
        {
          onConnected: (id) => {
            executionIdRef.current = id
            if (pendingStopRef.current) {
              void submitStopRequest()
            }
          },
          onDelta: (delta) => {
            if (isStoppedRef.current || hasStreamErrorRef.current) {
              return
            }
            appendAssistantDelta(assistantMessageId, delta)
          },
          onFinish: () => {
            if (isStoppedRef.current || hasStreamErrorRef.current) {
              return
            }
            finishAssistantMessage(assistantMessageId, 'COMPLETED')
          },
          onError: (message) => {
            hasStreamErrorRef.current = true
            finishAssistantMessage(assistantMessageId, 'FAILED')
            setStreamError(message)
          }
        },
        controller.signal
      )

      if (!isStoppedRef.current && !hasStreamErrorRef.current) {
        finishAssistantMessage(assistantMessageId, 'COMPLETED')
      }
    } catch {
      if (!isStoppedRef.current) {
        finishAssistantMessage(assistantMessageId, 'FAILED')
        setStreamError('流式消息发送失败')
      }
    } finally {
      setIsSending(false)
      resetStreamRuntime(controller)
    }
  }

  const handleStop = async () => {
    if (!isSending) {
      return
    }

    isStoppedRef.current = true
    pendingStopRef.current = true

    abortControllerRef.current?.abort()
    await submitStopRequest()

    markLastStreamingAsFailed()
    setStreamError((current) => current || '已手动中断')
    setIsSending(false)
  }

  return (
    <section>
      <h2 className="text-2xl font-semibold">聊天</h2>

      <div className="mt-3 flex items-center gap-3 text-sm">
        <label className="flex items-center gap-2">
          用户ID
          <input
            className="w-20 rounded border border-slate-300 px-2 py-1"
            type="number"
            value={userId}
            onChange={(event) => setUserId(Number(event.target.value) || 1)}
          />
        </label>
        <label className="flex items-center gap-2">
          AgentID
          <input
            className="w-20 rounded border border-slate-300 px-2 py-1"
            type="number"
            value={agentId}
            onChange={(event) => setAgentId(Number(event.target.value) || 1)}
          />
        </label>
      </div>

      <div className="mt-4 grid grid-cols-[260px_1fr] gap-4">
        <aside className="rounded border border-slate-200 p-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium">会话</h3>
            <button
              type="button"
              className="rounded bg-slate-900 px-2 py-1 text-xs text-white"
              onClick={handleCreateConversation}
            >
              新建
            </button>
          </div>

          {listError ? <p className="mt-2 text-xs text-red-600">{listError}</p> : null}

          <ul className="mt-3 space-y-2 text-sm">
            {conversations.map((conversation) => (
              <li key={conversation.id}>
                <button
                  type="button"
                  className={`w-full rounded border px-2 py-1 text-left ${
                    activeConversationId === conversation.id ? 'border-slate-800' : 'border-slate-200'
                  }`}
                  onClick={() => setActiveConversationId(conversation.id)}
                >
                  <div className="truncate font-medium">{conversation.title || conversation.id}</div>
                  <div className="truncate text-xs text-slate-500">{conversation.updatedAt}</div>
                </button>
              </li>
            ))}
          </ul>
        </aside>

        <div className="rounded border border-slate-200 p-3">
          <div className="h-[360px] overflow-y-auto rounded border border-slate-200 p-3">
            {messages.length === 0 ? <p className="text-sm text-slate-500">请选择会话或直接发送消息。</p> : null}

            <ul className="space-y-3">
              {messages.map((message) => (
                <li key={message.id}>
                  <div className="text-xs text-slate-500">{message.role}</div>
                  <div className="rounded bg-slate-50 px-3 py-2 text-sm whitespace-pre-wrap">{message.content || '...'}</div>
                  <div className="mt-1 text-xs text-slate-400">{message.status}</div>
                </li>
              ))}
            </ul>
          </div>

          {streamError ? <p className="mt-2 text-sm text-red-600">{streamError}</p> : null}

          <div className="mt-3 flex items-center gap-2">
            <input
              className="flex-1 rounded border border-slate-300 px-3 py-2 text-sm"
              value={input}
              placeholder="输入消息后发送"
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  void handleSend()
                }
              }}
            />
            <button
              type="button"
              className="rounded bg-slate-900 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:bg-slate-400"
              disabled={!canSend}
              onClick={() => void handleSend()}
            >
              发送
            </button>
            <button
              type="button"
              className="rounded border border-slate-300 px-3 py-2 text-sm disabled:cursor-not-allowed disabled:text-slate-400"
              disabled={!isSending}
              onClick={() => void handleStop()}
            >
              中断
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}

export default ChatPage
