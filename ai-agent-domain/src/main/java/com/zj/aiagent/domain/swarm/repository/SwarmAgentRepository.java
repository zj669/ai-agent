package com.zj.aiagent.domain.swarm.repository;

import com.zj.aiagent.domain.swarm.entity.SwarmAgent;

import java.util.List;
import java.util.Optional;

public interface SwarmAgentRepository {

    void save(SwarmAgent agent);

    void update(SwarmAgent agent);

    Optional<SwarmAgent> findById(Long id);

    List<SwarmAgent> findByWorkspaceId(Long workspaceId);

    /** 根据父Agent ID查找子Agent */
    List<SwarmAgent> findByParentId(Long parentId);

    /** 检查指定父Agent是否有子Agent */
    boolean hasChildren(Long parentId);

    /** 根据会话ID查找Agent */
    List<SwarmAgent> findBySessionId(Long sessionId);

    void updateStatus(Long id, String status);

    void updateLlmHistory(Long id, String llmHistory);

    void deleteByWorkspaceId(Long workspaceId);
}
