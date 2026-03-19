package com.zj.aiagent.domain.writing.repository;

import com.zj.aiagent.domain.writing.entity.WritingAgent;
import java.util.List;
import java.util.Optional;

public interface WritingAgentRepository {

    void save(WritingAgent agent);

    void update(WritingAgent agent);

    Optional<WritingAgent> findById(Long id);

    Optional<WritingAgent> findBySessionIdAndSwarmAgentId(Long sessionId, Long swarmAgentId);

    List<WritingAgent> findBySessionId(Long sessionId);
}
