package com.zj.aiagent.infrastructure.workflow.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.infrastructure.workflow.template.PromptTemplateResolver;
import com.zj.aiagent.infrastructure.workflow.template.PromptValueFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LlmNodeExecutorStrategyPromptTemplateTest {

    private LlmNodeExecutorStrategy strategy;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        PromptTemplateResolver resolver = new PromptTemplateResolver(new PromptValueFormatter(objectMapper));
        Executor directExecutor = Runnable::run;
        strategy = new LlmNodeExecutorStrategy(
            directExecutor,
            RestClient.builder(),
            objectMapper,
            mock(KnowledgeRetrievalService.class),
            mock(LlmProviderConfigRepository.class),
            resolver
        );
    }

    @Test
    @DisplayName("未配置 userPromptTemplate 时应回退到 input")
    void should_fallback_to_input_when_template_missing() {
        NodeConfig config = NodeConfig.builder().properties(Map.of()).build();
        Map<String, Object> resolvedInputs = Map.of("input", "请帮我总结");

        String prompt = strategy.buildUserPrompt(config, resolvedInputs);

        assertEquals("请帮我总结", prompt);
    }

    @Test
    @DisplayName("应解析模板中的多跳节点路径引用")
    void should_resolve_prompt_template_path_reference() {
        ExecutionContext context = ExecutionContext.builder()
            .inputs(Map.of("query", "介绍这个产品"))
            .build();
        context.setNodeOutput("knowledge-1", Map.of("knowledge_list", List.of("知识A", "知识B")));
        context.setNodeOutput("tool-1", Map.of("result", "库存充足"));

        NodeConfig config = NodeConfig.builder()
            .properties(Map.of(
                "userPromptTemplate",
                "知识：{{knowledge-1.output.knowledge_list}}\n工具：{{tool-1.output.result}}\n问题：{{inputs.query}}"
            ))
            .build();

        Map<String, Object> resolvedInputs = Map.of("__context__", context);

        String prompt = strategy.buildUserPrompt(config, resolvedInputs);

        assertEquals("知识：[\"知识A\",\"知识B\"]\n工具：库存充足\n问题：介绍这个产品", prompt);
    }
}
