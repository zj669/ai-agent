package com.zj.aiagent.application.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.domain.agent.entity.Agent;
import com.zj.aiagent.domain.agent.entity.AgentVersion;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.agent.service.GraphValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    private final AgentRepository agentRepository;
    private final GraphValidator graphValidator;
    private final ObjectMapper objectMapper;

    private String initialGraphTemplate;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/agent-initial-graph.json");
            initialGraphTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("[AgentService] Loaded initial graph template");
        } catch (IOException e) {
            log.error("[AgentService] Failed to load initial graph template", e);
            initialGraphTemplate = "{}";
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentCommand.CreateAgentCmd cmd) {
        String graphJson = generateInitialGraphJson();

        Agent agent = Agent.builder()
                .userId(cmd.getUserId())
                .name(cmd.getName())
                .description(cmd.getDescription())
                .icon(cmd.getIcon())
                .graphJson(graphJson)
                .version(1)
                .build();

        agentRepository.save(agent);
        return agent.getId();
    }

    /**
     * 生成初始化 graphJson，设置唯一的 dagId
     */
    private String generateInitialGraphJson() {
        try {
            JsonNode root = objectMapper.readTree(initialGraphTemplate);
            if (root.isObject()) {
                ((ObjectNode) root).put("dagId", "dag-" + UUID.randomUUID().toString().substring(0, 8));
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("[AgentService] Failed to generate initial graph JSON", e);
            return initialGraphTemplate;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(AgentCommand.UpdateAgentCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + cmd.getId()));

        checkOwnership(agent, cmd.getUserId());

        // Optimistic locking check is inside this method, but Repo handles the DB check
        // too
        agent.updateConfig(
                cmd.getName(),
                cmd.getDescription(),
                cmd.getIcon(),
                cmd.getGraphJson(),
                cmd.getVersion());

        agentRepository.save(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(AgentCommand.PublishAgentCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        checkOwnership(agent, cmd.getUserId());

        Integer nextVersion = agentRepository.findMaxVersion(agent.getId()).orElse(0) + 1;
        AgentVersion version = agent.publish(graphValidator, nextVersion);

        agentRepository.save(agent);
        agentRepository.saveVersion(version);

        // Update publishedVersionId on Agent to point to new version
        // (Wait, `agent.publish` updates `publishedVersionId` field in Agent?
        // No, `Agent.java` I wrote set status=PUBLISHED but didn't set
        // `publishedVersionId`.
        // I should set it here after getting the persisted version ID, OR Logic should
        // be:
        // agent.publish returns AgentVersion. We save AgentVersion. Get ID. Update
        // Agent?
        // Let's check Agent.java logic I wrote.
        // It returns AgentVersion.
        // To update `publishedVersionId` on Agent table, I need the ID of the inserted
        // version.
        // So:
        // 1. Repo.saveVersion(version) -> fills ID.
        // 2. agent.setPublishedVersionId(version.getId());
        // 3. Repo.save(agent).
        // I need to update Agent.java to allow setting publishedVersionId or add a
        // method `onPublished(id)`.
        // Or just `agent.setPublishedVersionId(...)` if Lombok Data is used (it is).
        agent.setPublishedVersionId(version.getId());
        agentRepository.save(agent); // Second save to update the pointer
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackAgent(AgentCommand.RollbackAgentCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        checkOwnership(agent, cmd.getUserId());

        AgentVersion targetVersion = agentRepository.findVersion(agent.getId(), cmd.getTargetVersion())
                .orElseThrow(() -> new IllegalArgumentException("Target version not found: " + cmd.getTargetVersion()));

        agent.rollbackTo(targetVersion);
        agentRepository.save(agent);
    }

    /**
     * 删除指定版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgentVersion(AgentCommand.DeleteVersionCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        checkOwnership(agent, cmd.getUserId());

        // 检查是否为已发布版本
        if (agent.getPublishedVersionId() != null &&
                agent.getPublishedVersionId().intValue() == cmd.getVersion()) {
            throw new IllegalStateException("Cannot delete published version. Unpublish first.");
        }

        agentRepository.deleteVersion(cmd.getAgentId(), cmd.getVersion());
        log.info("Deleted version {} of agent {}", cmd.getVersion(), cmd.getAgentId());
    }

    /**
     * 强制删除智能体（包括所有版本）
     */
    @Transactional(rollbackFor = Exception.class)
    public void forceDeleteAgent(AgentCommand.DeleteAgentCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        checkOwnership(agent, cmd.getUserId());

        // 1. 先删除所有版本
        agentRepository.deleteAllVersions(cmd.getId());
        log.info("Deleted all versions of agent {}", cmd.getId());

        // 2. 再删除智能体本身
        agentRepository.deleteById(cmd.getId());
        log.info("Force deleted agent {}", cmd.getId());
    }

    /**
     * 软删除智能体（保留版本，仅标记删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(AgentCommand.DeleteAgentCmd cmd) {
        Agent agent = agentRepository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        checkOwnership(agent, cmd.getUserId());

        // 软删除：标记 deleted = 1
        agent.setDeleted(1);
        agentRepository.save(agent);
        log.info("Soft deleted agent {}", cmd.getId());
    }

    // --- Helper ---

    private void checkOwnership(Agent agent, Long userId) {
        if (!agent.isOwnedBy(userId)) {
            throw new SecurityException("Unauthorized: Agent does not belong to user " + userId);
        }
    }

    /**
     * 获取智能体详情
     * 
     * @param agentId 智能体ID
     * @param userId  当前用户ID（用于权限校验）
     * @return 智能体详情结果
     */
    public com.zj.aiagent.application.agent.dto.AgentDetailResult getAgentDetail(Long agentId, Long userId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        checkOwnership(agent, userId);

        return com.zj.aiagent.application.agent.dto.AgentDetailResult.from(agent);
    }

    /**
     * 查询用户的智能体列表
     */
    public java.util.List<com.zj.aiagent.domain.agent.valobj.AgentSummary> listAgents(Long userId) {
        return agentRepository.findSummaryByUserId(userId);
    }

    /**
     * 查询智能体的版本历史
     */
    /**
     * 查询智能体的版本历史
     */
    public com.zj.aiagent.application.agent.dto.VersionHistoryResult getVersionHistory(Long agentId, Long userId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        checkOwnership(agent, userId);

        java.util.List<AgentVersion> versions = agentRepository.findVersionHistory(agentId);
        return com.zj.aiagent.application.agent.dto.VersionHistoryResult.from(versions);
    }
}
