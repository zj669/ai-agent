package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * 蜂群内置工具（@Tool 注解方式）
 * 每个 AgentRunner 创建一个实例，绑定 callerAgent 上下文
 */
@Slf4j
public class SwarmTools {

    private final SwarmWorkspaceService workspaceService;
    private final SwarmMessageService messageService;
    private final SwarmAgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private final Long callerAgentId;
    private final Long callerWorkspaceId;

    public SwarmTools(SwarmWorkspaceService workspaceService,
                      SwarmMessageService messageService,
                      SwarmAgentRepository agentRepository,
                      ObjectMapper objectMapper,
                      Long callerAgentId,
                      Long callerWorkspaceId) {
        this.workspaceService = workspaceService;
        this.messageService = messageService;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
        this.callerAgentId = callerAgentId;
        this.callerWorkspaceId = callerWorkspaceId;
    }

    @Tool(description = "创建子Agent。根据任务复杂度决定是否需要创建专门的子Agent来处理子任务。创建后会自动建立包含人类的任务群。")
    public String create(
            @ToolParam(description = "子Agent的角色名称，如 coder/researcher/reviewer/analyst") String role,
            @ToolParam(description = "子Agent的能力边界和职责描述，详细说明它负责什么任务") String description) {
        try {
            WorkspaceDefaultsDTO result = workspaceService.createAgent(
                    callerWorkspaceId, role, callerAgentId, description);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] create failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "向指定Agent发送消息。用于委派任务给子Agent或回复其他Agent的消息。注意：不要用此工具回复人类，你的文字输出会直接展示给人类。")
    public String send(
            @ToolParam(description = "目标Agent的ID") long agentId,
            @ToolParam(description = "消息内容") String message) {
        try {
            List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
            Long groupId = null;
            for (SwarmGroupDTO g : groups) {
                if (g.getMemberIds() != null && g.getMemberIds().contains(agentId)) {
                    groupId = g.getId();
                    break;
                }
            }

            if (groupId == null) {
                return "{\"error\": \"No group found with agent " + agentId + "\"}";
            }

            SendMessageRequest req = new SendMessageRequest();
            req.setSenderId(callerAgentId);
            req.setContent(message);
            SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
            return objectMapper.writeValueAsString(sent);
        } catch (Exception e) {
            log.error("[SwarmTools] send failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "返回自身信息，包括 agent_id、workspace_id、角色、状态等")
    public String self() {
        try {
            SwarmAgent agent = agentRepository.findById(callerAgentId).orElse(null);
            if (agent == null) return "{\"error\": \"Agent not found\"}";
            return objectMapper.writeValueAsString(SwarmAgentDTO.builder()
                    .id(agent.getId())
                    .workspaceId(agent.getWorkspaceId())
                    .role(agent.getRole())
                    .description(agent.getDescription())
                    .parentId(agent.getParentId())
                    .status(agent.getStatus() != null ? agent.getStatus().getCode() : "IDLE")
                    .build());
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "列出当前workspace中所有Agent，包括它们的ID、角色、状态和父子关系")
    public String listAgents() {
        try {
            List<SwarmAgentDTO> agents = workspaceService.listAgents(callerWorkspaceId);
            return objectMapper.writeValueAsString(agents);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "向指定群组发送消息")
    public String sendGroupMessage(
            @ToolParam(description = "群组ID") long groupId,
            @ToolParam(description = "消息内容") String message) {
        try {
            SendMessageRequest req = new SendMessageRequest();
            req.setSenderId(callerAgentId);
            req.setContent(message);
            SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
            return objectMapper.writeValueAsString(sent);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(description = "列出当前Agent可见的所有群组")
    public String listGroups() {
        try {
            List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
            return objectMapper.writeValueAsString(groups);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
