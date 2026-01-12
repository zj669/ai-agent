package com.zj.aiagent.domain.chat.valobj;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 思维链步骤 (递归结构)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtStep {
    private String stepId;
    private String title;
    private String content; // 详情或日志
    private Long durationMs;
    private String status; // RUNNING, SUCCESS, FAILED

    /**
     * 类型: log, tool, parallel_group, group
     */
    private String type;

    /**
     * 子步骤 (递归)
     */
    private List<ThoughtStep> children;

    /**
     * 工具调用详情 (可选)
     */
    private ToolCallDetail toolCall;
}
