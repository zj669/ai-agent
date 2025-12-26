package com.zj.aiagent.shared.design.workflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置 Reducer 实现
 * <p>
 * 提供常用的状态合并策略，类似 LangGraph 的内置 Reducer
 */
public class BuiltInReducers {

    private BuiltInReducers() {
        // 工具类，禁止实例化
    }

    /**
     * 覆盖策略（默认）
     * <p>
     * 总是使用新值覆盖旧值
     *
     * @param <T> 值类型
     * @return 覆盖 Reducer
     */
    public static <T> StateReducer<T> overwrite() {
        return (oldValue, newValue) -> newValue;
    }

    /**
     * 列表追加策略
     * <p>
     * 将新列表中的元素追加到旧列表末尾
     * <p>
     * 类似 LangGraph 的 {@code Annotated[list, operator.add]}
     *
     * @param <T> 列表元素类型
     * @return 列表追加 Reducer
     */
    public static <T> StateReducer<List<T>> appendList() {
        return (oldList, newList) -> {
            if (newList == null) {
                return oldList;
            }
            if (oldList == null) {
                return new ArrayList<>(newList);
            }
            List<T> result = new ArrayList<>(oldList);
            result.addAll(newList);
            return result;
        };
    }

    /**
     * 消息合并策略
     * <p>
     * 按消息 ID 合并：
     * - 如果消息 ID 已存在，更新为新消息
     * - 如果消息 ID 不存在，追加新消息
     * <p>
     * 类似 LangGraph 的 {@code add_messages}
     *
     * @param <T> 消息类型（需要有 getId() 方法）
     * @return 消息合并 Reducer
     */
    public static <T> StateReducer<List<T>> addMessages() {
        return (oldMessages, newMessages) -> {
            if (newMessages == null) {
                return oldMessages;
            }
            if (oldMessages == null) {
                return new ArrayList<>(newMessages);
            }

            // 使用 LinkedHashMap 保持插入顺序
            Map<String, T> messageById = new LinkedHashMap<>();

            // 先添加旧消息
            for (T msg : oldMessages) {
                String id = extractMessageId(msg);
                if (id != null) {
                    messageById.put(id, msg);
                }
            }

            // 再添加/覆盖新消息
            for (T msg : newMessages) {
                String id = extractMessageId(msg);
                if (id != null) {
                    messageById.put(id, msg);
                }
            }

            return new ArrayList<>(messageById.values());
        };
    }

    /**
     * 计数器累加策略
     * <p>
     * 将新值加到旧值上
     *
     * @return 计数器累加 Reducer
     */
    public static StateReducer<Integer> increment() {
        return (oldValue, newValue) -> {
            if (newValue == null) {
                return oldValue;
            }
            if (oldValue == null) {
                return newValue;
            }
            return oldValue + newValue;
        };
    }

    /**
     * Map 合并策略
     * <p>
     * 将新 Map 的键值对合并到旧 Map 中（新值覆盖同名键）
     *
     * @param <K> Map 键类型
     * @param <V> Map 值类型
     * @return Map 合并 Reducer
     */
    public static <K, V> StateReducer<Map<K, V>> mergeMap() {
        return (oldMap, newMap) -> {
            if (newMap == null) {
                return oldMap;
            }
            if (oldMap == null) {
                return new LinkedHashMap<>(newMap);
            }
            Map<K, V> result = new LinkedHashMap<>(oldMap);
            result.putAll(newMap);
            return result;
        };
    }

    /**
     * 从消息对象中提取 ID
     * <p>
     * 尝试调用 getId() 方法（通过反射）
     */
    private static String extractMessageId(Object message) {
        if (message == null) {
            return null;
        }

        try {
            // 尝试调用 getId() 方法
            var method = message.getClass().getMethod("getId");
            Object id = method.invoke(message);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            // 如果没有 getId() 方法，使用对象哈希码
            return String.valueOf(message.hashCode());
        }
    }
}
