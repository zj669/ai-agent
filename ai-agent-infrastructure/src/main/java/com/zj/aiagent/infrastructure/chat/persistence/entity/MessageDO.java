package com.zj.aiagent.infrastructure.chat.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.zj.aiagent.domain.chat.valobj.Citation;
import com.zj.aiagent.domain.chat.valobj.MessageRole;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.chat.valobj.ThoughtStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息持久化对象
 * 使用 MyBatis-Plus，配置 JacksonTypeHandler 处理 JSON 字段
 * 索引: idx_conversation_created (conversation_id, created_at) 在数据库 DDL 中创建
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "messages", autoResultMap = true)
public class MessageDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("role")
    private MessageRole role;

    @TableField("content")
    private String content;

    /**
     * 思维链过程 (JSON)
     */
    @TableField(value = "thought_process", typeHandler = JacksonTypeHandler.class)
    private List<ThoughtStep> thoughtProcess;

    /**
     * 引用源 (JSON)
     */
    @TableField(value = "citations", typeHandler = JacksonTypeHandler.class)
    private List<Citation> citations;

    /**
     * 元数据 (JSON)
     */
    @TableField(value = "meta_data", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    @TableField("status")
    private MessageStatus status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
