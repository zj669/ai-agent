package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;

import java.util.List;

/**
 * 节点模板仓储接口
 *
 * @author zj
 * @since 2025-12-27
 */
public interface INodeTemplateRepository {

    /**
     * 查询所有节点模板
     *
     * @return 节点模板列表
     */
    List<AiNodeTemplatePO> findAll();

    /**
     * 根据节点类型查询模板
     *
     * @param nodeType 节点类型
     * @return 节点模板
     */
    AiNodeTemplatePO findByNodeType(String nodeType);
}
