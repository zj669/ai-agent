package com.zj.aiagent.application.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.zj.aiagent.domain.writing.entity.WritingDraft;
import com.zj.aiagent.domain.writing.repository.WritingDraftRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingDraftService {

    private final WritingDraftRepository writingDraftRepository;
    private final WritingSessionService writingSessionService;

    @Transactional(rollbackFor = Exception.class)
    public WritingDraft createDraft(
        Long sessionId,
        Integer versionNo,
        String title,
        String content,
        JsonNode sourceResultIdsJson,
        String status,
        Long createdBySwarmAgentId,
        boolean setAsCurrent
    ) {
        log.info(
            "[Writing] Creating draft: sessionId={}, versionNo={}, title={}, status={}, createdBy={}, setAsCurrent={}",
            sessionId,
            versionNo,
            title,
            status,
            createdBySwarmAgentId,
            setAsCurrent
        );
        WritingDraft draft = WritingDraft.builder()
            .sessionId(sessionId)
            .versionNo(versionNo)
            .title(title)
            .content(content)
            .sourceResultIdsJson(sourceResultIdsJson)
            .status(status != null && !status.isBlank() ? status : "DRAFT")
            .createdBySwarmAgentId(createdBySwarmAgentId)
            .createdAt(LocalDateTime.now())
            .build();
        writingDraftRepository.save(draft);
        log.info(
            "[Writing] Draft created: draftId={}, sessionId={}, versionNo={}, status={}",
            draft.getId(),
            draft.getSessionId(),
            draft.getVersionNo(),
            draft.getStatus()
        );

        if (setAsCurrent) {
            writingSessionService.updateCurrentDraft(
                sessionId,
                draft.getId(),
                "DRAFTING"
            );
        }
        return draft;
    }

    public WritingDraft getDraft(Long draftId) {
        return writingDraftRepository
            .findById(draftId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing draft not found: " + draftId
                )
            );
    }

    public WritingDraft getLatestDraft(Long sessionId) {
        return writingDraftRepository
            .findLatestBySessionId(sessionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Latest writing draft not found: " + sessionId
                )
            );
    }

    public List<WritingDraft> listBySession(Long sessionId) {
        return writingDraftRepository.findBySessionId(sessionId);
    }
}
