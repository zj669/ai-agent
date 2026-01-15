package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;

/**
 * 流式推送端口接口
 * 用于节点执行过程中的实时事件推送
 * 
 * 实现类位于 Infrastructure 层
 */
public interface StreamPublisher {

    /**
     * 推送节点开始事件
     */
    void publishStart();

    /**
     * 推送增量文本（打字机效果）
     * 
     * @param delta 增量内容
     */
    void publishDelta(String delta);

    /**
     * 推送思考过程
     * 
     * @param thought 思考内容
     */
    void publishThought(String thought);

    /**
     * 推送节点完成事件
     * 
     * @param result 执行结果
     */
    void publishFinish(NodeExecutionResult result);

    /**
     * 推送错误信息
     * 
     * @param errorMessage 错误消息
     */
    void publishError(String errorMessage);

    /**
     * 推送结构化数据
     * 
     * @param data       数据对象
     * @param renderMode 渲染模式（JSON/TABLE 等）
     */
    void publishData(Object data, String renderMode);

    /**
     * 推送自定义事件
     * 
     * @param eventType 事件类型
     * @param payload   事件载荷
     */
    void publishEvent(String eventType, java.util.Map<String, Object> payload);
}
