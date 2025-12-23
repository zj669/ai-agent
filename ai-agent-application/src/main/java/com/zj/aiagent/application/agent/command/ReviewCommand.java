package com.zj.aiagent.application.agent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工介入审核命令
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommand {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 是否批准
     */
    private Boolean approved;
}
