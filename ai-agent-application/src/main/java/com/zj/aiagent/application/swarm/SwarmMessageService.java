package com.zj.aiagent.application.swarm;

import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.domain.swarm.entity.SwarmGroup;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmMessageService {

    private final SwarmMessageRepository messageRepository;
    private final SwarmGroupRepository groupRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SwarmUIEventBus uiEventBus;

    /**
     * 发送消息（写库 + 触发唤醒，唤醒逻辑在 P2 阶段接入）
     */
    @Transactional(rollbackFor = Exception.class)
    public SwarmMessageDTO sendMessage(Long groupId, SendMessageRequest request) {
        SwarmGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        SwarmMessage message = SwarmMessage.builder()
                .workspaceId(group.getWorkspaceId())
                .groupId(groupId)
                .senderId(request.getSenderId())
                .contentType(request.getContentType())
                .content(request.getContent())
                .sendTime(LocalDateTime.now())
                .build();
        messageRepository.save(message);

        log.info("[Swarm] Message sent: group={}, sender={}, id={}", groupId, request.getSenderId(), message.getId());

        // 触发 Agent 唤醒事件
        eventPublisher.publishEvent(new com.zj.aiagent.application.swarm.event.SwarmMessageSentEvent(
                this, groupId, request.getSenderId(), group.getWorkspaceId()));

        // emit UI 事件
        uiEventBus.emit(group.getWorkspaceId(), SwarmUIEventBus.UIEvent.builder()
                .type("ui.message.created")
                .data("{\"groupId\":" + groupId + ",\"messageId\":" + message.getId() + ",\"senderId\":" + request.getSenderId() + "}")
                .timestamp(System.currentTimeMillis())
                .build());

        return toDTO(message);
    }

    /**
     * 拉取群消息（可选标记已读）
     */
    public List<SwarmMessageDTO> getMessages(Long groupId, boolean markRead, Long readerId) {
        List<SwarmMessage> messages = messageRepository.findByGroupId(groupId);

        if (markRead && readerId != null && !messages.isEmpty()) {
            Long lastId = messages.get(messages.size() - 1).getId();
            groupRepository.updateLastReadMessageId(groupId, readerId, lastId);
        }

        return messages.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 列出 workspace 下的群组（含未读数、最后消息）
     */
    public List<SwarmGroupDTO> listGroups(Long workspaceId, Long agentId) {
        List<SwarmGroup> groups;
        if (agentId != null) {
            groups = groupRepository.findByAgentId(agentId);
        } else {
            groups = groupRepository.findByWorkspaceId(workspaceId);
        }

        return groups.stream().map(group -> {
            List<Long> memberIds = groupRepository.findMemberIds(group.getId());
            List<SwarmMessage> messages = messageRepository.findByGroupId(group.getId());

            SwarmMessageDTO lastMessage = null;
            int unreadCount = 0;

            if (!messages.isEmpty()) {
                lastMessage = toDTO(messages.get(messages.size() - 1));

                if (agentId != null) {
                    Long lastRead = groupRepository.getLastReadMessageId(group.getId(), agentId);
                    unreadCount = (int) messages.stream().filter(m -> m.getId() > lastRead).count();
                }
            }

            return SwarmGroupDTO.builder()
                    .id(group.getId())
                    .workspaceId(group.getWorkspaceId())
                    .name(group.getName())
                    .memberIds(memberIds)
                    .unreadCount(unreadCount)
                    .lastMessage(lastMessage)
                    .contextTokens(group.getContextTokens())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 创建 human-agent P2P 群（幂等：已存在则直接返回）
     */
    @Transactional(rollbackFor = Exception.class)
    public SwarmGroupDTO createP2PGroup(Long workspaceId, Long agentId1, Long agentId2) {
        // 先检查是否已存在
        List<SwarmGroup> existingGroups = groupRepository.findByWorkspaceId(workspaceId);
        for (SwarmGroup g : existingGroups) {
            List<Long> memberIds = groupRepository.findMemberIds(g.getId());
            if (memberIds.size() == 2 && memberIds.contains(agentId1) && memberIds.contains(agentId2)) {
                return SwarmGroupDTO.builder()
                        .id(g.getId())
                        .workspaceId(g.getWorkspaceId())
                        .name(g.getName())
                        .memberIds(memberIds)
                        .unreadCount(0)
                        .contextTokens(g.getContextTokens())
                        .build();
            }
        }

        // 不存在则创建
        SwarmGroup group = SwarmGroup.builder()
                .workspaceId(workspaceId)
                .name("p2p")
                .build();
        groupRepository.save(group);
        groupRepository.addMember(group.getId(), agentId1);
        groupRepository.addMember(group.getId(), agentId2);

        return SwarmGroupDTO.builder()
                .id(group.getId())
                .workspaceId(workspaceId)
                .name("p2p")
                .memberIds(List.of(agentId1, agentId2))
                .unreadCount(0)
                .contextTokens(0)
                .build();
    }

    private SwarmMessageDTO toDTO(SwarmMessage msg) {
        return SwarmMessageDTO.builder()
                .id(msg.getId())
                .groupId(msg.getGroupId())
                .senderId(msg.getSenderId())
                .content(msg.getContent())
                .contentType(msg.getContentType())
                .sendTime(msg.getSendTime())
                .build();
    }
}
