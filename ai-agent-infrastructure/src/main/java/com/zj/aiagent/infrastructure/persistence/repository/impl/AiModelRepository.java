package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiModelMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiModelRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class AiModelRepository implements IAiModelRepository {
    @Resource
    private AiModelMapper aiModelMapper;
    @Override
    public AiModelPO getById(String modelId) {
        return aiModelMapper.selectOne(new LambdaQueryWrapper<AiModelPO>()
                .eq(AiModelPO::getModelId, modelId)
        );
    }
}
