package com.zj.aiagent.infrastructure.dashboard.repository;

import com.zj.aiagent.domain.dashboard.repository.DashboardRepository;
import com.zj.aiagent.domain.dashboard.valobj.DashboardStats;
import com.zj.aiagent.infrastructure.dashboard.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Dashboard Repository 实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepository {
    
    private final DashboardMapper dashboardMapper;
    
    @Override
    public DashboardStats getStatsByUserId(Long userId) {
        // 获取各项统计数据
        Long agentCount = dashboardMapper.countAgentsByUserId(userId);
        Long conversationCount = dashboardMapper.countConversationsByUserId(userId);
        Long knowledgeDatasetCount = dashboardMapper.countKnowledgeDatasetsByUserId(userId);
        Map<String, Object> executionStats = dashboardMapper.getExecutionStatsByUserId(userId);
        
        // 构建统计数据对象
        return DashboardStats.builder()
                .agentCount(agentCount != null ? agentCount : 0L)
                .workflowCount(agentCount != null ? agentCount : 0L) // workflowCount 等于 agentCount
                .conversationCount(conversationCount != null ? conversationCount : 0L)
                .knowledgeDatasetCount(knowledgeDatasetCount != null ? knowledgeDatasetCount : 0L)
                .totalExecutions(getLongValue(executionStats, "total_executions"))
                .successfulExecutions(getLongValue(executionStats, "successful_executions"))
                .failedExecutions(getLongValue(executionStats, "failed_executions"))
                .avgResponseTime(getDoubleValue(executionStats, "avg_response_time"))
                .build();
    }
    
    /**
     * 从 Map 中获取 Long 值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return 0L;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
    
    /**
     * 从 Map 中获取 Double 值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return 0.0;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
