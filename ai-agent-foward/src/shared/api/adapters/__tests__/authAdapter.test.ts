import { describe, expect, it, vi } from 'vitest'
import { login } from '../authAdapter'
import type { ApiClientLike } from '../../client'

describe('authAdapter.login', () => {
  it('将登录响应适配为前端会话模型', async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: {
            token: 'token-1',
            refreshToken: 'refresh-1',
            expireIn: 604800,
            deviceId: 'device-1',
            user: {
              id: 1,
              username: '张三',
              email: 'zhangsan@example.com',
              avatarUrl: null,
              phone: null,
              status: 1,
              createdAt: '2026-02-19T10:00:00'
            }
          }
        }
      })
    }

    const result = await login({ email: 'zhangsan@example.com', password: '12345678' }, client)

    expect(client.post).toHaveBeenCalledWith('/client/user/login', {
      email: 'zhangsan@example.com',
      password: '12345678'
    })

    expect(result.token).toBe('token-1')
    expect(result.refreshToken).toBe('refresh-1')
    expect(result.deviceId).toBe('device-1')
    expect(result.user.username).toBe('张三')
  })

  it('注入 client 时透传上游错误对象', async () => {
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
      get: vi.fn(),
      post: vi.fn().mockRejectedValue(upstreamError)
    }

    await expect(login({ email: 'a@b.com', password: '12345678' }, client)).rejects.toBe(upstreamError)
  })
})
