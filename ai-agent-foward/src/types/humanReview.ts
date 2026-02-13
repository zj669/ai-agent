/**
 * 人工审核相关类型定义
 * 对应后端 HumanReviewDTO
 */

// 待审核任务（对应后端 PendingReviewDTO）
export interface HumanReviewTask {
  // 前端用 executionId 作为唯一标识（后端没有独立 taskId）
  taskId: string;
  executionId: string;
  nodeId: string;
  nodeName: string;
  agentName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  triggerPhase: 'BEFORE_EXECUTION' | 'AFTER_EXECUTION';
  inputData?: any;
  outputData?: any;
  reviewerId?: number;
  reviewComment?: string;
  createdAt: string;
  reviewedAt?: string;
}

// 审核详情（对应后端 ReviewDetailDTO）
export interface ReviewDetail {
  executionId: string;
  nodeId: string;
  nodeName: string;
  triggerPhase: 'BEFORE_EXECUTION' | 'AFTER_EXECUTION';
  contextData: Record<string, any>;
  config: {
    prompt?: string;
    editableFields?: string[];
  };
}

// 提交审核请求（对应后端 ResumeExecutionRequest）
export interface ResumeExecutionRequest {
  executionId: string;
  nodeId: string;
  edits?: Record<string, any>;
  comment?: string;
}
