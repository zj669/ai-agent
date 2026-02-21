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
  return unwrapResponse(response)
}
