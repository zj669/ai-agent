package com.zj.aiagent.infrastructure.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Dashboard 统计数据 Mapper
 */
@Mapper
public interface DashboardMapper {
    
    /**
     * 获取用户的 Agent 数量
     */
    Long countAgentsByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的对话数量
     */
    Long countConversationsByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的知识库数量
     */
    Long countKnowledgeDatasetsByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的执行统计数据
     * 返回 Map 包含: totalExecutions, successfulExecutions, failedExecutions, avgResponseTime
     */
    Map<String, Object> getExecutionStatsByUserId(@Param("userId") Long userId);
}
