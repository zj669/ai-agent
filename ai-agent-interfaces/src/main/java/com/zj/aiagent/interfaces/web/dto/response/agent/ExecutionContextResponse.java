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
     * 执行状态
     */
    private String status;

    /**
     * 暂停的节点ID
     */
    private String pausedNodeId;

    /**
     * 暂停的节点名称
     */
    private String pausedNodeName;

    /**
     * 暂停时间戳
     */
    private Long pausedAt;

    /**
     * 所有节点执行结果
     */
    private Map<String, Object> nodeResults;

    /**
     * 是否允许修改输出
     */
    private Boolean allowModifyOutput;

    /**
     * 审核提示消息
     */
    private String checkMessage;
}
