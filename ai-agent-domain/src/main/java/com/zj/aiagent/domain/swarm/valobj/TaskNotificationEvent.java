package com.zj.aiagent.domain.swarm.valobj;

import lombok.Builder;
import lombok.Data;

/**
 * Task Notification 事件 - Worker 任务完成时向 Coordinator 发送的结构化通知
 * 对应 Claude-Code 的 &lt;task-notification&gt; XML 格式
 */
@Data
@Builder
public class TaskNotificationEvent {
    /** 触发通知的 Worker Agent ID */
    private Long agentId;
    /** 任务状态：completed / failed / killed */
    private String status;
    /** 任务摘要（1-2句话） */
    private String summary;
    /** 任务详细结果 */
    private String result;
    /** 任务执行统计 */
    private Usage usage;
    /** 关联的 taskUuid（如果有） */
    private String taskUuid;
    /** 关联的 phase（RESEARCH/SYNTHESIS/IMPLEMENTATION/VERIFICATION） */
    private String phase;
    /** 通知发送时间 */
    private long timestamp;

    @Data
    @Builder
    public static class Usage {
        /** 总 token 数 */
        private Integer totalTokens;
        /** 工具调用次数 */
        private Integer toolUses;
        /** 执行耗时（毫秒） */
        private Long durationMs;
    }
}
