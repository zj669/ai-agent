package com.zj.aiagent.application.agent.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取执行上下文查询对象
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetExecutionContextQuery {

    /**
     * 会话ID
     */
    private String conversationId;
}
