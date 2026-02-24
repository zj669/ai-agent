package com.zj.aiagent.domain.swarm.repository;

import com.zj.aiagent.domain.swarm.entity.SwarmMessage;

import java.util.List;

public interface SwarmMessageRepository {

    void save(SwarmMessage message);

    List<SwarmMessage> findByGroupId(Long groupId);

    /** 查询某群组中 id 大于 afterId 的消息（未读消息） */
    List<SwarmMessage> findByGroupIdAfter(Long groupId, Long afterId);

    /** 查询 workspace 下两个 agent 之间的消息数量（用于 graph edges） */
    int countMessagesBetween(Long workspaceId, Long agentId1, Long agentId2);

    void deleteByWorkspaceId(Long workspaceId);

    void deleteByGroupId(Long groupId);
}
