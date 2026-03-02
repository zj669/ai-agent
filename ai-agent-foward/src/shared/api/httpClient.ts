import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { mapApiError } from './errorMapper'

export interface HttpClientLike {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
  delete?<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
}

export function createHttpClient(baseURL = '/'): AxiosInstance {
  const instance = axios.create({
    baseURL,
    timeout: 10000
  })

  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  })

  instance.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      const mapped = mapApiError(error)
      // 401 自动清除 token 并跳转登录页
      if (mapped.code === 'TOKEN_EXPIRED' || mapped.code === 'UNAUTHORIZED') {
        localStorage.removeItem('accessToken')
        sessionStorage.removeItem('accessToken')
        const currentPath = window.location.pathname
        if (currentPath !== '/login' && currentPath !== '/register') {
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
        }
      }
      return Promise.reject(mapped)
    }
  )

  return instance
}

export const httpClient = createHttpClient('/')
