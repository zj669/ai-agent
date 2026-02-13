import { useState, useEffect } from 'react';
import { message } from 'antd';
import { humanReviewService } from '../services/humanReviewService';
import { HumanReviewTask } from '../types/humanReview';

export const useHumanReview = () => {
  const [tasks, setTasks] = useState<HumanReviewTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);

  const loadTasks = async (status?: string) => {
    setLoading(true);
    try {
      const data = await humanReviewService.listTasks(status);
      setTasks(data);
    } catch (error: any) {
      message.error(error.response?.data?.message || '获取审核任务列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasks(statusFilter);
  }, [statusFilter]);

  const approveTask = async (taskId: string, comment: string) => {
    try {
      await humanReviewService.approveTask(taskId, comment);
      message.success('任务已批准');
      await loadTasks(statusFilter);
    } catch (error: any) {
      message.error(error.response?.data?.message || '批准任务失败');
      throw error;
    }
  };

  const rejectTask = async (taskId: string, comment: string) => {
    try {
      await humanReviewService.rejectTask(taskId, comment);
      message.success('任务已拒绝');
      await loadTasks(statusFilter);
    } catch (error: any) {
      message.error(error.response?.data?.message || '拒绝任务失败');
      throw error;
    }
  };

  const refreshTasks = () => {
    loadTasks(statusFilter);
  };

  return {
    tasks,
    loading,
    statusFilter,
    setStatusFilter,
    loadTasks,
    approveTask,
    rejectTask,
    refreshTasks
  };
};
