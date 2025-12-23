package com.zj.aiagent.domain.agent.dag.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * DAG 执行上下文快照
 * 用于暂停时保存完整执行状态，支持用户修改后恢复执行
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextSnapshot {

    // ==================== 只读字段 ====================

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 实例ID
     */
    private Long instanceId;

    /**
     * 暂停的节点ID
     */
    private String pausedNodeId;

    /**
     * 暂停的节点名称
     */
    private String pausedNodeName;

    /**
     * 暂停时间戳
     */
    private Long pausedAt;

    /**
     * 已执行节点ID列表
     */
    private Set<String> executedNodeIds;

    // ==================== 可编辑字段 ====================

    /**
     * 节点执行结果（可编辑）
     */
    private Map<String, Object> nodeResults;

    /**
     * 用户输入（可编辑）
     */
    private String userInput;

    /**
     * 自定义变量（可编辑）
     */
    private Map<String, Object> customVariables;

    /**
     * 对话历史（可编辑）
     */
    private List<ChatMessage> messageHistory;

    // ==================== 人工介入相关 ====================

    /**
     * 审核提示消息
     */
    private String checkMessage;

    /**
     * 是否允许修改输出
     */
    private Boolean allowModifyOutput;

    /**
     * 从 DagExecutionContext 创建快照
     *
     * @param context        执行上下文
     * @param pausedNodeId   暂停的节点ID
     * @param pausedNodeName 暂停的节点名称
     * @return 快照对象
     */
    public static ExecutionContextSnapshot fromContext(
            DagExecutionContext context,
            String pausedNodeId,
            String pausedNodeName) {

        // 提取自定义变量（排除系统内部使用的 key）
        Map<String, Object> customVars = extractCustomVariables(context);

        return ExecutionContextSnapshot.builder()
                .conversationId(context.getConversationId())
                .executionId(context.getExecutionId())
                .agentId(context.getAgentId())
                .instanceId(context.getInstanceId())
                .pausedNodeId(pausedNodeId)
                .pausedNodeName(pausedNodeName)
                .pausedAt(System.currentTimeMillis())
                .executedNodeIds(new HashSet<>(context.getAllNodeResults().keySet()))
                .nodeResults(new HashMap<>(context.getAllNodeResults()))
                .userInput(context.getEffectiveUserInput())
                .customVariables(customVars)
                .messageHistory(new ArrayList<>(context.getMessageHistory()))
                .build();
    }

    /**
     * 恢复到 DagExecutionContext
     *
     * @param context 目标执行上下文
     */
    public void restoreToContext(DagExecutionContext context) {
        // 恢复节点结果
        if (nodeResults != null) {
            for (Map.Entry<String, Object> entry : nodeResults.entrySet()) {
                context.setNodeResult(entry.getKey(), entry.getValue());
            }
        }

        // 恢复用户输入
        if (userInput != null) {
            context.setUserInput(userInput);
        }

        // 恢复自定义变量
        if (customVariables != null) {
            for (Map.Entry<String, Object> entry : customVariables.entrySet()) {
                context.setValue(entry.getKey(), entry.getValue());
            }
        }

        // 恢复消息历史
        if (messageHistory != null) {
            for (ChatMessage msg : messageHistory) {
                context.addMessage(msg);
            }
        }
    }

    /**
     * 提取自定义变量（排除系统内部 key）
     */
    private static Map<String, Object> extractCustomVariables(DagExecutionContext context) {
        Map<String, Object> customVars = new HashMap<>();

        // 获取 dataMap 中的所有变量
        Map<String, Object> dataMap = context.getDataMap();

        // 过滤掉系统内部使用的 key
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = entry.getKey();

            // 排除系统内部 key：
            // 1. 以 "__" 开头的（如 __DAG_GRAPH__、__PROGRESS_COMPLETED__）
            // 2. 以 "_node_log_" 开头的
            // 3. ContextKey 中定义的系统 key
            if (!isSystemKey(key)) {
                customVars.put(key, entry.getValue());
            }
        }

        return customVars;
    }

    /**
     * 判断是否为系统内部 key
     */
    private static boolean isSystemKey(String key) {
        // 系统内部 key 以 "__" 开头
        if (key.startsWith("__")) {
            return true;
        }

        // 节点日志前缀
        if (key.startsWith("_node_log_")) {
            return true;
        }

        // 检查是否为 ContextKey 中定义的系统 key
        // 这些是除了用户自定义数据外的所有系统使用的 key
        return key.equals("dag_completed")
                || key.equals("execution_history")
                || key.equals("previous_execution_result");
    }

    /**
     * 可编辑字段元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditableFieldMeta {
        private String key;
        private String label;
        private String type;
        private String description;
        private Boolean editable;
    }

    /**
     * 获取可编辑字段清单（基于实际快照数据动态生成）
     */
    public List<EditableFieldMeta> getEditableFields() {
        List<EditableFieldMeta> fields = new ArrayList<>();

        // 节点结果 - 如果有执行过的节点则显示
        if (nodeResults != null && !nodeResults.isEmpty()) {
            fields.add(EditableFieldMeta.builder()
                    .key("nodeResults")
                    .label("节点结果")
                    .type("json")
                    .description("各节点的执行结果（共 " + nodeResults.size() + " 个节点）")
                    .editable(true)
                    .build());
        }

        // 用户输入 - 始终显示（即使为空也允许编辑）
        fields.add(EditableFieldMeta.builder()
                .key("userInput")
                .label("用户输入")
                .type("text")
                .description(userInput != null && !userInput.isEmpty()
                        ? "用户输入的内容（" + userInput.length() + " 字符）"
                        : "用户输入的内容（当前为空）")
                .editable(true)
                .build());

        // 自定义变量 - 如果有自定义变量则显示
        if (customVariables != null && !customVariables.isEmpty()) {
            fields.add(EditableFieldMeta.builder()
                    .key("customVariables")
                    .label("自定义变量")
                    .type("json")
                    .description("执行过程中的自定义变量（共 " + customVariables.size() + " 个变量）")
                    .editable(true)
                    .build());
        }

        // 消息历史 - 如果有历史消息则显示
        if (messageHistory != null && !messageHistory.isEmpty()) {
            fields.add(EditableFieldMeta.builder()
                    .key("messageHistory")
                    .label("消息历史")
                    .type("messages")
                    .description("对话历史记录（共 " + messageHistory.size() + " 条消息）")
                    .editable(true)
                    .build());
        }

        return fields;
    }
}
