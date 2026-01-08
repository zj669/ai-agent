package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiApiPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiApiMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiApiRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class AiApiRepository implements IAiApiRepository {
    @Resource
    private AiApiMapper aiApiMapper;
    @Override
    public AiApiPO getById(String apiId) {
        return aiApiMapper.selectOne(new LambdaQueryWrapper<AiApiPO>()
                .eq(AiApiPO::getApiId, apiId)
        );
    }
}
