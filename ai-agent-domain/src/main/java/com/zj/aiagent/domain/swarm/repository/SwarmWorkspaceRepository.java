package com.zj.aiagent.domain.swarm.repository;

import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;

import java.util.List;
import java.util.Optional;

public interface SwarmWorkspaceRepository {

    void save(SwarmWorkspace workspace);

    Optional<SwarmWorkspace> findById(Long id);

    List<SwarmWorkspace> findByUserId(Long userId);

    void update(SwarmWorkspace workspace);

    void deleteById(Long id);
}
