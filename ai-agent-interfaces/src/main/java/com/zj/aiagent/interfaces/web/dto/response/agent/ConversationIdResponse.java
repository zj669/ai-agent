package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话ID响应
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationIdResponse {
    /**
     * 会话ID
     */
    private String conversationId;
}
