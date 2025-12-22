package com.zj.aiagent.application.agent.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询Agent详情Query
 *
 * @author zj
 * @since 2025-12-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAgentDetailQuery {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Agent ID
     */
    private String agentId;
}
