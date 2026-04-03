package com.zj.aiagent.application.swarm.tool;

import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Swarm Agent 工具白名单过滤器。
 *
 * <p>按 Agent 角色返回允许使用的工具集合。
 * 规则参照 Claude-Code 的 {@code COORDINATOR_MODE_ALLOWED_TOOLS} 设计：
 * <ul>
 *   <li>COORDINATOR - 调度工具：create_worker / delegate_task / send / self / listAgents / executeWorkflow</li>
 *   <li>WORKER - 执行工具：submit_result / send / self</li>
 *   <li>ASSISTANT - 无工具（纯通信）</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * boolean allowed = swarmToolFilter.isAllowed(role, "create_worker");
 * Set&lt;String&gt; tools = swarmToolFilter.getAllowedToolNames(role);
 * </pre>
 *
 * @see SwarmRole 角色枚举
 */
@Component
public class SwarmToolFilter {

    /** 工具名称常量 */
    public static final String TOOL_CREATE_WORKER = "create_worker";
    public static final String TOOL_DELEGATE_TASK = "delegate_task";
    public static final String TOOL_SUBMIT_RESULT = "submit_result";
    public static final String TOOL_SEND = "send";
    public static final String TOOL_SELF = "self";
    public static final String TOOL_LIST_AGENTS = "listAgents";
    public static final String TOOL_EXECUTE_WORKFLOW = "executeWorkflow";

    /** Coordinator 可用工具（调度者，不需要 submit_result）
     *  = create_worker / delegate_task / send / self / listAgents / executeWorkflow */
    private static final Set<String> COORDINATOR_TOOLS = Set.of(
        TOOL_CREATE_WORKER,
        TOOL_DELEGATE_TASK,
        TOOL_SEND,
        TOOL_SELF,
        TOOL_LIST_AGENTS,
        TOOL_EXECUTE_WORKFLOW
    );

    /** Worker 可用工具（执行者，不需要调度工具） */
    private static final Set<String> WORKER_TOOLS = Set.of(
        TOOL_SUBMIT_RESULT,
        TOOL_SEND,
        TOOL_SELF
    );

    /** 无工具角色 */
    private static final Set<String> NO_TOOLS = Collections.emptySet();

    /**
     * 返回指定角色允许使用的工具名称集合。
     *
     * @param role Agent 角色
     * @return 允许的工具名称集合，永不为 null
     */
    public Set<String> getAllowedToolNames(SwarmRole role) {
        if (role == null) {
            return NO_TOOLS;
        }
        return switch (role) {
            case COORDINATOR -> COORDINATOR_TOOLS;
            case WORKER -> WORKER_TOOLS;
            case ASSISTANT -> NO_TOOLS;
            default -> NO_TOOLS;
        };
    }

    /**
     * 检查指定工具是否在角色的白名单中。
     *
     * <p>MCP 工具（mcp__* 前缀）由 McpToolCallbackAdapter 动态注入，默认放行。
     *
     * @param role      Agent 角色
     * @param toolName  工具名称（方法名）
     * @return true 表示允许使用
     */
    public boolean isAllowed(SwarmRole role, String toolName) {
        // MCP 工具由 McpToolCallbackAdapter 动态注入，默认允许
        if (toolName != null && toolName.startsWith("mcp__")) {
            return true;
        }
        // toolName 为 null 或不在白名单中
        if (toolName == null) {
            return false;
        }
        return getAllowedToolNames(role).contains(toolName);
    }

    /**
     * 返回工具描述映射（用于动态生成提示词中的工具列表）。
     * 描述文本不包含 @Tool 注解的完整签名，仅提供 LLM 可理解的简短描述。
     *
     * @param toolName 工具名称
     * @return 工具描述，未知工具返回 null
     */
    public String getToolDescription(String toolName) {
        return switch (toolName) {
            case TOOL_CREATE_WORKER -> "创建 Worker Agent。Coordinator 用来动态创建子工作节点。可选传入 taskUuid/instruction 以在创建时立即派发任务。";
            case TOOL_DELEGATE_TASK -> "向指定的 Worker Agent 派发任务。Coordinator 使用。内部会创建 task 并通过 send 消息派发给目标 Agent。";
            case TOOL_SUBMIT_RESULT -> "Worker 提交任务结果。内部调用 writing_result_by_task_uuid 并将任务标记为完成。推荐 Worker 优先使用此工具记录结果。";
            case TOOL_SEND -> "向指定 Agent 发送消息。用于委派任务给子 Agent 或回复其他 Agent 的消息。建议使用结构化消息格式。";
            case TOOL_SELF -> "返回自身信息，包括 agent_id、workspace_id、角色、状态等。";
            case TOOL_LIST_AGENTS -> "列出当前 workspace 中所有 Agent，包括它们的 ID、角色、状态和父子关系。";
            case TOOL_EXECUTE_WORKFLOW -> "执行某个工作流 Agent，并等待执行完成后返回结果。适合主 AI 同步等待工作流输出。";
            default -> null;
        };
    }

    /**
     * 按角色白名单生成工具描述列表文本。
     *
     * @param role  Agent 角色
     * @return 格式化的工具描述块，用于提示词
     */
    public String buildToolSection(SwarmRole role) {
        Set<String> allowed = getAllowedToolNames(role);
        if (allowed.isEmpty()) {
            return "【可用工具】\n无工具可用。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【可用工具】\n");
        int idx = 1;
        for (String tool : allowed) {
            String desc = getToolDescription(tool);
            if (desc != null) {
                sb.append(idx++).append(". ").append(tool).append(" - ").append(desc).append("\n");
            }
        }
        return sb.toString();
    }
}
