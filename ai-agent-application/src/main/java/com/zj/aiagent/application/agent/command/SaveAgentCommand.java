package com.zj.aiagent.application.agent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存Agent配置Command
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAgentCommand {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Agent ID（可选，不传则创建新Agent）
     */
    private String agentId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 描述
     */
    private String description;

    /**
     * DAG配置JSON
     */
    private String graphJson;

    /**
     * 状态（可选，默认为0草稿）
     */
    private Integer status;
}
