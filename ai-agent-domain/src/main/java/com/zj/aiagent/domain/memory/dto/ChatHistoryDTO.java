package com.zj.aiagent.domain.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天历史响应 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryDTO {

    private String role; // user, assistant
    private String content; // 用户消息内容
    private List<NodeExecutionDTO> nodes; // AI 回复的节点详情
    private Long timestamp;
    private Boolean error;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NodeExecutionDTO {
        private String nodeId;
        private String nodeName;
        private String status; // pending, running, completed, error
        private String content; // 节点输出内容
        private Long duration; // 执行耗时(毫秒)
    }
}
