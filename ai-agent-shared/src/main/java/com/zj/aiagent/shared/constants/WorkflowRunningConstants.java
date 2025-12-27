package com.zj.aiagent.shared.constants;

public class WorkflowRunningConstants {
    private WorkflowRunningConstants() {
        // 禁止实例化
    }

    /**
     * 上下文 Context Keys
     */
    public static class Context {
        /** 对话历史 */
        public static final String CHAT_HISTORY_KEY = "chatHistory";

        /** 用户问题 */
        public static final String USER_QUESTION_KEY = "userQuestion";

        /** 相关知识（RAG） */
        public static final String RELEVANT_KNOWLEDGE_KEY = "relevantKnowledge";

        /** MCP 客户端列表 */
        public static final String MCP_CLIENTS_KEY = "mcpClients";

        /** 推荐的工具列表 */
        public static final String RECOMMENDED_TOOLS_KEY = "recommendedTools";

        /** 待执行的工具调用 */
        public static final String PENDING_TOOL_CALLS_KEY = "pendingToolCalls";

        /** 工具执行结果 */
        public static final String TOOL_RESULTS_KEY = "toolResults";

        /** 用户消息 */
        public static final String USER_MESSAGE_KEY = "userMessage";

        /** 助手消息 */
        public static final String ASSISTANT_MESSAGE_KEY = "assistantMessage";

        /** 长期记忆 */
        public static final String LONG_TERM_MEMORY_KEY = "longTermMemory";

        /** 新知识 */
        public static final String NEW_KNOWLEDGE_KEY = "newKnowledge";

        /** 最近消息（用于工具推荐） */
        public static final String RECENT_MESSAGES_KEY = "recentMessages";
    }

    public static class Memory {
    }

    public static class Prompt {
        public static final String USER_MESSAGE_KEY = "userMessage";
    }

    public static class Mcp {
        public static final String READY_EXECUTE_MCP_KEY = "readyExecuteMcp";
    }

    public static class Workflow {
        public static final String EXECUTION_ID_KEY = "executionId";

        /**
         * 用户 ID
         */
        public static final String USER_ID_KEY = "userId";

        /**
         * Agent ID
         */
        public static final String AGENT_ID_KEY = "agentId";

        /**
         * AI 响应内容的 key
         */
        public static final String AI_RESPONSE_KEY = "aiResponse";

        /**
         * 最后节点输出的 key
         */
        public static final String LAST_NODE_OUTPUT_KEY = "lastNodeOutput";

        /**
         * 节点执行时间戳的 key
         */
        public static final String NODE_EXECUTION_TIMESTAMP_KEY = "nodeExecutionTimestamp";
    }

    /**
     * 节点类型常量
     */
    public static class NodeType {
        /**
         * 标准 AI 对话节点
         */
        public static final String AI_CHAT = "AI_CHAT";

        /**
         * 反思评价节点
         */
        public static final String REFLECTION = "REFLECTION";

        /**
         * 总结节点
         */
        public static final String SUMMARY = "SUMMARY";

        /**
         * 路由节点
         */
        public static final String ROUTER = "ROUTER";

        /**
         * 函数调用节点
         */
        public static final String FUNCTION_CALL = "FUNCTION_CALL";
    }

    /**
     * 反思循环相关常量
     */
    public static class Reflection {
        // 循环控制
        public static final String LOOP_COUNT_KEY = "loopCount";
        public static final String MAX_LOOPS_KEY = "maxLoops";

        // 反思结果
        public static final String REFLECTION_RESULT_KEY = "reflectionResult";
        public static final String IS_GOAL_ACHIEVED_KEY = "isGoalAchieved";
        public static final String CAN_CONTINUE_KEY = "canContinue";
        public static final String REFLECTION_SCORE_KEY = "reflectionScore";

        // 累积历史
        public static final String THOUGHT_HISTORY_KEY = "thoughtHistory";
        public static final String ACTION_HISTORY_KEY = "actionHistory";
    }

    /**
     * ReAct 模式相关常量
     */
    public static class ReAct {
        // ==================== 输入 ====================
        /** 用户原始问题 */
        public static final String USER_QUESTION_KEY = "userQuestion";

        // ==================== Plan 节点输出 ====================
        /** 当前规划的步骤列表 (JSON Array) */
        public static final String PLAN_STEPS_KEY = "planSteps";

        /** 当前执行到第几步 (从 0 开始) */
        public static final String CURRENT_STEP_INDEX_KEY = "currentStepIndex";

        // ==================== Act 节点输出 ====================
        /** 执行历史列表 (JSON Array) */
        public static final String EXECUTION_HISTORY_KEY = "executionHistory";

        /** 当前步骤的工具调用结果 */
        public static final String CURRENT_TOOL_RESULT_KEY = "currentToolResult";

        // ==================== Summary 节点输出 ====================
        /** 是否有足够信息回答问题 */
        public static final String HAS_ENOUGH_INFO_KEY = "hasEnoughInfo";

        /** 反思结果 (为什么信息不足) */
        public static final String REFLECTION_REASON_KEY = "reflectionReason";

        /** 最终答案 (信息充足时生成) */
        public static final String FINAL_ANSWER_KEY = "finalAnswer";

        // ==================== 循环控制 ====================
        /** ReAct 循环次数 */
        public static final String REACT_LOOP_COUNT_KEY = "reactLoopCount";

        /** 最大循环次数 */
        public static final String MAX_REACT_LOOPS_KEY = "maxReactLoops";
    }
}
