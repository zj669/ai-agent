package com.zj.aiagent.domain.writing.repository;

import com.zj.aiagent.domain.writing.entity.WritingDraft;
import java.util.List;
import java.util.Optional;

public interface WritingDraftRepository {

    void save(WritingDraft draft);

    Optional<WritingDraft> findById(Long id);

    Optional<WritingDraft> findLatestBySessionId(Long sessionId);

    List<WritingDraft> findBySessionId(Long sessionId);
}
