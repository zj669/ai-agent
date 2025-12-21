package com.zj.aiagent.application.agent.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询历史会话ID查询对象
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetConversationIdsQuery {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Agent ID
     */
    private Long agentId;
}
