package com.zj.aiagent.interfaces.web.controller.client;

import com.zj.aiagent.application.agent.AgentApplicationService;
import com.zj.aiagent.application.agent.command.ChatCommand;
import com.zj.aiagent.application.agent.command.SaveAgentCommand;
import com.zj.aiagent.application.agent.query.GetUserAgentsQuery;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.agent.ChatRequest;
import com.zj.aiagent.interfaces.web.dto.request.agent.SaveAgentRequest;
import com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse;
import com.zj.aiagent.interfaces.web.dto.response.agent.SaveAgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.List;

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
     * 保存Agent配置
     *
     * @param request 保存请求
     * @return 保存响应
     */
    @PostMapping("/save")
    @Operation(summary = "保存Agent配置", description = "创建或更新Agent配置，支持拖拉拽配置保存")
    public Response<SaveAgentResponse> saveAgent(@Valid @RequestBody SaveAgentRequest request) {
        try {
            // 获取当前用户ID
            Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
            if (userId == null) {
                return Response.unauthorized("未登录");
            }

            log.info("保存Agent配置请求, userId: {}, agentId: {}, agentName: {}",
                    userId, request.getAgentId(), request.getAgentName());

            // 构建Command
            SaveAgentCommand command = SaveAgentCommand.builder()
                    .userId(userId)
                    .agentId(request.getAgentId())
                    .agentName(request.getAgentName())
                    .description(request.getDescription())
                    .graphJson(request.getGraphJson())
                    .status(request.getStatus())
                    .build();

            // 保存Agent
            String agentId = agentApplicationService.saveAgent(command);

            // 构建响应
            SaveAgentResponse response = SaveAgentResponse.builder()
                    .agentId(agentId)
                    .status(request.getStatus() != null ? request.getStatus() : 0)
                    .message("保存成功")
                    .build();

            return Response.success(response);

        } catch (RuntimeException e) {
            log.error("保存Agent配置失败", e);
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("保存Agent配置异常", e);
            return Response.fail("保存失败: " + e.getMessage());
        }
    }

    /**
     * 与 Agent 进行流式聊天
     * 
     * @param request 聊天请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与 Agent 进行流式聊天")
    public ResponseBodyEmitter chat(@Valid @RequestBody ChatRequest request, HttpServletResponse response) {
        log.info("收到聊天请求, agentId: {}, conversationId: {}, message: {}",
                request.getAgentId(), request.getConversationId(), request.getUserMessage());
        // 设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        // 创建 ResponseBodyEmitter 用于流式响应
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);

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
                    // 尝试完成响应,如果已经完成则会抛出 IllegalStateException
                    try {
                        emitter.complete();
                    } catch (IllegalStateException e) {
                        // emitter 已经完成,忽略此异常
                        log.debug("Emitter 已经完成,无需再次调用 complete");
                    }
                } catch (Exception e) {
                    log.error("聊天处理异常", e);
                    // 尝试发送错误信息,如果 emitter 已完成则忽略
                    try {
                        emitter.send("error: " + e.getMessage());
                        emitter.completeWithError(e);
                    } catch (IllegalStateException | IOException ex) {
                        // emitter 已经完成或发送失败,记录但不再抛出
                        log.warn("无法发送错误信息,emitter 可能已完成: {}", ex.getMessage());
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

    /**
     * 查询当前用户的 Agent 列表
     *
     * @return Agent 列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询当前用户的 Agent 列表")
    public Response<List<AgentResponse>> getUserAgents() {
        try {
            // 从 UserContext 获取当前用户 ID
            Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
            if (userId == null) {
                return Response.unauthorized("未登录");
            }

            log.info("查询用户 Agent 列表, userId: {}", userId);

            // 构建查询对象
            GetUserAgentsQuery query = GetUserAgentsQuery
                    .builder()
                    .userId(userId)
                    .build();

            // 查询 Agent 列表
            java.util.List<AgentApplicationService.AgentDTO> agentDTOList = agentApplicationService
                    .getUserAgents(query);

            // 转换为 Response
            java.util.List<com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse> responseList = agentDTOList
                    .stream()
                    .map(dto -> com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse.builder()
                            .id(dto.getId())
                            .agentName(dto.getAgentName())
                            .description(dto.getDescription())
                            .status(dto.getStatus())
                            .statusDesc(dto.getStatusDesc())
                            .createTime(dto.getCreateTime())
                            .updateTime(dto.getUpdateTime())
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            return com.zj.aiagent.interfaces.common.Response.success(responseList);

        } catch (Exception e) {
            log.error("查询用户 Agent 列表失败", e);
            return com.zj.aiagent.interfaces.common.Response.fail("查询失败: " + e.getMessage());
        }
    }
}
