package com.zj.aiagent.infrastructure.context.mcp;

import com.zj.aiagent.domain.toolbox.repository.McpClientRepository;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP Client Repository Spring Context 实现
 * <p>
 * 从 Spring 上下文获取 MCP 客户端
 */
@Slf4j
@Repository
@AllArgsConstructor
public class SpringContextMcpClientRepository implements McpClientRepository {

    private final ApplicationContext applicationContext;

    @Override
    public List<McpSyncClient> getAllClients(String executionId) {
        // 从 Spring Context 获取所有 McpSyncClient Bean
        Map<String, McpSyncClient> beans = applicationContext.getBeansOfType(McpSyncClient.class);
        List<McpSyncClient> clients = new ArrayList<>(beans.values());

        log.debug("[{}] [Spring Context] 获取 MCP 客户端: {} 个", executionId, clients.size());
        return clients;
    }

    @Override
    public Optional<McpSyncClient> findClientForTool(String executionId, String toolName) {
        List<McpSyncClient> clients = getAllClients(executionId);

        for (McpSyncClient client : clients) {
            try {
                McpSchema.ListToolsResult result = client.listTools();
                boolean supports = result.tools().stream()
                        .anyMatch(tool -> tool.name().equals(toolName));

                if (supports) {
                    log.debug("[{}] [Spring Context] 找到支持工具 {} 的客户端: {}",
                            executionId, toolName, client.getClass().getSimpleName());
                    return Optional.of(client);
                }
            } catch (Exception e) {
                log.warn("[{}] 检查 MCP 客户端工具失败: {}", executionId, e.getMessage());
            }
        }

        log.warn("[{}] [Spring Context] 未找到支持工具 {} 的客户端", executionId, toolName);
        return Optional.empty();
    }

    @Override
    public void refresh(String executionId) {
        // Spring Context 管理的 Bean 无需手动刷新
        log.info("[{}] [Spring Context] MCP 客户端由 Spring 管理，无需手动刷新", executionId);
    }
}
