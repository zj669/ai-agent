package com.zj.aiagent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息持久化对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_chat_message")
public class AiChatMessagePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;
    private Long agentId;
    private Long userId;
    private Long instanceId;

    private String role;
    private String content;
    private String finalResponse;
    private Boolean isError;
    private String errorMessage;

    private Long timestamp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
