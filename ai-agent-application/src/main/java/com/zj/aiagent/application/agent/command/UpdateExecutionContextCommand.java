package com.zj.aiagent.application.agent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新执行上下文命令对象
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExecutionContextCommand {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 需要修改的字段
     * key: nodeId
     * value: 新的执行结果
     */
    private Map<String, Object> modifications;
}
