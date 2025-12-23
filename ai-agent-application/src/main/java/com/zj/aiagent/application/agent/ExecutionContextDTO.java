package com.zj.aiagent.application.agent;

import com.zj.aiagent.domain.agent.dag.context.HumanInterventionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行上下文DTO
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextDTO {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 执行状态
     * RUNNING, PAUSED, COMPLETED, FAILED
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
     * key: nodeId
     * value: 执行结果
     */
    private Map<String, Object> nodeResults;

    /**
     * 人工介入请求详情
     */
    private HumanInterventionRequest interventionRequest;
}
