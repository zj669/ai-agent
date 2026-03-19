package com.zj.aiagent.application.writing;

import com.zj.aiagent.application.writing.dto.WritingCollaborationCardDTO;
import com.zj.aiagent.application.writing.dto.WritingDraftSummaryDTO;
import com.zj.aiagent.application.writing.dto.WritingMessageViewDTO;
import com.zj.aiagent.application.writing.dto.WritingResultSummaryDTO;
import com.zj.aiagent.application.writing.dto.WritingSessionOverviewDTO;
import com.zj.aiagent.application.writing.dto.WritingSessionSummaryDTO;
import com.zj.aiagent.application.writing.dto.WritingTaskSummaryDTO;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.writing.entity.WritingAgent;
import com.zj.aiagent.domain.writing.entity.WritingDraft;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WritingProjectionService {

    private final WritingSessionService writingSessionService;
    private final WritingAgentCoordinatorService writingAgentCoordinatorService;
    private final WritingTaskService writingTaskService;
    private final WritingResultService writingResultService;
    private final WritingDraftService writingDraftService;
    private final SwarmAgentRepository swarmAgentRepository;
    private final SwarmMessageRepository swarmMessageRepository;

    public List<WritingSessionSummaryDTO> listSessions(Long workspaceId) {
        return writingSessionService
            .listSessions(workspaceId)
            .stream()
            .map(this::toSessionSummary)
            .toList();
    }

    public WritingSessionOverviewDTO getSessionOverview(Long sessionId) {
        WritingSession session = writingSessionService.getSession(sessionId);
        List<WritingAgent> writingAgents =
            writingAgentCoordinatorService.listAgents(sessionId);
        List<WritingTask> tasks = writingTaskService.listBySession(sessionId);
        List<WritingResult> results = writingResultService.listBySession(
            sessionId
        );
        WritingDraft latestDraft = null;
        try {
            latestDraft = writingDraftService.getLatestDraft(sessionId);
        } catch (IllegalArgumentException ignored) {
            // no draft yet
        }

        Map<Long, SwarmAgent> swarmAgentMap = swarmAgentRepository
            .findByWorkspaceId(session.getWorkspaceId())
            .stream()
            .collect(Collectors.toMap(SwarmAgent::getId, Function.identity()));

        List<WritingMessageViewDTO> rootConversation = buildRootConversation(
            session,
            swarmAgentMap
        );

        List<WritingCollaborationCardDTO> collaborationCards = writingAgents
            .stream()
            .sorted(
                Comparator.comparing(
                    WritingAgent::getSortOrder,
                    Comparator.nullsLast(Integer::compareTo)
                ).thenComparing(
                    WritingAgent::getCreatedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
                )
            )
            .map(agent -> toCollaborationCard(agent, tasks, results))
            .toList();

        return WritingSessionOverviewDTO.builder()
            .session(toSessionSummary(session))
            .rootConversation(rootConversation)
            .collaborationCards(collaborationCards)
            .latestDraft(toDraftSummary(latestDraft))
            .build();
    }

    private List<WritingMessageViewDTO> buildRootConversation(
        WritingSession session,
        Map<Long, SwarmAgent> swarmAgentMap
    ) {
        if (session.getDefaultGroupId() == null) {
            return List.of();
        }
        return swarmMessageRepository
            .findByGroupId(session.getDefaultGroupId())
            .stream()
            .filter(
                message ->
                    session.getHumanAgentId().equals(message.getSenderId()) ||
                    session.getRootAgentId().equals(message.getSenderId())
            )
            .map(message -> toMessageView(message, swarmAgentMap))
            .toList();
    }

    private WritingCollaborationCardDTO toCollaborationCard(
        WritingAgent agent,
        List<WritingTask> tasks,
        List<WritingResult> results
    ) {
        WritingTask currentTask = tasks
            .stream()
            .filter(task -> agent.getId().equals(task.getWritingAgentId()))
            .sorted(
                Comparator.comparing((WritingTask task) ->
                    taskRank(task.getStatus())
                )
                    .thenComparing(
                        WritingTask::getPriority,
                        Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    .thenComparing(
                        WritingTask::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                    )
            )
            .findFirst()
            .orElse(null);

        WritingResult latestResult = results
            .stream()
            .filter(result -> agent.getId().equals(result.getWritingAgentId()))
            .max(
                Comparator.comparing(
                    WritingResult::getCreatedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
                )
            )
            .orElse(null);

        LocalDateTime updatedAt =
            latestResult != null
                ? latestResult.getCreatedAt()
                : currentTask != null
                    ? currentTask.getUpdatedAt()
                    : agent.getUpdatedAt();

        return WritingCollaborationCardDTO.builder()
            .writingAgentId(agent.getId())
            .swarmAgentId(agent.getSwarmAgentId())
            .role(agent.getRole())
            .description(agent.getDescription())
            .status(agent.getStatus())
            .sortOrder(agent.getSortOrder())
            .currentTask(toTaskSummary(currentTask))
            .latestResult(toResultSummary(latestResult))
            .updatedAt(updatedAt)
            .build();
    }

    private int taskRank(String status) {
        if ("RUNNING".equals(status)) {
            return 0;
        }
        if ("PENDING".equals(status)) {
            return 1;
        }
        if ("FAILED".equals(status)) {
            return 2;
        }
        if ("DONE".equals(status)) {
            return 3;
        }
        return 4;
    }

    private WritingMessageViewDTO toMessageView(
        SwarmMessage message,
        Map<Long, SwarmAgent> swarmAgentMap
    ) {
        SwarmAgent sender = swarmAgentMap.get(message.getSenderId());
        return WritingMessageViewDTO.builder()
            .id(message.getId())
            .senderId(message.getSenderId())
            .senderRole(sender != null ? sender.getRole() : null)
            .contentType(message.getContentType())
            .content(message.getContent())
            .sendTime(message.getSendTime())
            .build();
    }

    private WritingSessionSummaryDTO toSessionSummary(WritingSession session) {
        if (session == null) {
            return null;
        }
        return WritingSessionSummaryDTO.builder()
            .id(session.getId())
            .workspaceId(session.getWorkspaceId())
            .rootAgentId(session.getRootAgentId())
            .humanAgentId(session.getHumanAgentId())
            .defaultGroupId(session.getDefaultGroupId())
            .title(session.getTitle())
            .goal(session.getGoal())
            .status(session.getStatus())
            .currentDraftId(session.getCurrentDraftId())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .build();
    }

    private WritingTaskSummaryDTO toTaskSummary(WritingTask task) {
        if (task == null) {
            return null;
        }
        return WritingTaskSummaryDTO.builder()
            .id(task.getId())
            .taskUuid(task.getTaskUuid())
            .taskType(task.getTaskType())
            .title(task.getTitle())
            .instruction(task.getInstruction())
            .status(task.getStatus())
            .priority(task.getPriority())
            .createdAt(task.getCreatedAt())
            .startedAt(task.getStartedAt())
            .finishedAt(task.getFinishedAt())
            .build();
    }

    private WritingResultSummaryDTO toResultSummary(WritingResult result) {
        if (result == null) {
            return null;
        }
        return WritingResultSummaryDTO.builder()
            .id(result.getId())
            .taskId(result.getTaskId())
            .resultType(result.getResultType())
            .summary(result.getSummary())
            .content(result.getContent())
            .createdAt(result.getCreatedAt())
            .build();
    }

    private WritingDraftSummaryDTO toDraftSummary(WritingDraft draft) {
        if (draft == null) {
            return null;
        }
        return WritingDraftSummaryDTO.builder()
            .id(draft.getId())
            .versionNo(draft.getVersionNo())
            .title(draft.getTitle())
            .content(draft.getContent())
            .status(draft.getStatus())
            .createdAt(draft.getCreatedAt())
            .build();
    }
}
