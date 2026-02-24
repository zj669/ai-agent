package com.zj.aiagent.application.swarm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 蜂群消息发送事件：触发群内其他 Agent 唤醒
 */
@Getter
public class SwarmMessageSentEvent extends ApplicationEvent {
    private final Long groupId;
    private final Long senderId;
    private final Long workspaceId;

    public SwarmMessageSentEvent(Object source, Long groupId, Long senderId, Long workspaceId) {
        super(source);
        this.groupId = groupId;
        this.senderId = senderId;
        this.workspaceId = workspaceId;
    }
}
