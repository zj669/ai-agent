package com.zj.aiagent.application.agent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布Agent Command
 *
 * @author zj
 * @since 2025-12-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishAgentCommand {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Agent ID
     */
    private String agentId;
}
