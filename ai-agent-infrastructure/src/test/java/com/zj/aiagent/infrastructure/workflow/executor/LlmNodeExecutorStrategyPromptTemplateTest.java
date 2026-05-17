package com.zj.aiagent.infrastructure.workflow.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.FieldSchema;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
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
        PromptTemplateResolver resolver = new PromptTemplateResolver(
            new PromptValueFormatter(objectMapper)
        );
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
        context.setNodeOutput(
            "knowledge-1",
            Map.of("knowledge_list", List.of("知识A", "知识B"))
        );
        context.setNodeOutput("tool-1", Map.of("result", "库存充足"));

        NodeConfig config = NodeConfig.builder()
            .properties(
                Map.of(
                    "userPromptTemplate",
                    "知识：{{knowledge-1.output.knowledge_list}}\n工具：{{tool-1.output.result}}\n问题：{{inputs.query}}"
                )
            )
            .build();

        Map<String, Object> resolvedInputs = Map.of("__context__", context);

        String prompt = strategy.buildUserPrompt(config, resolvedInputs);

        assertEquals(
            "知识：[\"知识A\",\"知识B\"]\n工具：库存充足\n问题：介绍这个产品",
            prompt
        );
    }

    @Test
    @DisplayName("应解析 systemPrompt 中的节点路径引用")
    void should_resolve_system_prompt_template_reference() {
        ExecutionContext context = ExecutionContext.builder()
            .inputs(Map.of("query", "介绍这个产品"))
            .build();
        context.setNodeOutput(
            "knowledge-1",
            Map.of("knowledge_list", List.of("知识A", "知识B"))
        );

        Map<String, Object> resolvedInputs = Map.of("__context__", context);

        String prompt = strategy.resolvePromptTemplate(
            "系统参考：{{knowledge-1.output.knowledge_list}}",
            resolvedInputs
        );

        assertEquals("系统参考：[\"知识A\",\"知识B\"]", prompt);
    }

    @Test
    @DisplayName("JSON 模式应将用户定义字段和描述写入 system prompt")
    void should_include_json_fields_and_descriptions_in_system_prompt() {
        Node node = buildLlmNode(
            "json",
            List.of(
                jsonOutputField(),
                jsonField("title", "string", "文章标题"),
                jsonField("score", "number", "质量分")
            )
        );

        String prompt = strategy.buildSystemPrompt(
            node,
            node.getConfig(),
            ExecutionContext.builder().build(),
            Map.of(),
            null,
            "请生成文章"
        );

        assertTrue(prompt.contains("\"title\" (string): 文章标题"));
        assertTrue(prompt.contains("\"score\" (number): 质量分"));
        assertTrue(prompt.contains("\"title\" : \"string\""));
        assertTrue(prompt.contains("\"score\" : 0"));
    }

    @Test
    @DisplayName("JSON 模式未定义用户字段时应拒绝生成 system prompt")
    void should_reject_json_mode_without_user_defined_fields() {
        Node node = buildLlmNode("json", List.of(jsonOutputField()));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () ->
                strategy.buildSystemPrompt(
                    node,
                    node.getConfig(),
                    ExecutionContext.builder().build(),
                    Map.of(),
                    null,
                    "请生成文章"
                )
        );

        assertTrue(error.getMessage().contains("至少需要定义一个输出字段"));
    }

    @Test
    @DisplayName("JSON 模式用户字段缺少描述时应拒绝生成 system prompt")
    void should_reject_json_mode_field_without_description() {
        Node node = buildLlmNode(
            "json",
            List.of(jsonOutputField(), jsonField("title", "string", " "))
        );

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () ->
                strategy.buildSystemPrompt(
                    node,
                    node.getConfig(),
                    ExecutionContext.builder().build(),
                    Map.of(),
                    null,
                    "请生成文章"
                )
        );

        assertTrue(error.getMessage().contains("JSON 输出字段缺少描述: title"));
    }

    @Test
    @DisplayName("文本模式只产出 response 字段")
    void should_output_only_response_in_text_mode() throws Exception {
        Node node = buildLlmNode("text", List.of());

        Map<String, Object> outputs = strategy.buildOutputs("正常文本", node);

        assertEquals(1, outputs.size());
        assertEquals("正常文本", outputs.get("response"));
    }

    @Test
    @DisplayName("JSON 模式只产出 json_output 和用户定义字段")
    void should_output_only_json_output_and_defined_fields_in_json_mode() throws Exception {
        Node node = buildLlmNode(
            "json",
            List.of(
                jsonOutputField(),
                jsonField("title", "string", "文章标题"),
                jsonField("score", "number", "质量分")
            )
        );

        Map<String, Object> outputs = strategy.buildOutputs(
            "{\"title\":\"标题\",\"score\":98,\"extra\":\"忽略\"}",
            node
        );

        assertEquals(3, outputs.size());
        assertEquals("标题", outputs.get("title"));
        assertEquals(98, outputs.get("score"));
        assertFalse(outputs.containsKey("response"));
        assertFalse(outputs.containsKey("text"));
        assertFalse(outputs.containsKey("llm_output"));
        assertFalse(outputs.containsKey("extra"));
    }

    @Test
    @DisplayName("JSON 模式缺少用户定义字段时应失败")
    void should_fail_when_json_output_missing_defined_field() {
        Node node = buildLlmNode(
            "json",
            List.of(jsonOutputField(), jsonField("title", "string", "文章标题"))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> strategy.buildOutputs("{\"content\":\"正文\"}", node)
        );
    }

    @Test
    @DisplayName("JSON 模式没有用户定义字段时输出构建失败")
    void should_fail_when_json_mode_has_no_defined_fields() {
        Node node = buildLlmNode("json", List.of(jsonOutputField()));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> strategy.buildOutputs("{\"title\":\"标题\"}", node)
        );

        assertTrue(error.getMessage().contains("至少需要定义一个输出字段"));
    }

    private Node buildLlmNode(String outputMode, List<FieldSchema> outputSchema) {
        return Node.builder()
            .nodeId("llm-1")
            .name("LLM")
            .type(NodeType.LLM)
            .config(
                NodeConfig.builder()
                    .properties(Map.of("llmOutputMode", outputMode))
                    .build()
            )
            .outputSchema(outputSchema)
            .build();
    }

    private FieldSchema jsonOutputField() {
        return FieldSchema.builder()
            .key("json_output")
            .label("JSON 输出")
            .type("object")
            .system(true)
            .build();
    }

    private FieldSchema jsonField(String key, String type, String description) {
        return FieldSchema.builder()
            .key(key)
            .label(key)
            .type(type)
            .description(description)
            .required(true)
            .build();
    }
}
