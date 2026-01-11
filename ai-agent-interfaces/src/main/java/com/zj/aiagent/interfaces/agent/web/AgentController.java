package com.zj.aiagent.interfaces.agent.web;

import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.application.agent.service.AgentApplicationService;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.agent.valobj.AgentSummary;
import com.zj.aiagent.interfaces.agent.dto.AgentDTO;
import com.zj.aiagent.shared.response.Response; // Assuming shared response wrapper
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
        // Assume userId from context/interceptor. For now hardcoded or passed (TODO:
        // Integrate Auth)
        Long userId = 1L; // Placeholder

        AgentCommand.CreateAgentCmd cmd = new AgentCommand.CreateAgentCmd();
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setIcon(req.getIcon());

        return Response.success(agentApplicationService.createAgent(cmd));
    }

    @PutMapping("/update")
    public Response<Void> updateAgent(@Validated(AgentDTO.Update.class) @RequestBody AgentDTO.AgentSaveReq req) {
        Long userId = 1L; // Placeholder

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
        Long userId = 1L;
        AgentCommand.PublishAgentCmd cmd = new AgentCommand.PublishAgentCmd();
        cmd.setId(req.getId());
        cmd.setUserId(userId);

        agentApplicationService.publishAgent(cmd);
        return Response.success();
    }

    @PostMapping("/rollback")
    public Response<Void> rollbackAgent(@Validated @RequestBody AgentDTO.RollbackReq req) {
        Long userId = 1L;
        AgentCommand.RollbackAgentCmd cmd = new AgentCommand.RollbackAgentCmd();
        cmd.setId(req.getId()); // Fix: was using req.getId() but RollbackReq has id? Yes.
        cmd.setUserId(userId);
        cmd.setTargetVersion(req.getTargetVersion());

        agentApplicationService.rollbackAgent(cmd);
        return Response.success();
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deleteAgent(@PathVariable Long id) {
        Long userId = 1L;
        AgentCommand.DeleteAgentCmd cmd = new AgentCommand.DeleteAgentCmd();
        cmd.setId(id);
        cmd.setUserId(userId);

        agentApplicationService.deleteAgent(cmd);
        return Response.success();
    }

    // --- Queries ---

    @GetMapping("/list")
    public Response<List<AgentSummary>> listAgents() {
        Long userId = 1L;
        return Response.success(agentRepository.findSummaryByUserId(userId));
    }

    @GetMapping("/{id}")
    public Response<com.zj.aiagent.domain.agent.entity.Agent> getAgent(@PathVariable Long id) {
        // Direct repo call for query, or via Service if specific DTO needed
        return Response.success(agentRepository.findById(id).orElse(null));
    }

    // --- Debug ---

    @PostMapping("/debug")
    public SseEmitter debugAgent(@Validated @RequestBody AgentDTO.DebugReq req) {
        Long userId = 1L;
        AgentCommand.DebugAgentCmd cmd = new AgentCommand.DebugAgentCmd();
        cmd.setAgentId(req.getAgentId());
        cmd.setUserId(userId);
        cmd.setInputMessage(req.getInputMessage());
        cmd.setDebugMode(req.isDebugMode());

        SseEmitter emitter = new SseEmitter(180_000L); // 3 min timeout

        // Asynchronous execution invocation
        // Ideally pass emitter to service, or return Flux.
        // For now, placeholder for triggering execution.
        // agentApplicationService.debugAgent(cmd, emitter);

        try {
            emitter.send(SseEmitter.event().name("start").data("Debug Started"));
            // Mock streaming
            emitter.send(SseEmitter.event().name("log").data("Draft Mode: " + req.isDebugMode()));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
