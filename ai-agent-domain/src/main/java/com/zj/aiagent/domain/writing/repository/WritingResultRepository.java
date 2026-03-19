package com.zj.aiagent.domain.writing.repository;

import com.zj.aiagent.domain.writing.entity.WritingResult;
import java.util.List;
import java.util.Optional;

public interface WritingResultRepository {

    void save(WritingResult result);

    Optional<WritingResult> findById(Long id);

    List<WritingResult> findBySessionId(Long sessionId);

    List<WritingResult> findByTaskId(Long taskId);
}
