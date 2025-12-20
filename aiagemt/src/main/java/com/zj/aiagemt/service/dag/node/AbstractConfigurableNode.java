package com.zj.aiagemt.service.dag.node;

import com.alibaba.fastjson.JSON;
import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.common.design.dag.DagNodeExecutionException;
import com.zj.aiagemt.model.bo.AutoAgentExecuteResultEntity;
import com.zj.aiagemt.service.dag.config.*;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.exception.NodeConfigException;
import com.zj.aiagemt.service.dag.model.NodeType;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 可配置节点抽象基类
 * 提供节点配置管理和ChatClient构建能力
 */
@Slf4j
public abstract class AbstractConfigurableNode implements DagNode<DagExecutionContext, String> {

    protected final String nodeId;
    protected final String nodeName;
    protected final NodeConfig config;
    protected final ApplicationContext applicationContext;

    protected AbstractConfigurableNode(String nodeId, String nodeName, NodeConfig config,
            ApplicationContext applicationContext) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.config = config;
        this.applicationContext = applicationContext;

        if (config == null) {
            throw new NodeConfigException("NodeConfig cannot be null for node: " + nodeId);
        }
        if (config.getSystemPrompt() == null || config.getSystemPrompt().isEmpty()) {
            throw new NodeConfigException("SystemPrompt is required for node: " + nodeId);
        }
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void beforeExecute(DagExecutionContext context) {
        log.info("节点 [{}] ({}) 开始执行", nodeName, nodeId);
    }

    @Override
    public void afterExecute(DagExecutionContext context, String result, Exception exception) {
        if (exception != null) {
            log.error("节点 [{}] ({}) 执行失败", nodeName, nodeId, exception);
        } else {
            log.info("节点 [{}] ({}) 执行成功", nodeName, nodeId);
        }
    }

    @Override
    public long getTimeoutMillis() {
        return config.getTimeout() != null ? config.getTimeout() : 0;
    }

    /**
     * 构建配置化的ChatClient
     */
    protected ChatClient buildChatClient(DagExecutionContext context) {
        try {
            // 1. 构建ChatModel
            OpenAiChatModel chatModel = buildChatModel();

            // 2. 构建MCP工具
            List<McpSyncClient> mcpClients = buildMcpClients();

            // 3. 构建Advisors
            List<Advisor> advisors = buildAdvisors();

            // 4. 组装ChatClient
            ChatClient.Builder builder = ChatClient.builder(chatModel)
                    .defaultSystem(config.getSystemPrompt());

            // 获取用户提示词
            if (config.getUserPrompt() != null) {
                builder.defaultUser(config.getUserPrompt());
            }

            if (!mcpClients.isEmpty()) {
                builder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients.toArray(new McpSyncClient[0])));
            }

