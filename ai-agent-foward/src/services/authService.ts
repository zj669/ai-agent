import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { message as antdMessage } from 'antd';
import {
  LoginRequest,
  RegisterRequest,
  SendEmailCodeRequest,
  ResetPasswordRequest,
  ModifyUserRequest,
  LoginResponse,
  TokenRefreshRequest,
  TokenRefreshResponse,
  User,
  ApiResponse
} from '../types/auth';

// Auth 专用 axios 实例，baseURL 为 /client（后端 UserController 路径前缀）
const authClient: AxiosInstance = axios.create({
  baseURL: '/client',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 防止并发刷新 Token
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// 请求拦截器：自动添加 Token
authClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('auth_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：处理 401 和自动刷新 Token
authClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      if (originalRequest.url?.includes('/refresh')) {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_refresh_token');
        localStorage.removeItem('auth_device_id');
        localStorage.removeItem('auth_user');
        localStorage.removeItem('auth_expire_at');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return authClient(originalRequest);
          })
          .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('auth_refresh_token');
      const deviceId = localStorage.getItem('auth_device_id');

      if (!refreshToken || !deviceId) {
        isRefreshing = false;
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_refresh_token');
        localStorage.removeItem('auth_device_id');
        localStorage.removeItem('auth_user');
        localStorage.removeItem('auth_expire_at');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const response = await axios.post('/client/user/refresh', { refreshToken, deviceId });
        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data;
        const expireAt = Date.now() + expiresIn * 1000;

        localStorage.setItem('auth_token', accessToken);
        localStorage.setItem('auth_refresh_token', newRefreshToken);
        localStorage.setItem('auth_expire_at', expireAt.toString());

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        processQueue(null, accessToken);
        isRefreshing = false;
        return authClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);
        isRefreshing = false;
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_refresh_token');
        localStorage.removeItem('auth_device_id');
        localStorage.removeItem('auth_user');
        localStorage.removeItem('auth_expire_at');
        antdMessage.error('登录已过期，请重新登录');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    // 对于登录和注册接口，不在拦截器中显示错误消息，让调用方处理
    const isAuthEndpoint = originalRequest?.url?.includes('/login') ||
                          originalRequest?.url?.includes('/register') ||
                          originalRequest?.url?.includes('/sendCode');

    if (!isAuthEndpoint) {
      if (error.response) {
        const { status, data } = error.response as any;
        switch (status) {
          case 400:
            antdMessage.error(data?.message || '请求参数错误');
            break;
          case 403:
            antdMessage.error('没有权限访问');
            break;
          case 404:
            antdMessage.error('请求的资源不存在');
            break;
          case 500:
            antdMessage.error('服务器错误，请稍后重试');
            break;
          default:
            antdMessage.error(data?.message || '请求失败');
        }
      } else if (error.request) {
        antdMessage.error('网络错误，请检查网络连接');
      } else {
        antdMessage.error('请求失败，请稍后重试');
      }
    }

    return Promise.reject(error);
  }
);

class AuthService {
  private readonly baseURL = '/user';

  constructor() {
    // 使用配置好的 axios 实例，自动处理 Token 和刷新
  }

  async sendEmailCode(data: SendEmailCodeRequest): Promise<void> {
    await authClient.post<ApiResponse<void>>(`${this.baseURL}/email/sendCode`, data);
  }

  async register(data: RegisterRequest): Promise<LoginResponse> {
    const response = await authClient.post<ApiResponse<LoginResponse>>(`${this.baseURL}/email/register`, data);
    return response.data.data;
  }

  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await authClient.post<ApiResponse<LoginResponse>>(`${this.baseURL}/login`, data);
    return response.data.data;
  }

  async getUserInfo(): Promise<User> {
    // Token 由拦截器自动添加
    const response = await authClient.get<ApiResponse<User>>(`${this.baseURL}/info`);
    return response.data.data;
  }

  async updateProfile(data: ModifyUserRequest): Promise<User> {
    // Token 由拦截器自动添加
    const response = await authClient.post<ApiResponse<User>>(`${this.baseURL}/profile`, data);
    return response.data.data;
  }

  async logout(token: string, deviceId?: string | null): Promise<void> {
    // Token 由拦截器自动添加
    await authClient.post<ApiResponse<void>>(`${this.baseURL}/logout`, { token, deviceId });
  }

  async refreshToken(data: TokenRefreshRequest): Promise<TokenRefreshResponse> {
    const response = await authClient.post<ApiResponse<TokenRefreshResponse>>(`${this.baseURL}/refresh`, data);
    return response.data.data;
  }

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await authClient.post<ApiResponse<void>>(`${this.baseURL}/resetPassword`, data);
  }
}

export const authService = new AuthService();
