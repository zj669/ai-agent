package com.zj.aiagemt.service;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.bo.ExecuteCommandEntity;
import com.zj.aiagemt.model.dto.AgentInfoDTO;
import com.zj.aiagemt.model.dto.AutoAgentRequestDTO;
import com.zj.aiagemt.model.entity.AiAgent;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.service.agent.IAgentRepository;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
import com.zj.aiagemt.service.agent.factory.IExecuteStrategy;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class AiAgentService {
    @Resource
    private DefaultAgentArmoryFactory defaultArmoryStrategyFactory;
    @Resource
    private IAgentRepository agentRepository;
    @Resource
    private SpringContextUtil springContextUtil;
    @Resource
    private DagLoaderService dagLoaderService;
    @Resource
    private DagExecutor dagExecutor;

    /**
     * AI智能体自动对话 - SSE流式返回
     *
     * @param request AutoAgent请求参数
     * @param emitter SSE发射器
     * @param userId  用户ID
     * @return ResponseBodyEmitter
     */
    public ResponseBodyEmitter autoAgent(AutoAgentRequestDTO request, ResponseBodyEmitter emitter, Long userId) {
        log.info("开始执行AutoAgent流式对话，aiAgentId: {}, sessionId: {}, userId: {}",
                request.getAiAgentId(), request.getSessionId(), userId);

        // 异步执行DAG流程，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 加载DAG配置
                DagGraph dagGraph = dagLoaderService.loadDagByAgentId(request.getAiAgentId());
                log.info("成功加载DAG，dagId: {}, 节点数: {}", dagGraph.getDagId(), dagGraph.getNodes().size());

                // 2. 创建DAG执行上下文
                DagExecutionContext context = new DagExecutionContext(request.getSessionId());
                context.setValue("userInput", request.getMessage());
                context.setValue("userMessage", request.getMessage());
                context.setValue("userId", userId);
                context.setEmitter(emitter); // 设置emitter到上下文，供节点使用

                // 3. 执行DAG
                DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);

                // 4. 发送最终结果
                if ("SUCCESS".equals(result.getStatus())) {
                    String finalMessage = buildSuccessMessage(result);
                    emitter.send(finalMessage);
                    log.info("DAG执行成功，sessionId: {}, 耗时: {}ms",
                            request.getSessionId(), result.getDurationMs());
                } else {
                    String errorMessage = buildErrorMessage(result);
                    emitter.send(errorMessage);
                    log.error("DAG执行失败，sessionId: {}, 原因: {}",
                            request.getSessionId(), result.getMessage());
                }

                // 5. 完成流式传输
                emitter.complete();

            } catch (Exception e) {
                log.error("AutoAgent执行异常，sessionId: {}", request.getSessionId(), e);
                try {
                    String errorMsg = buildExceptionMessage(e);
                    emitter.send(errorMsg);
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                }
            }
        }).exceptionally(throwable -> {
            log.error("异步执行AutoAgent失败", throwable);
            try {
                emitter.completeWithError(throwable);
            } catch (Exception e) {
                log.error("完成emitter失败", e);
            }
            return null;
        });

        return emitter;
    }

    /**
     * 构建成功消息
     */
    private String buildSuccessMessage(DagExecutor.DagExecutionResult result) {
        return String.format(
                "data: {\"type\":\"complete\",\"status\":\"success\",\"executionId\":\"%s\",\"duration\":%d}\n\n",
                result.getExecutionId(), result.getDurationMs());
    }

    /**
     * 构建错误消息
     */
    private String buildErrorMessage(DagExecutor.DagExecutionResult result) {
        return String.format(
                "data: {\"type\":\"error\",\"status\":\"failed\",\"message\":\"%s\",\"executionId\":\"%s\"}\n\n",
                escapeJson(result.getMessage()), result.getExecutionId());
    }

    /**
     * 构建异常消息
     */
    private String buildExceptionMessage(Exception e) {
        return String.format("data: {\"type\":\"error\",\"status\":\"exception\",\"message\":\"%s\"}\n\n",
                escapeJson(e.getMessage()));
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 查询用户的智能体列表
     */
    public List<AiAgent> queryAgentDtoList(Long userId) {
        return agentRepository.queryAgentDtoList(userId);
    }
}
