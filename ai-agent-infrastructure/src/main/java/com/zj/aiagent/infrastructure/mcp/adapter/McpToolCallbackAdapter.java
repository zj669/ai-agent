package com.zj.aiagent.infrastructure.mcp.adapter;

import com.zj.aiagent.domain.mcp.port.IMcpToolRegistry;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * MCP 工具到 Spring AI ToolCallback 的桥接器
 * <p>
 * 将 McpToolDefinition 转换为 FunctionToolCallback，使 LLM 能直接发现和调用 MCP 工具。
 * 此转换在 Infrastructure 层完成，Domain 层保持纯净（无 Spring AI 依赖）。
 * <p>
 * inputType 使用 Map.class，让 Spring AI 自动完成 JSON 对象 → Map 的反序列化，
 * 避免 String.class 时 Jackson 无法将 JSON 对象反序列化为字符串的问题。
 * <p>
 * 支持动态工具策略：
 * <ul>
 *   <li>MCP 工具总数 &lt; 20 → 直接内嵌到 Prompt 工具列表</li>
 *   <li>MCP 工具总数 ≥ 20 → 提供 query_mcp_tools 查询工具</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolCallbackAdapter {

    /** 工具内嵌 vs 查询模式的阈值 */
    private static final int MAX_EMBED_TOOLS = 20;

    private final IMcpToolRegistry mcpToolRegistry;

    /**
     * 获取所有已连接 MCP 服务器的 ToolCallback 列表
     */
    public List<ToolCallback> getAllMcpToolCallbacks() {
        return mcpToolRegistry.getAllTools().stream()
                .map(this::toFunctionToolCallback)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定服务器的工具回调列表
     */
    public List<ToolCallback> getToolCallbacksByServer(Long serverId) {
        return mcpToolRegistry.getToolsByServer(serverId).stream()
                .map(this::toFunctionToolCallback)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 McpToolDefinition 转换为 FunctionToolCallback
     * <p>
     * 转换规则：
     * - name = "mcp__{serverId}__{toolName}"（全局唯一）
     * - description = "[MCP:serverName] 原始描述"（标注来源）
     * - inputType = Map.class（Spring AI 自动将 JSON 对象反序列化为 Map）
     * - call() 时委托给 McpToolRegistry.execute()
     */
    private FunctionToolCallback<Map<String, Object>, String> toFunctionToolCallback(
            McpToolDefinition def) {
        return FunctionToolCallback.<Map<String, Object>, String>builder(
                def.getFullName(),
                input -> executeMcpTool(def, input)
        )
        .description(buildDescription(def))
        .inputType(Map.class)
        .build();
    }

    private String executeMcpTool(McpToolDefinition def, Map<String, Object> input) {
        String executionId = "swarm-" + def.getServerId() + "-" + System.currentTimeMillis();

        try {
            McpToolResult result = mcpToolRegistry
                    .execute(def.getServerId(), def.getToolName(), input, executionId)
                    .join();

            if (result.isAborted()) {
                log.info("[McpToolCallback] Tool aborted executionId={}", executionId);
                return "Tool call was aborted.";
            }

            if (!result.isSuccess()) {
                log.warn("[McpToolCallback] Tool execution failed: {}",
                        result.getErrorMessage());
                return "Error: " + result.getErrorMessage();
            }

            return result.getContent();
        } catch (Exception e) {
            log.error("[McpToolCallback] Tool execution exception tool={}", def.getFullName(), e);
            return "Exception: " + e.getMessage();
        }
    }

    private String buildDescription(McpToolDefinition def) {
        String serverName = def.getServerName() != null ? def.getServerName() : String.valueOf(def.getServerId());
        String desc = def.getDescription();
        if (desc == null || desc.isBlank()) {
            return "[MCP:" + serverName + "]";
        }
        return "[MCP:" + serverName + "] " + desc;
    }

    // ===== 动态工具策略方法 =====

    /**
     * 判断当前 MCP 工具集是否应内嵌到 Prompt（&lt; 20 工具）或使用查询模式（≥ 20）。
     *
     * @param tools 工具列表
     * @return true 表示应内嵌，false 表示应使用查询工具
     */
    public boolean shouldEmbedInPrompt(List<McpToolDefinition> tools) {
        return tools == null || tools.size() < MAX_EMBED_TOOLS;
    }

    /**
     * 按 userId 获取用户工具并判断是否应内嵌。
     *
     * @param userId 用户 ID
     * @return true 表示应内嵌，false 表示应使用查询工具
     */
    public boolean shouldEmbedInPromptByUserId(Long userId) {
        List<McpToolDefinition> tools = mcpToolRegistry.getToolsByUserId(userId);
        return shouldEmbedInPrompt(tools);
    }

    /**
     * 构建内嵌到 Prompt 的工具描述文本（&lt; 20 工具时使用）。
     *
     * <p>格式：
     * <pre>
     * 【MCP 可用工具】（共 N 个）
     * 1. mcp__{serverId}__{toolName} - [MCP:serverName] 描述
     * ...
     * </pre>
     *
     * @param tools 工具列表
     * @return 格式化的工具描述块
     */
    public String buildEmbeddableToolSection(List<McpToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return "【MCP 可用工具】\n（当前无可用 MCP 工具）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【MCP 可用工具】（共 ").append(tools.size()).append(" 个）\n");
        int idx = 1;
        for (McpToolDefinition tool : tools) {
            sb.append(idx++).append(". ").append(tool.getFullName());
            String desc = tool.getDescription();
            if (desc != null && !desc.isBlank()) {
                sb.append(" - ").append(desc);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 按 userId 获取用户工具并构建内嵌工具描述文本。
     *
     * @param userId 用户 ID
     * @return 格式化的工具描述块
     */
    public String buildEmbeddableToolSectionByUserId(Long userId) {
        List<McpToolDefinition> tools = mcpToolRegistry.getToolsByUserId(userId);
        return buildEmbeddableToolSection(tools);
    }

    /**
     * 获取查询工具的 @Tool 描述（≥ 20 工具时追加到 SwarmTools）。
     *
     * <p>工具名：query_mcp_tools
     * 功能：按关键词查询 MCP 工具列表，返回匹配的工具名 + 描述。
     *
     * @param userId      用户 ID
     * @param workspaceId workspace ID
     * @return 查询工具的 ToolCallback
     */
    public ToolCallback createQueryMcpToolsCallback(Long userId, Long workspaceId) {
        return FunctionToolCallback.<Map<String, Object>, String>builder(
                "query_mcp_tools",
                input -> executeQueryMcpTools(userId, input)
        )
        .description("按关键词搜索当前 workspace 的 MCP 工具列表。返回匹配的工具名、服务器来源和描述。")
        .inputType(Map.class)
        .build();
    }

    /**
     * 构建 query_mcp_tools 工具的提示词描述（≥ 20 工具时使用）。
     *
     * @param role Agent 角色
     * @return 格式化的工具描述块
     */
    public String buildQueryToolSection(SwarmRole role) {
        StringBuilder sb = new StringBuilder();
        sb.append("【MCP 工具查询】\n");
        sb.append("MCP 工具数量较多，使用 query_mcp_tools 工具按需查询。\n");
        sb.append("1. query_mcp_tools - 按关键词搜索 MCP 工具列表，返回匹配的工具名、服务器来源和描述。参数：keyword（搜索关键词）。\n");
        return sb.toString();
    }

    /**
     * 执行 query_mcp_tools 查询。
     *
     * @param userId 用户 ID
     * @param input  查询参数（包含 keyword）
     * @return 匹配的工具列表文本
     */
    private String executeQueryMcpTools(Long userId, Map<String, Object> input) {
        String keyword = input != null ? String.valueOf(input.getOrDefault("keyword", "")) : "";
        List<McpToolDefinition> allTools = mcpToolRegistry.getToolsByUserId(userId);

        if (keyword == null || keyword.isBlank()) {
            // 无关键词，返回所有工具（分页截断）
            return formatToolList(allTools, "所有 MCP 工具", 20);
        }

        // 关键词过滤
        String lower = keyword.toLowerCase();
        List<McpToolDefinition> matched = allTools.stream()
                .filter(t -> {
                    boolean nameMatch = t.getToolName() != null
                            && t.getToolName().toLowerCase().contains(lower);
                    boolean descMatch = t.getDescription() != null
                            && t.getDescription().toLowerCase().contains(lower);
                    boolean serverMatch = t.getServerName() != null
                            && t.getServerName().toLowerCase().contains(lower);
                    return nameMatch || descMatch || serverMatch;
                })
                .toList();

        return formatToolList(matched, "MCP 工具搜索结果", 20);
    }

    private String formatToolList(List<McpToolDefinition> tools, String title, int limit) {
        if (tools == null || tools.isEmpty()) {
            return title + "：无匹配工具";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("（共 ").append(tools.size()).append(" 个）：\n");

        int count = 0;
        for (McpToolDefinition tool : tools) {
            if (count++ >= limit) {
                sb.append("... 还有 ").append(tools.size() - count + 1).append(" 个工具未显示。\n");
                break;
            }
            sb.append("- ").append(tool.getFullName());
            String desc = tool.getDescription();
            if (desc != null && !desc.isBlank()) {
                sb.append("：").append(desc);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
