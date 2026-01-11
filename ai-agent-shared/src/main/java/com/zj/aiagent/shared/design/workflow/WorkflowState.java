package com.zj.aiagent.shared.design.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流状态
 * <p>
 * 借鉴 LangGraph 的统一 State 模型，所有节点共享同一个状态对象。
 * 节点执行时读取状态，返回状态更新，框架使用 Reducer 合并更新到 State。
 * <p>
 * 核心改进：
 * - 添加 Reducer 机制，定义状态合并策略（覆盖/追加/合并等）
 * - 类似 LangGraph 的 {@code Annotated[list, add]} 机制
 */
@Slf4j
@AllArgsConstructor
public class WorkflowState {

    private final ConcurrentHashMap<String, Object> data;

    // Reducer 注册表：定义每个 Key 的合并策略
    private final ConcurrentHashMap<String, StateReducer<?>> reducers;

    @Getter
    @Setter
    private WorkflowStateListener workflowStateListener;

    public WorkflowState(WorkflowStateListener workflowStateListener) {
        this.data = new ConcurrentHashMap<>();
        this.reducers = new ConcurrentHashMap<>();
        this.workflowStateListener = workflowStateListener;
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
     * 移除指定的键
     */
    public Object remove(String key) {
        return data.remove(key);
    }

    /**
     * 创建状态副本
     */
    /**
     * 创建状态副本
     * <p>
     * 注意：Reducer 注册表也会被复制
     */
    public WorkflowState copy() {
        ConcurrentHashMap<String, StateReducer<?>> copiedReducers = new ConcurrentHashMap<>(this.reducers);
        return new WorkflowState(new ConcurrentHashMap<>(this.data), copiedReducers, workflowStateListener);
    }

    /**
     * 应用状态更新
     * <p>
     * 将 StateUpdate 中的更新合并到当前状态。
     * 如果某个 Key 注册了 Reducer，则使用 Reducer 合并；否则默认覆盖。
     * <p>
     * 这是调度器流转状态的核心方法。
     *
     * @param update 状态更新
     * @return 当前状态（链式调用）
     */
    @SuppressWarnings("unchecked")
    public WorkflowState apply(StateUpdate update) {
        if (update == null || update.getUpdates() == null) {
            return this;
        }

        for (Map.Entry<String, Object> entry : update.getUpdates().entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();

            StateReducer<?> reducer = reducers.get(key);
            if (reducer != null) {
                // 有 Reducer：使用合并策略
                Object oldValue = data.get(key);
                try {
                    @SuppressWarnings("unchecked")
                    StateReducer<Object> typedReducer = (StateReducer<Object>) reducer;
                    Object mergedValue = typedReducer.reduce(oldValue, newValue);
                    data.put(key, mergedValue);
                    log.debug("Applied Reducer for key '{}': oldValue={}, newValue={}, merged={}",
                            key, oldValue, newValue, mergedValue);
                } catch (Exception e) {
                    log.warn("Reducer failed for key '{}', falling back to overwrite. Error: {}",
                            key, e.getMessage());
                    data.put(key, newValue);
                }
            } else {
                // 无 Reducer：默认覆盖
                data.put(key, newValue);
            }
        }

        return this;
    }

    /**
     * 注册 Reducer
     * <p>
     * 为指定的 State Key 注册合并策略
     *
     * @param key     状态 Key
     * @param reducer Reducer 实现
     * @param <T>     值类型
     */
    public <T> void registerReducer(String key, StateReducer<T> reducer) {
        if (key == null || reducer == null) {
            throw new IllegalArgumentException("Key and reducer cannot be null");
        }
        reducers.put(key, reducer);
        log.debug("Registered Reducer for key '{}'", key);
    }

    public void update(ConcurrentHashMap<String, Object> initialData) {
        this.data.putAll(initialData);
    }

}
