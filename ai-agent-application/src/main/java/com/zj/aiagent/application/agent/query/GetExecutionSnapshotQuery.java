package com.zj.aiagent.application.agent.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取执行快照查询对象
 *
 * @author zj
 * @since 2025-12-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetExecutionSnapshotQuery {

    /**
     * 会话ID (executionId)
     */
    private String conversationId;
}
