package com.zj.aiagent.domain.chat.valobj;

/**
 * 消息状态
 */
public enum MessageStatus {
    PENDING,
    STREAMING,
    COMPLETED,
    FAILED,
    CANCELLED
}
