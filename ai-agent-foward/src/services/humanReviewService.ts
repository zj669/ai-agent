import apiClient from './apiClient';
import type {
  HumanReviewTask,
  ReviewDetail,
  ResumeExecutionRequest
} from '../types/humanReview';

/**
 * 人工审核服务
 * 对应后端 HumanReviewController: /api/workflow/reviews
 */
class HumanReviewService {
  /**
   * 获取待审核列表
   * GET /api/workflow/reviews/pending
   */
  async listTasks(_status?: string): Promise<HumanReviewTask[]> {
    const response = await apiClient.get<any[]>('/workflow/reviews/pending');
    const pendingList = response.data || [];
    // 将后端 PendingReviewDTO 转换为前端 HumanReviewTask
    return pendingList.map((item: any) => ({
      taskId: item.executionId, // 用 executionId 作为唯一标识
      executionId: item.executionId,
      nodeId: item.nodeId,
      nodeName: item.nodeName || item.nodeId,
      agentName: item.agentName || '',
      status: 'PENDING' as const,
      triggerPhase: item.triggerPhase || 'AFTER_EXECUTION',
      createdAt: item.pausedAt || new Date().toISOString()
    }));
  }

  /**
   * 获取审核详情
   * GET /api/workflow/reviews/{executionId}
   */
  async getTaskDetail(executionId: string): Promise<ReviewDetail> {
    const response = await apiClient.get<ReviewDetail>(
      `/workflow/reviews/${executionId}`
    );
    return response.data;
  }

  /**
   * 批准审核（恢复执行）
   * POST /api/workflow/reviews/resume
   */
  async approveTask(executionId: string, comment: string, nodeId?: string, edits?: Record<string, any>): Promise<void> {
    const request: ResumeExecutionRequest = {
      executionId,
      nodeId: nodeId || '',
      edits,
      comment
    };
    await apiClient.post('/workflow/reviews/resume', request);
  }

  /**
   * 拒绝审核 - 后端没有独立的 reject 端点，通过 resume 传递拒绝信息
   * 实际上后端只有 resume，前端可以在 comment 中标注拒绝
   */
  async rejectTask(executionId: string, comment: string, nodeId?: string): Promise<void> {
    // 后端只有 resume 端点，拒绝时不传 edits，在 comment 中标注
    const request: ResumeExecutionRequest = {
      executionId,
      nodeId: nodeId || '',
      comment: `[拒绝] ${comment}`
    };
    await apiClient.post('/workflow/reviews/resume', request);
  }
}

export const humanReviewService = new HumanReviewService();
