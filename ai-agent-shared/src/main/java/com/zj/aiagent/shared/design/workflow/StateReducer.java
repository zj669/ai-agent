package com.zj.aiagent.shared.design.workflow;

/**
 * 状态合并策略接口
 * <p>
 * 借鉴 LangGraph 的 Reducer 机制，定义如何将节点返回的新值合并到 State 中。
 * <p>
 * 示例：
 * 
 * <pre>
 * // 覆盖策略（默认）
 * StateReducer&lt;String&gt; overwrite = (old, newVal) -> newVal;
 * 
 * // 列表追加策略
 * StateReducer&lt;List&lt;String&gt;&gt; append = (old, newVal) -> {
 *     List&lt;String&gt; result = new ArrayList&lt;&gt;(old);
 *     result.addAll(newVal);
 *     return result;
 * };
 * </pre>
 *
 * @param <T> 状态值的类型
 */
@FunctionalInterface
public interface StateReducer<T> {

    /**
     * 合并旧值和新值
     *
     * @param oldValue 状态中的旧值（可能为 null）
     * @param newValue 节点返回的新值（可能为 null）
     * @return 合并后的值
     */
    T reduce(T oldValue, T newValue);
}
