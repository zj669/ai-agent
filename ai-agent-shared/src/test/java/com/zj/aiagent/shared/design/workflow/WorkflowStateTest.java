package com.zj.aiagent.shared.design.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowState 和 StateReducer 单元测试
 */
class WorkflowStateTest {

    private WorkflowState state;

    @BeforeEach
    void setUp() {
        state = new WorkflowState(null);
    }

    @Test
    void testOverwriteReducer() {
        // 注册覆盖 Reducer
        state.registerReducer("count", BuiltInReducers.overwrite());

        // 第一次更新
        StateUpdate update1 = StateUpdate.of(Map.of("count", 10));
        state.apply(update1);
        assertEquals(10, state.get("count"));

        // 第二次更新：覆盖
        StateUpdate update2 = StateUpdate.of(Map.of("count", 20));
        state.apply(update2);
        assertEquals(20, state.get("count"));
    }

    @Test
    void testAppendListReducer() {
        // 注册列表追加 Reducer
        state.registerReducer("messages", BuiltInReducers.appendList());

        // 第一次更新
        StateUpdate update1 = StateUpdate.of(Map.of("messages", List.of("msg1", "msg2")));
        state.apply(update1);

        @SuppressWarnings("unchecked")
        List<String> messages = state.get("messages");
        assertEquals(2, messages.size());
        assertEquals("msg1", messages.get(0));

        // 第二次更新：追加
        StateUpdate update2 = StateUpdate.of(Map.of("messages", List.of("msg3")));
        state.apply(update2);

        @SuppressWarnings("unchecked")
        List<String> updatedMessages = state.get("messages");
        assertEquals(3, updatedMessages.size());
        assertEquals("msg3", updatedMessages.get(2));
    }

    @Test
    void testIncrementReducer() {
        // 注册计数器累加 Reducer
        state.registerReducer("loopCount", BuiltInReducers.increment());

        // 第一次更新
        StateUpdate update1 = StateUpdate.of(Map.of("loopCount", 1));
        state.apply(update1);
        assertEquals(1, state.get("loopCount"));

        // 第二次更新：累加
        StateUpdate update2 = StateUpdate.of(Map.of("loopCount", 1));
        state.apply(update2);
        assertEquals(2, state.get("loopCount"));

        // 第三次更新：累加
        StateUpdate update3 = StateUpdate.of(Map.of("loopCount", 3));
        state.apply(update3);
        assertEquals(5, state.get("loopCount"));
    }

    @Test
    void testMergeMapReducer() {
        // 注册 Map 合并 Reducer
        state.registerReducer("config", BuiltInReducers.mergeMap());

        // 第一次更新
        Map<String, Object> config1 = new HashMap<>();
        config1.put("key1", "value1");
        config1.put("key2", "value2");
        StateUpdate update1 = StateUpdate.of(Map.of("config", config1));
        state.apply(update1);

        @SuppressWarnings("unchecked")
        Map<String, Object> currentConfig = state.get("config");
        assertEquals(2, currentConfig.size());

        // 第二次更新：合并（覆盖 key1，新增 key3）
        Map<String, Object> config2 = new HashMap<>();
        config2.put("key1", "newValue1");
        config2.put("key3", "value3");
        StateUpdate update2 = StateUpdate.of(Map.of("config", config2));
        state.apply(update2);

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedConfig = state.get("config");
        assertEquals(3, mergedConfig.size());
        assertEquals("newValue1", mergedConfig.get("key1"));
        assertEquals("value2", mergedConfig.get("key2"));
        assertEquals("value3", mergedConfig.get("key3"));
    }

    @Test
    void testDefaultOverwriteBehavior() {
        // 未注册 Reducer，默认覆盖
        StateUpdate update1 = StateUpdate.of(Map.of("data", "value1"));
        state.apply(update1);
        assertEquals("value1", state.get("data"));

        StateUpdate update2 = StateUpdate.of(Map.of("data", "value2"));
        state.apply(update2);
        assertEquals("value2", state.get("data"));
    }

    @Test
    void testMixedReducers() {
        // 混合使用多个 Reducer
        state.registerReducer("count", BuiltInReducers.increment());
        state.registerReducer("messages", BuiltInReducers.appendList());

        // 同时更新多个字段
        Map<String, Object> updates = new HashMap<>();
        updates.put("count", 1);
        updates.put("messages", List.of("msg1"));
        updates.put("name", "test"); // 无 Reducer，默认覆盖

        state.apply(StateUpdate.of(updates));

        assertEquals(1, state.get("count"));
        assertEquals(1, ((List<?>) state.get("messages")).size());
        assertEquals("test", state.get("name"));

        // 第二次更新
        Map<String, Object> updates2 = new HashMap<>();
        updates2.put("count", 2);
        updates2.put("messages", List.of("msg2"));
        updates2.put("name", "test2");

        state.apply(StateUpdate.of(updates2));

        assertEquals(3, state.get("count")); // 累加
        assertEquals(2, ((List<?>) state.get("messages")).size()); // 追加
        assertEquals("test2", state.get("name")); // 覆盖
    }
}
