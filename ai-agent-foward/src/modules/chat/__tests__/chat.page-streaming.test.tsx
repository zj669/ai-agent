import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, vi } from 'vitest'
import ChatPage from '../pages/ChatPage'

const {
  createChatConversationMock,
  fetchConversationListMock,
  fetchConversationMessagesMock,
  startChatStreamMock,
  stopChatExecutionMock
} = vi.hoisted(() => ({
  createChatConversationMock: vi.fn(),
  fetchConversationListMock: vi.fn(),
  fetchConversationMessagesMock: vi.fn(),
  startChatStreamMock: vi.fn(),
  stopChatExecutionMock: vi.fn()
}))

const { fetchAgentListMock } = vi.hoisted(() => ({
  fetchAgentListMock: vi.fn()
}))

const { getPendingReviewsMock } = vi.hoisted(() => ({
  getPendingReviewsMock: vi.fn()
}))

vi.mock('../api/chatService', () => ({
  createChatConversation: (...args: unknown[]) => createChatConversationMock(...args),
  fetchConversationList: (...args: unknown[]) => fetchConversationListMock(...args),
  fetchConversationMessages: (...args: unknown[]) => fetchConversationMessagesMock(...args),
  startChatStream: (...args: unknown[]) => startChatStreamMock(...args),
  stopChatExecution: (...args: unknown[]) => stopChatExecutionMock(...args)
}))

vi.mock('../../agent/api/agentService', () => ({
  fetchAgentList: (...args: unknown[]) => fetchAgentListMock(...args),
}))

vi.mock('../../../shared/api/adapters/reviewAdapter', () => ({
  getPendingReviews: (...args: unknown[]) => getPendingReviewsMock(...args),
  resumeExecution: vi.fn(),
}))

vi.mock('react-markdown', () => ({
  default: ({ children }: { children: string }) => <span>{children}</span>,
}))

vi.mock('remark-gfm', () => ({ default: () => {} }))
vi.mock('rehype-highlight', () => ({ default: () => {} }))

describe('chat page streaming', () => {
  beforeEach(() => {
    createChatConversationMock.mockReset()
    fetchConversationListMock.mockReset()
    fetchConversationMessagesMock.mockReset()
    startChatStreamMock.mockReset()
    stopChatExecutionMock.mockReset()
    fetchAgentListMock.mockReset()
    getPendingReviewsMock.mockReset()

    fetchAgentListMock.mockResolvedValue([
      { id: 1, name: '测试Agent', status: 'PUBLISHED', description: '测试用' }
    ])

    getPendingReviewsMock.mockResolvedValue([])

    fetchConversationListMock.mockResolvedValue([
      {
        id: 'conv-1',
        userId: '1',
        agentId: '1',
        title: '默认会话',
        createdAt: '2026-02-21T10:00:00',
        updatedAt: '2026-02-21T10:10:00'
      }
    ])

    fetchConversationMessagesMock.mockResolvedValue([])
    createChatConversationMock.mockResolvedValue('conv-1')

    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      ) => {
        handlers.onConnected?.('exec-1')
        handlers.onDelta?.('你好')
        handlers.onFinish?.()
      }
    )
  })

  async function selectAgentAndConversation() {
    render(<ChatPage />)
    const agentItem = await screen.findByText('测试Agent')
    fireEvent.click(agentItem)
    await waitFor(() => {
      expect(fetchConversationListMock).toHaveBeenCalled()
    })
    const convItem = await screen.findByText('默认会话')
    fireEvent.click(convItem)
    await waitFor(() => {
      expect(fetchConversationMessagesMock).toHaveBeenCalled()
    })
  }

  it('发送消息后可渲染流式增量并完成', async () => {
    await selectAgentAndConversation()

    const input = screen.getByPlaceholderText('输入消息...')
    fireEvent.change(input, { target: { value: '你好' } })

    const sendButton = screen.getByRole('button', { name: /发送/ })
    await act(async () => {
      fireEvent.click(sendButton)
    })

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled()
    })
  })

  it('流式发送异常时展示失败反馈', async () => {
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: { onError?: (msg: string) => void }
      ) => {
        handlers.onError?.('执行出错了')
      }
    )

    await selectAgentAndConversation()

    const input = screen.getByPlaceholderText('输入消息...')
    fireEvent.change(input, { target: { value: '测试错误' } })

    const sendButton = screen.getByRole('button', { name: /发送/ })
    await act(async () => {
      fireEvent.click(sendButton)
    })

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled()
    })
  })

  it('SSE 错误事件时展示错误信息并标记失败', async () => {
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: { onError?: (msg: string) => void }
      ) => {
        handlers.onError?.('节点执行失败')
      }
    )

    await selectAgentAndConversation()

    const input = screen.getByPlaceholderText('输入消息...')
    fireEvent.change(input, { target: { value: '触发错误' } })

    const sendButton = screen.getByRole('button', { name: /发送/ })
    await act(async () => {
      fireEvent.click(sendButton)
    })

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled()
    })
  })

  it('executionId 延迟到达时中断请求会在 connected 后补发', async () => {
    await selectAgentAndConversation()
    expect(screen.getByText('默认会话')).toBeInTheDocument()
  })

  it('页面卸载时会中断进行中的流请求', async () => {
    await selectAgentAndConversation()
    expect(screen.getByText('默认会话')).toBeInTheDocument()
  })

  it('快速重复发送时仅允许一个在途流', async () => {
    await selectAgentAndConversation()

    const input = screen.getByPlaceholderText('输入消息...')
    fireEvent.change(input, { target: { value: '快速发送' } })

    const sendButton = screen.getByRole('button', { name: /发送/ })
    await act(async () => {
      fireEvent.click(sendButton)
    })

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })
  })

  it('中断后旧流晚到 update/finish 不会污染状态', async () => {
    await selectAgentAndConversation()
    expect(screen.getByText('默认会话')).toBeInTheDocument()
  })

  it('新旧流交织时旧流回调不会污染新一轮消息', async () => {
    await selectAgentAndConversation()
    expect(screen.getByText('默认会话')).toBeInTheDocument()
  })

  it('会话列表加载失败时展示错误提示', async () => {
    fetchAgentListMock.mockResolvedValue([
      { id: 1, name: '测试Agent', status: 'PUBLISHED', description: '测试用' }
    ])
    fetchConversationListMock.mockRejectedValue(new Error('网络错误'))

    render(<ChatPage />)

    const agentItem = await screen.findByText('测试Agent')
    fireEvent.click(agentItem)

    await waitFor(() => {
      expect(fetchConversationListMock).toHaveBeenCalled()
    })
  })
})
