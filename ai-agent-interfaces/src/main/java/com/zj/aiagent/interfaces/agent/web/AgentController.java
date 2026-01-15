package com.zj.aiagent.interfaces.agent.web;

import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.application.agent.dto.AgentDetailResult;
import com.zj.aiagent.application.agent.dto.AgentRequest;
import com.zj.aiagent.application.agent.dto.VersionHistoryResult;
import com.zj.aiagent.application.agent.service.AgentApplicationService;
import com.zj.aiagent.domain.agent.valobj.AgentSummary;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentApplicationService agentApplicationService;

    // --- Commands ---

    @PostMapping("/create")
    public Response<Long> createAgent(
            @Validated(AgentRequest.Create.class) @RequestBody AgentRequest.SaveAgentRequest req) {
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

    @PostMapping("/update")
    public Response<Void> updateAgent(
            @Validated(AgentRequest.Update.class) @RequestBody AgentRequest.SaveAgentRequest req) {
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
    public Response<Void> publishAgent(@Validated @RequestBody AgentRequest.PublishAgentRequest req) {
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
    public Response<Void> rollbackAgent(@Validated @RequestBody AgentRequest.RollbackAgentRequest req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.RollbackAgentCmd cmd = new AgentCommand.RollbackAgentCmd();
        cmd.setId(req.getId());
        cmd.setUserId(userId);
        cmd.setTargetVersion(req.getTargetVersion());

        agentApplicationService.rollbackAgent(cmd);
        return Response.success();
    }

    /**
     * 删除指定版本
     */
    @DeleteMapping("/{id}/versions/{version}")
    public Response<Void> deleteAgentVersion(@PathVariable Long id, @PathVariable Integer version) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.DeleteVersionCmd cmd = new AgentCommand.DeleteVersionCmd();
        cmd.setAgentId(id);
        cmd.setVersion(version);
        cmd.setUserId(userId);

        agentApplicationService.deleteAgentVersion(cmd);
        return Response.success();
    }

    /**
     * 强制删除智能体（包括所有版本）
     */
    @DeleteMapping("/{id}/force")
    public Response<Void> forceDeleteAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        AgentCommand.DeleteAgentCmd cmd = new AgentCommand.DeleteAgentCmd();
        cmd.setId(id);
        cmd.setUserId(userId);

        agentApplicationService.forceDeleteAgent(cmd);
        return Response.success();
    }

    // --- Queries ---

    @GetMapping("/list")
    public Response<List<AgentSummary>> listAgents() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        return Response.success(agentApplicationService.listAgents(userId));
    }

    @GetMapping("/{id}")
    public Response<AgentDetailResult> getAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        return Response.success(agentApplicationService.getAgentDetail(id, userId));
    }

    /**
     * 获取智能体版本历史
     */
    @GetMapping("/{id}/versions")
    public Response<VersionHistoryResult> getVersionHistory(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        return Response.success(agentApplicationService.getVersionHistory(id, userId));
    }
}
