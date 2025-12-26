package com.zj.aiagent.infrastructure.workflow;

import com.zj.aiagent.domain.workflow.interfaces.ContextProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
@Component
public class AgentContextProvider implements ContextProvider {
    @Override
    public ConcurrentHashMap<String, Object> loadContext(String executionId, ConcurrentHashMap<String, Object> initialInput) {
        return initialInput;
    }

    @Override
    public void saveContext(String executionId, ConcurrentHashMap<String, Object> delta) {

    }
}
