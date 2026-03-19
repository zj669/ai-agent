package com.zj.aiagent.application.swarm.event;

import com.zj.aiagent.application.swarm.SwarmAgentRuntimeService;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听消息发送事件，唤醒群内其他 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwarmMessageEventListener {

    private final SwarmAgentRuntimeService runtimeService;
    private final SwarmGroupRepository groupRepository;

    @Async
    @EventListener
    public void onMessageSent(SwarmMessageSentEvent event) {
        List<Long> memberIds = groupRepository.findMemberIds(
            event.getGroupId()
        );
        log.info(
            "[Swarm] Message event received: workspace={}, group={}, sender={}, members={}",
            event.getWorkspaceId(),
            event.getGroupId(),
            event.getSenderId(),
            memberIds
        );
        for (Long memberId : memberIds) {
            if (!memberId.equals(event.getSenderId())) {
                log.info(
                    "[Swarm] Wake target resolved: workspace={}, group={}, sender={}, targetAgent={}",
                    event.getWorkspaceId(),
                    event.getGroupId(),
                    event.getSenderId(),
                    memberId
                );
                runtimeService.wakeAgent(memberId);
            }
        }
    }
}
