package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiApiPO;

public interface IAiApiRepository {
    AiApiPO getById(String apiId);
}
