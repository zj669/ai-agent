package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行上下文响应DTO
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 最后执行的节点ID
     */
    private String lastNodeId;

    /**
     * 执行状态 (RUNNING, PAUSED, COMPLETED, ERROR)
     */
    private String status;

    /**
     * 快照时间戳
     */
    private Long timestamp;

    /**
     * 工作流状态数据
     * <p>
     * 包含所有状态键值对，如用户输入、执行历史、思考历史等
     */
    private Map<String, Object> stateData;
}
