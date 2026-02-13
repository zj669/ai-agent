import { create } from 'zustand';
import { User, LoginRequest, RegisterRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  deviceId: string | null;
  tokenExpireAt: number | null; // timestamp in milliseconds
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
  checkTokenExpiration: () => boolean;
  refreshAccessToken: () => Promise<void>;
  initializeAuth: () => void;
}

const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';
const DEVICE_ID_KEY = 'auth_device_id';
const USER_KEY = 'auth_user';
const EXPIRE_AT_KEY = 'auth_expire_at';

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: null,
  refreshToken: null,
  deviceId: null,
  tokenExpireAt: null,
  isAuthenticated: false,
  isLoading: false,

  login: async (data: LoginRequest) => {
    set({ isLoading: true });
    try {
      const response = await authService.login(data);
      const expireAt = Date.now() + response.expireIn * 1000;

      // Save to localStorage
      localStorage.setItem(TOKEN_KEY, response.token);
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
      localStorage.setItem(DEVICE_ID_KEY, response.deviceId);
      localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      localStorage.setItem(EXPIRE_AT_KEY, expireAt.toString());

      set({
        user: response.user,
        token: response.token,
        refreshToken: response.refreshToken,
        deviceId: response.deviceId,
        tokenExpireAt: expireAt,
        isAuthenticated: true,
        isLoading: false
      });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  register: async (data: RegisterRequest) => {
    set({ isLoading: true });
    try {
      const response = await authService.register(data);
      const expireAt = Date.now() + response.expireIn * 1000;

      // Save to localStorage
      localStorage.setItem(TOKEN_KEY, response.token);
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
      localStorage.setItem(DEVICE_ID_KEY, response.deviceId);
      localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      localStorage.setItem(EXPIRE_AT_KEY, expireAt.toString());

      set({
        user: response.user,
        token: response.token,
        refreshToken: response.refreshToken,
        deviceId: response.deviceId,
        tokenExpireAt: expireAt,
        isAuthenticated: true,
        isLoading: false
      });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  logout: async () => {
    const { token, deviceId } = get();
    if (token) {
      try {
        await authService.logout(token, deviceId);
      } catch (error) {
        console.error('Logout error:', error);
      }
    }

    // Clear localStorage
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(DEVICE_ID_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(EXPIRE_AT_KEY);

    set({
      user: null,
      token: null,
      refreshToken: null,
      deviceId: null,
      tokenExpireAt: null,
      isAuthenticated: false
    });
  },

  updateUser: (user: User) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ user });
  },

  checkTokenExpiration: () => {
    const { tokenExpireAt } = get();
    if (!tokenExpireAt) return false;

    const now = Date.now();
    const timeLeft = tokenExpireAt - now;

    // Token expired
    if (timeLeft <= 0) {
      get().logout();
      return false;
    }

    // Token will expire in 5 minutes, trigger auto refresh
    if (timeLeft <= 5 * 60 * 1000) {
      get().refreshAccessToken().catch(() => {
        // Refresh failed, logout
        get().logout();
      });
      return true;
    }

    return false;
  },

  refreshAccessToken: async () => {
    const { refreshToken, deviceId } = get();
    if (!refreshToken || !deviceId) {
      throw new Error('No refresh token or device ID');
    }

    try {
      const response = await authService.refreshToken({ refreshToken, deviceId });
      const expireAt = Date.now() + response.expiresIn * 1000;

      // Update localStorage
      localStorage.setItem(TOKEN_KEY, response.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
      localStorage.setItem(EXPIRE_AT_KEY, expireAt.toString());

      set({
        token: response.accessToken,
        refreshToken: response.refreshToken,
        tokenExpireAt: expireAt
      });
    } catch (error) {
      console.error('Token refresh failed:', error);
      throw error;
    }
  },

  initializeAuth: () => {
    const token = localStorage.getItem(TOKEN_KEY);
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    const deviceId = localStorage.getItem(DEVICE_ID_KEY);
    const userStr = localStorage.getItem(USER_KEY);
    const expireAtStr = localStorage.getItem(EXPIRE_AT_KEY);

    if (token && userStr && expireAtStr) {
      const user = JSON.parse(userStr) as User;
      const expireAt = parseInt(expireAtStr, 10);

      // Check if token is expired
      if (Date.now() < expireAt) {
        set({
          user,
          token,
          refreshToken,
          deviceId,
          tokenExpireAt: expireAt,
          isAuthenticated: true
        });
      } else {
        // Token expired, try to refresh
        if (refreshToken && deviceId) {
          get().refreshAccessToken().catch(() => {
            // Refresh failed, clear storage
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(REFRESH_TOKEN_KEY);
            localStorage.removeItem(DEVICE_ID_KEY);
            localStorage.removeItem(USER_KEY);
            localStorage.removeItem(EXPIRE_AT_KEY);
          });
        } else {
          // No refresh token, clear storage
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(REFRESH_TOKEN_KEY);
          localStorage.removeItem(DEVICE_ID_KEY);
          localStorage.removeItem(USER_KEY);
          localStorage.removeItem(EXPIRE_AT_KEY);
        }
      }
    }
  }
}));
