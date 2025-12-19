package com.zj.aiagemt.common.design.dag;

import java.util.Map;

/**
 * DAG执行上下文 - 在DAG执行过程中传递数据
 */
public interface DagContext {

    /**
     * 存储键值对数据
     * 
     * @param key   键
     * @param value 值
     */
    <T> void setValue(String key, T value);

    /**
     * 获取数据
     * 
     * @param key 键
     * @return 值
     */
    <T> T getValue(String key);

    /**
     * 获取数据，如果不存在返回默认值
     * 
     * @param key          键
     * @param defaultValue 默认值
     * @return 值或默认值
     */
    <T> T getValue(String key, T defaultValue);

    /**
     * 存储节点执行结果
     * 
     * @param nodeId 节点ID
     * @param result 执行结果
     */
    <R> void setNodeResult(String nodeId, R result);

    /**
     * 获取节点执行结果
     * 
     * @param nodeId 节点ID
     * @return 执行结果
     */
    <R> R getNodeResult(String nodeId);

    /**
     * 获取所有节点执行结果
     * 
     * @return 节点ID -> 执行结果的映射
     */
    Map<String, Object> getAllNodeResults();

    /**
     * 判断节点是否已执行
     * 
     * @param nodeId 节点ID
     * @return true表示已执行
     */
    boolean isNodeExecuted(String nodeId);

    /**
     * 获取执行ID
     */
    String getExecutionId();

    /**
     * 获取会话ID
     */
    String getConversationId();
}
