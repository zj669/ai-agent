package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 蜂群内置工具执行器：执行 6 个工具的实际逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwarmToolExecutor {

    private final SwarmWorkspaceService workspaceService;
    private final SwarmMessageService messageService;
    private final ObjectMapper objectMapper;

    public String execute(String toolName, String arguments, SwarmAgent callerAgent) {
        try {
            JsonNode args = objectMapper.readTree(arguments != null ? arguments : "{}");
            return switch (toolName) {
                case "create" -> executeCreate(args, callerAgent);
                case "send" -> executeSend(args, callerAgent);
                case "self" -> executeSelf(callerAgent);
                case "list_agents" -> executeListAgents(callerAgent);
                case "send_group_message" -> executeSendGroupMessage(args, callerAgent);
                case "list_groups" -> executeListGroups(callerAgent);
                default -> "{\"error\": \"Unknown tool: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.error("[Swarm] Tool execution failed: tool={}, agent={}", toolName, callerAgent.getId(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String executeCreate(JsonNode args, SwarmAgent caller) throws Exception {
        String role = args.has("role") ? args.get("role").asText() : "assistant";
        String description = args.has("description") ? args.get("description").asText() : null;
        WorkspaceDefaultsDTO result = workspaceService.createAgent(
                caller.getWorkspaceId(), role, caller.getId(), description);
        return objectMapper.writeValueAsString(result);
    }

    private String executeSend(JsonNode args, SwarmAgent caller) throws Exception {
        long targetAgentId = args.get("agent_id").asLong();
        String message = args.get("message").asText();

        List<SwarmGroupDTO> groups = messageService.listGroups(caller.getWorkspaceId(), caller.getId());
        Long groupId = null;
        for (SwarmGroupDTO g : groups) {
            if (g.getMemberIds() != null && g.getMemberIds().contains(targetAgentId) && g.getMemberIds().size() == 2) {
                groupId = g.getId();
                break;
            }
        }

        if (groupId == null) {
            WorkspaceDefaultsDTO newAgent = workspaceService.createAgent(
                    caller.getWorkspaceId(), "p2p", caller.getId(), null);
            groupId = newAgent.getDefaultGroupId();
        }

        SendMessageRequest req = new SendMessageRequest();
        req.setSenderId(caller.getId());
        req.setContent(message);
        SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
        return objectMapper.writeValueAsString(sent);
    }

    private String executeSelf(SwarmAgent caller) throws Exception {
        return objectMapper.writeValueAsString(SwarmAgentDTO.builder()
                .id(caller.getId())
                .workspaceId(caller.getWorkspaceId())
                .role(caller.getRole())
                .parentId(caller.getParentId())
                .status(caller.getStatus() != null ? caller.getStatus().getCode() : "IDLE")
                .build());
    }

    private String executeListAgents(SwarmAgent caller) throws Exception {
        List<SwarmAgentDTO> agents = workspaceService.listAgents(caller.getWorkspaceId());
        return objectMapper.writeValueAsString(agents);
    }

    private String executeSendGroupMessage(JsonNode args, SwarmAgent caller) throws Exception {
        long groupId = args.get("group_id").asLong();
        String message = args.get("message").asText();

        SendMessageRequest req = new SendMessageRequest();
        req.setSenderId(caller.getId());
        req.setContent(message);
        SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
        return objectMapper.writeValueAsString(sent);
    }

    private String executeListGroups(SwarmAgent caller) throws Exception {
        List<SwarmGroupDTO> groups = messageService.listGroups(caller.getWorkspaceId(), caller.getId());
        return objectMapper.writeValueAsString(groups);
    }
}
