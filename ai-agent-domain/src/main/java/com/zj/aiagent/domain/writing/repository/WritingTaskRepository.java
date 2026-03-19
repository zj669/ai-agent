package com.zj.aiagent.domain.writing.repository;

import com.zj.aiagent.domain.writing.entity.WritingTask;
import java.util.List;
import java.util.Optional;

public interface WritingTaskRepository {
    void save(WritingTask task);

    void update(WritingTask task);

    Optional<WritingTask> findById(Long id);

    Optional<WritingTask> findByTaskUuid(String taskUuid);

    List<WritingTask> findBySessionId(Long sessionId);

    List<WritingTask> findByWritingAgentId(Long writingAgentId);
}
