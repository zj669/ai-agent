package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;

import java.util.List;

/**
 * 配置字段定义仓储接口
 *
 * @author zj
 * @since 2025-12-27
 */
public interface IConfigFieldDefinitionRepository {

    /**
     * 根据配置类型查询字段定义
     *
     * @param configType 配置类型
     * @return 字段定义列表
     */
    List<AiConfigFieldDefinitionPO> findByConfigType(String configType);
}
