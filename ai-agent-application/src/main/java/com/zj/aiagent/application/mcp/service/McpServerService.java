package com.zj.aiagent.application.mcp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.mcp.cmd.CreateMcpServerCmd;
import com.zj.aiagent.application.mcp.cmd.UpdateMcpServerCmd;
import com.zj.aiagent.application.mcp.dto.McpServerDTO;
import com.zj.aiagent.application.mcp.dto.McpToolDTO;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.port.IMcpConnectionManager;
import com.zj.aiagent.domain.mcp.port.IMcpServerRepository;
import com.zj.aiagent.domain.mcp.port.IMcpToolRegistry;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 服务器应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final IMcpServerRepository repository;
    private final IMcpConnectionManager connectionManager;
    private final IMcpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MCP 服务器
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createServer(CreateMcpServerCmd cmd) {
        McpServerConfig config = parseConfig(cmd.getConfigJson());

        McpServer server = McpServer.builder()
                .userId(cmd.getUserId())
                .name(cmd.getName())
                .serverType(cmd.getServerType())
                .config(config)
                .enabled(cmd.getEnabled() != null ? cmd.getEnabled() : true)
                .status(McpServerStatus.DISCONNECTED)
                .description(cmd.getDescription())
                .deleted(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        repository.save(server);
        log.info("[McpServerService] Created server id={} name={}", server.getId(), server.getName());
        return server.getId();
    }

    /**
     * 更新 MCP 服务器
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateServer(UpdateMcpServerCmd cmd) {
        McpServer server = repository.findById(cmd.getId())
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + cmd.getId()));

        checkOwnership(server, cmd.getUserId());

        // 如果配置变化，先断开旧连接
        boolean configChanged = hasConfigChanged(server, cmd);
        if (configChanged && connectionManager.isConnected(cmd.getId())) {
            connectionManager.disconnect(cmd.getId());
        }

        server.setName(cmd.getName());
        server.setServerType(cmd.getServerType());
        server.setConfig(parseConfig(cmd.getConfigJson()));
        server.setEnabled(cmd.getEnabled());
        server.setDescription(cmd.getDescription());
        server.setUpdateTime(LocalDateTime.now());

        repository.save(server);
        log.info("[McpServerService] Updated server id={}", cmd.getId());
    }

    /**
     * 删除 MCP 服务器
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long serverId, Long userId) {
        McpServer server = repository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));

        checkOwnership(server, userId);

        // 断开连接
        if (connectionManager.isConnected(serverId)) {
            connectionManager.disconnect(serverId);
        }

        repository.deleteById(serverId);
        log.info("[McpServerService] Deleted server id={}", serverId);
    }

    /**
     * 触发连接
     */
    public void connectServer(Long serverId, Long userId) {
        McpServer server = repository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));

        checkOwnership(server, userId);

        if (!Boolean.TRUE.equals(server.getEnabled())) {
            throw new IllegalStateException("Server is disabled");
        }

        if (connectionManager.isConnected(serverId)) {
            log.info("[McpServerService] Server already connected id={}", serverId);
            return;
        }

        connectionManager.connectAndDiscover(serverId);
        log.info("[McpServerService] Triggered connect for server id={}", serverId);
    }

    /**
     * 断开连接
     */
    public void disconnectServer(Long serverId, Long userId) {
        McpServer server = repository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));

        checkOwnership(server, userId);

        if (!connectionManager.isConnected(serverId)) {
            // 即使池中已无记录，DB 状态也可能还是 CONNECTED（上次异常断开）
            // 强制同步 DB 状态为 DISCONNECTED，确保前端状态一致
            if (server.getStatus() != McpServerStatus.DISCONNECTED) {
                server.markDisconnected();
                repository.save(server);
                log.info("[McpServerService] Force sync DB status to DISCONNECTED for server id={}", serverId);
            } else {
                log.info("[McpServerService] Server already disconnected id={}", serverId);
            }
            return;
        }

        connectionManager.disconnect(serverId);
        // disconnect 已将 DB 状态更新为 DISCONNECTED
        log.info("[McpServerService] Disconnected server id={}", serverId);
    }

    /**
     * 获取服务器状态
     */
    public McpServerStatus getServerStatus(Long serverId) {
        McpServer server = repository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));
        return server.getStatus();
    }

    /**
     * 获取用户的所有服务器
     */
    public List<McpServerDTO> listServers(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(McpServerDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 获取服务器详情
     */
    public McpServerDTO getServer(Long serverId, Long userId) {
        McpServer server = repository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("MCP Server not found: " + serverId));
        checkOwnership(server, userId);
        return McpServerDTO.from(server);
    }

    // --- Helper ---

    private void checkOwnership(McpServer server, Long userId) {
        if (!server.isOwnedBy(userId)) {
            throw new SecurityException("Unauthorized: Server does not belong to user " + userId);
        }
    }

    private McpServerConfig parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, McpServerConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid config JSON: " + e.getMessage(), e);
        }
    }

    private boolean hasConfigChanged(McpServer server, UpdateMcpServerCmd cmd) {
        if (!server.getServerType().equals(cmd.getServerType())) {
            return true;
        }
        try {
            String oldJson = objectMapper.writeValueAsString(server.getConfig());
            String newJson = objectMapper.writeValueAsString(parseConfig(cmd.getConfigJson()));
            return !oldJson.equals(newJson);
        } catch (JsonProcessingException e) {
            return true;
        }
    }
}
