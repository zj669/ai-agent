package com.zj.aiagent.infrastructure.parse.adpater;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.NodeTemplate;
import com.zj.aiagent.domain.workflow.entity.StateField;
import com.zj.aiagent.infrastructure.context.AgentContextProvider;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 LLM 节点
 * <p>
 * 借鉴 LangGraph 的节点设计，所有 LLM 类节点共享同一实现。
 * 通过 NodeTemplate 配置实现不同的行为（PlanNode、SummaryNode 等）。
 * <p>
 * 核心流程：
 * 1. 从 State 提取变量
 * 2. 替换 Prompt 模板中的占位符
 * 3. 调用 AI
 * 4. 解析响应并构建 StateUpdate
 */
@Slf4j
public class GenericLLMNode extends BaseChatNodeExecutorAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 节点模板配置
     */
    private final NodeTemplate template;

    public GenericLLMNode(String nodeId, String nodeName, String description, String nodeType,
            OpenAiChatModel chatModel, String systemPrompt,
            AgentContextProvider contextProvider,
            NodeTemplate template) {
        super(nodeId, nodeName, description, nodeType, chatModel, systemPrompt, contextProvider);
        this.template = template;
    }

    @Override
    protected String buildPrompt(WorkflowState state) {
        String executionId = state.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
        return contextProvider.buildCompletePrompt(executionId, systemPrompt, state);
    }

    @Override
    protected StateUpdate processResponse(String aiResponse, WorkflowState state) {
        // 根据模板的 outputSchema 解析响应
        Map<String, Object> updates = new HashMap<>();

        if (template != null && template.getOutputSchema() != null && !template.getOutputSchema().isEmpty()) {
            // 有明确的 outputSchema：尝试解析 JSON 响应
            updates = parseStructuredOutput(aiResponse, template.getOutputSchema());
        } else {
            // 无 outputSchema：将整个响应作为 nodeId 的输出
            updates.put(nodeId + "_output", aiResponse);
        }

        log.debug("GenericLLMNode [{}] generated {} state updates", nodeName, updates.size());

        return StateUpdate.of(updates);
    }

    /**
     * 根据 outputSchema 解析结构化输出
     * <p>
     * 尝试将 AI 响应解析为 JSON，然后根据 Schema 提取字段
     *
     * @param aiResponse   AI 响应
     * @param outputSchema 输出 Schema
     * @return 状态更新 Map
     */
    private Map<String, Object> parseStructuredOutput(String aiResponse, List<StateField> outputSchema) {
        Map<String, Object> updates = new HashMap<>();

        try {
            // 提取 JSON 内容（处理 Markdown 代码块）
            String jsonContent = extractJson(aiResponse);

            // 解析为 Map
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(jsonContent, Map.class);

            // 根据 outputSchema 提取字段
            for (StateField field : outputSchema) {
                String fieldName = field.getName();
                if (parsed.containsKey(fieldName)) {
                    updates.put(fieldName, parsed.get(fieldName));
                } else if (field.isRequired()) {
                    log.warn("Required field '{}' not found in AI response", fieldName);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse structured output: {}, falling back to raw response", e.getMessage());
            // 解析失败，将原始响应作为第一个 outputSchema 字段的值
            if (!outputSchema.isEmpty()) {
                updates.put(outputSchema.get(0).getName(), aiResponse);
            }
        }

        return updates;
    }

    /**
     * 提取 JSON 内容（处理 Markdown 代码块）
     * <p>
     * 去除可能的 ```json ... ``` 包装
     */
    private String extractJson(String response) {
        String content = response.trim();

        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.trim();
    }
}
