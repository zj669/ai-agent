package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiConfigFieldDefinitionMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IConfigFieldDefinitionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 配置字段定义仓储实现
 *
 * @author zj
 * @since 2025-12-27
 */
@Slf4j
@Repository
public class ConfigFieldDefinitionRepository implements IConfigFieldDefinitionRepository {

    @Resource
    private AiConfigFieldDefinitionMapper configFieldDefinitionMapper;

    @Override
    public List<AiConfigFieldDefinitionPO> findByConfigType(String configType) {
        if (configType == null || configType.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<AiConfigFieldDefinitionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfigFieldDefinitionPO::getConfigType, configType)
                .orderByAsc(AiConfigFieldDefinitionPO::getSortOrder);
        return configFieldDefinitionMapper.selectList(wrapper);
    }
}
