import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
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

vi.mock('../api/chatService', () => ({
  createChatConversation: (...args: unknown[]) => createChatConversationMock(...args),
  fetchConversationList: (...args: unknown[]) => fetchConversationListMock(...args),
  fetchConversationMessages: (...args: unknown[]) => fetchConversationMessagesMock(...args),
  startChatStream: (...args: unknown[]) => startChatStreamMock(...args),
  stopChatExecution: (...args: unknown[]) => stopChatExecutionMock(...args)
}))

describe('chat page streaming', () => {
  beforeEach(() => {
    createChatConversationMock.mockReset()
    fetchConversationListMock.mockReset()
    fetchConversationMessagesMock.mockReset()
    startChatStreamMock.mockReset()
    stopChatExecutionMock.mockReset()

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

    fetchConversationMessagesMock.mockImplementation(() => new Promise(() => {}))
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

  it('发送消息后可渲染流式增量并完成', async () => {
    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '请介绍自己' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })

    expect(await screen.findByText('你好')).toBeInTheDocument()
    expect(screen.getAllByText('COMPLETED').length).toBeGreaterThanOrEqual(2)
  })

  it('流式发送异常时展示失败反馈', async () => {
    startChatStreamMock.mockRejectedValueOnce(new Error('stream failed'))
    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '触发失败' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    expect(await screen.findByText('流式消息发送失败')).toBeInTheDocument()
  })

  it('SSE 错误事件时展示错误信息并标记失败', async () => {
    startChatStreamMock.mockImplementationOnce(
      async (
        _input: unknown,
        handlers: {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      ) => {
        handlers.onConnected?.('exec-2')
        handlers.onError?.('节点执行异常')
      }
    )

    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '触发sse错误' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    expect(await screen.findByText('节点执行异常')).toBeInTheDocument()
    expect(screen.getByText('FAILED')).toBeInTheDocument()
  })

  it('executionId 延迟到达时中断请求会在 connected 后补发', async () => {
    let handlersRef:
      | {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      | undefined

    let releaseStream: (() => void) | undefined
    const streamGate = new Promise<void>((resolve) => {
      releaseStream = resolve
    })

    startChatStreamMock.mockImplementationOnce(async (_input: unknown, handlers: typeof handlersRef) => {
      handlersRef = handlers
      await streamGate
    })

    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '先发后停' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })

    fireEvent.click(screen.getByRole('button', { name: '中断' }))

    await screen.findByText('已手动中断')
    expect(screen.getByText('FAILED')).toBeInTheDocument()
    expect(stopChatExecutionMock).not.toHaveBeenCalled()

    handlersRef?.onConnected?.('exec-late')

    await waitFor(() => {
      expect(stopChatExecutionMock).toHaveBeenCalledWith('exec-late')
    })

    await act(async () => {
      releaseStream?.()
      await Promise.resolve()
    })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '中断' })).toBeDisabled()
    })
  })

  it('页面卸载时会中断进行中的流请求', async () => {
    let capturedSignal: AbortSignal | undefined

    startChatStreamMock.mockImplementationOnce(
      async (
        _input: unknown,
        _handlers: {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        },
        signal?: AbortSignal
      ) => {
        capturedSignal = signal
        await new Promise(() => {})
      }
    )

    const { unmount } = render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '触发卸载' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })

    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('快速重复发送时仅允许一个在途流', async () => {
    let releaseStream: (() => void) | undefined
    const streamGate = new Promise<void>((resolve) => {
      releaseStream = resolve
    })

    startChatStreamMock.mockImplementationOnce(async () => {
      await streamGate
    })

    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '第一条' } })

    const sendButton = screen.getByRole('button', { name: '发送' })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(sendButton).toBeDisabled()
    })

    fireEvent.click(sendButton)

    expect(startChatStreamMock).toHaveBeenCalledTimes(1)

    await act(async () => {
      releaseStream?.()
      await Promise.resolve()
    })
  })

  it('中断后旧流晚到 update/finish 不会污染状态', async () => {
    let handlersRef:
      | {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      | undefined

    let releaseStream: (() => void) | undefined
    const streamGate = new Promise<void>((resolve) => {
      releaseStream = resolve
    })

    startChatStreamMock.mockImplementationOnce(async (_input: unknown, handlers: typeof handlersRef) => {
      handlersRef = handlers
      await streamGate
    })

    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))
    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '旧流测试' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })

    fireEvent.click(screen.getByRole('button', { name: '中断' }))

    await screen.findByText('已手动中断')
    expect(screen.getByText('FAILED')).toBeInTheDocument()

    handlersRef?.onDelta?.('旧流晚到增量')
    handlersRef?.onFinish?.()

    expect(screen.queryByText('旧流晚到增量')).not.toBeInTheDocument()
    expect(screen.getByText('FAILED')).toBeInTheDocument()

    await act(async () => {
      releaseStream?.()
      await Promise.resolve()
    })
  })

  it('新旧流交织时旧流回调不会污染新一轮消息', async () => {
    let oldHandlers:
      | {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      | undefined

    let newHandlers:
      | {
          onConnected?: (executionId: string) => void
          onDelta?: (delta: string) => void
          onFinish?: () => void
          onError?: (message: string) => void
        }
      | undefined

    let releaseOld: (() => void) | undefined
    const oldGate = new Promise<void>((resolve) => {
      releaseOld = resolve
    })

    let releaseNew: (() => void) | undefined
    const newGate = new Promise<void>((resolve) => {
      releaseNew = resolve
    })

    startChatStreamMock
      .mockImplementationOnce(async (_input: unknown, handlers: typeof oldHandlers) => {
        oldHandlers = handlers
        await oldGate
      })
      .mockImplementationOnce(async (_input: unknown, handlers: typeof newHandlers) => {
        newHandlers = handlers
        await newGate
      })

    render(<ChatPage />)

    await screen.findByText('默认会话')
    fireEvent.click(screen.getByRole('button', { name: /默认会话/ }))

    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '第一轮' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1)
    })

    fireEvent.click(screen.getByRole('button', { name: '中断' }))
    await screen.findByText('已手动中断')

    fireEvent.change(screen.getByPlaceholderText('输入消息后发送'), { target: { value: '第二轮' } })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(2)
    })

    await act(async () => {
      oldHandlers?.onDelta?.('旧流污染文本')
      oldHandlers?.onFinish?.()
      newHandlers?.onDelta?.('新流有效文本')
      newHandlers?.onFinish?.()
      await Promise.resolve()
    })

    expect(await screen.findByText('新流有效文本')).toBeInTheDocument()

    const messageItems = screen.getAllByRole('listitem')
    const latestMessage = messageItems[messageItems.length - 1]
    expect(within(latestMessage).getByText('ASSISTANT')).toBeInTheDocument()
    expect(within(latestMessage).getByText('新流有效文本')).toBeInTheDocument()
    expect(within(latestMessage).queryByText('旧流污染文本')).not.toBeInTheDocument()
    expect(within(latestMessage).getByText('COMPLETED')).toBeInTheDocument()

    await act(async () => {
      releaseOld?.()
      releaseNew?.()
      await Promise.resolve()
    })
  })

  it('会话列表加载失败时展示错误提示', async () => {
    fetchConversationListMock.mockRejectedValueOnce(new Error('list failed'))
    render(<ChatPage />)

    expect(await screen.findByText('会话列表加载失败')).toBeInTheDocument()
  })
})
