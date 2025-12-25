package com.zj.aiagent.domain.workflow.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流状态
 * 
 * 借鉴 LangGraph 的统一 State 模型，所有节点共享同一个状态对象
 * 节点执行时读取状态，返回状态更新
 */
public class WorkflowState {

    private final ConcurrentHashMap<String, Object> data;

    public WorkflowState() {
        this.data = new ConcurrentHashMap<>();
    }

    public WorkflowState(Map<String, Object> initialData) {
        this.data = new ConcurrentHashMap<>(initialData);
    }

    /**
     * 获取状态值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(data.get(key));
    }

    /**
     * 获取状态值，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return data.containsKey(key) ? (T) data.get(key) : defaultValue;
    }

    /**
     * 设置状态值
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 批量更新状态
     */
    public void putAll(Map<String, Object> updates) {
        if (updates != null) {
            data.putAll(updates);
        }
    }

    /**
     * 获取所有状态
     */
    public ConcurrentHashMap<String, Object> getAll() {
        return new ConcurrentHashMap<>(data);
    }

    /**
     * 检查是否包含某个键
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * 创建状态副本
     */
    public WorkflowState copy() {
        return new WorkflowState(this.data);
    }

    /**
     * 应用状态更新
     * 
     * 将 StateUpdate 中的更新合并到当前状态
     * 这是调度器流转状态的核心方法
     * 
     * @param update 状态更新
     * @return 当前状态（链式调用）
     */
    public WorkflowState apply(StateUpdate update) {
        if (update != null && update.getUpdates() != null) {
            this.data.putAll(update.getUpdates());
        }
        return this;
    }

    public void update(ConcurrentHashMap<String, Object> initialData){
        this.data.putAll(initialData);
    }
}
