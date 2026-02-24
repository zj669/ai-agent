package com.zj.aiagent.infrastructure.llm.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import com.zj.aiagent.infrastructure.llm.mapper.LlmProviderConfigMapper;
import com.zj.aiagent.infrastructure.llm.po.LlmProviderConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LlmProviderConfigRepositoryImpl implements LlmProviderConfigRepository {

    private final LlmProviderConfigMapper mapper;

    @Override
    public void save(LlmProviderConfig config) {
        LlmProviderConfigPO po = toPO(config);
        mapper.insert(po);
        config.setId(po.getId());
    }

    @Override
    public Optional<LlmProviderConfig> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<LlmProviderConfig> findByUserId(Long userId) {
        LambdaQueryWrapper<LlmProviderConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LlmProviderConfigPO::getUserId, userId).orderByDesc(LlmProviderConfigPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<LlmProviderConfig> findDefault(Long userId) {
        LambdaQueryWrapper<LlmProviderConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LlmProviderConfigPO::getUserId, userId)
                .eq(LlmProviderConfigPO::getIsDefault, true)
                .eq(LlmProviderConfigPO::getStatus, 1);
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public void update(LlmProviderConfig config) {
        mapper.updateById(toPO(config));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void clearDefault(Long userId) {
        LambdaUpdateWrapper<LlmProviderConfigPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LlmProviderConfigPO::getUserId, userId)
                .set(LlmProviderConfigPO::getIsDefault, false);
        mapper.update(null, wrapper);
    }

    private LlmProviderConfigPO toPO(LlmProviderConfig domain) {
        LlmProviderConfigPO po = new LlmProviderConfigPO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setName(domain.getName());
        po.setProvider(domain.getProvider());
        po.setBaseUrl(domain.getBaseUrl());
        po.setApiKey(domain.getApiKey());
        po.setModel(domain.getModel());
        po.setIsDefault(domain.getIsDefault());
        po.setStatus(domain.getStatus());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private LlmProviderConfig toDomain(LlmProviderConfigPO po) {
        return LlmProviderConfig.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .name(po.getName())
                .provider(po.getProvider())
                .baseUrl(po.getBaseUrl())
                .apiKey(po.getApiKey())
                .model(po.getModel())
                .isDefault(po.getIsDefault())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
