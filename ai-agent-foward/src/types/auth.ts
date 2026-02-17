export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  status: number;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  deviceId?: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  code: string;
  password: string;
  username?: string;
  deviceId?: string;
}

export interface SendEmailCodeRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ModifyUserRequest {
  username?: string;
  avatarUrl?: string;
  phone?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  expireIn: number; // seconds
  deviceId: string;
  user: User;
}

export interface TokenRefreshRequest {
  refreshToken: string;
  deviceId: string;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // seconds
  tokenType: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
