import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';

const apiHost = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '');
const apiBaseURL = apiHost ? `${apiHost}/api` : '/api';

const apiClient = axios.create({
  baseURL: apiBaseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add token to request headers
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

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.response) {
      const { status } = error.response;

      switch (status) {
        case 401:
          // Unauthorized - redirect to login
          message.error('登录已过期，请重新登录');

          // Clear auth data
          localStorage.removeItem('auth_token');
          localStorage.removeItem('auth_user');
          localStorage.removeItem('auth_expire_at');

          // Save current path for redirect after login
          const currentPath = window.location.pathname;
          if (currentPath !== '/login') {
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
          }
          break;

        case 403:
          message.error('没有权限访问该资源');
          break;

        case 404:
          message.error('请求的资源不存在');
          break;

        case 500:
          message.error('服务器错误，请稍后重试');
          break;

        default:
          // Try to get error message from response
          const errorData = error.response.data as { message?: string };
          message.error(errorData?.message || '请求失败，请稍后重试');
      }
    } else if (error.request) {
      // Network error
      message.error('网络错误，请检查网络连接');
    } else {
      message.error('请求配置错误');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
