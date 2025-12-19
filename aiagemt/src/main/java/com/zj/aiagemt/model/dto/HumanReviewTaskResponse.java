package com.zj.aiagemt.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 人工审核任务响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReviewTaskResponse {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 审核消息
     */
    private String checkMessage;

    /**
     * 当前上下文数据
     */
    private Map<String, Object> contextData;

    /**
     * 节点执行结果
     */
    private Map<String, Object> nodeResults;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 状态
     */
    private String status; // WAITING, APPROVED, REJECTED
}
