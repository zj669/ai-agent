package com.zj.aiagent.domain.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 拦截结果
 * <p>
 * 表示拦截器的决策
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
public class InterceptResult {
    /**
     * 拦截动作
     */
    private InterceptAction action;

    /**
     * 原因说明
     */
    private String reason;

    /**
     * 元数据
     */
    private Object metadata;

    /**
     * 拦截动作枚举
     */
    public enum InterceptAction {
        /** 继续执行 */
        PROCEED,
        /** 暂停 */
        PAUSE,
        /** 跳过 */
        SKIP,
        /** 错误 */
        ERROR
    }

    /**
     * 继续执行
     */
    public static InterceptResult proceed() {
        return InterceptResult.builder()
                .action(InterceptAction.PROCEED)
                .build();
    }

    /**
     * 暂停
     */
    public static InterceptResult pause(String reason) {
        return InterceptResult.builder()
                .action(InterceptAction.PAUSE)
                .reason(reason)
                .build();
    }

    /**
     * 跳过
     */
    public static InterceptResult skip(String reason) {
        return InterceptResult.builder()
                .action(InterceptAction.SKIP)
                .reason(reason)
                .build();
    }

    /**
     * 错误
     */
    public static InterceptResult error(String reason) {
        return InterceptResult.builder()
                .action(InterceptAction.ERROR)
                .reason(reason)
                .build();
    }

    /**
     * 是否应该暂停
     */
    public boolean shouldPause() {
        return action == InterceptAction.PAUSE;
    }

    /**
     * 是否应该跳过
     */
    public boolean shouldSkip() {
        return action == InterceptAction.SKIP;
    }

    /**
     * 是否应该继续
     */
    public boolean shouldProceed() {
        return action == InterceptAction.PROCEED;
    }
}
