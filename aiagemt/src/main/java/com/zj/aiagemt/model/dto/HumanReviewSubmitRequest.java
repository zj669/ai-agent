package com.zj.aiagemt.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 人工审核提交请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReviewSubmitRequest {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 是否批准
     */
    private Boolean approved;

    /**
     * 上下文修改（可选）
     */
    private Map<String, Object> contextModifications;

    /**
     * 审核评论
     */
    private String comments;
}
