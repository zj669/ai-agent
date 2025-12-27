package com.zj.aiagent.domain.context;

import java.util.concurrent.ConcurrentHashMap;

public interface IContextProvider {
    ConcurrentHashMap<String, Object> loadContext(
            String executionId,
            ConcurrentHashMap<String, Object> initialInput);

    void saveContext(
            String executionId,
            ConcurrentHashMap<String, Object> delta);
}
