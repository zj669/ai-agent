import {
  createConversation,
  getConversationList,
  getConversationMessages,
  stopWorkflowExecution,
  getReviewDetail,
  resumeExecution,
  type ConversationSummary,
  type MessageDTO,
  type MessageStatus,
  type ReviewDetailData,
  type ResumeExecutionInput
} from '../../../shared/api/adapters/chatAdapter'

export type ChatConversation = ConversationSummary

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  status: MessageStatus
  createdAt: string
}

export interface SendMessageInput {
  agentId: number
  userId: number
  conversationId: string
  content: string
  versionId?: number
}

export interface StartExecutionEvent {
  event: string
  data: Record<string, unknown> | null
}

export interface StartExecutionHandlers {
  onConnected?: (executionId: string) => void
  onDelta?: (delta: string) => void
  onFinish?: () => void
  onError?: (message: string) => void
  onPaused?: (executionId: string, nodeId: string) => void
}

const textDecoder = new TextDecoder()

function getAccessToken(): string | null {
  return localStorage.getItem('accessToken') ?? sessionStorage.getItem('accessToken')
}

function parseSseBlock(block: string): StartExecutionEvent | null {
  const lines = block
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)

  if (lines.length === 0) {
    return null
  }

  let event = 'message'
  const dataLines: string[] = []

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      return
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  })

  const rawData = dataLines.join('\n')

  if (!rawData) {
    return { event, data: null }
  }

  if (rawData === 'pong') {
    return { event, data: { pong: true } }
  }

  try {
    return {
      event,
      data: JSON.parse(rawData) as Record<string, unknown>
    }
  } catch {
    return {
      event,
      data: { raw: rawData }
    }
  }
}

function extractDelta(data: Record<string, unknown> | null): string {
  if (!data) {
    return ''
  }

  const directDelta = data.delta
  if (typeof directDelta === 'string') {
    return directDelta
  }

  const payload = data.payload
  if (typeof payload === 'object' && payload !== null && 'delta' in payload) {
    const value = (payload as { delta?: unknown }).delta
    return typeof value === 'string' ? value : ''
  }

  const content = data.content
  return typeof content === 'string' ? content : ''
}

export async function fetchConversationList(userId: number, agentId: number): Promise<ChatConversation[]> {
  const data = await getConversationList({ userId, agentId })
  return data.list
}

export async function createChatConversation(userId: number, agentId: number): Promise<string> {
  return createConversation({ userId, agentId })
}

export async function fetchConversationMessages(userId: number, conversationId: string): Promise<ChatMessage[]> {
  const messages = await getConversationMessages({
    conversationId,
    userId,
    order: 'asc'
  })

  return messages.map(toChatMessage)
}

function toChatMessage(message: MessageDTO): ChatMessage {
  return {
    id: message.id,
    role: message.role,
    content: message.content,
    status: message.status,
    createdAt: message.createdAt
  }
}

export async function stopChatExecution(executionId: string): Promise<void> {
  await stopWorkflowExecution({ executionId })
}

export async function fetchReviewDetail(executionId: string): Promise<ReviewDetailData> {
  return getReviewDetail(executionId)
}

export async function submitResumeExecution(input: ResumeExecutionInput): Promise<void> {
  return resumeExecution(input)
}

export type { ReviewDetailData }

export async function startChatStream(
  input: SendMessageInput,
  handlers: StartExecutionHandlers,
  signal?: AbortSignal
): Promise<void> {
  const token = getAccessToken()

  const response = await fetch('/api/workflow/execution/start', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'debug-user': '1',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({
      agentId: input.agentId,
      userId: input.userId,
      conversationId: input.conversationId,
      versionId: input.versionId,
      inputs: {
        query: input.content
      },
      mode: 'STANDARD'
    }),
    signal
  })

  if (!response.ok || !response.body) {
    throw new Error('启动流式会话失败')
  }

  const reader = response.body.getReader()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        // 某些情况下服务端会直接关闭 SSE 连接而没有显式 finish 事件
        // 这里兜底触发完成，避免前端一直停留在 STREAMING
        handlers.onFinish?.()
        break
      }

      buffer += textDecoder.decode(value, { stream: true })

      let delimiterIndex = buffer.indexOf('\n\n')
      while (delimiterIndex >= 0) {
        const rawBlock = buffer.slice(0, delimiterIndex)
        buffer = buffer.slice(delimiterIndex + 2)

        const parsed = parseSseBlock(rawBlock)
        if (parsed) {
          handleSseEvent(parsed, handlers)
        }

        delimiterIndex = buffer.indexOf('\n\n')
      }
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // noop
    }
    reader.releaseLock()
  }
}

function handleSseEvent(event: StartExecutionEvent, handlers: StartExecutionHandlers): void {
  if (!event.data) {
    return
  }

  if (event.event === 'connected') {
    const executionId = event.data.executionId
    if (typeof executionId === 'string') {
      handlers.onConnected?.(executionId)
    }
    return
  }

  if (event.event === 'update') {
    // Check for custom JSON events (e.g., workflow_paused)
    const payload = event.data?.payload as Record<string, unknown> | undefined
    if (payload?.renderMode === 'JSON_EVENT' && typeof payload?.title === 'string') {
      const eventTitle = payload.title
      if (eventTitle === 'workflow_paused') {
        try {
          const content = typeof payload.content === 'string' ? JSON.parse(payload.content) : payload.content
          const executionId = content?.executionId
          const nodeId = content?.nodeId
          if (typeof executionId === 'string' && typeof nodeId === 'string') {
            handlers.onPaused?.(executionId, nodeId)
          }
        } catch {
          // ignore parse errors
        }
      }
      return
    }

    const delta = extractDelta(event.data)
    if (delta) {
      handlers.onDelta?.(delta)
    }
    return
  }

  if (event.event === 'finish') {
    // 仅在执行级完成事件或 END 节点完成时触发 onFinish
    const nodeType = event.data?.nodeType
    const status = event.data?.status
    const executionComplete =
      status === 'SUCCEEDED' ||
      status === 'FAILED' ||
      status === 'COMPLETED' ||
      status === 'CANCELLED'
    if (executionComplete || nodeType === 'END') {
      handlers.onFinish?.()
    }
    return
  }

  if (event.event === 'execution_complete') {
    handlers.onFinish?.()
    return
  }

  if (event.event === 'error') {
    const message = event.data.message
    handlers.onError?.(typeof message === 'string' ? message : '流式执行失败')
  }

  if (event.event === 'workflow_paused') {
    const executionId = event.data.executionId
    const nodeId = event.data.nodeId
    if (typeof executionId === 'string' && typeof nodeId === 'string') {
      handlers.onPaused?.(executionId, nodeId)
    }
  }
}
