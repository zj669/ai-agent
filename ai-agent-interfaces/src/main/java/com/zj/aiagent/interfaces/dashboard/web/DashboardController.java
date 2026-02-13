package com.zj.aiagent.interfaces.dashboard.web;

import com.zj.aiagent.application.dashboard.dto.DashboardStatsResponse;
import com.zj.aiagent.application.dashboard.service.DashboardApplicationService;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardApplicationService dashboardApplicationService;
    
    /**
     * 获取统计数据
     * 
     * @return 统计数据响应
     */
    @GetMapping("/stats")
    public Response<DashboardStatsResponse> getStats() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        
        log.info("获取 Dashboard 统计数据, userId={}", userId);
        DashboardStatsResponse stats = dashboardApplicationService.getStats(userId);
        return Response.success(stats);
    }
}
