package com.zj.aiagent.domain.workflow.base;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态更新
 * 
 * 节点执行后返回的状态更新，包含：
 * 1. updates: 要更新的状态字段
 * 2. signal: 控制信号（继续/暂停/结束）
 */
@Data
public class StateUpdate {

    private final Map<String, Object> updates;
    private final ControlSignal signal;
    private final String message;

    private StateUpdate(Map<String, Object> updates, ControlSignal signal, String message) {
        this.updates = updates != null ? updates : new HashMap<>();
        this.signal = signal;
        this.message = message;
    }

    /**
     * 创建正常的状态更新（继续执行）
     */
    public static StateUpdate of(Map<String, Object> updates) {
        return new StateUpdate(updates, ControlSignal.CONTINUE, null);
    }

    /**
     * 创建单个字段的状态更新
     */
    public static StateUpdate of(String key, Object value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(key, value);
        return new StateUpdate(updates, ControlSignal.CONTINUE, null);
    }

    /**
     * 创建暂停信号（等待人工介入）
     */
    public static StateUpdate pause(String reason) {
        return new StateUpdate(null, ControlSignal.PAUSE, reason);
    }

    /**
     * 创建暂停信号并更新状态
     */
    public static StateUpdate pause(Map<String, Object> updates, String reason) {
        return new StateUpdate(updates, ControlSignal.PAUSE, reason);
    }

    /**
     * 创建结束信号
     */
    public static StateUpdate end() {
        return new StateUpdate(null, ControlSignal.END, null);
    }

    /**
     * 创建结束信号并更新最终状态
     */
    public static StateUpdate end(Map<String, Object> finalUpdates) {
        return new StateUpdate(finalUpdates, ControlSignal.END, null);
    }

    /**
     * 创建错误信号
     */
    public static StateUpdate error(String errorMessage) {
        return new StateUpdate(null, ControlSignal.ERROR, errorMessage);
    }
}
