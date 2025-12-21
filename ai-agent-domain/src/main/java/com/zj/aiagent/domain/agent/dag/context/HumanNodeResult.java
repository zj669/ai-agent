package com.zj.aiagent.domain.agent.dag.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工节点执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanNodeResult {

    /**
     * 是否等待人工处理
     */
    private boolean waitingForHuman;

    /**
     * 审核是否通过
     */
    private Boolean approved;

    /**
     * 人工介入请求
     */
    private HumanInterventionRequest request;

    /**
     * 审核评论
     */
    private String comments;

    /**
     * 创建等待人工处理的结果
     */
    public static String waitingForHuman(HumanInterventionRequest request) {
        return "WAITING_FOR_HUMAN: " + request.getExecutionId();
    }

    /**
     * 创建审核完成的结果
     */
    public static String approved(String comments) {
        return "APPROVED: " + comments;
    }

    /**
     * 创建审核拒绝的结果
     */
    public static String rejected(String comments) {
        return "REJECTED: " + comments;
    }
}
