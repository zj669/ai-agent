package com.zj.aiagent.domain.llm.repository;

import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;

import java.util.List;
import java.util.Optional;

public interface LlmProviderConfigRepository {

    void save(LlmProviderConfig config);

    Optional<LlmProviderConfig> findById(Long id);

    List<LlmProviderConfig> findByUserId(Long userId);

    Optional<LlmProviderConfig> findDefault(Long userId);

    void update(LlmProviderConfig config);

    void deleteById(Long id);

    void clearDefault(Long userId);
}
