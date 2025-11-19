package com.zj.aiagemt.service;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.bo.ExecuteCommandEntity;
import com.zj.aiagemt.model.dto.AutoAgentRequestDTO;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
import com.zj.aiagemt.service.agent.factory.IExecuteStrategy;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class AiAgentService {
    @Resource(name = "autoAgentExecuteStrategy")
    private IExecuteStrategy autoAgentExecuteStrategy;
    @Resource
    private DefaultAgentArmoryFactory defaultArmoryStrategyFactory;
    @Resource
    private SpringContextUtil springContextUtil;


    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    public ResponseBodyEmitter autoAgent(AutoAgentRequestDTO request, ResponseBodyEmitter emitter) {
        try {
            ExecuteCommandEntity executeCommandEntity = ExecuteCommandEntity.builder()
                    .aiAgentId(request.getAiAgentId())
                    .userMessage(request.getMessage())
                    .sessionId(request.getSessionId())
                    .maxStep(request.getMaxStep())
                    .build();

            // 3. 异步执行AutoAgent
            threadPoolExecutor.execute(() -> {
                try {
                    autoAgentExecuteStrategy.execute(executeCommandEntity, emitter);
                } catch (Exception e) {
                    log.error("AutoAgent执行异常：{}", e.getMessage(), e);
                    try {
                        emitter.send("执行异常：" + e.getMessage());
                    } catch (Exception ex) {
                        log.error("发送异常信息失败：{}", ex.getMessage(), ex);
                    }
                } finally {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("完成流式输出失败：{}", e.getMessage(), e);
                    }
                }
            });

            return emitter;

        } catch (Exception e) {
            log.error("AutoAgent请求处理异常：{}", e.getMessage(), e);
            ResponseBodyEmitter errorEmitter = new ResponseBodyEmitter();
            try {
                errorEmitter.send("请求处理异常：" + e.getMessage());
                errorEmitter.complete();
            } catch (Exception ex) {
                log.error("发送错误信息失败：{}", ex.getMessage(), ex);
            }
            return errorEmitter;
        }
    }


    public void reload(List<String> clientId) throws Exception {
        StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> armoryStrategyHandler =
                defaultArmoryStrategyFactory.strategyHandler();

        AgentArmoryVO result = armoryStrategyHandler.apply(
                ArmoryCommandEntity.builder()
                        .commandType(AiAgentEnumVO.AI_CLIENT.getLoadDataStrategy())
                        .commandIdList(clientId)
                        .build(),
                new DefaultAgentArmoryFactory.DynamicContext());
    }

    public Flux<String> modelChat(AutoAgentRequestDTO request) {
        String aiAgentId = request.getAiAgentId();
        ChatClient chatClient = springContextUtil.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(aiAgentId));
        return chatClient.prompt()
                .messages(new UserMessage(request.getMessage()))
                .stream().content();
    }
}
