package com.zj.aiagent.application.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import com.zj.aiagent.domain.writing.repository.WritingTaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingTaskService {

    private final WritingTaskRepository writingTaskRepository;
    private final WritingSessionService writingSessionService;
    private final WritingAgentCoordinatorService writingAgentCoordinatorService;

    @Transactional(rollbackFor = Exception.class)
    public WritingTask createTask(
        Long sessionId,
        Long writingAgentId,
        Long swarmAgentId,
        String taskType,
        String title,
        String instruction,
        JsonNode inputPayloadJson,
        JsonNode expectedOutputSchemaJson,
        Integer priority,
        Long createdBySwarmAgentId
    ) {
        log.info(
            "[Writing] Creating task: sessionId={}, writingAgentId={}, swarmAgentId={}, taskType={}, title={}, priority={}, createdBy={}",
            sessionId,
            writingAgentId,
            swarmAgentId,
            taskType,
            title,
            priority,
            createdBySwarmAgentId
        );
        String taskUuid = generateTaskUuid();
        WritingTask task = WritingTask.builder()
            .taskUuid(taskUuid)
            .sessionId(sessionId)
            .writingAgentId(writingAgentId)
            .swarmAgentId(swarmAgentId)
            .taskType(
                taskType != null && !taskType.isBlank() ? taskType : "WRITING"
            )
            .title(title)
            .instruction(instruction)
            .inputPayloadJson(inputPayloadJson)
            .expectedOutputSchemaJson(expectedOutputSchemaJson)
            .status("PLANNED")
            .priority(priority != null ? priority : 0)
            .createdBySwarmAgentId(createdBySwarmAgentId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        writingTaskRepository.save(task);
        writingAgentCoordinatorService.updateStatus(writingAgentId, "PLANNED");
        writingSessionService.updateStatus(sessionId, "RUNNING");
        log.info(
            "[Writing] Task created: taskId={}, taskUuid={}, sessionId={}, writingAgentId={}, swarmAgentId={}, status={}",
            task.getId(),
            task.getTaskUuid(),
            task.getSessionId(),
            task.getWritingAgentId(),
            task.getSwarmAgentId(),
            task.getStatus()
        );
        return task;
    }

    public WritingTask getTask(Long taskId) {
        return writingTaskRepository
            .findById(taskId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing task not found: " + taskId
                )
            );
    }

    public WritingTask getTaskByUuid(String taskUuid) {
        return writingTaskRepository
            .findByTaskUuid(taskUuid)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing task not found by uuid: " + taskUuid
                )
            );
    }

    public List<WritingTask> listBySession(Long sessionId) {
        return writingTaskRepository.findBySessionId(sessionId);
    }

    public List<WritingTask> listByWritingAgent(Long writingAgentId) {
        return writingTaskRepository.findByWritingAgentId(writingAgentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingTask markDispatchedByUuid(String taskUuid) {
        WritingTask task = getTaskByUuid(taskUuid);
        String previousStatus = task.getStatus();
        task.setStatus("PENDING");
        task.setUpdatedAt(LocalDateTime.now());
        writingTaskRepository.update(task);
        writingAgentCoordinatorService.updateStatus(
            task.getWritingAgentId(),
            "ASSIGNED"
        );
        log.info(
            "[Writing] Task dispatched: taskId={}, taskUuid={}, from={}, to={}, writingAgentId={}, swarmAgentId={}",
            task.getId(),
            task.getTaskUuid(),
            previousStatus,
            task.getStatus(),
            task.getWritingAgentId(),
            task.getSwarmAgentId()
        );
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingTask markRunning(Long taskId) {
        WritingTask task = getTask(taskId);
        String previousStatus = task.getStatus();
        task.setStatus("RUNNING");
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        writingTaskRepository.update(task);
        log.info(
            "[Writing] Task status updated: taskId={}, taskUuid={}, from={}, to={}, startedAt={}",
            taskId,
            task.getTaskUuid(),
            previousStatus,
            task.getStatus(),
            task.getStartedAt()
        );
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingTask markCompleted(Long taskId) {
        WritingTask task = getTask(taskId);
        String previousStatus = task.getStatus();
        task.setStatus("DONE");
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        writingTaskRepository.update(task);
        log.info(
            "[Writing] Task completed: taskId={}, taskUuid={}, from={}, to={}, writingAgentId={}, swarmAgentId={}, finishedAt={}",
            taskId,
            task.getTaskUuid(),
            previousStatus,
            task.getStatus(),
            task.getWritingAgentId(),
            task.getSwarmAgentId(),
            task.getFinishedAt()
        );
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingTask markFailed(Long taskId) {
        WritingTask task = getTask(taskId);
        String previousStatus = task.getStatus();
        task.setStatus("FAILED");
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        writingTaskRepository.update(task);
        log.info(
            "[Writing] Task failed: taskId={}, taskUuid={}, from={}, to={}, writingAgentId={}, swarmAgentId={}, finishedAt={}",
            taskId,
            task.getTaskUuid(),
            previousStatus,
            task.getStatus(),
            task.getWritingAgentId(),
            task.getSwarmAgentId(),
            task.getFinishedAt()
        );
        return task;
    }

    private String generateTaskUuid() {
        return "wtask_" + UUID.randomUUID().toString().replace("-", "");
    }
}
