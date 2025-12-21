//package com.zj.aiagent.domain.agent.armory.node;
//
//
//import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
//import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
//import com.zj.aiagemt.model.enums.AiAgentEnumVO;
//import com.zj.aiagemt.model.vo.AiClientToolMcpVO;
//import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
//import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
//import io.modelcontextprotocol.client.McpClient;
//import io.modelcontextprotocol.client.McpSyncClient;
//import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
//import io.modelcontextprotocol.client.transport.ServerParameters;
//import io.modelcontextprotocol.client.transport.StdioClientTransport;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.stereotype.Component;
//
//import java.net.http.HttpRequest;
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//
//@Component
//@Slf4j
//public class McpNode extends AgentAromorSupport {
//    @Resource
//    private AdvisorNode advisorNode;
//    @Override
//    protected String beanName(String beanId) {
//        return AiAgentEnumVO.AI_TOOL_MCP.getBeanName(beanId);
//    }
//
//    @Override
//    protected String dataName() {
//        return AiAgentEnumVO.AI_TOOL_MCP.getDataName();
//    }
//
//    @Override
//    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) throws Exception {
//        log.info("Ai Agent 构建节点，客户端,McpNode");
//        List<AiClientToolMcpVO> value = context.getValue(dataName());
//        for (AiClientToolMcpVO mcpVO : value) {
//            // 创建 MCP 服务
//            McpSyncClient mcpSyncClient = createMcpSyncClient(mcpVO);
//
//            // 注册 MCP 对象
//            registerBean(beanName(mcpVO.getMcpId()), McpSyncClient.class, mcpSyncClient);
//        }
//        return router(requestParams, context);
//    }
//
//    @Override
//    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
//        return advisorNode;
//    }
//
//private McpSyncClient createMcpSyncClient(AiClientToolMcpVO aiClientToolMcpVO) {
//    String transportType = aiClientToolMcpVO.getTransportType();
//
//    switch (transportType) {
//        case "sse" -> {
//            AiClientToolMcpVO.TransportConfigSse transportConfigSse = aiClientToolMcpVO.getTransportConfigSse();
//            String originalBaseUri = transportConfigSse.getBaseUri();
//            String baseUri;
//            String sseEndpoint;
//
//            int queryParamStartIndex = originalBaseUri.indexOf("sse");
//            if (queryParamStartIndex != -1) {
//                baseUri = originalBaseUri.substring(0, queryParamStartIndex - 1);
//                sseEndpoint = originalBaseUri.substring(queryParamStartIndex - 1);
//            } else {
//                baseUri = originalBaseUri;
//                sseEndpoint = transportConfigSse.getSseEndpoint();
//            }
//
//            sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;
//            Consumer<HttpRequest.Builder> requestCustomizer = null;
//            if(transportConfigSse.getHeaders() != null){
//                requestCustomizer = builder ->
//                        builder.header(HttpHeaders.AUTHORIZATION, transportConfigSse.getHeaders())
//                                .header(HttpHeaders.ACCEPT, "application/json");
//            }
//            HttpClientSseClientTransport.Builder ssedEndpointBuilder = HttpClientSseClientTransport
//                    .builder(baseUri) // 使用截取后的 baseUri
//                    .sseEndpoint(sseEndpoint);
//            if(requestCustomizer != null){
//                ssedEndpointBuilder.customizeRequest(requestCustomizer);
//            }
//            HttpClientSseClientTransport sseClientTransport = ssedEndpointBuilder.build();
//            McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(aiClientToolMcpVO.getRequestTimeout())).build();
//            var init_sse = mcpSyncClient.initialize();
//
//            log.info("Tool SSE MCP Initialized {}", init_sse);
//            return mcpSyncClient;
//        }
//        case "stdio" -> {
//            AiClientToolMcpVO.TransportConfigStdio transportConfigStdio = aiClientToolMcpVO.getTransportConfigStdio();
//            Map<String, AiClientToolMcpVO.TransportConfigStdio.Stdio> stdioMap = transportConfigStdio.getStdio();
//            AiClientToolMcpVO.TransportConfigStdio.Stdio stdio = stdioMap.get(aiClientToolMcpVO.getMcpName());
//
//            // https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
//            var stdioParams = ServerParameters.builder(stdio.getCommand())
//                    .args(stdio.getArgs())
//                    .env(stdio.getEnv())
//                    .build();
//            var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
//                    .requestTimeout(Duration.ofSeconds(aiClientToolMcpVO.getRequestTimeout())).build();
//            var init_stdio = mcpClient.initialize();
//
//            log.info("Tool Stdio MCP Initialized {}", init_stdio);
//            return mcpClient;
//        }
//    }
//
//    throw new RuntimeException("err! transportType " + transportType + " not exist!");
//}
//}
