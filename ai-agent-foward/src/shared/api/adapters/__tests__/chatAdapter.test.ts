import { describe, expect, it, vi } from 'vitest'
import {
  createConversation,
  getConversationList,
  getConversationMessages,
  stopWorkflowExecution,
  type ConversationListData,
  type MessageDTO
} from '../chatAdapter'
import type { ApiClientLike } from '../../client'

describe('chatAdapter', () => {
  it('createConversation should call chat conversations endpoint', async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: 'conv-1'
        }
      })
    }

    const result = await createConversation({ userId: 1, agentId: 2 }, client)

    expect(client.post).toHaveBeenCalledWith('/api/chat/conversations', undefined, {
      params: {
        userId: 1,
        agentId: 2
      }
    })
    expect(result).toBe('conv-1')
  })

  it('getConversationList should unwrap list response', async () => {
    const payload: ConversationListData = {
      total: 1,
      pages: 1,
      list: [
        {
          id: 'conv-1',
          userId: '1',
          agentId: '2',
          title: 'New Chat',
          createdAt: '2026-02-21T00:00:00',
          updatedAt: '2026-02-21T00:01:00'
        }
      ]
    }

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: payload
        }
      }),
      post: vi.fn()
    }

    const result = await getConversationList({ userId: 1, agentId: 2 }, client)

    expect(client.get).toHaveBeenCalledWith('/api/chat/conversations', {
      params: {
        userId: 1,
        agentId: 2,
        page: 1,
        size: 20
      }
    })
    expect(result).toEqual(payload)
  })

  it('getConversationMessages should unwrap message array', async () => {
    const messages: MessageDTO[] = [
      {
        id: 'm1',
        conversationId: 'conv-1',
        role: 'USER',
        content: 'hello',
        status: 'COMPLETED',
        createdAt: '2026-02-21T00:00:00'
      }
    ]

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: messages
        }
      }),
      post: vi.fn()
    }

    const result = await getConversationMessages({ userId: 1, conversationId: 'conv-1' }, client)

    expect(client.get).toHaveBeenCalledWith('/api/chat/conversations/conv-1/messages', {
      params: {
        userId: 1,
        page: 1,
        size: 50,
        order: 'asc'
      }
    })
    expect(result).toEqual(messages)
  })

  it('stopWorkflowExecution should call stop endpoint', async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: null
        }
      })
    }

    await stopWorkflowExecution({ executionId: 'exec-1' }, client)

    expect(client.post).toHaveBeenCalledWith('/api/workflow/execution/stop', { executionId: 'exec-1' })
  })
})
