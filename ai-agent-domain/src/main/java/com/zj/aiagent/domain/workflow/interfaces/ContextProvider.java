package com.zj.aiagent.domain.workflow.interfaces;

import java.util.concurrent.ConcurrentHashMap;

public interface ContextProvider {
    // 获取上下文数据 ConcurrentHashMap
    ConcurrentHashMap<String, Object> loadContext(String executionId, ConcurrentHashMap<String, Object> initialInput);
    
    // 保存执行结果
    void saveContext(String executionId, ConcurrentHashMap<String, Object> delta);
}