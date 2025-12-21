package com.zj.aiagent.domain.agent.config.repository;

import com.zj.aiagent.domain.agent.config.entity.AdvisorEntity;
import com.zj.aiagent.domain.agent.config.entity.McpToolEntity;
import com.zj.aiagent.domain.agent.config.entity.ModelEntity;
import com.zj.aiagent.domain.agent.config.entity.NodeTemplateEntity;
import com.zj.aiagent.domain.agent.config.entity.SystemPromptEntity;

import java.util.List;
import java.util.Map;

/**
 * Agent 配置仓储接口
 *
 * @author zj
 * @since 2025-12-21
 */
public interface IAgentConfigRepository {

    /**
     * 查询所有节点模板
     *
     * @return 节点模板列表
     */
    List<NodeTemplateEntity> findAllNodeTemplates();

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

    /**
     * 根据promptId查询系统提示词
     *
     * @param promptId 提示词ID
     * @return 系统提示词实体，不存在则返回null
     */
    SystemPromptEntity findSystemPromptByPromptId(String promptId);

    Map<String, Object> findModelWithApiByModelId(String modelId);

    NodeTemplateEntity findNodeTemplateByNodeType(String nodeType);
}
