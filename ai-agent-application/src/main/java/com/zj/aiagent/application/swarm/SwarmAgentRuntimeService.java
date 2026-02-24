package com.zj.aiagent.application.swarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.runtime.SwarmAgentRunner;
import com.zj.aiagent.application.swarm.runtime.SwarmToolExecutor;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.domain.swarm.service.SwarmDomainService;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.tool.SwarmToolRegistry;
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
    private final SwarmWorkspaceRepository workspaceRepository;
    private final SwarmLlmCaller llmCaller;
    private final SwarmToolRegistry toolRegistry;
    private final SwarmToolExecutor toolExecutor;
    private final SwarmAgentEventBus agentEventBus;
    private final ObjectMapper objectMapper;

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

        SwarmAgentRunner runner = new SwarmAgentRunner(
                agent, domainService, agentRepository,
                llmCaller, toolRegistry, toolExecutor,
                objectMapper, agentEventBus, maxRounds);

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
