package com.zj.aiagent.infrastructure.workflow.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptTemplateResolverTest {

    private PromptTemplateResolver resolver;

    @BeforeEach
    void setUp() {
        PromptValueFormatter formatter = new PromptValueFormatter(new ObjectMapper());
        resolver = new PromptTemplateResolver(formatter);
    }

    @Test
    @DisplayName("应替换简单变量占位符")
    void should_resolve_simple_placeholder() {
        String template = "问题：{{input}}";
        Map<String, Object> resolvedInputs = Map.of("input", "什么是向量数据库");

        String result = resolver.resolve(template, resolvedInputs, ExecutionContext.builder().build());

        assertEquals("问题：什么是向量数据库", result);
    }

    @Test
    @DisplayName("应替换全局输入引用")
    void should_resolve_inputs_reference() {
        String template = "问题：{{inputs.query}}";
        ExecutionContext context = ExecutionContext.builder()
            .inputs(Map.of("query", "今天天气怎么样"))
            .build();

        String result = resolver.resolve(template, Map.of(), context);

        assertEquals("问题：今天天气怎么样", result);
    }

    @Test
    @DisplayName("应替换节点输出路径引用")
    void should_resolve_node_output_reference() {
        String template = "知识库：{{knowledge-1.output.knowledge_list}}";
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput("knowledge-1", Map.of("knowledge_list", List.of("片段1", "片段2")));

        String result = resolver.resolve(template, Map.of(), context);

        assertEquals("知识库：[\"片段1\",\"片段2\"]", result);
    }

    @Test
    @DisplayName("应兼容旧的 #{} 占位符")
    void should_resolve_hash_placeholder() {
        String template = "问题：#{input}";
        Map<String, Object> resolvedInputs = Map.of("input", "帮我总结这个文档");

        String result = resolver.resolve(template, resolvedInputs, ExecutionContext.builder().build());

        assertEquals("问题：帮我总结这个文档", result);
    }

    @Test
    @DisplayName("路径不存在时应保留原始占位符")
    void should_keep_original_placeholder_when_reference_missing() {
        String template = "结果：{{tool-1.output.result}}";

        String result = resolver.resolve(template, Map.of(), ExecutionContext.builder().build());

        assertEquals("结果：{{tool-1.output.result}}", result);
    }

    @Test
    @DisplayName("找到路径但值为 null 时应替换为空串")
    void should_replace_with_empty_string_when_value_is_null() {
        String template = "工具结果：{{tool-1.output.result}}";
        ExecutionContext context = ExecutionContext.builder().build();
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("result", null);
        context.setNodeOutput("tool-1", outputs);

        String result = resolver.resolve(template, Map.of(), context);

        assertEquals("工具结果：", result);
    }
}
