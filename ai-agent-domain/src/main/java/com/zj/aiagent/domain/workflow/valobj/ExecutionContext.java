package com.zj.aiagent.domain.workflow.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文值对象（智能黑板）
 * 承载工作流执行的全部上下文信息，包括：
 * - 基础数据：inputs, nodeOutputs, sharedState
 * - 长期记忆 (LTM)：启动时从向量库检索的系统级知识
 * - 短期记忆 (STM)：本次会话之前的对话历史
 * - 环境感知 (Awareness)：动态更新的执行日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    // ========== 原有字段 ==========

    /**
     * 全局输入参数
     */
    @Builder.Default
    private Map<String, Object> inputs = new ConcurrentHashMap<>();

    /**
     * 节点输出结果 (nodeId -> outputs)
     */
    @Builder.Default
    private Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    /**
     * 共享状态数据
     */
    @Builder.Default
    private Map<String, Object> sharedState = new ConcurrentHashMap<>();

    // ========== 长期记忆 (LTM) ==========

    /**
     * 长期记忆列表
     * 启动时根据用户意图从向量库检索，注入 Context，所有节点共享
     * 示例：用户偏好、过往类似任务经验等
     */
    @Builder.Default
    private List<String> longTermMemories = new ArrayList<>();

    // ========== 短期记忆 (STM) ==========

    /**
     * 会话历史消息
     * 启动时从 MySQL 加载，LLM 节点按需拼接
     * 格式：List of { role: USER/ASSISTANT, content: String }
     */
    @Builder.Default
    private List<Map<String, String>> chatHistory = new ArrayList<>();

    // ========== 环境感知 (Awareness) ==========

    /**
     * 执行日志
     * 动态更新的执行流水账，记录 "谁在什么时候做了什么"
     * 让 LLM 节点知道当前执行进度
     */
    @Builder.Default
    private StringBuilder executionLog = new StringBuilder();

    // --- 核心方法 ---

    /**
     * 设置全局输入
     */
    public void setInputs(Map<String, Object> inputs) {
        this.inputs = new ConcurrentHashMap<>(inputs);
    }

    /**
     * 存储节点输出
     */
    public void setNodeOutput(String nodeId, Map<String, Object> outputs) {
        this.nodeOutputs.put(nodeId, new HashMap<>(outputs));
    }

    /**
     * 获取节点输出
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        return nodeOutputs.getOrDefault(nodeId, new HashMap<>());
    }

    /**
     * 解析 SpEL 表达式
     * 支持:
     * - #{inputs.key}
     * - #{nodeId.output.key}
     * - #{sharedState.key}
     */
    public Object resolve(String expression) {
        if (expression == null || !expression.startsWith("#{") || !expression.endsWith("}")) {
            return expression; // 非表达式，直接返回
        }

        String spelExpression = expression.substring(2, expression.length() - 1);
        EvaluationContext context = buildEvaluationContext();
        Expression exp = PARSER.parseExpression(spelExpression);
        return exp.getValue(context);
    }

    /**
     * 批量解析输入参数
     */
    public Map<String, Object> resolveInputs(Map<String, Object> inputMappings) {
        Map<String, Object> resolved = new HashMap<>();

        if (inputMappings == null) {
            return resolved;
        }

        for (Map.Entry<String, Object> entry : inputMappings.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolve((String) value));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
    }

    /**
     * 构建 SpEL 评估上下文
     */
    private EvaluationContext buildEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 注册 inputs
        context.setVariable("inputs", inputs);

        // 注册 sharedState
        context.setVariable("sharedState", sharedState);

        // 注册所有节点输出
        for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        return context;
    }

    /**
     * 创建快照（用于检查点）
     */
    public ExecutionContext snapshot() {
        return ExecutionContext.builder()
                .inputs(new HashMap<>(this.inputs))
                .nodeOutputs(new HashMap<>(this.nodeOutputs))
                .sharedState(new HashMap<>(this.sharedState))
                .longTermMemories(new ArrayList<>(this.longTermMemories))
                .chatHistory(new ArrayList<>(this.chatHistory))
                .executionLog(new StringBuilder(this.executionLog.toString()))
                .build();
    }

    // ========== 环境感知方法 ==========

    /**
     * 追加执行日志
     * 用于记录节点执行摘要，让 LLM 节点知道当前进度
     * 
     * @param nodeId   节点ID
     * @param nodeName 节点名称
     * @param summary  执行摘要（如："完成意图识别，结果为 '查询天气'"）
     */
    public void appendLog(String nodeId, String nodeName, String summary) {
        this.executionLog.append(String.format("[%s-%s]: %s%n", nodeId, nodeName, summary));
    }

    /**
     * 获取执行日志内容
     */
    public String getExecutionLogContent() {
        return this.executionLog.toString();
    }

    /**
     * 清空执行日志
     */
    public void clearExecutionLog() {
        this.executionLog.setLength(0);
    }
}
