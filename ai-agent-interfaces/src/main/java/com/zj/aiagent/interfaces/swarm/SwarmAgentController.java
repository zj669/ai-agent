package com.zj.aiagent.interfaces.swarm;

import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.SwarmAgentDTO;
import com.zj.aiagent.application.swarm.dto.WorkspaceDefaultsDTO;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SwarmAgentController {

    private final SwarmWorkspaceService workspaceService;

    @GetMapping("/api/swarm/workspace/{wid}/agents")
    public Response<List<SwarmAgentDTO>> listAgents(@PathVariable Long wid) {
        return Response.success(workspaceService.listAgents(wid));
    }

    @PostMapping("/api/swarm/workspace/{wid}/agents")
    public Response<WorkspaceDefaultsDTO> createAgent(
            @PathVariable Long wid,
            @RequestBody Map<String, Object> body) {
        String role = (String) body.getOrDefault("role", "assistant");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return Response.success(workspaceService.createAgent(wid, role, parentId));
    }

    @GetMapping("/api/swarm/agent/{id}")
    public Response<SwarmAgentDTO> getAgent(@PathVariable Long id) {
        SwarmAgent agent = workspaceService.getAgent(id);
        SwarmAgentDTO dto = SwarmAgentDTO.builder()
                .id(agent.getId())
                .workspaceId(agent.getWorkspaceId())
                .agentId(agent.getAgentId())
                .role(agent.getRole())
                .parentId(agent.getParentId())
                .status(agent.getStatus() != null ? agent.getStatus().getCode() : "IDLE")
                .createdAt(agent.getCreatedAt())
                .build();
        return Response.success(dto);
    }

    @PostMapping("/api/swarm/agents/interrupt-all")
    public Response<Void> interruptAll(@RequestBody Map<String, Long> body) {
        // TODO P2: 实现 Agent Runtime 中断逻辑
        log.info("[Swarm] Interrupt all agents for workspace: {}", body.get("workspaceId"));
        return Response.success();
    }
}
