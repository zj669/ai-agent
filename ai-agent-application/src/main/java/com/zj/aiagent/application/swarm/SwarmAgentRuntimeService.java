package com.zj.aiagent.application.swarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.runtime.SwarmAgentRunner;
import com.zj.aiagent.application.swarm.runtime.SwarmTools;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.domain.swarm.service.SwarmDomainService;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Runtime 管理：管理所有 AgentRunner 的生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmAgentRuntimeService {

    private final SwarmDomainService domainService;
    private final SwarmAgentRepository agentRepository;
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmWorkspaceRepository workspaceRepository;
    private final SwarmLlmCaller llmCaller;
    private final SwarmWorkspaceService workspaceService;
    private final SwarmMessageService messageService;
    private final SwarmAgentEventBus agentEventBus;
    private final SwarmUIEventBus uiEventBus;
    private final ObjectMapper objectMapper;
    private final LlmProviderConfigRepository llmProviderConfigRepository;

    /** agentId -> runner */
    private final Map<Long, SwarmAgentRunner> runners = new ConcurrentHashMap<>();

    /**
     * 启动 Agent Runner（虚拟线程）
     */
    public void startAgent(SwarmAgent agent) {
        if (runners.containsKey(agent.getId())) {
            log.warn("[Swarm] Agent {} already running, skip", agent.getId());
            return;
        }

        int maxRounds = workspaceRepository.findById(agent.getWorkspaceId())
                .map(SwarmWorkspace::getMaxRoundsPerTurn)
                .orElse(10);

        // 查 workspace 的 LLM 配置
        LlmProviderConfig llmConfig = workspaceRepository.findById(agent.getWorkspaceId())
                .map(SwarmWorkspace::getLlmConfigId)
                .flatMap(configId -> configId != null ? llmProviderConfigRepository.findById(configId) : java.util.Optional.empty())
                .orElse(null);

        // 查 workspace 下 role=human 的 agent id
        Long humanAgentId = agentRepository.findByWorkspaceId(agent.getWorkspaceId()).stream()
                .filter(a -> "human".equals(a.getRole()))
                .map(SwarmAgent::getId)
                .findFirst()
                .orElse(null);

        // 为每个 agent 创建独立的 SwarmTools 实例
        SwarmTools swarmTools = new SwarmTools(
                workspaceService, messageService, agentRepository,
                objectMapper, agent.getId(), agent.getWorkspaceId());

        SwarmAgentRunner runner = new SwarmAgentRunner(
                agent, domainService, agentRepository, groupRepository, messageRepository,
                llmCaller, swarmTools,
                objectMapper, agentEventBus, uiEventBus, maxRounds, humanAgentId, llmConfig);

        runners.put(agent.getId(), runner);

        Thread.ofVirtual()
                .name("swarm-agent-" + agent.getId())
                .start(runner);

        log.info("[Swarm] Started runner for agent {}", agent.getId());
    }

    /**
     * 唤醒 Agent（有新消息时调用）
     */
    public void wakeAgent(Long agentId) {
        SwarmAgentRunner runner = runners.get(agentId);
        if (runner != null) {
            runner.wake();
            log.debug("[Swarm] Woke agent {}", agentId);
        } else {
            // 如果 runner 不存在，尝试启动
            agentRepository.findById(agentId).ifPresent(agent -> {
                if (!"human".equals(agent.getRole())) {
                    startAgent(agent);
                    // 启动后再唤醒
                    SwarmAgentRunner newRunner = runners.get(agentId);
                    if (newRunner != null) newRunner.wake();
                }
            });
        }
    }

    /**
     * 停止 Agent
     */
    public void stopAgent(Long agentId) {
        SwarmAgentRunner runner = runners.remove(agentId);
        if (runner != null) {
            runner.stop();
            log.info("[Swarm] Stopped runner for agent {}", agentId);
        }
        // 停止后唤醒父 agent
        agentRepository.findById(agentId).ifPresent(agent -> {
            if (agent.getParentId() != null) {
                wakeAgent(agent.getParentId());
            }
        });
    }

    /**
     * 停止 workspace 下所有 Agent
     */
    public List<Long> interruptAll(Long workspaceId) {
        List<SwarmAgent> agents = agentRepository.findByWorkspaceId(workspaceId);
        List<Long> interrupted = agents.stream()
                .map(SwarmAgent::getId)
                .filter(runners::containsKey)
                .peek(this::stopAgent)
                .toList();
        log.info("[Swarm] Interrupted {} agents in workspace {}", interrupted.size(), workspaceId);
        return interrupted;
    }

    /**
     * 启动 workspace 下所有 AI Agent 的 runner
     */
    public void startWorkspaceAgents(Long workspaceId) {
        agentRepository.findByWorkspaceId(workspaceId).stream()
                .filter(a -> !"human".equals(a.getRole()))
                .forEach(this::startAgent);
    }
}
