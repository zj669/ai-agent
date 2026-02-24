package com.zj.aiagent.domain.swarm.service;

import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 蜂群领域服务：未读消息查询、已读标记等
 */
@Service
@RequiredArgsConstructor
public class SwarmDomainService {

    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;

    /**
     * 查询 Agent 的未读消息，按 group 聚合
     * @return Map<groupId, List<SwarmMessage>>
     */
    public Map<Long, List<SwarmMessage>> getUnreadMessagesByAgent(Long agentId) {
        Map<Long, List<SwarmMessage>> result = new LinkedHashMap<>();

        // 查询该 Agent 所在的所有群组
        groupRepository.findByAgentId(agentId).forEach(group -> {
            Long lastRead = groupRepository.getLastReadMessageId(group.getId(), agentId);
            List<SwarmMessage> unread = messageRepository.findByGroupIdAfter(group.getId(), lastRead);
            if (!unread.isEmpty()) {
                result.put(group.getId(), unread);
            }
        });

        return result;
    }

    /**
     * 标记已读：更新 Agent 在某群组的已读位置
     */
    public void markRead(Long groupId, Long agentId, Long messageId) {
        groupRepository.updateLastReadMessageId(groupId, agentId, messageId);
    }
}
