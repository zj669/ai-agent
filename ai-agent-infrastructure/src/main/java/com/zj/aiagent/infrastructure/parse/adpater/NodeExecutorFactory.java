package com.zj.aiagent.infrastructure.parse.adpater;

import com.zj.aiagent.infrastructure.context.AgentContextProvider;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

/**
 * 节点执行器工厂
 * <p>
 * 根据节点类型创建对应的 NodeExecutor 实例
 */
@Slf4j
@Component
public class NodeExecutorFactory {

    private final AgentContextProvider contextProvider;

    public NodeExecutorFactory(AgentContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    /**
     * 创建节点执行器
     *
     * @param nodeType     节点类型
     * @param nodeId       节点 ID
     * @param nodeName     节点名称
     * @param description  节点描述
     * @param chatModel    AI 模型
     * @param systemPrompt 系统提示词
     * @return 节点执行器实例
     */
    public NodeExecutor createNodeExecutor(
            String nodeType,
            String nodeId,
            String nodeName,
            String description,
            OpenAiChatModel chatModel,
            String systemPrompt
            ) {

        log.debug("创建节点执行器: nodeId={}, nodeType={}", nodeId, nodeType);

        return new GenericLLMNode(nodeId, nodeName, description, nodeType, chatModel, systemPrompt, contextProvider,
                null);
    }
}
