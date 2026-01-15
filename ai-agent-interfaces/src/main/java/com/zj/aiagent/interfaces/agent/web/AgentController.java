package com.zj.aiagent.interfaces.agent.web;

import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.application.agent.service.AgentApplicationService;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.agent.valobj.AgentSummary;
import com.zj.aiagent.interfaces.agent.dto.AgentDTO;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentApplicationService agentApplicationService;
    private final AgentRepository agentRepository; // For Query

    // --- Commands ---

    @PostMapping("/create")
    public Response<Long> createAgent(@Validated(AgentDTO.Create.class) @RequestBody AgentDTO.AgentSaveReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        AgentCommand.CreateAgentCmd cmd = new AgentCommand.CreateAgentCmd();
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setIcon(req.getIcon());

        return Response.success(agentApplicationService.createAgent(cmd));
    }

    @PutMapping("/update")
    public Response<Void> updateAgent(@Validated(AgentDTO.Update.class) @RequestBody AgentDTO.AgentSaveReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        AgentCommand.UpdateAgentCmd cmd = new AgentCommand.UpdateAgentCmd();
        cmd.setId(req.getId());
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setIcon(req.getIcon());
        cmd.setGraphJson(req.getGraphJson());
        cmd.setVersion(req.getVersion());

        agentApplicationService.updateAgent(cmd);
        return Response.success();
    }

    @PostMapping("/publish")
    public Response<Void> publishAgent(@Validated @RequestBody AgentDTO.PublishReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.PublishAgentCmd cmd = new AgentCommand.PublishAgentCmd();
        cmd.setId(req.getId());
        cmd.setUserId(userId);

        agentApplicationService.publishAgent(cmd);
        return Response.success();
    }

    @PostMapping("/rollback")
    public Response<Void> rollbackAgent(@Validated @RequestBody AgentDTO.RollbackReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.RollbackAgentCmd cmd = new AgentCommand.RollbackAgentCmd();
        cmd.setId(req.getId()); // Fix: was using req.getId() but RollbackReq has id? Yes.
        cmd.setUserId(userId);
        cmd.setTargetVersion(req.getTargetVersion());

        agentApplicationService.rollbackAgent(cmd);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.DeleteAgentCmd cmd = new AgentCommand.DeleteAgentCmd();
        cmd.setId(id);
        cmd.setUserId(userId);

        agentApplicationService.deleteAgent(cmd);
        return Response.success();
    }

    @Deprecated
    @PostMapping("/delete/{id}")
    public Response<Void> deleteAgentDeprecated(@PathVariable Long id,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("X-Deprecated-API", "true");
        return deleteAgent(id);
    }

    // --- Queries ---

    @GetMapping("/list")
    public Response<List<AgentSummary>> listAgents() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        return Response.success(agentRepository.findSummaryByUserId(userId));
    }

    @GetMapping("/{id}")
    public Response<com.zj.aiagent.domain.agent.entity.Agent> getAgent(@PathVariable Long id) {
        // Direct repo call for query, or via Service if specific DTO needed
        return Response.success(agentRepository.findById(id).orElse(null));
    }

    // --- Debug ---
    // Note: Debug functionality moved to WorkflowController.startExecution with
    // mode=DEBUG
    // This endpoint is removed. Use POST /api/workflow/execution/start with
    // mode="DEBUG" instead.
}
