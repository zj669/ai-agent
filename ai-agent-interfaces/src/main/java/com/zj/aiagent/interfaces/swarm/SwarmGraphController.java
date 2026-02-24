package com.zj.aiagent.interfaces.swarm;

import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.SwarmGraphDTO;
import com.zj.aiagent.application.swarm.dto.SwarmSearchDTO;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SwarmGraphController {

    private final SwarmWorkspaceService workspaceService;

    @GetMapping("/api/swarm/workspace/{wid}/graph")
    public Response<SwarmGraphDTO> getGraph(@PathVariable Long wid) {
        return Response.success(workspaceService.getGraph(wid));
    }

    @GetMapping("/api/swarm/workspace/{wid}/search")
    public Response<SwarmSearchDTO> search(@PathVariable Long wid, @RequestParam String q) {
        return Response.success(workspaceService.search(wid, q));
    }
}
