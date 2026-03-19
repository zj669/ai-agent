package com.zj.aiagent.interfaces.writing;

import com.zj.aiagent.application.writing.WritingProjectionService;
import com.zj.aiagent.application.writing.dto.WritingSessionOverviewDTO;
import com.zj.aiagent.application.writing.dto.WritingSessionSummaryDTO;
import com.zj.aiagent.shared.response.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingProjectionService writingProjectionService;

    @GetMapping("/workspace/{workspaceId}/sessions")
    public Response<List<WritingSessionSummaryDTO>> listSessions(
        @PathVariable Long workspaceId
    ) {
        return Response.success(writingProjectionService.listSessions(workspaceId));
    }

    @GetMapping("/session/{sessionId}/overview")
    public Response<WritingSessionOverviewDTO> getSessionOverview(
        @PathVariable Long sessionId
    ) {
        return Response.success(writingProjectionService.getSessionOverview(sessionId));
    }
}
