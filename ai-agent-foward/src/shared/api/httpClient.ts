import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { mapApiError } from './errorMapper'

export interface HttpClientLike {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
}

export function createHttpClient(baseURL = '/'): AxiosInstance {
  const instance = axios.create({
    baseURL,
    timeout: 10000
  })

  instance.interceptors.request.use((config) => {
    config.headers.set('debug-user', '1')
    return config
  })

  instance.interceptors.response.use(
    (response) => response,
    (error: unknown) => Promise.reject(mapApiError(error))
  )

  return instance
}

export const httpClient = createHttpClient('/')
