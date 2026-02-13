package com.zj.aiagent.application.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.dashboard.dto.DashboardStatsResponse;
import com.zj.aiagent.domain.dashboard.repository.DashboardRepository;
import com.zj.aiagent.domain.dashboard.valobj.DashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Dashboard 应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {
    
    private final DashboardRepository dashboardRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_KEY_PREFIX = "dashboard:stats:";
    private static final long CACHE_TTL_MINUTES = 5;
    
    /**
     * 获取用户的统计数据
     * 
     * @param userId 用户ID
     * @return 统计数据响应
     */
    public DashboardStatsResponse getStats(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        
        // 尝试从缓存获取
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String cachedData = bucket.get();
        
        if (cachedData != null) {
            try {
                log.debug("从缓存获取 Dashboard 统计数据, userId={}", userId);
                return objectMapper.readValue(cachedData, DashboardStatsResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("解析缓存数据失败, userId={}, error={}", userId, e.getMessage());
                // 缓存数据损坏，删除缓存
                bucket.delete();
            }
        }
        
        // 从数据库查询
        log.info("从数据库查询 Dashboard 统计数据, userId={}", userId);
        DashboardStats stats = dashboardRepository.getStatsByUserId(userId);
        
        // 转换为响应对象
        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .agentCount(stats.getAgentCount())
                .workflowCount(stats.getWorkflowCount())
                .conversationCount(stats.getConversationCount())
                .knowledgeDatasetCount(stats.getKnowledgeDatasetCount())
                .totalExecutions(stats.getTotalExecutions())
                .successfulExecutions(stats.getSuccessfulExecutions())
                .failedExecutions(stats.getFailedExecutions())
                .avgResponseTime(stats.getAvgResponseTime())
                .build();
        
        // 写入缓存
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            bucket.set(jsonData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Dashboard 统计数据已缓存, userId={}, ttl={}分钟", userId, CACHE_TTL_MINUTES);
        } catch (JsonProcessingException e) {
            log.error("序列化统计数据失败, userId={}, error={}", userId, e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * 清除用户的统计数据缓存
     * 
     * @param userId 用户ID
     */
    public void clearStatsCache(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        redissonClient.getBucket(cacheKey).delete();
        log.info("已清除 Dashboard 统计数据缓存, userId={}", userId);
    }
}
