package com.zj.aiagent.application.swarm.prompt;

import com.zj.aiagent.application.swarm.tool.SwarmToolFilter;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Swarm Agent 动态提示词服务。
 *
 * <p>核心职责：
 * <ul>
 *   <li>按角色组合提示词 Section（BASE → COORDINATOR/WORKER → TOOL）</li>
 *   <li>运行时替换模板变量（{agentId}、{workspaceId} 等）</li>
 *   <li>通过 {@link SwarmToolFilter} 动态生成角色专属的工具描述列表</li>
 * </ul>
 *
 * <p>设计参照 Claude-Code 的 {@code buildEffectiveSystemPrompt()} 模式：
 * <pre>
 * [BASE] → [COORDINATOR / WORKER] → [TOOL] → [CUSTOM]
 * </pre>
 *
 * <p>使用方式：
 * <pre>
 * String prompt = swarmPromptService.getPrompt(agent, SwarmRole.COORDINATOR, humanAgentId);
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

    /**
     * 构建指定 Agent 的完整系统提示词。
     *
     * @param agent         Swarm Agent 实体
     * @param role          Agent 角色（COORDINATOR / WORKER / ROOT / HUMAN）
     * @param humanAgentId  人类 Agent ID（用于禁止 send 的检查）
     * @return 组合后的完整提示词字符串
     */
    public String getPrompt(SwarmAgent agent, SwarmRole role, Long humanAgentId) {
        return getPrompt(agent, role, humanAgentId, null);
    }

    /**
     * 构建指定 Agent 的完整系统提示词（含自定义附加提示）。
     *
     * @param agent         Swarm Agent 实体
     * @param role          Agent 角色
     * @param humanAgentId  人类 Agent ID
     * @param customPrompt  可选的自定义附加提示（来自 workspace.customPrompt），可为 null
     * @return 组合后的完整提示词字符串
     */
    public String getPrompt(SwarmAgent agent, SwarmRole role, Long humanAgentId, String customPrompt) {
        Map<String, String> ctx = buildContext(agent, role, humanAgentId);
        StringJoiner joiner = new StringJoiner("\n\n");

        // 1. BASE — 所有角色共享
        joiner.add(resolveVariables(SwarmPromptSection.BASE.getTemplate(), ctx));

        // 2. Role Section — 按角色选择
        if (role == SwarmRole.COORDINATOR) {
            joiner.add(resolveVariables(SwarmPromptSection.COORDINATOR.getTemplate(), ctx));
        } else if (role == SwarmRole.WORKER) {
            // Worker 的 Phase 信息从上下文传入，默认 IMPLMENTATION
            String workerSection = SwarmPromptSection.WORKER.getTemplate();
            String phase = ctx.getOrDefault("currentPhase", "IMPLEMENTATION");
            Map<String, String> workerCtx = new LinkedHashMap<>(ctx);
            workerCtx.put("currentPhase", phase);
            joiner.add(resolveVariables(workerSection, workerCtx));
        }

        // 3. Tool Format
        joiner.add(SwarmPromptSection.TOOL_FORMAT.getTemplate());

        // 4. Tool List — 按角色白名单动态生成
        joiner.add(toolFilter.buildToolSection(role));

        // 5. Custom Section（可选）
        if (customPrompt != null && !customPrompt.isBlank()) {
            joiner.add("【自定义附加提示】\n" + customPrompt);
        }

        return joiner.toString();
    }

    /**
     * 构建提示词变量上下文。
     */
    private Map<String, String> buildContext(SwarmAgent agent, SwarmRole role, Long humanAgentId) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("agentId", String.valueOf(agent.getId()));
        ctx.put("workspaceId", String.valueOf(agent.getWorkspaceId()));
        ctx.put("role", role != null ? role.getDesc() : "unknown");
        ctx.put("description", agent.getDescription() != null ? agent.getDescription() : "");
        ctx.put("parentAgentId", agent.getParentId() != null ? String.valueOf(agent.getParentId()) : "null");
        ctx.put("humanAgentId", humanAgentId != null ? String.valueOf(humanAgentId) : "null");
        ctx.put("currentPhase", "IMPLEMENTATION");
        return ctx;
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
     * @param agent         Swarm Agent 实体
     * @param humanAgentId  人类 Agent ID
     * @param currentPhase  当前工作阶段（RESEARCH/SYNTHESIS/IMPLEMENTATION/VERIFICATION）
     * @return Worker 提示词
     */
    public String getWorkerPrompt(SwarmAgent agent, Long humanAgentId, String currentPhase) {
        Map<String, String> ctx = buildContext(agent, SwarmRole.WORKER, humanAgentId);
        ctx.put("currentPhase", currentPhase != null ? currentPhase : "IMPLEMENTATION");

        StringJoiner joiner = new StringJoiner("\n\n");
        joiner.add(resolveVariables(SwarmPromptSection.BASE.getTemplate(), ctx));
        joiner.add(resolveVariables(SwarmPromptSection.WORKER.getTemplate(), ctx));
        joiner.add(SwarmPromptSection.TOOL_FORMAT.getTemplate());
        joiner.add(toolFilter.buildToolSection(SwarmRole.WORKER));

        return joiner.toString();
    }

    /**
     * 构建 Coordinator 的提示词。
     *
     * @param agent         Swarm Agent 实体
     * @param humanAgentId  人类 Agent ID
     * @return Coordinator 提示词
     */
    public String getCoordinatorPrompt(SwarmAgent agent, Long humanAgentId) {
        return getPrompt(agent, SwarmRole.COORDINATOR, humanAgentId);
    }

    /**
     * 构建 ROOT Agent 的提示词（拥有全部工具）。
     *
     * @param agent         Swarm Agent 实体
     * @param humanAgentId  人类 Agent ID
     * @return ROOT 提示词
     */
    public String getRootPrompt(SwarmAgent agent, Long humanAgentId) {
        return getPrompt(agent, SwarmRole.ROOT, humanAgentId);
    }

    // JDK 21 StringJoiner is in java.util, using simple concat for older compatibility
    private static class StringJoiner {
        private final String delimiter;
        private final StringBuilder sb = new StringBuilder();
        private boolean first = true;

        StringJoiner(String delimiter) {
            this.delimiter = delimiter;
        }

        void add(String part) {
            if (!first) sb.append(delimiter);
            first = false;
            sb.append(part);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
