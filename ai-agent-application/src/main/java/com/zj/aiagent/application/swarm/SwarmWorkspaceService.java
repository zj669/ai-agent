package com.zj.aiagent.application.swarm;

import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.application.writing.WritingSessionService;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmGroup;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmWorkspaceService {

    private final SwarmWorkspaceRepository workspaceRepository;
    private final SwarmAgentRepository agentRepository;
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmUIEventBus uiEventBus;
    private final WritingSessionService writingSessionService;

    /**
     * 创建 workspace，自动创建 assistant + P2P 群。
     * 用户直接作为发送方（通过 userId）参与 swarm 消息路由，不再需要 human 占位 Agent。
     * assistant agent (parentId=null) 作为 COORDINATOR 直接服务用户，自动投递回复到 P2P 群。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDefaultsDTO createWorkspace(
        Long userId,
        CreateWorkspaceRequest request
    ) {
        log.info(
            "[Swarm] Creating workspace: userId={}, name={}, llmConfigId={}",
            userId,
            request.getName(),
            request.getLlmConfigId()
        );
        // 1. 创建 workspace
        SwarmWorkspace workspace = SwarmWorkspace.builder()
            .name(request.getName())
            .userId(userId)
            .llmConfigId(request.getLlmConfigId())
            .maxRoundsPerTurn(10)
            .build();
        workspaceRepository.save(workspace);

        // 2. 创建 assistant agent（parentId = null，作为 COORDINATOR 直接服务用户，自动投递回复到 P2P 群）
        SwarmAgent assistantAgent = SwarmAgent.builder()
            .workspaceId(workspace.getId())
            .role("assistant")
            .parentId(null)
            .build();
        agentRepository.save(assistantAgent);

        // 3. 创建 P2P 群组
        SwarmGroup group = SwarmGroup.builder()
            .workspaceId(workspace.getId())
            .name("P2P")
            .build();
        groupRepository.save(group);

        // 4. 添加 assistant 到 P2P 群
        groupRepository.addMember(group.getId(), assistantAgent.getId());

        // 5. 创建 WritingSession（Workspace 级别的协作容器，绑定 Coordinator + P2P 群）
        WritingSession session = writingSessionService.createSession(
            workspace.getId(),
            assistantAgent.getId(),
            group.getId(),
            request.getName(),
            "Workspace collaboration session",
            null
        );

        // 6. 将 Coordinator 的 sessionId 和 sortOrder 直接写入 SwarmAgent
        assistantAgent.setSessionId(session.getId());
        assistantAgent.setSortOrder(0);
        // Update existing agent (already has ID from line 69)
        agentRepository.update(assistantAgent);

        log.info(
            "[Swarm] Created workspace: id={}, name={}, assistant={}, group={}",
            workspace.getId(),
            workspace.getName(),
            assistantAgent.getId(),
            group.getId()
        );

        return WorkspaceDefaultsDTO.builder()
            .workspaceId(workspace.getId())
            .userId(userId)
            .assistantAgentId(assistantAgent.getId())
            .defaultGroupId(group.getId())
            .build();
    }

    /**
     * 列出用户所有 workspace
     */
    public List<WorkspaceDTO> listWorkspaces(Long userId) {
        List<SwarmWorkspace> workspaces = workspaceRepository.findByUserId(
            userId
        );
        return workspaces
            .stream()
            .map(ws -> {
                int agentCount = agentRepository
                    .findByWorkspaceId(ws.getId())
                    .size();
                return WorkspaceDTO.builder()
                    .id(ws.getId())
                    .name(ws.getName())
                    .userId(ws.getUserId())
                    .agentCount(agentCount)
                    .maxRoundsPerTurn(ws.getMaxRoundsPerTurn())
                    .llmConfigId(ws.getLlmConfigId())
                    .createdAt(ws.getCreatedAt())
                    .updatedAt(ws.getUpdatedAt())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * workspace 详情
     */
    public WorkspaceDTO getWorkspace(Long id) {
        SwarmWorkspace ws = workspaceRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Workspace not found: " + id)
            );
        int agentCount = agentRepository.findByWorkspaceId(ws.getId()).size();
        return WorkspaceDTO.builder()
            .id(ws.getId())
            .name(ws.getName())
            .userId(ws.getUserId())
            .agentCount(agentCount)
            .maxRoundsPerTurn(ws.getMaxRoundsPerTurn())
            .llmConfigId(ws.getLlmConfigId())
            .createdAt(ws.getCreatedAt())
            .updatedAt(ws.getUpdatedAt())
            .build();
    }

    /**
     * 更新 workspace 配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkspace(Long id, UpdateWorkspaceRequest request) {
        SwarmWorkspace ws = workspaceRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Workspace not found: " + id)
            );
        if (request.getName() != null) ws.setName(request.getName());
        if (request.getMaxRoundsPerTurn() != null) ws.setMaxRoundsPerTurn(
            request.getMaxRoundsPerTurn()
        );
        if (request.getLlmConfigId() != null) ws.setLlmConfigId(
            request.getLlmConfigId()
        );
        workspaceRepository.update(ws);
    }

    /**
     * 删除 workspace（级联删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long id) {
        messageRepository.deleteByWorkspaceId(id);
        groupRepository.deleteByWorkspaceId(id);
        agentRepository.deleteByWorkspaceId(id);
        workspaceRepository.deleteById(id);
        log.info("[Swarm] Deleted workspace: id={}", id);
    }

    /**
     * 列出 workspace 下所有 Agent
     */
    public List<SwarmAgentDTO> listAgents(Long workspaceId) {
        return agentRepository
            .findByWorkspaceId(workspaceId)
            .stream()
            .map(a ->
                SwarmAgentDTO.builder()
                    .id(a.getId())
                    .workspaceId(a.getWorkspaceId())
                    .agentId(a.getAgentId())
                    .role(a.getRole())
                    .description(a.getDescription())
                    .parentId(a.getParentId())
                    .status(
                        a.getStatus() != null ? a.getStatus().getCode() : "IDLE"
                    )
                    .createdAt(a.getCreatedAt())
                    .build()
            )
            .collect(Collectors.toList());
    }

    /**
     * 创建 Agent（支持 description + 二方群：parent + 新agent）
     * 新创建的子 Agent 会自动加入父 Agent 所在群组。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDefaultsDTO createAgent(
        Long workspaceId,
        String role,
        Long parentId,
        String description
    ) {
        log.info(
            "[Swarm] Creating agent: workspace={}, role={}, parentId={}, descriptionPreview={}",
            workspaceId,
            role,
            parentId,
            preview(description)
        );
        SwarmAgent agent = SwarmAgent.builder()
            .workspaceId(workspaceId)
            .role(role)
            .description(description)
            .parentId(parentId)
            .build();
        agentRepository.save(agent);

        // emit UI event
        emitAgentCreated(workspaceId, agent);

        // 创建任务群：parent + 新agent
        Long groupId = null;
        if (parentId != null) {
            SwarmGroup group = SwarmGroup.builder()
                .workspaceId(workspaceId)
                .name(role)
                .build();
            groupRepository.save(group);
            groupRepository.addMember(group.getId(), parentId);
            groupRepository.addMember(group.getId(), agent.getId());
            groupId = group.getId();
        }

        log.info(
            "[Swarm] Agent created: workspace={}, agentId={}, role={}, parentId={}, groupId={}",
            workspaceId,
            agent.getId(),
            agent.getRole(),
            agent.getParentId(),
            groupId
        );

        return WorkspaceDefaultsDTO.builder()
            .workspaceId(workspaceId)
            .assistantAgentId(agent.getId())
            .defaultGroupId(groupId)
            .build();
    }

    // emit agent created event after createAgent
    private void emitAgentCreated(Long workspaceId, SwarmAgent agent) {
        uiEventBus.emit(
            workspaceId,
            SwarmUIEventBus.UIEvent.builder()
                .type("ui.agent.created")
                .data(
                    "{\"agentId\":" +
                        agent.getId() +
                        ",\"role\":\"" +
                        agent.getRole() +
                        "\",\"parentId\":" +
                        agent.getParentId() +
                        "}"
                )
                .timestamp(System.currentTimeMillis())
                .build()
        );
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

    /**
     * 获取 Agent 详情（含 llmHistory）
     */
    public SwarmAgent getAgent(Long agentId) {
        return agentRepository
            .findById(agentId)
            .orElseThrow(() ->
                new IllegalArgumentException("Agent not found: " + agentId)
            );
    }

    /**
     * 获取/确保默认资源（assistant + P2P 群）
     */
    public WorkspaceDefaultsDTO getDefaults(Long workspaceId) {
        SwarmWorkspace ws = workspaceRepository.findById(workspaceId).orElse(null);
        List<SwarmAgent> agents = agentRepository.findByWorkspaceId(
            workspaceId
        );
        SwarmAgent assistant = agents
            .stream()
            .filter(a -> "assistant".equals(a.getRole()))
            .findFirst()
            .orElse(null);
        List<SwarmGroup> groups = groupRepository.findByWorkspaceId(
            workspaceId
        );
        SwarmGroup defaultGroup = groups.isEmpty() ? null : groups.get(0);

        return WorkspaceDefaultsDTO.builder()
            .workspaceId(workspaceId)
            .userId(ws != null ? ws.getUserId() : null)
            .assistantAgentId(assistant != null ? assistant.getId() : null)
            .defaultGroupId(defaultGroup != null ? defaultGroup.getId() : null)
            .build();
    }

    /**
     * 获取 Agent 拓扑图数据（节点 + 边）
     */
    public SwarmGraphDTO getGraph(Long workspaceId) {
        List<SwarmAgent> agents = agentRepository.findByWorkspaceId(
            workspaceId
        );

        List<SwarmGraphDTO.GraphNode> nodes = agents
            .stream()
            .map(a ->
                SwarmGraphDTO.GraphNode.builder()
                    .id(a.getId())
                    .role(a.getRole())
                    .parentId(a.getParentId())
                    .status(
                        a.getStatus() != null ? a.getStatus().getCode() : "IDLE"
                    )
                    .build()
            )
            .collect(Collectors.toList());

        // 计算边：有 parent 关系的 agent 之间的消息数
        List<SwarmGraphDTO.GraphEdge> edges = agents
            .stream()
            .filter(a -> a.getParentId() != null)
            .map(a ->
                SwarmGraphDTO.GraphEdge.builder()
                    .from(a.getParentId())
                    .to(a.getId())
                    .count(
                        messageRepository.countMessagesBetween(
                            workspaceId,
                            a.getParentId(),
                            a.getId()
                        )
                    )
                    .build()
            )
            .collect(Collectors.toList());

        return SwarmGraphDTO.builder().nodes(nodes).edges(edges).build();
    }

    /**
     * 搜索 Agent 和 Group
     */
    public SwarmSearchDTO search(Long workspaceId, String query) {
        String q = query.toLowerCase();

        List<SwarmAgentDTO> matchedAgents = agentRepository
            .findByWorkspaceId(workspaceId)
            .stream()
            .filter(
                a ->
                    a.getRole() != null && a.getRole().toLowerCase().contains(q)
            )
            .map(a ->
                SwarmAgentDTO.builder()
                    .id(a.getId())
                    .workspaceId(a.getWorkspaceId())
                    .agentId(a.getAgentId())
                    .role(a.getRole())
                    .description(a.getDescription())
                    .parentId(a.getParentId())
                    .status(
                        a.getStatus() != null ? a.getStatus().getCode() : "IDLE"
                    )
                    .createdAt(a.getCreatedAt())
                    .build()
            )
            .collect(Collectors.toList());

        List<SwarmGroupDTO> matchedGroups = groupRepository
            .findByWorkspaceId(workspaceId)
            .stream()
            .filter(
                g ->
                    g.getName() != null && g.getName().toLowerCase().contains(q)
            )
            .map(g ->
                SwarmGroupDTO.builder()
                    .id(g.getId())
                    .workspaceId(g.getWorkspaceId())
                    .name(g.getName())
                    .build()
            )
            .collect(Collectors.toList());

        return SwarmSearchDTO.builder()
            .agents(matchedAgents)
            .groups(matchedGroups)
            .build();
    }
}
