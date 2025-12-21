package com.zj.aiagent.domain.agent.config.repository;

import com.zj.aiagent.domain.agent.config.entity.AdvisorEntity;
import com.zj.aiagent.domain.agent.config.entity.McpToolEntity;
import com.zj.aiagent.domain.agent.config.entity.ModelEntity;

import java.util.List;

/**
 * Agent 配置仓储接口
 *
 * @author zj
 * @since 2025-12-21
 */
public interface IAgentConfigRepository {

    /**
     * 查询所有启用的模型
     *
     * @return 模型列表
     */
    List<ModelEntity> findAllModels();

    /**
     * 查询所有启用的 Advisor
     *
     * @return Advisor 列表
     */
    List<AdvisorEntity> findAllAdvisors();

    /**
     * 查询所有启用的 MCP 工具
     *
     * @return MCP 工具列表
     */
    List<McpToolEntity> findAllMcpTools();
}
