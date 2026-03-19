package com.zj.aiagent.interfaces.swarm;

import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/swarm/workspace")
@RequiredArgsConstructor
public class SwarmWorkspaceController {

    private final SwarmWorkspaceService workspaceService;

    @PostMapping
    public Response<WorkspaceDefaultsDTO> createWorkspace(
        @RequestBody CreateWorkspaceRequest request
    ) {
        Long userId = UserContext.getUserId();
        log.info(
            "[SwarmAPI] Create workspace request received: userId={}, name={}, llmConfigId={}",
            userId,
            request.getName(),
            request.getLlmConfigId()
        );
        return Response.success(
            workspaceService.createWorkspace(userId, request)
        );
    }

    @GetMapping
    public Response<List<WorkspaceDTO>> listWorkspaces() {
        Long userId = UserContext.getUserId();
        return Response.success(workspaceService.listWorkspaces(userId));
    }

    @GetMapping("/{id}")
    public Response<WorkspaceDTO> getWorkspace(@PathVariable Long id) {
        return Response.success(workspaceService.getWorkspace(id));
    }

    @PutMapping("/{id}")
    public Response<Void> updateWorkspace(
        @PathVariable Long id,
        @RequestBody UpdateWorkspaceRequest request
    ) {
        workspaceService.updateWorkspace(id, request);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return Response.success();
    }

    @GetMapping("/{id}/defaults")
    public Response<WorkspaceDefaultsDTO> getDefaults(@PathVariable Long id) {
        return Response.success(workspaceService.getDefaults(id));
    }
}
