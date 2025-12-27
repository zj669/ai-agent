package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;

public interface IAiModelRepository {
    AiModelPO getById(String modelId);
}
