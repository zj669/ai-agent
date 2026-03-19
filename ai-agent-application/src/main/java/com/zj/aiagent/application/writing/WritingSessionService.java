package com.zj.aiagent.application.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.repository.WritingSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingSessionService {

    private final WritingSessionRepository writingSessionRepository;

    @Transactional(rollbackFor = Exception.class)
    public WritingSession createSession(
        Long workspaceId,
        Long rootAgentId,
        Long humanAgentId,
        Long defaultGroupId,
        String title,
        String goal,
        JsonNode constraintsJson
    ) {
        log.info(
            "[Writing] Creating session: workspaceId={}, rootAgentId={}, humanAgentId={}, defaultGroupId={}, title={}",
            workspaceId,
            rootAgentId,
            humanAgentId,
            defaultGroupId,
            title
        );
        WritingSession session = WritingSession.builder()
            .workspaceId(workspaceId)
            .rootAgentId(rootAgentId)
            .humanAgentId(humanAgentId)
            .defaultGroupId(defaultGroupId)
            .title(title)
            .goal(goal)
            .constraintsJson(constraintsJson)
            .status("PLANNING")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        writingSessionRepository.save(session);
        log.info(
            "[Writing] Session created: sessionId={}, workspaceId={}, status={}",
            session.getId(),
            session.getWorkspaceId(),
            session.getStatus()
        );
        return session;
    }

    public WritingSession getSession(Long sessionId) {
        return writingSessionRepository
            .findById(sessionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing session not found: " + sessionId
                )
            );
    }

    public List<WritingSession> listSessions(Long workspaceId) {
        return writingSessionRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingSession updateStatus(Long sessionId, String status) {
        WritingSession session = getSession(sessionId);
        String previousStatus = session.getStatus();
        session.setStatus(status);
        session.setUpdatedAt(LocalDateTime.now());
        writingSessionRepository.update(session);
        log.info(
            "[Writing] Session status updated: sessionId={}, from={}, to={}",
            sessionId,
            previousStatus,
            status
        );
        return session;
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingSession updateCurrentDraft(
        Long sessionId,
        Long draftId,
        String status
    ) {
        WritingSession session = getSession(sessionId);
        session.setCurrentDraftId(draftId);
        String previousStatus = session.getStatus();
        if (status != null && !status.isBlank()) {
            session.setStatus(status);
        }
        session.setUpdatedAt(LocalDateTime.now());
        writingSessionRepository.update(session);
        log.info(
            "[Writing] Session current draft updated: sessionId={}, draftId={}, statusFrom={}, statusTo={}",
            sessionId,
            draftId,
            previousStatus,
            session.getStatus()
        );
        return session;
    }
}
