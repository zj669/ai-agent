package com.zj.aiagent.infrastructure.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.knowledge.RagProvider;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeChunk;
import com.zj.aiagent.domain.memory.MemoryProvider;
import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.Memory;
import com.zj.aiagent.domain.prompt.PromptProvider;
import com.zj.aiagent.domain.toolbox.McpProvider;
import com.zj.aiagent.domain.workflow.interfaces.ContextProvider;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 上下文Provider - 统一管理所有上下文相关功能
 * <p>
 * 聚合了 Memory、RAG、Prompt、MCP 等子Provider
 */
@Slf4j
@Component
public class AgentContextProvider implements ContextProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 变量占位符正则：匹配 {state.fieldName} 格式
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{state\\.(\\w+)\\}");

    private final MemoryProvider memoryProvider;
    private final RagProvider ragProvider;
    private final PromptProvider promptProvider;
    private final McpProvider mcpProvider;

    @Autowired(required = false)
    public AgentContextProvider(
            MemoryProvider memoryProvider,
            RagProvider ragProvider,
            PromptProvider promptProvider,
            McpProvider mcpProvider) {
        this.memoryProvider = memoryProvider;
        this.ragProvider = ragProvider;
        this.promptProvider = promptProvider;
        this.mcpProvider = mcpProvider;

        log.info("AgentContextProvider 初始化完成:");
        log.info("  - MemoryProvider: {}", memoryProvider != null ? "已注入" : "未配置");
        log.info("  - RagProvider: {}", ragProvider != null ? "已注入" : "未配置");
        log.info("  - PromptProvider: {}", promptProvider != null ? "已注入" : "未配置");
        log.info("  - McpProvider: {}", mcpProvider != null ? "已注入" : "未配置");
    }

    @Override
    public ConcurrentHashMap<String, Object> loadContext(
            String executionId,
            ConcurrentHashMap<String, Object> initialInput) {

        ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>(initialInput);

        // 1. 加载对话历史(Memory)
        if (memoryProvider != null) {
            try {
                List<ChatMessage> chatHistory = memoryProvider.loadChatHistory(executionId, 50);
                context.put(WorkflowRunningConstants.Context.CHAT_HISTORY_KEY, chatHistory);
                log.debug("[{}] 加载对话历史: {} 条消息", executionId, chatHistory.size());
            } catch (Exception e) {
                log.warn("[{}] 加载对话历史失败: {}", executionId, e.getMessage());
            }
        }

        // 2. 加载相关知识(RAG)
        if (ragProvider != null && context.containsKey(WorkflowRunningConstants.Context.USER_QUESTION_KEY)) {
            try {
                String query = (String) context.get(WorkflowRunningConstants.Context.USER_QUESTION_KEY);
                List<KnowledgeChunk> knowledge = ragProvider.retrieveKnowledge(executionId, query, 5);
                context.put(WorkflowRunningConstants.Context.RELEVANT_KNOWLEDGE_KEY, knowledge);
                log.debug("[{}] 检索相关知识: {} 个片段", executionId, knowledge.size());
            } catch (Exception e) {
                log.warn("[{}] 检索知识失败: {}", executionId, e.getMessage());
            }
        }

        // 4. 【新增】智能推荐工具
        if (mcpProvider != null) {
            try {
                Map<String, Object> contextHints = buildContextHints(context);
                List<com.zj.aiagent.domain.toolbox.entity.ToolMetadata> recommendedTools = mcpProvider
                        .recommendTools(executionId, contextHints);

                if (!recommendedTools.isEmpty()) {
                    context.put(WorkflowRunningConstants.Context.RECOMMENDED_TOOLS_KEY, recommendedTools);
                    log.info("[{}] 智能推荐 {} 个工具", executionId, recommendedTools.size());
                }
            } catch (Exception e) {
                log.warn("[{}] 智能推荐工具失败: {}", executionId, e.getMessage());
            }
        }

        // 5. 【新增】自动执行待执行工具
        if (mcpProvider != null && context.containsKey(WorkflowRunningConstants.Context.PENDING_TOOL_CALLS_KEY)) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pendingCalls = (List<Map<String, Object>>) context
                        .get(WorkflowRunningConstants.Context.PENDING_TOOL_CALLS_KEY);

                List<com.zj.aiagent.domain.toolbox.entity.ToolExecutionResult> toolResults = new ArrayList<>();

                for (Map<String, Object> call : pendingCalls) {
                    String toolName = (String) call.get("tool");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) call.get("params");

                    var result = mcpProvider.executeTool(executionId, toolName, params);
                    toolResults.add(result);
                }

                // 保存执行结果
                context.put(WorkflowRunningConstants.Context.TOOL_RESULTS_KEY, toolResults);
                // 清除待执行列表
                context.remove(WorkflowRunningConstants.Context.PENDING_TOOL_CALLS_KEY);

                log.info("[{}] 自动执行 {} 个工具", executionId, toolResults.size());

            } catch (Exception e) {
                log.error("[{}] 自动执行工具失败: {}", executionId, e.getMessage(), e);
            }
        }

        return context;
    }

    @Override
    public void saveContext(
            String executionId,
            ConcurrentHashMap<String, Object> delta) {

        // 1. 保存对话消息
        if (memoryProvider != null) {
            if (delta.containsKey(WorkflowRunningConstants.Context.USER_MESSAGE_KEY)) {
                try {
                    ChatMessage userMsg = (ChatMessage) delta.get(WorkflowRunningConstants.Context.USER_MESSAGE_KEY);
                    memoryProvider.saveChatMessage(executionId, userMsg);
                } catch (Exception e) {
                    log.warn("[{}] 保存用户消息失败: {}", executionId, e.getMessage());
                }
            }

            if (delta.containsKey(WorkflowRunningConstants.Context.ASSISTANT_MESSAGE_KEY)) {
                try {
                    ChatMessage assistantMsg = (ChatMessage) delta
                            .get(WorkflowRunningConstants.Context.ASSISTANT_MESSAGE_KEY);
                    memoryProvider.saveChatMessage(executionId, assistantMsg);
                } catch (Exception e) {
                    log.warn("[{}] 保存助手消息失败: {}", executionId, e.getMessage());
                }
            }

            // 保存长期记忆
            if (delta.containsKey(WorkflowRunningConstants.Context.LONG_TERM_MEMORY_KEY)) {
                try {
                    Memory memory = (Memory) delta.get(WorkflowRunningConstants.Context.LONG_TERM_MEMORY_KEY);
                    memoryProvider.saveLongTermMemory(executionId, memory);
                } catch (Exception e) {
                    log.warn("[{}] 保存长期记忆失败: {}", executionId, e.getMessage());
                }
            }
        }

        // 2. 保存知识片段
        if (ragProvider != null && delta.containsKey(WorkflowRunningConstants.Context.NEW_KNOWLEDGE_KEY)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> knowledgeData = (Map<String, Object>) delta
                        .get(WorkflowRunningConstants.Context.NEW_KNOWLEDGE_KEY);
                ragProvider.addKnowledge(
                        executionId,
                        (String) knowledgeData.get("content"),
                        (Map<String, Object>) knowledgeData.get("metadata"));
            } catch (Exception e) {
                log.warn("[{}] 保存知识失败: {}", executionId, e.getMessage());
            }
        }

        log.debug("[{}] 保存上下文变更: {} 个字段", executionId, delta.size());
    }

    // ========== 便捷方法:暴露子Provider能力 ==========

    public MemoryProvider memory() {
        return memoryProvider;
    }

    public RagProvider rag() {
        return ragProvider;
    }

    public PromptProvider prompt() {
        return promptProvider;
    }

    public McpProvider mcp() {
        return mcpProvider;
    }

    // ========== Prompt 构建 ==========

    /**
     * 构建完整的 Prompt
     * <p>
     * 整合以下内容：
     * 1. 系统提示词模板（含变量替换）
     * 2. 对话历史（Memory）
     * 3. RAG 相关知识
     * 4. State 中的其他变量
     *
     * @param executionId    执行 ID
     * @param promptTemplate 系统提示词模板
     * @param state          工作流状态
     * @return 完整的 Prompt
     */
    public String buildCompletePrompt(
            String executionId,
            String promptTemplate,
            WorkflowState state) {

        log.info("[{}] 开始构建完整 Prompt", executionId);
        StringBuilder prompt = new StringBuilder();

        // ==================== SYSTEM CONTEXT & GROUNDING LAYER ====================
        prompt.append("# [SYSTEM: CONTEXT & GROUNDING LAYER]\n");
        prompt.append(
                "(The following information is the objective runtime environment. You must execute based on this context.)\n\n");

        // 1. Runtime Environment
        prompt.append("## 1. 🕒 Runtime Environment\n");
        prompt.append(String.format("- **Current Time**: %s\n", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        prompt.append(String.format("- **Execution ID**: %s\n", executionId));
        prompt.append(String.format("- **Agent ID**: %s\n",
                state.get(WorkflowRunningConstants.Workflow.AGENT_ID_KEY, String.class)));

        // 添加全局变量
        appendWorkflowStateContext(prompt, state);
        prompt.append("\n");

        log.debug("[{}] 添加运行时环境信息", executionId);

        // 2. Retrieved Knowledge (RAG)
        boolean hasRAG = false;
        if (ragProvider != null && state.get(WorkflowRunningConstants.Context.USER_QUESTION_KEY) != null) {
            try {
                String query = state.get(WorkflowRunningConstants.Context.USER_QUESTION_KEY, String.class);
                List<KnowledgeChunk> knowledge = ragProvider.retrieveKnowledge(executionId, query, 5);
                if (knowledge != null && !knowledge.isEmpty()) {
                    prompt.append("## 2. 📚 Retrieved Knowledge (RAG)\n");
                    prompt.append("(Facts retrieved from the knowledge base. **High Priority**.)\n\n");
                    int idx = 1;
                    for (KnowledgeChunk chunk : knowledge) {
                        prompt.append(String.format("### Document %d\n%s\n\n", idx++, chunk.getContent()));
                    }
                    prompt.append(
                            "> **Instruction**: If the user's question is related to the content above, answer strictly based on these facts. ");
                    prompt.append("If contradictory, trust the RAG content over your internal knowledge.\n\n");
                    hasRAG = true;
                    log.debug("[{}] 检索到 {} 个RAG知识片段", executionId, knowledge.size());
                }
            } catch (Exception e) {
                log.warn("[{}] 检索知识失败: {}", executionId, e.getMessage());
            }
        }
        if (!hasRAG) {
            prompt.append("## 2. 📚 Retrieved Knowledge (RAG)\n");
            prompt.append("(No relevant documents retrieved from knowledge base.)\n\n");
        }

        // 3. Memory Context - Long-term
        prompt.append("## 3. 🧠 Memory Context\n");
        prompt.append("(Relevant information recalled from long-term memory or user profile.)\n\n");
        Object longTermMemory = state.get(WorkflowRunningConstants.Context.LONG_TERM_MEMORY_KEY);
        if (longTermMemory != null) {
            prompt.append(formatValue(longTermMemory));
            prompt.append("\n\n");
        } else {
            prompt.append("(No long-term memory available for this session.)\n\n");
        }

        // 4. Action Space (Available Tools)
        Object recommendedTools = state.get(WorkflowRunningConstants.Context.RECOMMENDED_TOOLS_KEY);
        if (recommendedTools != null) {
            prompt.append("## 4. 🛠️ Action Space (Available MCP Tools)\n");
            prompt.append("(You have the ability to call these tools if necessary.)\n\n");
            appendRecommendedTools(prompt, state);
            prompt.append("> **Instruction**: To use a tool, output the specific JSON format: ");
            prompt.append("{\"pendingToolCalls\": [{\"tool\": \"name\", \"params\": {...}}]}\n\n");
        } else {
            prompt.append("## 4. 🛠️ Action Space (Available MCP Tools)\n");
            prompt.append("(No tools available for this task.)\n\n");
        }

        // 5. Recent Execution History (Tool Results)
        Object toolResults = state.get(WorkflowRunningConstants.Context.TOOL_RESULTS_KEY);
        if (toolResults != null) {
            prompt.append("## 5. ⚡ Recent Execution History (Tool Outputs)\n");
            prompt.append("(Results from previous tool executions. These are established facts.)\n\n");
            appendToolResults(prompt, state);
            prompt.append("> **Instruction**: Use these results to answer the user's question or plan the next step. ");
            prompt.append("Do not hallucinate results if they are not listed here.\n\n");
        } else {
            prompt.append("## 5. ⚡ Recent Execution History (Tool Outputs)\n");
            prompt.append("(No tool executions in this session yet.)\n\n");
        }

        // 6. Conversation History (Short-term Memory)
        prompt.append("## 6. 💬 Conversation History (Short-term Memory)\n");
        boolean hasHistory = false;
        if (memoryProvider != null) {
            try {
                List<ChatMessage> chatHistory = memoryProvider.loadChatHistory(executionId, 10);
                if (chatHistory != null && !chatHistory.isEmpty()) {
                    for (ChatMessage msg : chatHistory) {
                        prompt.append(String.format("**%s**: %s\n",
                                msg.getRole().equals("user") ? "User" : "Assistant",
                                msg.getContent()));
                    }
                    prompt.append("\n");
                    hasHistory = true;
                    log.debug("[{}] 加载了 {} 条历史消息", executionId, chatHistory.size());
                }
            } catch (Exception e) {
                log.warn("[{}] 加载对话历史失败: {}", executionId, e.getMessage());
            }
        }
        if (!hasHistory) {
            prompt.append("(This is the beginning of the conversation.)\n\n");
        }

        // 7. Current User Query
        String userQuestion = state.get(WorkflowRunningConstants.Context.USER_QUESTION_KEY, String.class);
        if (userQuestion == null) {
            userQuestion = state.get(WorkflowRunningConstants.Prompt.USER_MESSAGE_KEY, String.class);
        }

        prompt.append("## 7. 🎯 Current User Query\n");
        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            prompt.append(userQuestion);
            prompt.append("\n\n");
            log.info("[{}] 用户问题: {}", executionId,
                    userQuestion.length() > 100 ? userQuestion.substring(0, 100) + "..." : userQuestion);
        } else {
            prompt.append("(No specific user query provided.)\n\n");
            log.warn("[{}] 警告: 未找到用户问题！State keys: {}", executionId, state.getAll().keySet());
        }

        // End of System Context
        prompt.append("---\n");
        prompt.append("[END OF SYSTEM CONTEXT]\n\n");

        // Role/Persona Instructions
        String systemPrompt = replaceVariables(promptTemplate, state);
        prompt.append("# Your Role\n");
        prompt.append(systemPrompt);
        prompt.append("\n\n");
        prompt.append(
                "> Now, strictly follow your role to process the \"Current User Query\" based on the context above.\n");

        log.debug("[{}] System Prompt 长度: {} 字符", executionId, systemPrompt.length());

        String finalPrompt = prompt.toString();
        log.info("[{}] 完整 Prompt 构建完成: {} 字符", executionId, finalPrompt.length());
        log.debug("[{}] 完整 Prompt 内容:\n{}", executionId,
                finalPrompt.length() > 500 ? finalPrompt.substring(0, 500) + "\n..." : finalPrompt);

        return finalPrompt;
    }

    /**
     * 追加 WorkflowState 中的重要上下文变量
     */
    private void appendWorkflowStateContext(StringBuilder prompt, WorkflowState state) {
        Map<String, Object> contextVars = new HashMap<>();

        // 检查常用的上下文字段
        String[] contextKeys = {
                WorkflowRunningConstants.ReAct.PLAN_STEPS_KEY,
                WorkflowRunningConstants.ReAct.EXECUTION_HISTORY_KEY,
                WorkflowRunningConstants.ReAct.CURRENT_TOOL_RESULT_KEY,
                WorkflowRunningConstants.Reflection.THOUGHT_HISTORY_KEY,
                WorkflowRunningConstants.Reflection.ACTION_HISTORY_KEY
        };

        for (String key : contextKeys) {
            Object value = state.get(key);
            if (value != null) {
                contextVars.put(key, value);
            }
        }

        if (!contextVars.isEmpty()) {
            prompt.append("## 工作流上下文\n");
            prompt.append("当前工作流的执行状态：\n\n");
            for (Map.Entry<String, Object> entry : contextVars.entrySet()) {
                String displayName = getDisplayName(entry.getKey());
                String value = formatValue(entry.getValue());
                if (value.length() > 200) {
                    value = value.substring(0, 200) + "...";
                }
                prompt.append(String.format("- **%s**: %s\n", displayName, value));
            }
            prompt.append("\n");
            log.debug("追加了 {} 个工作流上下文变量", contextVars.size());
        }
    }

    /**
     * 获取字段的可读显示名称
     */
    private String getDisplayName(String key) {
        switch (key) {
            case "planSteps":
                return "规划步骤";
            case "executionHistory":
                return "执行历史";
            case "currentToolResult":
                return "当前工具结果";
            case "thoughtHistory":
                return "思考历史";
            case "actionHistory":
                return "行动历史";
            default:
                return key;
        }
    }

    /**
     * 替换 Prompt 模板中的变量占位符
     * <p>
     * 支持语法: {@code {state.fieldName}}
     * <p>
     * 示例:
     * 
     * <pre>
     * 用户问题: {state.userQuestion}
     * 执行历史: {state.executionHistory}
     * </pre>
     *
     * @param template Prompt 模板
     * @param state    工作流状态
     * @return 替换后的 Prompt
     */
    private String replaceVariables(String template, WorkflowState state) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            Object value = state.get(fieldName);
            String replacement = formatValue(value);

            // 使用 Matcher.quoteReplacement 防止特殊字符导致错误
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

            log.debug("Replaced variable {state.{}} with value: {}", fieldName,
                    replacement.length() > 100 ? replacement.substring(0, 100) + "..." : replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 格式化值为字符串
     * <p>
     * 根据值的类型进行适当的格式化：
     * - List/Array: JSON 格式
     * - Map/Object: JSON 格式
     * - null: 空字符串
     * - 其他: toString()
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof List || value instanceof Map || value.getClass().isArray()) {
            try {
                return OBJECT_MAPPER.writeValueAsString(value);
            } catch (Exception e) {
                log.warn("Failed to format value as JSON: {}", e.getMessage());
                return value.toString();
            }
        }

        return value.toString();
    }

    /**
     * 构建上下文提示（用于工具推荐）
     */
    private Map<String, Object> buildContextHints(Map<String, Object> context) {
        Map<String, Object> hints = new HashMap<>();

        if (context.containsKey(WorkflowRunningConstants.Context.USER_QUESTION_KEY)) {
            hints.put(WorkflowRunningConstants.Context.USER_QUESTION_KEY,
                    context.get(WorkflowRunningConstants.Context.USER_QUESTION_KEY));
        }

        if (context.containsKey(WorkflowRunningConstants.Context.CHAT_HISTORY_KEY)) {
            hints.put(WorkflowRunningConstants.Context.RECENT_MESSAGES_KEY,
                    context.get(WorkflowRunningConstants.Context.CHAT_HISTORY_KEY));
        }

        return hints;
    }

    /**
     * 追加推荐的工具信息到 Prompt
     */
    private void appendRecommendedTools(StringBuilder prompt, WorkflowState state) {
        Object toolsObj = state.get(WorkflowRunningConstants.Context.RECOMMENDED_TOOLS_KEY);
        if (toolsObj == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<com.zj.aiagent.domain.toolbox.entity.ToolMetadata> tools = (List<com.zj.aiagent.domain.toolbox.entity.ToolMetadata>) toolsObj;

        if (!tools.isEmpty()) {
            prompt.append("\n\n## 可用工具\n");
            for (var tool : tools) {
                prompt.append(String.format(
                        "- **%s**: %s\n",
                        tool.getName(),
                        tool.getDescription()));
            }

            prompt.append("\n如需使用工具，请输出如下 JSON 格式：\n");
            prompt.append("```json\n");
            prompt.append("{\n");
            prompt.append("  \"pendingToolCalls\": [\n");
            prompt.append("    {\"tool\": \"tool_name\", \"params\": {...}}\n");
            prompt.append("  ]\n");
            prompt.append("}\n");
            prompt.append("```\n");
        }
    }

    /**
     * 追加工具执行结果到 Prompt
     */
    private void appendToolResults(StringBuilder prompt, WorkflowState state) {
        Object resultsObj = state.get(WorkflowRunningConstants.Context.TOOL_RESULTS_KEY);
        if (resultsObj == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<com.zj.aiagent.domain.toolbox.entity.ToolExecutionResult> results = (List<com.zj.aiagent.domain.toolbox.entity.ToolExecutionResult>) resultsObj;

        if (!results.isEmpty()) {
            prompt.append("\n\n## 工具执行结果\n");
            for (var result : results) {
                if (result.isSuccess()) {
                    prompt.append(String.format(
                            "- **%s**: %s\n",
                            result.getToolName(),
                            result.getResult()));
                } else {
                    prompt.append(String.format(
                            "- **%s**: [执行失败] %s\n",
                            result.getToolName(),
                            result.getErrorMessage()));
                }
            }
        }
    }
}
