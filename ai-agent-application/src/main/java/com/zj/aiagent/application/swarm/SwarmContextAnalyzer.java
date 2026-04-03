package com.zj.aiagent.application.swarm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.valobj.SwarmTaskContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Swarm Context 分析器。
 *
 * <p>核心职责：
 * <ul>
 *   <li>收集 workspace 下各 Worker 的 SwarmTaskContext</li>
 *   <li>计算新任务与现有上下文的重叠度</li>
 *   <li>为 Coordinator 的 Continue vs Spawn 决策提供数据支撑</li>
 * </ul>
 *
 * <p>重叠度评分维度：
 * <ul>
 *   <li>fileOverlap — 文件路径重叠率（0.0 - 1.0）</li>
 *   <li>moduleOverlap — 模块重叠率（0.0 - 1.0）</li>
 *   <li>taskRelevance — 历史任务相关性（0.0 - 1.0）</li>
 * </ul>
 *
 * @see SwarmTaskContext 任务上下文值对象
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmContextAnalyzer {

    private final SwarmAgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Context Overlap Score — 上下文重叠度评分。
     *
     * @param fileOverlap    文件路径重叠率（0.0 = 无重叠，1.0 = 完全重叠）
     * @param moduleOverlap  模块重叠率（0.0 = 无重叠，1.0 = 完全重叠）
     * @param taskRelevance  历史任务相关性（0.0 = 完全无关，1.0 = 高度相关）
     * @param level          重叠等级（HIGH / MEDIUM / LOW）
     * @param recommendedStrategy 推荐策略（Continue / Spawn）
     */
    public record ContextOverlapScore(
        double fileOverlap,
        double moduleOverlap,
        double taskRelevance,
        String level,
        String recommendedStrategy
    ) {}

    /**
     * 分析给定 Worker Agent 与新任务范围之间的上下文重叠度。
     *
     * @param workerAgentId  Worker Agent ID
     * @param newTaskScope  新任务的范围描述（用于关键词匹配）
     * @return 上下文重叠度评分
     */
    public ContextOverlapScore analyze(Long workerAgentId, String newTaskScope) {
        return analyze(workerAgentId, newTaskScope, null);
    }

    /**
     * 分析给定 Worker Agent 与新任务范围之间的上下文重叠度。
     *
     * @param workerAgentId  Worker Agent ID
     * @param newTaskScope   新任务的范围描述
     * @param newTaskFiles   新任务涉及的文件列表（可选）
     * @return 上下文重叠度评分
     */
    public ContextOverlapScore analyze(Long workerAgentId, String newTaskScope, Set<String> newTaskFiles) {
        SwarmTaskContext ctx = loadContext(workerAgentId);
        if (ctx == null) {
            log.debug(
                "[Swarm] No context found for worker agent={}, returning LOW overlap",
                workerAgentId
            );
            return new ContextOverlapScore(0.0, 0.0, 0.0, "LOW", "Spawn");
        }

        // 计算文件重叠率
        double fileOverlap = 0.0;
        if (newTaskFiles != null && !newTaskFiles.isEmpty() && !ctx.getExploredFiles().isEmpty()) {
            Set<String> intersection = new HashSet<>(ctx.getExploredFiles());
            intersection.retainAll(newTaskFiles);
            fileOverlap = (double) intersection.size() / Math.max(ctx.getExploredFiles().size(), newTaskFiles.size());
        }

        // 计算模块重叠率
        double moduleOverlap = 0.0;
        if (newTaskScope != null && !newTaskScope.isBlank() && !ctx.getExploredModules().isEmpty()) {
            String scopeLower = newTaskScope.toLowerCase();
            long matched = ctx.getExploredModules().stream()
                .filter(m -> scopeLower.contains(m.toLowerCase()))
                .count();
            moduleOverlap = (double) matched / ctx.getExploredModules().size();
        }

        // 计算任务相关性（基于关键词匹配）
        double taskRelevance = 0.0;
        if (newTaskScope != null && !newTaskScope.isBlank()) {
            taskRelevance = calculateTaskRelevance(ctx, newTaskScope);
        }

        // 综合评分（加权平均）
        double composite = fileOverlap * 0.4 + moduleOverlap * 0.3 + taskRelevance * 0.3;

        // 确定等级
        String level = composite >= 0.6 ? "HIGH" : composite >= 0.3 ? "MEDIUM" : "LOW";

        // 确定推荐策略
        String strategy = composite >= 0.4 ? "Continue" : "Spawn";

        log.info(
            "[Swarm] Context analysis: workerAgentId={}, fileOverlap={:.2f}, moduleOverlap={:.2f}, "
                + "taskRelevance={:.2f}, composite={:.2f}, level={}, strategy={}",
            workerAgentId, fileOverlap, moduleOverlap, taskRelevance, composite, level, strategy
        );

        return new ContextOverlapScore(fileOverlap, moduleOverlap, taskRelevance, level, strategy);
    }

    /**
     * 分析 workspace 下所有 Worker 的上下文。
     * 用于 Coordinator 在派发任务前评估整体上下文状态。
     *
     * @param workspaceId 工作空间 ID
     * @return Map&lt;WorkerAgentId, ContextOverlapScore&gt;
     */
    public Map<Long, ContextOverlapScore> analyzeAllWorkers(Long workspaceId, String taskScope) {
        List<SwarmAgent> workers = agentRepository.findByWorkspaceId(workspaceId)
            .stream()
            .filter(SwarmAgent::isWorker)
            .toList();

        Map<Long, ContextOverlapScore> results = new LinkedHashMap<>();
        for (SwarmAgent worker : workers) {
            results.put(worker.getId(), analyze(worker.getId(), taskScope));
        }
        return results;
    }

    /**
     * 从 SwarmAgent 的 llmHistory 中解析 SwarmTaskContext。
     * llmHistory 是一个 JSON 数组，每条消息是 {"role": "...", "content": "..."}。
     * context 条目的 role="_swarm_context"，content 是 JSON 字符串。
     */
    public SwarmTaskContext loadContext(Long agentId) {
        try {
            Optional<SwarmAgent> agentOpt = agentRepository.findById(agentId);
            if (agentOpt.isEmpty()) {
                return null;
            }

            SwarmAgent agent = agentOpt.get();
            String historyJson = agent.getLlmHistory();
            if (historyJson == null || historyJson.isBlank()) {
                return null;
            }

            // 解析 JSON 数组，查找最后一个 _swarm_context 条目
            List<Map<String, Object>> historyList = objectMapper.readValue(historyJson,
                new TypeReference<List<Map<String, Object>>>() {});

            Map<String, Object> lastContextEntry = null;
            for (Map<String, Object> entry : historyList) {
                Object role = entry.get("role");
                if (role instanceof String roleStr && "_swarm_context".equals(roleStr)) {
                    lastContextEntry = entry;
                }
            }

            if (lastContextEntry == null) {
                return null;
            }

            // 解析 content 字段中的 JSON
            Object content = lastContextEntry.get("content");
            if (!(content instanceof String contentJson)) {
                return null;
            }

            Map<String, Object> data = objectMapper.readValue(contentJson,
                new TypeReference<Map<String, Object>>() {});

            Set<String> files = parseStringSet(data.get("exploredFiles"));
            Set<String> modules = parseStringSet(data.get("exploredModules"));
            List<String> findings = parseStringList(data.get("findings"));

            SwarmTaskContext.Phase phase = SwarmTaskContext.Phase.RESEARCH;
            Object phaseObj = data.get("phase");
            if (phaseObj instanceof String phaseName) {
                try {
                    phase = SwarmTaskContext.Phase.valueOf(phaseName);
                } catch (IllegalArgumentException ignored) { }
            }

            return SwarmTaskContext.builder()
                .agentId(agentId)
                .exploredFiles(files)
                .exploredModules(modules)
                .findings(findings)
                .currentPhase(phase)
                .lastUpdated(java.time.Instant.now())
                .build();
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to load context for agent={}",
                agentId,
                e
            );
            return null;
        }
    }

    /**
     * 保存 SwarmTaskContext 到 SwarmAgent。
     * 上下文作为特殊角色消息追加到 llmHistory 中。
     * 格式：llmHistory 维护一个 JSON 数组，每条消息是 {"role": "...", "content": "..."}
     * 其中 context 条目的 role="_swarm_context"，content=JSON字符串
     */
    public void saveContext(Long agentId, SwarmTaskContext context) {
        if (context == null) {
            return;
        }
        try {
            // 构建 context 条目
            Map<String, Object> contextData = new LinkedHashMap<>();
            contextData.put("exploredFiles", new ArrayList<>(context.getExploredFiles()));
            contextData.put("exploredModules", new ArrayList<>(context.getExploredModules()));
            contextData.put("findings", new ArrayList<>(context.getFindings()));
            contextData.put("phase", context.getCurrentPhase().name());

            Map<String, Object> contextEntry = new LinkedHashMap<>();
            contextEntry.put("role", "_swarm_context");
            contextEntry.put("content", objectMapper.writeValueAsString(contextData));

            // 追加到 llmHistory 数组
            Optional<SwarmAgent> agentOpt = agentRepository.findById(agentId);
            if (agentOpt.isPresent()) {
                String existing = agentOpt.get().getLlmHistory();
                List<Map<String, Object>> historyList;
                if (existing != null && !existing.isBlank()) {
                    historyList = objectMapper.readValue(existing,
                        new TypeReference<List<Map<String, Object>>>() {});
                } else {
                    historyList = new ArrayList<>();
                }
                historyList.add(contextEntry);
                String updated = objectMapper.writeValueAsString(historyList);
                agentRepository.updateLlmHistory(agentId, updated);
                log.debug(
                    "[Swarm] Context saved for agent={}, files={}, modules={}, findings={}",
                    agentId,
                    context.getExploredFiles().size(),
                    context.getExploredModules().size(),
                    context.getFindings().size()
                );
            }
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to save context for agent={}",
                agentId,
                e
            );
        }
    }

    /**
     * 合并多个 Worker 的上下文。
     * 用于 Coordinator 整合所有 Worker 的探索结果。
     */
    public SwarmTaskContext mergeWorkerContexts(Collection<Long> workerAgentIds) {
        SwarmTaskContext merged = null;
        for (Long workerId : workerAgentIds) {
            SwarmTaskContext ctx = loadContext(workerId);
            if (ctx != null) {
                merged = merged == null ? ctx : merged.merge(ctx);
            }
        }
        return merged;
    }

    // ── 内部辅助方法 ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<String> parseStringSet(Object obj) {
        if (obj == null) {
            return new HashSet<>();
        }
        if (obj instanceof Collection<?> col) {
            Set<String> result = new HashSet<>();
            for (Object item : col) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
            return result;
        }
        return new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * 计算任务相关性。
     * 基于新任务范围中的关键词与 Worker 上下文中发现/模块的匹配度。
     */
    private double calculateTaskRelevance(SwarmTaskContext ctx, String taskScope) {
        if (taskScope == null || taskScope.isBlank()) {
            return 0.0;
        }
        String scopeLower = taskScope.toLowerCase();

        // 检查模块匹配
        long moduleMatches = ctx.getExploredModules().stream()
            .filter(m -> scopeLower.contains(m.toLowerCase()))
            .count();

        // 检查发现中的关键词匹配
        long findingMatches = ctx.getFindings().stream()
            .filter(f -> {
                String lower = f.toLowerCase();
                return scopeLower.contains(lower.substring(0, Math.min(10, lower.length())));
            })
            .count();

        // 综合评分
        double moduleScore = ctx.getExploredModules().isEmpty()
            ? 0.0
            : (double) moduleMatches / ctx.getExploredModules().size();
        double findingScore = ctx.getFindings().isEmpty()
            ? 0.0
            : Math.min(1.0, (double) findingMatches / 3.0); // 最多计 3 个发现

        return moduleScore * 0.6 + findingScore * 0.4;
    }
}
