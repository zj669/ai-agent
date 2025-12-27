package com.zj.aiagent.shared.design.workflow;

/**
 * 工作流状态监听器
 * <p>
 * 用于监听工作流执行过程中的各种状态变化事件，
 * 支持SSE等方式向客户端推送实时状态。
 */
public interface WorkflowStateListener {

    // ==================== 工作流级别事件 ====================

    /**
     * 工作流开始执行
     * 
     * @param totalNodes 总节点数，用于进度计算
     */
    void onWorkflowStarted(int totalNodes);

    /**
     * 工作流执行完成
     * 
     * @param success true表示成功完成，false表示失败
     */
    void onWorkflowCompleted(boolean success);

    /**
     * 工作流执行失败
     * 
     * @param t 异常信息
     */
    void onWorkflowFailed(Throwable t);

    // ==================== 节点级别事件 ====================

    /**
     * 节点开始执行
     * 
     * @param nodeId   节点ID
     * @param nodeName 节点名称
     */
    void onNodeStarted(String nodeId, String nodeName);

    /**
     * 节点产生流式内容
     * <p>
     * 用于节点内部的思考过程、日志等中间输出
     * 
     * @param nodeId       节点ID
     * @param nodeName     节点名称（前端需要用于匹配节点）
     * @param contentChunk 内容块
     */
    void onNodeStreaming(String nodeId, String nodeName, String contentChunk);

    /**
     * 节点执行完成
     * 
     * @param nodeId     节点ID
     * @param nodeName   节点名称
     * @param result     执行结果
     * @param durationMs 执行耗时（毫秒）
     */
    void onNodeCompleted(String nodeId, String nodeName, WorkflowState result, long durationMs);

    /**
     * 节点执行失败
     * 
     * @param nodeId     节点ID
     * @param nodeName   节点名称
     * @param error      错误信息
     * @param durationMs 执行耗时（毫秒）
     */
    void onNodeFailed(String nodeId, String nodeName, String error, long durationMs);

    /**
     * 节点暂停等待人工介入
     * 
     * @param nodeId   节点ID
     * @param nodeName 节点名称
     * @param message  暂停原因/提示信息
     */
    void onNodePaused(String nodeId, String nodeName, String message);

    // ==================== 用户交互事件 ====================

    /**
     * 推送最终答案给用户
     * <p>
     * 用于流式输出用户可见的回复内容
     * 
     * @param contentChunk 回复内容块
     */
    void onFinalAnswer(String contentChunk);
}