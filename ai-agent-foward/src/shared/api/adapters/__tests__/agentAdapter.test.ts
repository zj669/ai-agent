import { describe, expect, it, vi } from 'vitest'
import {
  createAgent,
  getAgentDetail,
  getAgentList,
  publishAgent,
  updateAgent,
  type AgentDetail,
  type AgentSummary
} from '../agentAdapter'
import type { ApiClientLike } from '../../client'

describe('agentAdapter', () => {
  it('getAgentList 应该解包后端响应', async () => {
    const data: AgentSummary[] = [
      {
        id: 1001,
        userId: 1,
        name: '客服助手',
        description: '智能客服对话机器人',
        icon: 'robot',
        status: 'PUBLISHED',
        publishedVersionId: 5,
        updateTime: '2026-02-18T10:30:00'
      }
    ]

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data
        }
      }),
      post: vi.fn()
    }

    const result = await getAgentList(client)

    expect(client.get).toHaveBeenCalledWith('/api/agent/list')
    expect(result).toEqual(data)
  })

  it('getAgentDetail 应该解包后端响应', async () => {
    const data: AgentDetail = {
      id: 1001,
      name: '客服助手',
      description: '智能客服对话机器人',
      icon: 'robot',
      graphJson: '{"nodes":[]}',
      version: 3,
      publishedVersionId: 5,
      status: 1
    }

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data
        }
      }),
      post: vi.fn()
    }

    const result = await getAgentDetail(1001, client)

    expect(client.get).toHaveBeenCalledWith('/api/agent/1001')
    expect(result).toEqual(data)
  })

  it('createAgent 应该调用 POST /api/agent/create 并解包响应', async () => {
    const payload = {
      name: '未命名 Agent',
      description: '新建测试'
    }

    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: 2001
        }
      })
    }

    const result = await createAgent(payload, client)

    expect(client.post).toHaveBeenCalledWith('/api/agent/create', payload)
    expect(result).toBe(2001)
  })

  it('updateAgent 应该调用 POST /api/agent/update', async () => {
    const payload = {
      id: 1001,
      name: '客服助手',
      description: '更新描述',
      icon: 'robot',
      version: 3,
      graphJson: '{"nodes":[]}'
    }

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

    await updateAgent(payload, client)

    expect(client.post).toHaveBeenCalledWith('/api/agent/update', payload)
  })

  it('publishAgent 应该调用 POST /api/agent/publish', async () => {
    const payload = { id: 1001 }

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

    await publishAgent(payload, client)

    expect(client.post).toHaveBeenCalledWith('/api/agent/publish', payload)
  })

  it('createAgent 注入 client 时透传上游错误对象', async () => {
    const payload = {
      name: '未命名 Agent'
    }

    const upstreamError = {
      isAxiosError: true,
      response: {
        status: 500,
        data: {
          code: 'INTERNAL_ERROR',
          message: '创建失败'
        }
      }
    }

    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockRejectedValue(upstreamError)
    }

    await expect(createAgent(payload, client)).rejects.toBe(upstreamError)
  })

  it('getAgentList 注入 client 时透传上游错误对象', async () => {
    const upstreamError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: 'UNAUTHORIZED',
          message: 'Token 无效或已过期'
        }
      }
    }

    const client: ApiClientLike = {
      get: vi.fn().mockRejectedValue(upstreamError),
      post: vi.fn()
    }

    await expect(getAgentList(client)).rejects.toBe(upstreamError)
  })
})
