import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock dependencies before importing httpClient
vi.mock('../../feedback/toast', () => ({
  showToast: vi.fn(),
}))

vi.mock('../../../app/auth', () => ({
  clearAccessToken: vi.fn(),
}))

import { createHttpClient } from '../httpClient'
import { showToast } from '../../feedback/toast'
import { clearAccessToken } from '../../../app/auth'
import type { NormalizedApiError } from '../errorMapper'

describe('httpClient error side effects', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    delete (window as unknown as Record<string, unknown>).location
    Object.defineProperty(window, 'location', {
      value: { href: '/' },
      writable: true,
      configurable: true,
    })
  })

  it('TOKEN_EXPIRED clears token, shows toast, and redirects to /login', async () => {
    const client = createHttpClient('/')

    // Simulate a 401 UNAUTHORIZED response
    const axiosError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: { code: 'UNAUTHORIZED', message: 'Token expired' },
      },
    }

    // Manually trigger the error interceptor
    const interceptor = (client.interceptors.response as unknown as { handlers: Array<{ rejected: (e: unknown) => unknown }> }).handlers[0]
    try {
      await interceptor.rejected(axiosError)
    } catch (e) {
      const err = e as NormalizedApiError
      expect(err.code).toBe('TOKEN_EXPIRED')
    }

    expect(clearAccessToken).toHaveBeenCalled()
    expect(showToast).toHaveBeenCalledWith('登录已过期，请重新登录', 'error')
    expect(window.location.href).toBe('/login')
  })

  it('NETWORK_ERROR shows warning toast', async () => {
    const client = createHttpClient('/')

    const axiosError = {
      isAxiosError: true,
      message: 'Network Error',
    }

    const interceptor = (client.interceptors.response as unknown as { handlers: Array<{ rejected: (e: unknown) => unknown }> }).handlers[0]
    try {
      await interceptor.rejected(axiosError)
    } catch (e) {
      const err = e as NormalizedApiError
      expect(err.code).toBe('NETWORK_ERROR')
    }

    expect(showToast).toHaveBeenCalledWith('网络错误，请检查网络连接', 'warning')
  })

  it('5xx error shows system error toast', async () => {
    const client = createHttpClient('/')

    const axiosError = {
      isAxiosError: true,
      response: {
        status: 500,
        data: { code: 'INTERNAL_ERROR', message: 'Server error' },
      },
    }

    const interceptor = (client.interceptors.response as unknown as { handlers: Array<{ rejected: (e: unknown) => unknown }> }).handlers[0]
    try {
      await interceptor.rejected(axiosError)
    } catch (e) {
      const err = e as NormalizedApiError
      expect(err.code).toBe('UNKNOWN_ERROR')
    }

    expect(showToast).toHaveBeenCalledWith('系统错误，请稍后再试', 'error')
  })
})
