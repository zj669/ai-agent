package com.zj.aiagent.application.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard 统计数据响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    
    /**
     * Agent 数量
     */
    private Long agentCount;
    
    /**
     * 工作流数量（等于 agentCount）
     */
    private Long workflowCount;
    
    /**
     * 对话数量
     */
    private Long conversationCount;
    
    /**
     * 知识库数量
     */
    private Long knowledgeDatasetCount;
    
    /**
     * 总执行次数
     */
    private Long totalExecutions;
    
    /**
     * 成功执行次数
     */
    private Long successfulExecutions;
    
    /**
     * 失败执行次数
     */
    private Long failedExecutions;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;
}
