package com.zj.aiagent.domain.writing.repository;

import com.zj.aiagent.domain.writing.entity.WritingSession;
import java.util.List;
import java.util.Optional;

public interface WritingSessionRepository {

    void save(WritingSession session);

    void update(WritingSession session);

    Optional<WritingSession> findById(Long id);

    List<WritingSession> findByWorkspaceId(Long workspaceId);
}
