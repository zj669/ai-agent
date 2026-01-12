package com.zj.aiagent.domain.chat.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    private String id;
    private String userId;
    private String agentId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateTitle(String newTitle) {
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
