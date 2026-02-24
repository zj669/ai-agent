package com.zj.aiagent.domain.swarm.repository;

import com.zj.aiagent.domain.swarm.entity.SwarmGroup;

import java.util.List;
import java.util.Optional;

public interface SwarmGroupRepository {

    void save(SwarmGroup group);

    Optional<SwarmGroup> findById(Long id);

    List<SwarmGroup> findByWorkspaceId(Long workspaceId);

    /** 查询某个 Agent 所在的所有群组 */
    List<SwarmGroup> findByAgentId(Long agentId);

    void deleteByWorkspaceId(Long workspaceId);

    // --- 群成员操作 ---

    void addMember(Long groupId, Long agentId);

    void removeMember(Long groupId, Long agentId);

    List<Long> findMemberIds(Long groupId);

    void updateLastReadMessageId(Long groupId, Long agentId, Long messageId);

    Long getLastReadMessageId(Long groupId, Long agentId);
}
