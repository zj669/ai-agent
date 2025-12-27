package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiModelMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiModelRepository;
import jakarta.annotation.Resource;

public class AiModelRepository implements IAiModelRepository {
    @Resource
    private AiModelMapper aiModelMapper;
    @Override
    public AiModelPO getById(String modelId) {
        return aiModelMapper.selectById(modelId);
    }
}
