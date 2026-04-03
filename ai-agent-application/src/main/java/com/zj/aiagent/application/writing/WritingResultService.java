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

    @Transactional(rollbackFor = Exception.class)
    public WritingResult createResult(
        Long sessionId,
        Long taskId,
        Long swarmAgentId,
        String resultType,
        String summary,
        String content,
        JsonNode structuredPayloadJson
    ) {
        log.info(
            "[Writing] Creating result: sessionId={}, taskId={}, swarmAgentId={}, resultType={}, summary={}",
            sessionId,
            taskId,
            swarmAgentId,
            resultType,
            summary
        );
        WritingResult result = WritingResult.builder()
            .sessionId(sessionId)
            .taskId(taskId)
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
            "[Writing] Result created: resultId={}, sessionId={}, taskId={}, swarmAgentId={}",
            result.getId(),
            result.getSessionId(),
            result.getTaskId(),
            result.getSwarmAgentId()
        );
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingResult recordTaskResult(
        Long taskId,
        Long inputSessionId,
        String resultType,
        String summary,
        String content,
        JsonNode structuredPayloadJson
    ) {
        WritingTask task = writingTaskService.markCompleted(taskId);
        Long resolvedSessionId = task.getSessionId();
        Long resolvedSwarmAgentId = task.getSwarmAgentId();

        if (
            inputSessionId != null &&
                !inputSessionId.equals(resolvedSessionId)
        ) {
            log.warn(
                "[Writing] Corrected writing_result identifiers by task context: taskId={}, inputSessionId={}, resolvedSessionId={}, resolvedSwarmAgentId={}",
                taskId,
                inputSessionId,
                resolvedSessionId,
                resolvedSwarmAgentId
            );
        }

        return createResult(
            resolvedSessionId,
            taskId,
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
            "[Writing] Recording task result by uuid: taskUuid={}, taskId={}, swarmAgentId={}",
            taskUuid,
            task.getId(),
            task.getSwarmAgentId()
        );
        return recordTaskResult(
            task.getId(),
            task.getSessionId(),
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
