package com.zj.aiagent.interfaces.web.controller.client;

import cn.hutool.core.util.IdUtil;
import com.zj.aiagent.application.chat.ICharApplicationService;
import com.zj.aiagent.application.chat.command.ChatCommand;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.chat.ReviewRequest;
import com.zj.aiagent.interfaces.web.dto.request.chat.ChatRequest;
import com.zj.aiagent.interfaces.web.dto.response.chat.ChatHistoryResponse;
import com.zj.aiagent.shared.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/client/chat")
@Tag(name = "聊天管理", description = "聊天管理")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.OPTIONS
})
public class ChatController {
    @Resource
    private ICharApplicationService charApplicationService;

    @PostMapping()
    public ResponseBodyEmitter autoAgent(@RequestBody ChatRequest request, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
        ChatCommand command = ChatCommand.builder()
                .agentId(request.getAgentId())
                .userMessage(request.getUserMessage())
                .conversationId(request.getConversationId())
                .emitter(emitter)
                .build();
        charApplicationService.chat(command);
        return emitter;
    }

    @GetMapping("newChat")
    @Operation(summary = "发起流式问答前生成会话ID")
    public Response<String> newChat() {
        return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
    }

    @GetMapping("/conversations/{agentId}")
    @Operation(summary = "历史会话ID")
    public Response<List<String>> oldChat(@PathVariable("agentId") String agentId) {
        Long userId = UserContext.getUserId();
        return Response.success(charApplicationService.queryHistoryId(userId, agentId));
    }

    @GetMapping("/history/{agentId}/{conversationId}")
    @Operation(summary = "聊天历史消息")
    public Response<List<ChatHistoryResponse>> chatHistory(@PathVariable("agentId") String agentId,
            @PathVariable("conversationId") String conversationId) {
        Long userId = UserContext.getUserId();
        // charApplicationService.queryHistory(userId, agentId, conversationId)
        return Response.success(null);
    }

    @PostMapping("/review")
    @Operation(summary = "人工审核")
    public ResponseBodyEmitter review(@RequestBody ReviewRequest request, HttpServletResponse response) {
        // 设置 SSE 响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
        Long userId = UserContext.getUserId();

        charApplicationService.review(
                userId,
                request.getConversationId(),
                request.getNodeId(),
                request.getApproved(),
                request.getAgentId(),
                emitter);

        return emitter;
    }

    @GetMapping("/snapshot/{agentId}/{conversationId}")
    @Operation(summary = "获取会话快照")
    public Response<String> snapshot(@PathVariable("agentId") String agentId,
            @PathVariable("conversationId") String conversationId) {
        Long userId = UserContext.getUserId();
        // return Response.success(charApplicationService.snapshot(userId, agentId,
        // conversationId));
        return Response.success();
    }

    @PostMapping("/snapshot/{agentId}/{conversationId}")
    @Operation(summary = "更新会话快照")
    public Response<String> updateSnapshot(@PathVariable("agentId") String agentId,
            @PathVariable("conversationId") String conversationId,
            @RequestBody String snapshot) {
        Long userId = UserContext.getUserId();
        // return Response.success(charApplicationService.snapshot(userId, agentId,
        // conversationId));
        return Response.success();
    }
}
