package com.zj.aiagent.interfaces.web.controller.client;

import com.zj.aiagent.application.dag.AgentApplicationService;
import com.zj.aiagent.application.dag.command.ChatCommand;
import com.zj.aiagent.interfaces.web.dto.request.agent.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

/**
 * Agent 控制器
 *
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@RestController
@RequestMapping("/client/agent")
@Tag(name = "Agent 管理", description = "Agent 聊天等接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.OPTIONS
})
public class AgentController {

    @Resource
    private AgentApplicationService agentApplicationService;

    /**
     * 与 Agent 进行流式聊天
     * 
     * @param request 聊天请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与 Agent 进行流式聊天")
    public ResponseBodyEmitter chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求, agentId: {}, conversationId: {}, message: {}",
                request.getAgentId(), request.getConversationId(), request.getUserMessage());

        // 创建 ResponseBodyEmitter 用于流式响应
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        try {
            // 构建命令
            ChatCommand command = ChatCommand.builder()
                    .agentId(request.getAgentId())
                    .userMessage(request.getUserMessage())
                    .conversationId(request.getConversationId())
                    .emitter(emitter)
                    .build();

            // 异步执行聊天，避免阻塞请求线程
            new Thread(() -> {
                try {
                    agentApplicationService.chat(command);
                    emitter.complete();
                } catch (Exception e) {
                    log.error("聊天处理异常", e);
                    try {
                        emitter.send("error: " + e.getMessage());
                        emitter.completeWithError(e);
                    } catch (IOException ioException) {
                        log.error("发送错误信息失败", ioException);
                    }
                }
            }).start();

        } catch (Exception e) {
            log.error("创建聊天会话异常", e);
            try {
                emitter.send("error: " + e.getMessage());
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("发送错误信息失败", ioException);
            }
        }

        return emitter;
    }
}
