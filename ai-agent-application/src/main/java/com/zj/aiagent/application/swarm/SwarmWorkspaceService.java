package com.zj.aiagent.application.swarm;

import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmGroup;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmWorkspaceService {

    private final SwarmWorkspaceRepository workspaceRepository;
    private final SwarmAgentRepository agentRepository;
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmUIEventBus uiEventBus;

    /**
     * 创建 workspace，自动创建 human + assistant + P2P 群
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDefaultsDTO createWorkspace(Long userId, CreateWorkspaceRequest request) {
        // 1. 创建 workspace
        SwarmWorkspace workspace = SwarmWorkspace.builder()
                .name(request.getName())
                .userId(userId)
                .llmConfigId(request.getLlmConfigId())
                .maxRoundsPerTurn(10)
                .build();
        workspaceRepository.save(workspace);

        // 2. 创建 human agent
        SwarmAgent humanAgent = SwarmAgent.builder()
                .workspaceId(workspace.getId())
                .role("human")
                .build();
        agentRepository.save(humanAgent);

        // 3. 创建 assistant agent（parent 指向 human）
        SwarmAgent assistantAgent = SwarmAgent.builder()
                .workspaceId(workspace.getId())
                .role("assistant")
                .parentId(humanAgent.getId())
                .build();
        agentRepository.save(assistantAgent);

        // 4. 创建 P2P 群组
        SwarmGroup group = SwarmGroup.builder()
                .workspaceId(workspace.getId())
                .name("P2P")
                .build();
        groupRepository.save(group);

        // 5. 添加群成员
        groupRepository.addMember(group.getId(), humanAgent.getId());
        groupRepository.addMember(group.getId(), assistantAgent.getId());

        log.info("[Swarm] Created workspace: id={}, name={}, human={}, assistant={}, group={}",
                workspace.getId(), workspace.getName(), humanAgent.getId(), assistantAgent.getId(), group.getId());

        return WorkspaceDefaultsDTO.builder()
                .workspaceId(workspace.getId())
                .humanAgentId(humanAgent.getId())
                .assistantAgentId(assistantAgent.getId())
                .defaultGroupId(group.getId())
                .build();
    }

    /**
     * 列出用户所有 workspace
     */
    public List<WorkspaceDTO> listWorkspaces(Long userId) {
        List<SwarmWorkspace> workspaces = workspaceRepository.findByUserId(userId);
        return workspaces.stream().map(ws -> {
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
        }).collect(Collectors.toList());
    }

    /**
     * workspace 详情
     */
    public WorkspaceDTO getWorkspace(Long id) {
        SwarmWorkspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));
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
        SwarmWorkspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));
        if (request.getName() != null) ws.setName(request.getName());
        if (request.getMaxRoundsPerTurn() != null) ws.setMaxRoundsPerTurn(request.getMaxRoundsPerTurn());
        if (request.getLlmConfigId() != null) ws.setLlmConfigId(request.getLlmConfigId());
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
        return agentRepository.findByWorkspaceId(workspaceId).stream()
                .map(a -> SwarmAgentDTO.builder()
                        .id(a.getId())
                        .workspaceId(a.getWorkspaceId())
                        .agentId(a.getAgentId())
                        .role(a.getRole())
                        .description(a.getDescription())
                        .parentId(a.getParentId())
                        .status(a.getStatus() != null ? a.getStatus().getCode() : "IDLE")
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 创建 Agent（支持 description + 三方群：human + parent + 新agent）
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDefaultsDTO createAgent(Long workspaceId, String role, Long parentId, String description) {
        SwarmAgent agent = SwarmAgent.builder()
                .workspaceId(workspaceId)
                .role(role)
                .description(description)
                .parentId(parentId)
                .build();
        agentRepository.save(agent);

        // emit UI event
        emitAgentCreated(workspaceId, agent);

        // 创建任务群：parent + 新agent + human（三方群）
        Long groupId = null;
        if (parentId != null) {
            SwarmGroup group = SwarmGroup.builder()
                    .workspaceId(workspaceId)
                    .name(role)
                    .build();
            groupRepository.save(group);
            groupRepository.addMember(group.getId(), parentId);
            groupRepository.addMember(group.getId(), agent.getId());

            // 把 human 也加入群（三方群），让用户能看到对话并直接插话
            agentRepository.findByWorkspaceId(workspaceId).stream()
                    .filter(a -> "human".equals(a.getRole()))
                    .findFirst()
                    .ifPresent(human -> groupRepository.addMember(group.getId(), human.getId()));

            groupId = group.getId();
        }

        return WorkspaceDefaultsDTO.builder()
                .workspaceId(workspaceId)
                .assistantAgentId(agent.getId())
                .defaultGroupId(groupId)
                .build();
    }

    // emit agent created event after createAgent
    private void emitAgentCreated(Long workspaceId, SwarmAgent agent) {
        uiEventBus.emit(workspaceId, SwarmUIEventBus.UIEvent.builder()
                .type("ui.agent.created")
                .data("{\"agentId\":" + agent.getId() + ",\"role\":\"" + agent.getRole() + "\",\"parentId\":" + agent.getParentId() + "}")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * 获取 Agent 详情（含 llmHistory）
     */
    public SwarmAgent getAgent(Long agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
    }

    /**
     * 获取/确保默认资源（human + assistant + P2P 群）
     */
    public WorkspaceDefaultsDTO getDefaults(Long workspaceId) {
        List<SwarmAgent> agents = agentRepository.findByWorkspaceId(workspaceId);
        SwarmAgent human = agents.stream().filter(a -> "human".equals(a.getRole())).findFirst().orElse(null);
        SwarmAgent assistant = agents.stream().filter(a -> "assistant".equals(a.getRole())).findFirst().orElse(null);
        List<SwarmGroup> groups = groupRepository.findByWorkspaceId(workspaceId);
        SwarmGroup defaultGroup = groups.isEmpty() ? null : groups.get(0);

        return WorkspaceDefaultsDTO.builder()
                .workspaceId(workspaceId)
                .humanAgentId(human != null ? human.getId() : null)
                .assistantAgentId(assistant != null ? assistant.getId() : null)
                .defaultGroupId(defaultGroup != null ? defaultGroup.getId() : null)
                .build();
    }

    /**
     * 获取 Agent 拓扑图数据（节点 + 边）
     */
    public SwarmGraphDTO getGraph(Long workspaceId) {
        List<SwarmAgent> agents = agentRepository.findByWorkspaceId(workspaceId);

        List<SwarmGraphDTO.GraphNode> nodes = agents.stream()
                .map(a -> SwarmGraphDTO.GraphNode.builder()
                        .id(a.getId())
                        .role(a.getRole())
                        .parentId(a.getParentId())
                        .status(a.getStatus() != null ? a.getStatus().getCode() : "IDLE")
                        .build())
                .collect(Collectors.toList());

        // 计算边：有 parent 关系的 agent 之间的消息数
        List<SwarmGraphDTO.GraphEdge> edges = agents.stream()
                .filter(a -> a.getParentId() != null)
                .map(a -> SwarmGraphDTO.GraphEdge.builder()
                        .from(a.getParentId())
                        .to(a.getId())
                        .count(messageRepository.countMessagesBetween(workspaceId, a.getParentId(), a.getId()))
                        .build())
                .collect(Collectors.toList());

        return SwarmGraphDTO.builder().nodes(nodes).edges(edges).build();
    }

    /**
     * 搜索 Agent 和 Group
     */
    public SwarmSearchDTO search(Long workspaceId, String query) {
        String q = query.toLowerCase();

        List<SwarmAgentDTO> matchedAgents = agentRepository.findByWorkspaceId(workspaceId).stream()
                .filter(a -> a.getRole() != null && a.getRole().toLowerCase().contains(q))
                .map(a -> SwarmAgentDTO.builder()
                        .id(a.getId())
                        .workspaceId(a.getWorkspaceId())
                        .agentId(a.getAgentId())
                        .role(a.getRole())
                        .description(a.getDescription())
                        .parentId(a.getParentId())
                        .status(a.getStatus() != null ? a.getStatus().getCode() : "IDLE")
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<SwarmGroupDTO> matchedGroups = groupRepository.findByWorkspaceId(workspaceId).stream()
                .filter(g -> g.getName() != null && g.getName().toLowerCase().contains(q))
                .map(g -> SwarmGroupDTO.builder()
                        .id(g.getId())
                        .workspaceId(g.getWorkspaceId())
                        .name(g.getName())
                        .build())
                .collect(Collectors.toList());

        return SwarmSearchDTO.builder().agents(matchedAgents).groups(matchedGroups).build();
    }
}
