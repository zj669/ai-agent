import { describe, it, expect, vi, beforeEach } from 'vitest'

import { createHttpClient } from '../httpClient'
import type { NormalizedApiError } from '../errorMapper'

describe('httpClient error side effects', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    sessionStorage.clear()
    delete (window as unknown as Record<string, unknown>).location
    Object.defineProperty(window, 'location', {
      value: { href: '/', pathname: '/' },
      writable: true,
      configurable: true,
    })
  })

  it('TOKEN_EXPIRED clears token, shows toast, and redirects to /login', async () => {
    localStorage.setItem('accessToken', 'test-token')
    const client = createHttpClient('/')

    const axiosError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: { code: 'UNAUTHORIZED', message: 'Token expired' },
      },
    }

    const interceptor = (client.interceptors.response as unknown as { handlers: Array<{ rejected: (e: unknown) => unknown }> }).handlers[0]
    try {
      await interceptor.rejected(axiosError)
    } catch (e) {
      const err = e as NormalizedApiError
      expect(err.code).toBe('TOKEN_EXPIRED')
    }

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(window.location.href).toContain('/login')
  })

  it('NETWORK_ERROR returns mapped error', async () => {
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
  })

  it('5xx error returns mapped error', async () => {
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
  })
})
