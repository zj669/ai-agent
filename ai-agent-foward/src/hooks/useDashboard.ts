import { useState, useEffect } from 'react';
import { message } from 'antd';
import { dashboardService } from '../services/dashboardService';
import type { DashboardStats } from '../types/dashboard';

export const useDashboard = () => {
  const [stats, setStats] = useState<DashboardStats>({
    agentCount: 0,
    workflowCount: 0,
    conversationCount: 0,
    knowledgeDatasetCount: 0,
    totalExecutions: 0,
    successfulExecutions: 0,
    failedExecutions: 0,
    avgResponseTime: 0
  });
  const [loading, setLoading] = useState(false);

  const loadStats = async () => {
    setLoading(true);
    try {
      const data = await dashboardService.getStats();
      setStats(data);
    } catch (error: any) {
      message.error('加载统计数据失败');
      console.error('Failed to load dashboard stats:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStats();

    // Auto refresh every 30 seconds
    const interval = setInterval(loadStats, 30000);
    return () => clearInterval(interval);
  }, []);

  return {
    stats,
    loading,
    refresh: loadStats
  };
};
