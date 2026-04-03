package com.zj.aiagent.application.swarm.prompt;

import com.zj.aiagent.application.swarm.SwarmContextAnalyzer;
import com.zj.aiagent.application.swarm.tool.SwarmToolFilter;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import com.zj.aiagent.domain.swarm.valobj.SwarmTaskContext;
import com.zj.aiagent.infrastructure.mcp.adapter.McpToolCallbackAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Swarm Agent 动态提示词服务。
 *
 * <p>核心职责：
 * <ul>
 *   <li>按角色组合提示词 Section（BASE → COORDINATOR/WORKER → TOOL → CONTEXT）</li>
 *   <li>运行时替换模板变量（{agentId}、{workspaceId} 等）</li>
 *   <li>通过 {@link SwarmToolFilter} 动态生成角色专属的工具描述列表</li>
 *   <li>通过 {@link SwarmContextAnalyzer} 注入上下文重叠度信息</li>
 * </ul>
 *
 * <p>设计参照 Claude-Code 的 {@code buildEffectiveSystemPrompt()} 模式：
 * <pre>
 * [BASE] → [COORDINATOR / WORKER] → [TOOL] → [CONTEXT] → [CUSTOM]
 * </pre>
 *
 * <p>使用方式：
 * <pre>
 * String prompt = swarmPromptService.getPrompt(agent, SwarmRole.COORDINATOR);
 * </pre>
 *
 * @see SwarmPromptSection Section 枚举定义
 * @see SwarmToolFilter 工具白名单过滤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwarmPromptService {

    private final SwarmToolFilter toolFilter;
    private final SwarmContextAnalyzer contextAnalyzer;
    private final SwarmAgentRepository agentRepository;
    private final SwarmWorkspaceRepository workspaceRepository;
    private final McpToolCallbackAdapter mcpToolCallbackAdapter;

    /**
     * 构建指定 Agent 的完整系统提示词。
     *
     * @param agent  Swarm Agent 实体
     * @param role   Agent 角色（COORDINATOR / WORKER）
     * @return 组合后的完整提示词字符串
     */
    public String getPrompt(SwarmAgent agent, SwarmRole role) {
        return getPrompt(agent, role, null);
    }

    /**
     * 构建指定 Agent 的完整系统提示词（含自定义附加提示）。
     *
     * @param agent         Swarm Agent 实体
     * @param role          Agent 角色
     * @param customPrompt  可选的自定义附加提示（来自 workspace.customPrompt），可为 null
     * @return 组合后的完整提示词字符串
     */
    public String getPrompt(SwarmAgent agent, SwarmRole role, String customPrompt) {
        Map<String, String> ctx = buildContext(agent, role);
        StringJoiner joiner = new StringJoiner("\n\n");

        // 1. BASE — 所有角色共享
        joiner.add(resolveVariables(SwarmPromptSection.BASE.getTemplate(), ctx));

        // 2. Role Section — 按角色选择
        if (role == SwarmRole.COORDINATOR) {
            joiner.add(resolveVariables(SwarmPromptSection.COORDINATOR.getTemplate(), ctx));

            // 2.5. CONTEXT_BLOCK — Coordinator 专用的结构化上下文（动态注入）
            joiner.add(buildContextBlock(agent));
        } else if (role == SwarmRole.WORKER) {
            // Worker 的 Phase 信息从上下文传入，初始为 RESEARCH
            String phase = ctx.getOrDefault("currentPhase", "RESEARCH");
            Map<String, String> workerCtx = new LinkedHashMap<>(ctx);
            workerCtx.put("currentPhase", phase);
            joiner.add(resolveVariables(SwarmPromptSection.WORKER.getTemplate(), workerCtx));
        }

        // 3. Tool Format
        joiner.add(SwarmPromptSection.TOOL_FORMAT.getTemplate());

        // 4. Tool List — 按角色白名单动态生成
        joiner.add(toolFilter.buildToolSection(role));

        // 4.5. MCP Tool List — 动态策略：内嵌（< 20 工具）或查询模式（≥ 20）
        Long userId = getUserIdFromWorkspace(agent.getWorkspaceId());
        if (userId != null) {
            if (mcpToolCallbackAdapter.shouldEmbedInPromptByUserId(userId)) {
                joiner.add(mcpToolCallbackAdapter.buildEmbeddableToolSectionByUserId(userId));
            } else {
                joiner.add(mcpToolCallbackAdapter.buildQueryToolSection(role));
            }
        }

        // 5. Custom Section（可选）
        if (customPrompt != null && !customPrompt.isBlank()) {
            joiner.add("【自定义附加提示】\n" + customPrompt);
        }

        return joiner.toString();
    }

    /**
     * 获取 workspace 的 userId。
     */
    private Long getUserIdFromWorkspace(Long workspaceId) {
        if (workspaceId == null) {
            return null;
        }
        try {
            return workspaceRepository.findById(workspaceId)
                    .map(w -> w.getUserId())
                    .orElse(null);
        } catch (Exception e) {
            log.debug("[SwarmPrompt] Failed to get userId for workspace={}", workspaceId, e);
            return null;
        }
    }

    /**
     * 构建提示词变量上下文。
     */
    private Map<String, String> buildContext(SwarmAgent agent, SwarmRole role) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("agentId", String.valueOf(agent.getId()));
        ctx.put("workspaceId", String.valueOf(agent.getWorkspaceId()));
        ctx.put("role", role != null ? role.getDesc() : "unknown");
        ctx.put("description", agent.getDescription() != null ? agent.getDescription() : "");
        ctx.put("parentAgentId", agent.getParentId() != null ? String.valueOf(agent.getParentId()) : "null");
        // Worker 初始 Phase 为 RESEARCH（运行时动态流转）
        ctx.put("currentPhase", "RESEARCH");
        return ctx;
    }

    /**
     * 构建 Coordinator 的结构化上下文块。
     * 从 SwarmContextAnalyzer 获取 Worker 的上下文重叠度评分。
     */
    private String buildContextBlock(SwarmAgent agent) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("taskType", agent.getDescription() != null && !agent.getDescription().isBlank()
            ? agent.getDescription() : "未指定任务");
        ctx.put("currentPhase", "RESEARCH");
        ctx.put("phaseDescription", "调研阶段");
        ctx.put("exploredFiles", "（暂无探索记录）");
        ctx.put("overlapScore", "N/A");
        ctx.put("overlapLevel", "UNKNOWN");
        ctx.put("recommendedStrategy", "Spawn（默认）");

        try {
            // 收集 workspace 下所有 Worker 的上下文
            var workers = agentRepository.findByWorkspaceId(agent.getWorkspaceId())
                .stream()
                .filter(SwarmAgent::isWorker)
                .toList();

            if (!workers.isEmpty()) {
                // 合并所有 Worker 的上下文
                SwarmTaskContext merged = contextAnalyzer.mergeWorkerContexts(
                    workers.stream().map(SwarmAgent::getId).toList()
                );

                if (merged != null) {
                    // 文件列表
                    var files = merged.getExploredFiles();
                    if (!files.isEmpty()) {
                        String fileList = String.join(", ",
                            files.stream().limit(10).toList()
                        );
                        if (files.size() > 10) {
                            fileList += "...+" + (files.size() - 10) + " more";
                        }
                        ctx.put("exploredFiles", fileList);
                    }

                    // 分析与最近 Worker 的重叠度
                    if (!workers.isEmpty()) {
                        Long lastWorkerId = workers.get(workers.size() - 1).getId();
                        // 使用 Coordinator 的 description 作为任务范围进行重叠度分析
                        String taskScope = agent.getDescription() != null ? agent.getDescription() : "";
                        SwarmContextAnalyzer.ContextOverlapScore score =
                            contextAnalyzer.analyze(lastWorkerId, taskScope);
                        ctx.put("overlapScore", String.format("%.1f",
                            (score.fileOverlap() + score.moduleOverlap() + score.taskRelevance()) / 3));
                        ctx.put("overlapLevel", score.level());
                        ctx.put("recommendedStrategy", score.recommendedStrategy());
                    }

                    // 当前 Phase
                    if (merged.getCurrentPhase() != null) {
                        ctx.put("currentPhase", merged.getCurrentPhase().name());
                        ctx.put("phaseDescription", merged.getCurrentPhase().getDescription());
                    }
                } else {
                    // 没有持久化的上下文，使用默认值
                    log.debug(
                        "[Swarm] No merged context for workspace={}, using defaults",
                        agent.getWorkspaceId()
                    );
                }
            }
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to build context block for agent={}",
                agent.getId(),
                e
            );
        }

        return resolveVariables(SwarmPromptSection.CONTEXT_BLOCK.getTemplate(), ctx);
    }

    /**
     * 替换模板中的 {variable} 占位符。
     *
     * <p>仅支持 {key} 形式，不支持 %s 格式。</p>
     *
     * @param template  原始模板字符串
     * @param context   变量名 → 值的映射
     * @return 替换后的字符串
     */
    public String resolveVariables(String template, Map<String, String> context) {
        if (template == null || context == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * 构建 Worker 的阶段化提示词（带当前 Phase）。
     *
     * @param agent        Swarm Agent 实体
     * @param currentPhase 当前工作阶段（RESEARCH/SYNTHESIS/IMPLEMENTATION/VERIFICATION）
     * @return Worker 提示词
     */
    public String getWorkerPrompt(SwarmAgent agent, String currentPhase) {
        Map<String, String> ctx = buildContext(agent, SwarmRole.WORKER);
        // Worker Phase 初始为 RESEARCH
        ctx.put("currentPhase", currentPhase != null ? currentPhase : "RESEARCH");

        StringJoiner joiner = new StringJoiner("\n\n");
        joiner.add(resolveVariables(SwarmPromptSection.BASE.getTemplate(), ctx));
        joiner.add(resolveVariables(SwarmPromptSection.WORKER.getTemplate(), ctx));
        joiner.add(SwarmPromptSection.TOOL_FORMAT.getTemplate());
        joiner.add(toolFilter.buildToolSection(SwarmRole.WORKER));

        // MCP Tool List — 动态策略
        Long userId = getUserIdFromWorkspace(agent.getWorkspaceId());
        if (userId != null) {
            if (mcpToolCallbackAdapter.shouldEmbedInPromptByUserId(userId)) {
                joiner.add(mcpToolCallbackAdapter.buildEmbeddableToolSectionByUserId(userId));
            } else {
                joiner.add(mcpToolCallbackAdapter.buildQueryToolSection(SwarmRole.WORKER));
            }
        }

        return joiner.toString();
    }

    /**
     * 构建 Coordinator 的提示词。
     *
     * @param agent Swarm Agent 实体
     * @return Coordinator 提示词
     */
    public String getCoordinatorPrompt(SwarmAgent agent) {
        return getPrompt(agent, SwarmRole.COORDINATOR);
    }

}
