import { describe, expect, it } from 'vitest'
import { mapApiError } from '../errorMapper'

describe('mapApiError', () => {
  it('将后端 401 + UNAUTHORIZED 映射为 TOKEN_EXPIRED', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: 'UNAUTHORIZED',
          message: 'Token 无效或已过期'
        }
      }
    }

    const mapped = mapApiError(error)

    expect(mapped.code).toBe('TOKEN_EXPIRED')
    expect(mapped.status).toBe(401)
    expect(mapped.rawCode).toBe('UNAUTHORIZED')
    expect(mapped.message).toBe('Token 无效或已过期')
  })

  it('将后端 401 + INVALID_CREDENTIALS 映射为 UNAUTHORIZED', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: 'INVALID_CREDENTIALS',
          message: '用户名或密码错误'
        }
      }
    }

    const mapped = mapApiError(error)

    expect(mapped.code).toBe('UNAUTHORIZED')
    expect(mapped.status).toBe(401)
    expect(mapped.rawCode).toBe('INVALID_CREDENTIALS')
  })

  it('将超时错误映射为 TIMEOUT', () => {
    const error = {
      isAxiosError: true,
      code: 'ECONNABORTED',
      message: 'timeout'
    }

    const mapped = mapApiError(error)

    expect(mapped.code).toBe('TIMEOUT')
  })

  it('已规范化错误再次映射时保持原值', () => {
    const normalized = {
      code: 'TOKEN_EXPIRED' as const,
      message: 'Token 无效或已过期',
      status: 401,
      rawCode: 'UNAUTHORIZED'
    }

    const mapped = mapApiError(normalized)

    expect(mapped).toEqual(normalized)
  })

  it('将未知异常映射为 UNKNOWN_ERROR', () => {
    const mapped = mapApiError(new Error('boom'))

    expect(mapped.code).toBe('UNKNOWN_ERROR')
    expect(mapped.message).toBe('boom')
  })
})
