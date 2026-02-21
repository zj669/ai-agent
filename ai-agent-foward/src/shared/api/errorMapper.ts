export type NormalizedErrorCode =
  | 'TOKEN_EXPIRED'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'RATE_LIMITED'
  | 'TIMEOUT'
  | 'NETWORK_ERROR'
  | 'UNKNOWN_ERROR'

export interface NormalizedApiError {
  code: NormalizedErrorCode
  message: string
  status?: number
  rawCode?: string
}

interface ErrorPayload {
  code?: string
  message?: string
}

const NORMALIZED_ERROR_CODES: ReadonlySet<NormalizedErrorCode> = new Set([
  'TOKEN_EXPIRED',
  'UNAUTHORIZED',
  'FORBIDDEN',
  'RATE_LIMITED',
  'TIMEOUT',
  'NETWORK_ERROR',
  'UNKNOWN_ERROR'
])

interface AxiosLikeError {
  isAxiosError?: boolean
  code?: string
  message?: string
  response?: {
    status?: number
    data?: ErrorPayload
  }
}

export function mapApiError(error: unknown): NormalizedApiError {
  if (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    'message' in error &&
    typeof (error as { code?: unknown }).code === 'string' &&
    typeof (error as { message?: unknown }).message === 'string' &&
    NORMALIZED_ERROR_CODES.has((error as { code: NormalizedErrorCode }).code)
  ) {
    return error as NormalizedApiError
  }

  const err = error as AxiosLikeError

  if (err?.isAxiosError) {
    if (err.code === 'ECONNABORTED') {
      return {
        code: 'TIMEOUT',
        message: '请求超时'
      }
    }

    const status = err.response?.status
    const rawCode = err.response?.data?.code
    const message = err.response?.data?.message ?? err.message ?? '请求失败'

    if (status === 401) {
      if (rawCode === 'UNAUTHORIZED') {
        return {
          code: 'TOKEN_EXPIRED',
          status,
          rawCode,
          message
        }
      }

      return {
        code: 'UNAUTHORIZED',
        status,
        rawCode,
        message
      }
    }

    if (status === 403) {
      return {
        code: 'FORBIDDEN',
        status,
        rawCode,
        message
      }
    }

    if (status === 429) {
      return {
        code: 'RATE_LIMITED',
        status,
        rawCode,
        message
      }
    }

    if (status === undefined) {
      return {
        code: 'NETWORK_ERROR',
        message
      }
    }

    return {
      code: 'UNKNOWN_ERROR',
      status,
      rawCode,
      message
    }
  }

  if (error instanceof Error) {
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message
    }
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: '未知错误'
  }
}
