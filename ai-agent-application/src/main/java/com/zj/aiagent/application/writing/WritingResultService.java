package com.zj.aiagent.application.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import com.zj.aiagent.domain.writing.repository.WritingResultRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingResultService {

    private final WritingResultRepository writingResultRepository;
    private final WritingTaskService writingTaskService;
    private final WritingAgentCoordinatorService writingAgentCoordinatorService;

    @Transactional(rollbackFor = Exception.class)
    public WritingResult createResult(
        Long sessionId,
        Long taskId,
        Long writingAgentId,
        Long swarmAgentId,
        String resultType,
        String summary,
        String content,
        JsonNode structuredPayloadJson
    ) {
        log.info(
            "[Writing] Creating result: sessionId={}, taskId={}, writingAgentId={}, swarmAgentId={}, resultType={}, summary={}",
            sessionId,
            taskId,
            writingAgentId,
            swarmAgentId,
            resultType,
            summary
        );
        WritingResult result = WritingResult.builder()
            .sessionId(sessionId)
            .taskId(taskId)
            .writingAgentId(writingAgentId)
            .swarmAgentId(swarmAgentId)
            .resultType(
                resultType != null && !resultType.isBlank()
                    ? resultType
                    : "TEXT"
            )
            .summary(summary)
            .content(content)
            .structuredPayloadJson(structuredPayloadJson)
            .createdAt(LocalDateTime.now())
            .build();
        writingResultRepository.save(result);
        log.info(
            "[Writing] Result created: resultId={}, sessionId={}, taskId={}, writingAgentId={}",
            result.getId(),
            result.getSessionId(),
            result.getTaskId(),
            result.getWritingAgentId()
        );
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingResult recordTaskResult(
        Long taskId,
        Long inputSessionId,
        Long inputWritingAgentId,
        String resultType,
        String summary,
        String content,
        JsonNode structuredPayloadJson
    ) {
        WritingTask task = writingTaskService.markCompleted(taskId);
        Long resolvedSessionId = task.getSessionId();
        Long resolvedWritingAgentId = task.getWritingAgentId();
        Long resolvedSwarmAgentId = task.getSwarmAgentId();

        if (
            (inputSessionId != null &&
                !inputSessionId.equals(resolvedSessionId)) ||
            (inputWritingAgentId != null &&
                !inputWritingAgentId.equals(resolvedWritingAgentId))
        ) {
            log.warn(
                "[Writing] Corrected writing_result identifiers by task context: taskId={}, inputSessionId={}, resolvedSessionId={}, inputWritingAgentId={}, resolvedWritingAgentId={}, resolvedSwarmAgentId={}",
                taskId,
                inputSessionId,
                resolvedSessionId,
                inputWritingAgentId,
                resolvedWritingAgentId,
                resolvedSwarmAgentId
            );
        }

        writingAgentCoordinatorService.updateStatus(
            resolvedWritingAgentId,
            "DONE"
        );

        return createResult(
            resolvedSessionId,
            taskId,
            resolvedWritingAgentId,
            resolvedSwarmAgentId,
            resultType,
            summary,
            content,
            structuredPayloadJson
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingResult recordTaskResultByUuid(
        String taskUuid,
        String resultType,
        String summary,
        String content,
        JsonNode structuredPayloadJson
    ) {
        WritingTask task = writingTaskService.getTaskByUuid(taskUuid);
        log.info(
            "[Writing] Recording task result by uuid: taskUuid={}, taskId={}, writingAgentId={}, swarmAgentId={}",
            taskUuid,
            task.getId(),
            task.getWritingAgentId(),
            task.getSwarmAgentId()
        );
        return recordTaskResult(
            task.getId(),
            task.getSessionId(),
            task.getWritingAgentId(),
            resultType,
            summary,
            content,
            structuredPayloadJson
        );
    }

    public WritingResult getResult(Long resultId) {
        return writingResultRepository
            .findById(resultId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing result not found: " + resultId
                )
            );
    }

    public List<WritingResult> listBySession(Long sessionId) {
        return writingResultRepository.findBySessionId(sessionId);
    }

    public List<WritingResult> listByTask(Long taskId) {
        return writingResultRepository.findByTaskId(taskId);
    }
}
