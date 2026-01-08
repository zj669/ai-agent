package com.zj.aiagent.domain.toolbox;

import com.zj.aiagent.domain.toolbox.entity.ToolExecutionResult;
import com.zj.aiagent.domain.toolbox.entity.ToolMetadata;
import io.modelcontextprotocol.client.McpSyncClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP Provider - 管理 MCP 工具客户端
 */
public interface McpProvider {

    /**
     * 获取所有 MCP 客户端
     * 
     * @param executionId 执行ID
     * @return MCP 客户端列表
     */
    List<McpSyncClient> getMcpClients(String executionId);

    /**
     * 获取支持指定工具的客户端
     * 
     * @param executionId 执行ID
     * @param toolName    工具名称
     * @return MCP 客户端(如果找到)
     */
    Optional<McpSyncClient> findClientForTool(String executionId, String toolName);

    /**
     * 刷新 MCP 客户端
     * 
     * @param executionId 执行ID
     */
    void refreshClients(String executionId);

    // ========== 智能工具推荐和执行 ==========

    /**
     * 智能推荐工具
     * <p>
     * 根据当前上下文（用户问题、对话历史等）智能推荐相关工具
     *
     * @param executionId  执行ID
     * @param contextHints 上下文提示
     * @return 推荐的工具元数据列表（按相关性排序）
     */
    List<ToolMetadata> recommendTools(
            String executionId,
            Map<String, Object> contextHints);

    /**
     * 执行指定工具
     *
     * @param executionId 执行ID
     * @param toolName    工具名称
     * @param params      工具参数
     * @return 工具执行结果
     */
    ToolExecutionResult executeTool(
            String executionId,
            String toolName,
            Map<String, Object> params);

    /**
     * 获取所有可用工具的元数据
     *
     * @param executionId 执行ID
     * @return 所有工具元数据列表
     */
    List<ToolMetadata> getAllTools(String executionId);
}
