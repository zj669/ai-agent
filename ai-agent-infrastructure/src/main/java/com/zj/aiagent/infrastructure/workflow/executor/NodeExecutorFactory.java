package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器工厂
 * 使用 Registry 模式管理所有策略实现
 */
@Component
public class NodeExecutorFactory {

    private final Map<NodeType, NodeExecutorStrategy> strategyRegistry = new HashMap<>();

    /**
     * 构造器注入所有策略实现
     */
    public NodeExecutorFactory(List<NodeExecutorStrategy> strategies) {
        for (NodeExecutorStrategy strategy : strategies) {
            strategyRegistry.put(strategy.getSupportedType(), strategy);
        }
    }

    /**
     * 根据节点类型获取对应的执行策略
     * 
     * @param nodeType 节点类型
     * @return 执行策略
     * @throws IllegalArgumentException 如果找不到对应策略
     */
    public NodeExecutorStrategy getStrategy(NodeType nodeType) {
        NodeExecutorStrategy strategy = strategyRegistry.get(nodeType);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到节点类型 [" + nodeType + "] 的执行策略");
        }
        return strategy;
    }

    /**
     * 检查是否支持指定节点类型
     */
    public boolean supports(NodeType nodeType) {
        return strategyRegistry.containsKey(nodeType);
    }

    /**
     * 获取所有已注册的节点类型
     */
    public java.util.Set<NodeType> getRegisteredTypes() {
        return strategyRegistry.keySet();
    }
}
