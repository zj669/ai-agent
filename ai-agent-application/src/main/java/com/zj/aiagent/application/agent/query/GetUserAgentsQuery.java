package com.zj.aiagent.application.agent.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询用户 Agent 列表查询对象
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserAgentsQuery {
    /**
     * 用户ID
     */
    private Long userId;
}
