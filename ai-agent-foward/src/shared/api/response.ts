import type { AxiosResponse } from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>): T {
  return response.data.data
}
