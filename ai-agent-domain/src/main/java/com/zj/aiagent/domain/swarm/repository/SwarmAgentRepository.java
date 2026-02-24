package com.zj.aiagent.domain.swarm.repository;

import com.zj.aiagent.domain.swarm.entity.SwarmAgent;

import java.util.List;
import java.util.Optional;

public interface SwarmAgentRepository {

    void save(SwarmAgent agent);

    Optional<SwarmAgent> findById(Long id);

    List<SwarmAgent> findByWorkspaceId(Long workspaceId);

    void updateStatus(Long id, String status);

    void updateLlmHistory(Long id, String llmHistory);

    void deleteByWorkspaceId(Long workspaceId);
}
