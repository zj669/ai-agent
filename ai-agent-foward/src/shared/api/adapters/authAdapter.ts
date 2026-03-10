import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

interface LoginRequest {
  email: string
  password: string
}

interface LoginUserDTO {
  id: number
  username: string
  email: string
  avatarUrl: string | null
  phone: string | null
  status: number
  createdAt: string
}

interface LoginDataDTO {
  token: string
  refreshToken: string
  expireIn: number
  deviceId: string
  user: LoginUserDTO
}

export interface AuthSession {
  token: string
  refreshToken: string
  expireIn: number
  deviceId: string
  user: LoginUserDTO
}

export async function login(input: LoginRequest, client: ApiClientLike = apiClient): Promise<AuthSession> {
  const response = await client.post<ApiResponse<LoginDataDTO>>('/client/user/login', input)
  const session = unwrapResponse(response)
  if (session.user) {
    localStorage.setItem('userInfo', JSON.stringify(session.user))
  }
  return session
}

export function getSavedUserInfo(): LoginUserDTO | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function clearSavedUserInfo(): void {
  localStorage.removeItem('userInfo')
}

export async function sendEmailCode(email: string, client: ApiClientLike = apiClient): Promise<void> {
  const response = await client.post<ApiResponse<null>>('/client/user/email/sendCode', { email })
  unwrapResponse(response)
}

interface RegisterRequest {
  email: string
  code: string
  password: string
  username?: string
}

export async function register(input: RegisterRequest, client: ApiClientLike = apiClient): Promise<AuthSession> {
  const response = await client.post<ApiResponse<LoginDataDTO>>('/client/user/email/register', input)
  return unwrapResponse(response)
}
