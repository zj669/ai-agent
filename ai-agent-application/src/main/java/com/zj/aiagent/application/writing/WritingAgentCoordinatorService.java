package com.zj.aiagent.application.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.WorkspaceDefaultsDTO;
import com.zj.aiagent.domain.writing.entity.WritingAgent;
import com.zj.aiagent.domain.writing.repository.WritingAgentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingAgentCoordinatorService {

    private final SwarmWorkspaceService swarmWorkspaceService;
    private final WritingAgentRepository writingAgentRepository;

    @Transactional(rollbackFor = Exception.class)
    public WritingAgent createWritingAgent(
        Long sessionId,
        Long workspaceId,
        String role,
        Long parentSwarmAgentId,
        String description,
        JsonNode skillTagsJson,
        Integer sortOrder
    ) {
        log.info(
            "[Writing] Creating writing agent: sessionId={}, workspaceId={}, role={}, parentSwarmAgentId={}, descriptionPreview={}",
            sessionId,
            workspaceId,
            role,
            parentSwarmAgentId,
            preview(description)
        );
        WorkspaceDefaultsDTO agentDefaults = swarmWorkspaceService.createAgent(
            workspaceId,
            role,
            parentSwarmAgentId,
            description
        );

        WritingAgent agent = WritingAgent.builder()
            .sessionId(sessionId)
            .swarmAgentId(agentDefaults.getAssistantAgentId())
            .role(role)
            .description(description)
            .skillTagsJson(skillTagsJson)
            .status("IDLE")
            .sortOrder(sortOrder != null ? sortOrder : 0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        writingAgentRepository.save(agent);
        log.info(
            "[Writing] Writing agent created: sessionId={}, writingAgentId={}, swarmAgentId={}, role={}, status={}",
            sessionId,
            agent.getId(),
            agent.getSwarmAgentId(),
            agent.getRole(),
            agent.getStatus()
        );
        return agent;
    }

    public WritingAgent getAgent(Long writingAgentId) {
        return writingAgentRepository
            .findById(writingAgentId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing agent not found: " + writingAgentId
                )
            );
    }

    public List<WritingAgent> listAgents(Long sessionId) {
        return writingAgentRepository.findBySessionId(sessionId);
    }

    public WritingAgent findBySessionAndSwarmAgent(
        Long sessionId,
        Long swarmAgentId
    ) {
        return writingAgentRepository
            .findBySessionIdAndSwarmAgentId(sessionId, swarmAgentId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Writing agent not found by session/swarm: sessionId=" +
                        sessionId +
                        ", swarmAgentId=" +
                        swarmAgentId
                )
            );
    }

    @Transactional(rollbackFor = Exception.class)
    public WritingAgent updateStatus(Long writingAgentId, String status) {
        WritingAgent agent = getAgent(writingAgentId);
        String previousStatus = agent.getStatus();
        agent.setStatus(status);
        agent.setUpdatedAt(LocalDateTime.now());
        writingAgentRepository.update(agent);
        log.info(
            "[Writing] Writing agent status updated: writingAgentId={}, swarmAgentId={}, from={}, to={}",
            writingAgentId,
            agent.getSwarmAgentId(),
            previousStatus,
            status
        );
        return agent;
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120
            ? normalized
            : normalized.substring(0, 120) + "...";
    }
}
