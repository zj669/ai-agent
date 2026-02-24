package com.zj.aiagent.application.llm;

import com.zj.aiagent.application.llm.dto.*;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private final LlmProviderConfigRepository repository;
    private final RestClient.Builder restClientBuilder;

    public List<LlmConfigDTO> listConfigs(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(Long userId, CreateLlmConfigRequest request) {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .userId(userId)
                .name(request.getName())
                .provider(request.getProvider())
                .baseUrl(request.getBaseUrl())
                .apiKey(request.getApiKey())
                .model(request.getModel())
                .isDefault(false)
                .status(1)
                .build();
        repository.save(config);
        log.info("[LLM] Created config: id={}, name={}", config.getId(), config.getName());
        return config.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, UpdateLlmConfigRequest request) {
        LlmProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + id));
        if (request.getName() != null) config.setName(request.getName());
        if (request.getBaseUrl() != null) config.setBaseUrl(request.getBaseUrl());
        if (request.getApiKey() != null) config.setApiKey(request.getApiKey());
        if (request.getModel() != null) config.setModel(request.getModel());
        if (request.getIsDefault() != null && request.getIsDefault()) {
            repository.clearDefault(config.getUserId());
            config.setIsDefault(true);
        }
        repository.update(config);
    }

    public void deleteConfig(Long id) {
        repository.deleteById(id);
    }

    /**
     * 测试连通性：尝试调用 LLM
     */
    public TestResultDTO testConfig(Long id) {
        LlmProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + id));
        long start = System.currentTimeMillis();
        try {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .restClientBuilder(restClientBuilder)
                    .build();
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(config.getModel()).build())
                    .build();
            chatModel.call("ping");
            long latency = System.currentTimeMillis() - start;
            return TestResultDTO.builder().ok(true).latencyMs(latency).build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return TestResultDTO.builder().ok(false).latencyMs(latency).error(e.getMessage()).build();
        }
    }

    public LlmProviderConfig getDefaultConfig(Long userId) {
        return repository.findDefault(userId).orElse(null);
    }

    private LlmConfigDTO toDTO(LlmProviderConfig config) {
        return LlmConfigDTO.builder()
                .id(config.getId())
                .name(config.getName())
                .provider(config.getProvider())
                .baseUrl(config.getBaseUrl())
                .model(config.getModel())
                .isDefault(config.getIsDefault())
                .status(config.getStatus())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
