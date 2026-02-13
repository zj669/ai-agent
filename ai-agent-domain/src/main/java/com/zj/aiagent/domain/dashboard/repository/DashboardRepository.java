package com.zj.aiagent.domain.dashboard.repository;

import com.zj.aiagent.domain.dashboard.valobj.DashboardStats;

/**
 * Dashboard Repository Interface
 */
public interface DashboardRepository {
    
    /**
     * 获取用户的统计数据
     * 
     * @param userId 用户ID
     * @return 统计数据
     */
    DashboardStats getStatsByUserId(Long userId);
}
