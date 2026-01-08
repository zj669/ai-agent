package com.zj.aiagent.application.config;

import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;
import com.zj.aiagent.infrastructure.persistence.repository.IConfigFieldDefinitionRepository;
import com.zj.aiagent.infrastructure.persistence.repository.INodeTemplateRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 配置应用服务实现
 *
 * @author zj
 * @since 2025-12-27
 */
@Slf4j
@Service
public class AgentConfigApplicationService implements IAgentConfigApplicationService {

    @Resource
    private INodeTemplateRepository nodeTemplateRepository;

    @Resource
    private IConfigFieldDefinitionRepository configFieldDefinitionRepository;

    @Override
    public List<AiNodeTemplatePO> getNodeTemplates() {
        log.info("查询所有节点模板");
        return nodeTemplateRepository.findAll();
    }

    @Override
    public List<AiConfigFieldDefinitionPO> getConfigSchema(String module) {
        log.info("查询配置Schema: module={}", module);
        return configFieldDefinitionRepository.findByConfigType(module);
    }
}
