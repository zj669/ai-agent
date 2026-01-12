package com.zj.aiagent.application.chat.event;

import com.zj.aiagent.domain.chat.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息追加事件
 */
@Getter
public class MessageAppendedEvent extends ApplicationEvent {

    private final Message message;

    public MessageAppendedEvent(Message message) {
        super(message);
        this.message = message;
    }
}
