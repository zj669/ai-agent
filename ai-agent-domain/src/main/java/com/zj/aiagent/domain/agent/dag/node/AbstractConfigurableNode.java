package com.zj.aiagent.domain.agent.dag.node;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.agent.dag.config.AdvisorConfig;
import com.zj.aiagent.domain.agent.dag.config.McpToolConfig;
import com.zj.aiagent.domain.agent.dag.config.ModelConfig;
import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.AutoAgentExecuteResultEntity;
import com.zj.aiagent.domain.agent.dag.entity.NodeExecutionLog;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.shared.design.dag.DagNode;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

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

        try {
            // 创建节点执行日志
            NodeExecutionLog executionLog = NodeExecutionLog.builder()
                    .instanceId(context.getInstanceId())
                    .agentId(context.getAgentId())
                    .conversationId(context.getConversationId())
                    .nodeId(nodeId)
                    .nodeType(getNodeType() != null ? getNodeType().name() : "UNKNOWN")
                    .nodeName(nodeName)
                    .build();

            // 记录输入数据
            // 如果是第一个节点(没有前序结果),记录用户输入;否则记录前序节点结果
            String inputJson;
            Map<String, Object> nodeResults = context.getAllNodeResults();
            if (nodeResults == null || nodeResults.isEmpty()) {
                // 第一个节点 - 记录用户输入
                String userInput = context.getValue("userInput", "");
                Map<String, Object> inputData = new HashMap<>();
                inputData.put("userInput", userInput);
                inputJson = JSON.toJSONString(inputData);
            } else {
                // 后续节点 - 记录前序节点结果
                inputJson = JSON.toJSONString(nodeResults);
            }
            executionLog.start(inputJson);

            // 保存日志并存入context
            IDagExecutionRepository repository = getExecutionRepository();
            if (repository != null) {
                executionLog = repository.saveNodeLog(executionLog);
                context.setValue("_node_log_" + nodeId, executionLog);
            }
        } catch (Exception e) {
            log.warn("保存节点执行日志失败，不影响执行", e);
        }
    }

    @Override
    public void afterExecute(DagExecutionContext context, String result, Exception exception) {
        // 获取执行日志
        NodeExecutionLog executionLog = (NodeExecutionLog) context.getValue("_node_log_" + nodeId);

        if (exception != null) {
            log.error("节点 [{}] ({}) 执行失败", nodeName, nodeId, exception);

            // 更新日志为失败
            if (executionLog != null) {
                try {
                    executionLog.fail(exception.getMessage(), getStackTrace(exception));
                    IDagExecutionRepository repository = getExecutionRepository();
                    if (repository != null) {
                        repository.updateNodeLog(executionLog);
                    }
                } catch (Exception e) {
                    log.warn("更新节点执行日志失败，不影响执行", e);
                }
            }
        } else {
            log.info("节点 [{}] ({}) 执行成功", nodeName, nodeId);

            // 更新日志为成功
            if (executionLog != null) {
                try {
                    String outputJson = result != null ? JSON.toJSONString(result) : "";
                    executionLog.succeed(outputJson);
                    IDagExecutionRepository repository = getExecutionRepository();
                    if (repository != null) {
                        repository.updateNodeLog(executionLog);
                    }
                } catch (Exception e) {
                    log.warn("更新节点执行日志失败，不影响执行", e);
                }
            }
        }
    }

    @Override
    public long getTimeoutMillis() {
        return config.getTimeout() != null ? config.getTimeout() : 0;
    }

    /**
     * 获取仓储
     */
    private IDagExecutionRepository getExecutionRepository() {
        try {
            return applicationContext.getBean(IDagExecutionRepository.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取堆栈跟踪
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        // 截断堆栈,避免太长
        if (stackTrace.length() > 2000) {
            stackTrace = stackTrace.substring(0, 2000) + "...";
        }
        return stackTrace;
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
     * 支持流式推送到客户端
     */
    protected String callAI(String userMessage, DagExecutionContext context) throws DagNodeExecutionException {
        ChatClient chatClient = buildChatClient(context);
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt(userMessage);

        // 配置会话记忆
        String conversationId = context.getConversationId();
        spec = spec.advisors(a -> a
                .param("chat_memory_conversation_id", conversationId)
                .param("chat_memory_response_size", 10));

        // 检查是否有 emitter,决定使用同步还是流式
        ResponseBodyEmitter emitter = context.getEmitter();
        if (emitter != null) {
            // 使用流式调用
            return callAIStream(spec, context);
        } else {
            // 同步调用
            return spec.call().content();
        }
    }

    /**
     * 流式调用 AI 并推送到客户端
     */
    private String callAIStream(ChatClient.ChatClientRequestSpec spec, DagExecutionContext context)
            throws DagNodeExecutionException {
        StringBuilder fullResponse = new StringBuilder();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

        try {
            reactor.core.publisher.Flux<String> stream = spec.stream().content();

            stream.subscribe(
                    chunk -> {
                        // 推送每个 chunk
                        pushMessage(chunk, context);
                        fullResponse.append(chunk);
                    },
                    error -> {
                        log.error("流式调用失败", error);
                        errorRef.set((Exception) error);
                        latch.countDown();
                    },
                    latch::countDown);

            // 等待流完成,最多等待5分钟
            boolean completed = latch.await(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!completed) {
                throw new DagNodeExecutionException("流式调用超时", null, nodeId, true);
            }

            // 检查是否有错误
            Exception error = errorRef.get();
            if (error != null) {
                throw new DagNodeExecutionException("流式调用失败: " + error.getMessage(), error, nodeId, true);
            }

            return fullResponse.toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DagNodeExecutionException("流式调用被中断", e, nodeId, true);
        }
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
        // 获取节点类型,如果为 null 则使用默认值
        NodeType nodeType = getNodeType();
        String typeLabel = (nodeType != null) ? nodeType.getLabel() : "UNKNOWN";

        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.builder()
                .type("node_execute")
                .nodeName(typeLabel)
                .content(message)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(conversationId)
                .build();
        return "data: " + JSON.toJSONString(result) + "\n\n";
    }

    public abstract NodeType getNodeType();
}
