import { beforeEach, describe, expect, it, vi } from 'vitest'
import { startChatStream } from '../chatService'

function createSseReader(chunks: string[]) {
  const encoder = new TextEncoder()
  let index = 0

  return {
    read: vi.fn(async () => {
      if (index >= chunks.length) {
        return { done: true as const, value: undefined }
      }

      const value = encoder.encode(chunks[index])
      index += 1
      return { done: false as const, value }
    }),
    cancel: vi.fn(async () => undefined),
    releaseLock: vi.fn()
  }
}

describe('chatService startChatStream', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should parse connected/update/finish events', async () => {
    localStorage.setItem('accessToken', 'token-1')

    const reader = createSseReader([
      'event: connected\ndata: {"executionId":"exec-1"}\n\n',
      'event: update\ndata: {"delta":"你好"}\n\n',
      'event: finish\ndata: {"status":"SUCCEEDED"}\n\n'
    ])

    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: {
        getReader: () => reader
      }
    } as unknown as Response)

    const onConnected = vi.fn()
    const onDelta = vi.fn()
    const onFinish = vi.fn()

    await startChatStream(
      {
        agentId: 2,
        userId: 1,
        conversationId: 'conv-1',
        content: 'hello'
      },
      {
        onConnected,
        onDelta,
        onFinish
      }
    )

    expect(fetchMock).toHaveBeenCalled()
    expect(onConnected).toHaveBeenCalledWith('exec-1')
    expect(onDelta).toHaveBeenCalledWith('你好')
    expect(onFinish).toHaveBeenCalledTimes(1)
    expect(reader.cancel).toHaveBeenCalledTimes(1)
    expect(reader.releaseLock).toHaveBeenCalledTimes(1)
  })

  it('should parse payload delta and error events', async () => {
    const reader = createSseReader([
      'event: update\ndata: {"payload":{"delta":"分段"}}\n\n',
      'event: error\ndata: {"message":"节点执行失败"}\n\n'
    ])

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: {
        getReader: () => reader
      }
    } as unknown as Response)

    const onDelta = vi.fn()
    const onError = vi.fn()

    await startChatStream(
      {
        agentId: 2,
        userId: 1,
        conversationId: 'conv-1',
        content: 'hello'
      },
      {
        onDelta,
        onError
      }
    )

    expect(onDelta).toHaveBeenCalledWith('分段')
    expect(onError).toHaveBeenCalledWith('节点执行失败')
  })

  it('should throw when stream start failed', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      body: null
    } as unknown as Response)

    await expect(
      startChatStream(
        {
          agentId: 2,
          userId: 1,
          conversationId: 'conv-1',
          content: 'hello'
        },
        {}
      )
    ).rejects.toThrow('启动流式会话失败')
  })
})
