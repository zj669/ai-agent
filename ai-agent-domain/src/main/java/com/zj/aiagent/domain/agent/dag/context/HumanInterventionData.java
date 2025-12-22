package com.zj.aiagent.domain.agent.dag.context;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 人工介入数据
 * 封装人工审核和介入相关的所有数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanInterventionData {

    /** 是否已审核通过 */
    private Boolean approved;

    /** 人工评论 */
    private String comments;

    /** 人工介入请求 */
    private HumanInterventionRequest request;

    /** 暂停时间戳 */
    private Long pausedAt;

    /** 暂停的节点ID */
    private String pausedNodeId;

    /**
     * 检查是否已审核通过
     */
    public boolean isApproved() {
        return Boolean.TRUE.equals(approved);
    }

    /**
     * 检查是否已审核（不论结果）
     */
    public boolean isReviewed() {
        return approved != null;
    }

    /**
     * 检查是否正在等待人工介入
     */
    public boolean isWaitingForHuman() {
        return request != null && !isReviewed();
    }

    /**
     * 获取评论，带默认值
     */
    public String getComments(String defaultValue) {
        return comments != null ? comments : defaultValue;
    }

    /**
     * 设置暂停状态
     */
    public void setPaused(String nodeId) {
        this.pausedAt = System.currentTimeMillis();
        this.pausedNodeId = nodeId;
    }

    /**
     * 设置审核结果
     */
    public void setReviewResult(boolean approved, String comments) {
        this.approved = approved;
        this.comments = comments;
    }
}
