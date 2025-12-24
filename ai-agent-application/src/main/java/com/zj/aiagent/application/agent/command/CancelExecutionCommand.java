package com.zj.aiagent.application.agent.command;

import lombok.Data;

/**
 * 取消执行命令
 *
 * @author zj
 * @since 2025-12-24
 */
@Data
public class CancelExecutionCommand {
    /**
     * 会话ID
     */
    private String conversationId;
}
