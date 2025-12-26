package com.zj.aiagent.domain.toolbox.service;

import com.zj.aiagent.domain.toolbox.McpProvider;
import com.zj.aiagent.domain.toolbox.entity.ToolExecutionResult;
import com.zj.aiagent.domain.toolbox.entity.ToolMetadata;
import com.zj.aiagent.domain.toolbox.repository.McpClientRepository;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MCP 领域服务
 * <p>
 * 实现工具管理的业务逻辑，依赖技术接口（Repository）
 */
@Slf4j
@Service
@AllArgsConstructor
public class McpService implements McpProvider {

    private final McpClientRepository mcpClientRepository;

    @Override
    public List<McpSyncClient> getMcpClients(String executionId) {
        return mcpClientRepository.getAllClients(executionId);
    }

    @Override
    public Optional<McpSyncClient> findClientForTool(String executionId, String toolName) {
        return mcpClientRepository.findClientForTool(executionId, toolName);
    }

    @Override
    public void refreshClients(String executionId) {
        log.info("[{}] 刷新 MCP 客户端", executionId);
        mcpClientRepository.refresh(executionId);
    }

    @Override
    public List<ToolMetadata> recommendTools(String executionId, Map<String, Object> contextHints) {
        try {
            List<ToolMetadata> allTools = getAllTools(executionId);

            if (contextHints == null || contextHints.isEmpty()) {
                log.debug("[{}] 无上下文提示，返回所有工具: {} 个", executionId, allTools.size());
                return allTools;
            }

            String userQuestion = (String) contextHints.get("userQuestion");
            List<ToolMetadata> recommended = allTools.stream()
                    .map(tool -> calculateRelevance(tool, userQuestion))
                    .filter(tool -> tool.getRelevanceScore() > 0.3)
                    .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                    .limit(5)
                    .collect(Collectors.toList());

            log.info("[{}] 智能推荐 {} 个工具（总共 {} 个）",
                    executionId, recommended.size(), allTools.size());

            return recommended;

        } catch (Exception e) {
            log.error("[{}] 智能推荐工具失败: {}", executionId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public ToolExecutionResult executeTool(String executionId, String toolName, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        try {
            Optional<McpSyncClient> clientOpt = findClientForTool(executionId, toolName);
            if (!clientOpt.isPresent()) {
                String errorMsg = "工具未找到: " + toolName;
                log.warn("[{}] {}", executionId, errorMsg);
                return ToolExecutionResult.failure(toolName, errorMsg);
            }

            McpSyncClient client = clientOpt.get();
            log.info("[{}] 执行工具: {}, 参数: {}", executionId, toolName, params);
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, params != null ? params :
                    Map.of());
            McpSchema.CallToolResult result = client.callTool(request);
            String resultText = extractToolResult(result);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] 工具执行成功: {}, 耗时: {}ms", executionId, toolName, duration);
            return ToolExecutionResult.success(toolName, resultText, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 工具执行失败: {} (耗时: {}ms)", executionId, e.getMessage(), duration, e);
            return ToolExecutionResult.failure(toolName, e.getMessage());
        }
    }

    @Override
    public List<ToolMetadata> getAllTools(String executionId) {
        try {
            List<McpSyncClient> clients = getMcpClients(executionId);
            List<ToolMetadata> tools = clients.stream()
                    .flatMap(client -> convertToolsToMetadata(client).stream())
                    .collect(Collectors.toList());

            log.debug("[{}] 获取所有工具: {} 个", executionId, tools.size());
            return tools;

        } catch (Exception e) {
            log.error("[{}] 获取工具列表失败: {}", executionId, e.getMessage(), e);
            return List.of();
        }
    }

    // ========== 私有业务逻辑方法 ==========

    /**
     * 计算工具相关性（简单关键词匹配）
     */
    private ToolMetadata calculateRelevance(ToolMetadata tool, String userQuestion) {
        if (userQuestion == null || userQuestion.isEmpty()) {
            tool.setRelevanceScore(0.5);
            return tool;
        }

        String question = userQuestion.toLowerCase();
        String description = tool.getDescription().toLowerCase();
        double score = 0.0;

        // 关键词匹配
        if ((question.contains("天气") || question.contains("weather")) && description.contains("weather")) {
            score = 0.9;
        } else if ((question.contains("搜索") || question.contains("search")) && description.contains("search")) {
            score = 0.9;
        } else if ((question.contains("计算") || question.contains("math")) && description.contains("calc")) {
            score = 0.9;
        } else {
            score = 0.5;
        }

        tool.setRelevanceScore(score);
        return tool;
    }

    /**
     * 转换 MCP 工具为元数据
     */
    private List<ToolMetadata> convertToolsToMetadata(McpSyncClient client) {
        try {
            McpSchema.ListToolsResult result = client.listTools();
            return result.tools().stream()
                    .map(tool -> ToolMetadata.builder()
                            .name(tool.name())
                            .description(tool.description() != null ? tool.description() : "")
                            .category(inferCategory(tool.name()))
                            .inputSchema(Map.of())
                            .relevanceScore(0.5)
                            .provider(client.getClass().getSimpleName())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("转换工具元数据失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 推断工具分类
     */
    private String inferCategory(String toolName) {
        String name = toolName.toLowerCase();
        if (name.contains("search") || name.contains("query")) {
            return "search";
        } else if (name.contains("weather")) {
            return "weather";
        } else if (name.contains("calc") || name.contains("math")) {
            return "calculator";
        }
        return "general";
    }

    /**
     * 提取工具执行结果
     */
    private String extractToolResult(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null) {
            return "";
        }
        return result.content().stream()
                .map(content -> {
                    if(content.type().equals("text")){
                        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
                        return textContent.text();
                    }else{
                        return "";
                    }

                })
                .collect(Collectors.joining("\n"));
    }
}
