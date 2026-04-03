package com.zj.aiagent.infrastructure.mcp.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.port.IMcpServerRepository;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import com.zj.aiagent.infrastructure.mcp.mapper.McpServerConfigMapper;
import com.zj.aiagent.infrastructure.mcp.po.McpServerConfigPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MCP 服务器仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class McpServerRepositoryImpl implements IMcpServerRepository {

    private final McpServerConfigMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(McpServer server) {
        McpServerConfigPO po = toPO(server);
        if (po.getId() == null) {
            mapper.insert(po);
            server.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public Optional<McpServer> findById(Long id) {
        McpServerConfigPO po = mapper.selectById(id);
        return Optional.ofNullable(toDomain(po));
    }

    @Override
    public List<McpServer> findByUserId(Long userId) {
        LambdaQueryWrapper<McpServerConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServerConfigPO::getUserId, userId)
                .eq(McpServerConfigPO::getDeleted, 0)
                .orderByDesc(McpServerConfigPO::getUpdateTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        // 逻辑删除
        McpServerConfigPO po = new McpServerConfigPO();
        po.setId(id);
        po.setDeleted(1);
        mapper.updateById(po);
        log.info("[McpServerRepository] Logical delete server id={}", id);
    }

    // --- Converter ---

    private McpServer toDomain(McpServerConfigPO po) {
        if (po == null) return null;
        return McpServer.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .name(po.getName())
                .serverType(po.getServerType())
                .config(toMcpServerConfig(po.getConfigJson()))
                .enabled(po.getEnabled())
                .status(parseStatus(po.getStatus()))
                .description(po.getDescription())
                .deleted(po.getDeleted())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private McpServerConfigPO toPO(McpServer domain) {
        if (domain == null) return null;
        McpServerConfigPO po = new McpServerConfigPO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setName(domain.getName());
        po.setServerType(domain.getServerType());
        po.setConfigJson(toConfigJson(domain.getConfig()));
        po.setEnabled(domain.getEnabled());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : "DISCONNECTED");
        po.setDescription(domain.getDescription());
        po.setDeleted(domain.getDeleted());
        return po;
    }

    private McpServerConfig toMcpServerConfig(McpServerConfigPO.McpServerConfigJson json) {
        if (json == null) return null;
        return McpServerConfig.builder()
                .type(json.getType())
                .command(json.getCommand())
                .args(json.getArgs())
                .env(json.getEnv())
                .url(json.getUrl())
                .headers(json.getHeaders())
                .endpoint(json.getEndpoint())
                .build();
    }

    private McpServerConfigPO.McpServerConfigJson toConfigJson(McpServerConfig config) {
        if (config == null) return null;
        McpServerConfigPO.McpServerConfigJson json = new McpServerConfigPO.McpServerConfigJson();
        json.setType(config.getType());
        json.setCommand(config.getCommand());
        json.setArgs(config.getArgs());
        json.setEnv(config.getEnv());
        json.setUrl(config.getUrl());
        json.setHeaders(config.getHeaders());
        json.setEndpoint(config.getEndpoint());
        return json;
    }

    private McpServerStatus parseStatus(String status) {
        if (status == null) return McpServerStatus.DISCONNECTED;
        try {
            return McpServerStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return McpServerStatus.DISCONNECTED;
        }
    }
}
