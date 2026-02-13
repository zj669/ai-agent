import apiClient from './apiClient';
import type { ApiResponse } from '../types/auth';
import type { DashboardData, DashboardStats } from '../types/dashboard';

class DashboardService {
  // Get dashboard statistics
  async getStats(): Promise<DashboardStats> {
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats');
    return response.data.data;
  }

  // Get dashboard data (stats + trends + recent executions)
  async getDashboardData(): Promise<DashboardData> {
    // For now, only return stats. Trends and recent executions can be added later
    const stats = await this.getStats();
    return {
      stats,
      executionTrend: [],
      recentExecutions: []
    };
  }
}

export const dashboardService = new DashboardService();
