package com.zj.aiagent.application.config;

import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;

import java.util.List;

/**
 * Agent 配置应用服务接口
 *
 * @author zj
 * @since 2025-12-27
 */
public interface IAgentConfigApplicationService {

    /**
     * 获取所有节点模板
     *
     * @return 节点模板列表
     */
    List<AiNodeTemplatePO> getNodeTemplates();

    /**
     * 根据配置模块获取配置字段定义
     *
     * @param module 配置模块 (如: MODEL_CONFIG, MCP_TOOL等)
     * @return 配置字段列表
     */
    List<AiConfigFieldDefinitionPO> getConfigSchema(String module);
}
