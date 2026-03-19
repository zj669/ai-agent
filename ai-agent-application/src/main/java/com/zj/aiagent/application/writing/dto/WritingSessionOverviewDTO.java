package com.zj.aiagent.application.writing.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingSessionOverviewDTO {

    private WritingSessionSummaryDTO session;
    private List<WritingMessageViewDTO> rootConversation;
    private List<WritingCollaborationCardDTO> collaborationCards;
    private WritingDraftSummaryDTO latestDraft;
}