            if (!advisors.isEmpty()) {
                builder.defaultAdvisors(advisors.toArray(new Advisor[0]));
            }
            return builder.build();

        } catch (Exception e) {
            throw new NodeConfigException("Failed to build ChatClient for node: " + nodeId, e);
        }
    }

    /**
     * 构建ChatModel
     */
    private OpenAiChatModel buildChatModel() {
        ModelConfig modelConfig = config.getModel();
        if (modelConfig == null) {
            throw new NodeConfigException("Model configuration is required for node: " + nodeId);
        }

        // 构建OpenAiApi
        WebClient.Builder webClientBuilder = applicationContext.getBean("webClientBuilder1", WebClient.Builder.class);
        RestClient.Builder restClientBuilder = applicationContext.getBean("restClientBuilder1",
                RestClient.Builder.class);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .webClientBuilder(webClientBuilder)
                .restClientBuilder(restClientBuilder)
                .build();

        // 构建ChatOptions
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelConfig.getModelName());

        if (modelConfig.getTemperature() != null) {
            optionsBuilder.temperature(modelConfig.getTemperature());
        }
        if (modelConfig.getMaxTokens() != null) {
            optionsBuilder.maxTokens(modelConfig.getMaxTokens());
        }
        if (modelConfig.getTopP() != null) {
            optionsBuilder.topP(modelConfig.getTopP());
        }
        if (modelConfig.getFrequencyPenalty() != null) {
            optionsBuilder.frequencyPenalty(modelConfig.getFrequencyPenalty());
        }
        if (modelConfig.getPresencePenalty() != null) {
            optionsBuilder.presencePenalty(modelConfig.getPresencePenalty());
        }

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    /**
     * 构建MCP客户端列表
     */
    private List<McpSyncClient> buildMcpClients() {
        List<McpSyncClient> mcpClients = new ArrayList<>();
        if (config.getMcpTools() != null) {
            for (McpToolConfig mcpConfig : config.getMcpTools()) {
                try {
                    String beanName = "ai_client_tool_mcp_" + mcpConfig.getMcpId();
                    McpSyncClient mcpClient = applicationContext.getBean(beanName, McpSyncClient.class);
                    mcpClients.add(mcpClient);
                } catch (Exception e) {
                    log.warn("Failed to load MCP tool: {}", mcpConfig.getMcpId(), e);
                }
            }
        }
        return mcpClients;
    }

    /**
     * 构建Advisor列表
     */
    private List<Advisor> buildAdvisors() {
        List<Advisor> advisors = new ArrayList<>();
        if (config.getAdvisors() != null) {
            for (AdvisorConfig advisorConfig : config.getAdvisors()) {
                try {
                    String beanName = "ai_client_advisor_" + advisorConfig.getAdvisorId();
                    Advisor advisor = applicationContext.getBean(beanName, Advisor.class);
                    advisors.add(advisor);
                } catch (Exception e) {
                    log.warn("Failed to load advisor: {}", advisorConfig.getAdvisorId(), e);
                }
            }
        }
        return advisors;
    }

    /**
     * 执行AI调用
     */
    protected String callAI(String userMessage, DagExecutionContext context) {
        ChatClient chatClient = buildChatClient(context);

        ChatClient.ChatClientRequestSpec spec = chatClient.prompt(userMessage);

        // // 处理记忆配置
        // if (config.getMemory() != null &&
        // Boolean.TRUE.equals(config.getMemory().getEnabled())) {
        // String conversationId =
        // resolveConversationId(config.getMemory().getConversationId(), context);
        // Integer retrieveSize = config.getMemory().getRetrieveSize() != null ?
        // config.getMemory().getRetrieveSize()
        // : 10;
        //
        // spec = spec.advisors(a -> a
        // .param("chat_memory_conversation_id", conversationId)
        // .param("chat_memory_response_size", retrieveSize));
        // }
        // todo
        String conversationId = context.getConversationId();
        spec = spec.advisors(a -> a
                .param("chat_memory_conversation_id", conversationId)
                .param("chat_memory_response_size", 10));
        return spec.call().content();
    }

    /**
     * 解析会话ID（支持占位符）
     */
    private String resolveConversationId(String conversationIdTemplate, DagExecutionContext context) {
        if (conversationIdTemplate == null) {
            return context.getConversationId();
        }
        return conversationIdTemplate.replace("${conversationId}", context.getConversationId());
    }

    /**
     * 子类实现具体执行逻辑
     */
    protected abstract String doExecute(DagExecutionContext context) throws DagNodeExecutionException;

    @Override
    public String execute(DagExecutionContext context) throws DagNodeExecutionException {
        return doExecute(context);
    }

    public void pushMessage(String message, DagExecutionContext context) {
        ResponseBodyEmitter emitter = context.getEmitter();
        String conversationId = context.getConversationId();
        if (emitter == null) {
            log.warn("Emitter is null, message will not be pushed to client");
            return;
        }
        try {
            emitter.send(buildMessage(message, conversationId));
        } catch (Exception e) {
            log.error("Failed to push message to client", e);
        }
    }

    private String buildMessage(String message, String conversationId) {
        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.builder()
                .type(getNodeType().getLabel())
                .nodeName(getNodeName())
                .content(message)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(conversationId)
                .build();
        return "data: " + JSON.toJSONString(result) + "\n\n";
    }

    public abstract NodeType getNodeType();
}
