package com.zj.aiagent.interfaces.web.controller.client;

import cn.hutool.core.util.IdUtil;
import com.zj.aiagent.application.agent.AgentApplicationService;
import com.zj.aiagent.application.agent.command.ChatCommand;
import com.zj.aiagent.application.agent.command.PublishAgentCommand;
import com.zj.aiagent.application.agent.command.SaveAgentCommand;
import com.zj.aiagent.application.agent.query.GetUserAgentsQuery;
import com.zj.aiagent.domain.agent.chat.entity.ChatMessageEntity;
import com.zj.aiagent.domain.agent.dag.executor.DagExecutor;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.common.annotation.Idempotent;
import com.zj.aiagent.interfaces.web.dto.request.agent.ChatRequest;
import com.zj.aiagent.interfaces.web.dto.request.agent.SaveAgentRequest;
import com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse;
import com.zj.aiagent.interfaces.web.dto.response.agent.ChatHistoryResponse;
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
import java.util.stream.Collectors;

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

    @Resource
    private com.zj.aiagent.application.agent.ChatMessageApplicationService chatMessageApplicationService;

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

    @GetMapping("newChat")
    @Operation(summary = "发起流式问答前生成会话ID")
    public Response<String> newChat() {
        return Response.<String>builder()
                .code("0000")
                .info("成功")
                .data(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()))
                .build();
    }

    /**
     * 与 Agent 进行流式聊天
     * 
     * @param request 聊天请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与 Agent 进行流式聊天")
    @Idempotent()
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
                String conversationId = command.getConversationId();
                Long instanceId = null;

                try {
                    // 1. 生成会话ID
                    if (conversationId == null || conversationId.isEmpty()) {
                        conversationId = String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
                        command.setConversationId(conversationId);
                    }

                    // 2. 保存用户消息
                    Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
                    chatMessageApplicationService.saveUserMessage(
                            conversationId,
                            Long.valueOf(command.getAgentId()),
                            userId,
                            command.getUserMessage());

                    // 3. 执行聊天并获取结果
                    DagExecutor.DagExecutionResult result = agentApplicationService.chat(command);

                    // 4. 获取 instanceId
                    if (result != null && result.getInstanceId() != null) {
                        instanceId = result.getInstanceId();
                    }

                    // 5. 保存 AI 回复（成功）
                    chatMessageApplicationService.saveAssistantMessage(
                            conversationId,
                            Long.valueOf(command.getAgentId()),
                            instanceId,
                            null,
                            false,
                            null);

                    // 尝试完成响应,如果已经完成则会抛出 IllegalStateException
                    try {
                        emitter.complete();
                    } catch (IllegalStateException e) {
                        // emitter 已经完成,忽略此异常
                        log.debug("Emitter 已经完成,无需再次调用 complete");
                    }
                } catch (Exception e) {
                    log.error("聊天处理异常", e);

                    // 保存 AI 回复（失败）
                    try {
                        chatMessageApplicationService.saveAssistantMessage(
                                conversationId,
                                Long.valueOf(command.getAgentId()),
                                instanceId,
                                null,
                                true,
                                e.getMessage());
                    } catch (Exception saveEx) {
                        log.error("保存错误消息失败", saveEx);
                    }

                    // 尝试发送错误信息,如果 emitter 已完成则忽略
                    try {
                        String errorMsg = buildErrorEvent(
                                "CHAT_EXECUTION_FAILED",
                                "EXECUTION_ERROR",
                                e.getMessage(),
                                command.getConversationId());
                        emitter.send(errorMsg);
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
                String errorMsg = buildErrorEvent(
                        "CHAT_SESSION_CREATION_FAILED",
                        "INITIALIZATION_ERROR",
                        e.getMessage(),
                        null);
                emitter.send(errorMsg);
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("发送错误信息失败", ioException);
            }
        }

        return emitter;
    }

    /**
     * 人工介入审核
     */
    @PostMapping("/review")
    @Operation(summary = "人工介入审核", description = "审核暂停的节点，批准或拒绝继续执行")
    public Response<Void> reviewHumanIntervention(
            @Valid @RequestBody com.zj.aiagent.interfaces.web.dto.request.agent.ReviewRequest request) {
        try {
            log.info("收到人工介入审核请求: conversationId={}, nodeId={}, approved={}",
                    request.getConversationId(), request.getNodeId(), request.getApproved());

            // 转换为 Command
            com.zj.aiagent.application.agent.command.ReviewCommand command = com.zj.aiagent.application.agent.command.ReviewCommand
                    .builder()
                    .conversationId(request.getConversationId())
                    .nodeId(request.getNodeId())
                    .approved(request.getApproved())
                    .comments(request.getComments())
                    .modifiedOutput(request.getModifiedOutput())
                    .build();

            // 调用应用服务处理审核
            agentApplicationService.reviewAndResume(command);

            return Response.success(null);

        } catch (Exception e) {
            log.error("人工介入审核失败", e);
            return Response.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 查询执行上下文
     */
    @GetMapping("/context/{conversationId}")
    @Operation(summary = "查询执行上下文", description = "获取指定会话的执行上下文，包含节点执行结果和暂停状态")
    public Response<com.zj.aiagent.interfaces.web.dto.response.agent.ExecutionContextResponse> getExecutionContext(
            @PathVariable String conversationId) {
        try {
            log.info("收到查询执行上下文请求: conversationId={}", conversationId);

            // 构建查询对象
            com.zj.aiagent.application.agent.query.GetExecutionContextQuery query = com.zj.aiagent.application.agent.query.GetExecutionContextQuery
                    .builder()
                    .conversationId(conversationId)
                    .build();

            // 调用应用服务查询
            com.zj.aiagent.application.agent.ExecutionContextDTO dto = agentApplicationService
                    .getExecutionContext(query);

            // 转换为响应对象
            com.zj.aiagent.interfaces.web.dto.response.agent.ExecutionContextResponse response = com.zj.aiagent.interfaces.web.dto.response.agent.ExecutionContextResponse
                    .builder()
                    .conversationId(dto.getConversationId())
                    .status(dto.getStatus())
                    .pausedNodeId(dto.getPausedNodeId())
                    .pausedNodeName(dto.getPausedNodeName())
                    .pausedAt(dto.getPausedAt())
                    .nodeResults(dto.getNodeResults())
                    .allowModifyOutput(dto.getInterventionRequest() != null
                            ? dto.getInterventionRequest().getAllowModifyOutput()
                            : null)
                    .checkMessage(dto.getInterventionRequest() != null
                            ? dto.getInterventionRequest().getCheckMessage()
                            : null)
                    .build();

            return Response.success(response);

        } catch (Exception e) {
            log.error("查询执行上下文失败: conversationId={}", conversationId, e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新执行上下文
     */
    @PutMapping("/context/{conversationId}")
    @Operation(summary = "更新执行上下文", description = "修改指定会话的执行上下文中的节点结果")
    public Response<Void> updateExecutionContext(
            @PathVariable String conversationId,
            @Valid @RequestBody com.zj.aiagent.interfaces.web.dto.request.agent.UpdateContextRequest request) {
        try {
            log.info("收到更新执行上下文请求: conversationId={}, modifications={}",
                    conversationId, request.getModifications().keySet());

            // 构建命令对象
            com.zj.aiagent.application.agent.command.UpdateExecutionContextCommand command = com.zj.aiagent.application.agent.command.UpdateExecutionContextCommand
                    .builder()
                    .conversationId(conversationId)
                    .modifications(request.getModifications())
                    .build();

            // 调用应用服务更新
            agentApplicationService.updateExecutionContext(command);

            return Response.success(null);

        } catch (Exception e) {
            log.error("更新执行上下文失败: conversationId={}", conversationId, e);
            return Response.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 查询会话历史消息
     */
    @GetMapping("/chat/history/{conversationId}")
    @Operation(summary = "查询会话历史消息", description = "根据会话ID查询历史消息,包含节点执行详情")
    public Response<List<ChatHistoryResponse>> getChatHistory(@PathVariable String conversationId) {
        try {
            log.info("查询会话历史: conversationId={}", conversationId);

            List<ChatMessageEntity> messages = chatMessageApplicationService.getConversationHistory(conversationId);

            List<ChatHistoryResponse> responses = messages.stream()
                    .map(this::convertToChatHistoryResponse)
                    .collect(Collectors.toList());

            log.info("查询会话历史成功: conversationId={}, count={}", conversationId, responses.size());
            return Response.success(responses);

        } catch (Exception e) {
            log.error("查询聊天历史失败: conversationId={}", conversationId, e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 转换为前端响应格式
     */
    private ChatHistoryResponse convertToChatHistoryResponse(ChatMessageEntity entity) {
        ChatHistoryResponse.ChatHistoryResponseBuilder builder = ChatHistoryResponse.builder()
                .role(entity.getRole().getValue())
                .timestamp(entity.getTimestamp())
                .error(entity.getIsError());

        // 用户消息
        if (entity.getRole() == ChatMessageEntity.MessageRole.USER) {
            builder.content(entity.getContent());
        }

        // AI 回复 - 转换节点执行详情
        if (entity.getNodeExecutions() != null && !entity.getNodeExecutions().isEmpty()) {
            List<ChatHistoryResponse.NodeExecution> nodes = entity.getNodeExecutions().stream()
                    .map(node -> ChatHistoryResponse.NodeExecution.builder()
                            .nodeId(node.getNodeId())
                            .nodeName(node.getNodeName())
                            .status(convertExecuteStatus(node.getExecuteStatus()))
                            .content(node.getOutputData())
                            .duration(node.getDurationMs())
                            .build())
                    .collect(Collectors.toList());
            builder.nodes(nodes);
        }

        return builder.build();
    }

    /**
     * 转换执行状态
     */
    private String convertExecuteStatus(String executeStatus) {
        if ("SUCCESS".equals(executeStatus))
            return "completed";
        if ("FAILED".equals(executeStatus))
            return "error";
        if ("RUNNING".equals(executeStatus))
            return "running";
        return "pending";
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
                            .agentId(dto.getAgentId())
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

    /**
     * 发布Agent（将草稿状态变更为发布状态）
     *
     * @param agentId Agent ID
     * @return 响应结果
     */
    @PostMapping("/publish/{agentId}")
    @Operation(summary = "发布Agent", description = "将草稿状态的Agent更改为已发布状态")
    public Response<String> publishAgent(@PathVariable String agentId) {
        try {
            // 从 UserContext 获取当前用户 ID
            Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
            if (userId == null) {
                return Response.unauthorized("未登录");
            }

            log.info("发布Agent, userId: {}, agentId: {}", userId, agentId);

            // 构建命令
            PublishAgentCommand command = PublishAgentCommand.builder()
                    .userId(userId)
                    .agentId(agentId)
                    .build();

            // 执行发布
            agentApplicationService.publishAgent(command);

            return Response.success("发布成功");

        } catch (RuntimeException e) {
            log.error("发布Agent失败", e);
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发布Agent异常", e);
            return Response.fail("发布失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户在指定Agent下的历史会话ID列表
     *
     * @param agentId Agent ID
     * @return 会话ID列表
     */
    @GetMapping("/conversations/{agentId}")
    @Operation(summary = "查询历史会话ID", description = "查询当前用户在指定Agent下的所有历史会话ID")
    public Response<List<com.zj.aiagent.interfaces.web.dto.response.agent.ConversationIdResponse>> getConversationIds(
            @PathVariable Long agentId) {
        try {
            // 从 UserContext 获取当前用户 ID
            Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
            if (userId == null) {
                return Response.unauthorized("未登录");
            }

            log.info("查询历史会话ID, userId: {}, agentId: {}", userId, agentId);

            // 构建查询对象
            com.zj.aiagent.application.agent.query.GetConversationIdsQuery query = com.zj.aiagent.application.agent.query.GetConversationIdsQuery
                    .builder()
                    .userId(userId)
                    .agentId(agentId)
                    .build();

            // 查询会话ID列表
            List<String> conversationIds = agentApplicationService.getConversationIds(query);

            // 转换为响应对象
            List<com.zj.aiagent.interfaces.web.dto.response.agent.ConversationIdResponse> responseList = conversationIds
                    .stream()
                    .map(id -> com.zj.aiagent.interfaces.web.dto.response.agent.ConversationIdResponse.builder()
                            .conversationId(id)
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询历史会话ID失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询Agent详情
     *
     * @param agentId Agent ID
     * @return Agent详情
     */
    @GetMapping("/detail/{agentId}")
    @Operation(summary = "查询Agent详情", description = "根据agentId查询Agent的详细配置信息")
    public Response<com.zj.aiagent.interfaces.web.dto.response.agent.AgentDetailResponse> getAgentDetail(
            @PathVariable String agentId) {
        try {
            // 从 UserContext 获取当前用户 ID
            Long userId = com.zj.aiagent.shared.utils.UserContext.getUserId();
            if (userId == null) {
                return Response.unauthorized("未登录");
            }

            log.info("查询Agent详情, userId: {}, agentId: {}", userId, agentId);

            // 构建查询对象
            com.zj.aiagent.application.agent.query.GetAgentDetailQuery query = com.zj.aiagent.application.agent.query.GetAgentDetailQuery
                    .builder()
                    .userId(userId)
                    .agentId(agentId)
                    .build();

            // 查询Agent详情
            AgentApplicationService.AgentDetailDTO detailDTO = agentApplicationService.getAgentDetail(query);

            // 转换为响应对象
            com.zj.aiagent.interfaces.web.dto.response.agent.AgentDetailResponse response = com.zj.aiagent.interfaces.web.dto.response.agent.AgentDetailResponse
                    .builder()
                    .agentId(detailDTO.getAgentId())
                    .agentName(detailDTO.getAgentName())
                    .description(detailDTO.getDescription())
                    .status(detailDTO.getStatus())
                    .statusDesc(detailDTO.getStatusDesc())
                    .graphJson(detailDTO.getGraphJson())
                    .createTime(detailDTO.getCreateTime())
                    .updateTime(detailDTO.getUpdateTime())
                    .build();

            return Response.success(response);

        } catch (RuntimeException e) {
            log.error("查询Agent详情失败", e);
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询Agent详情异常", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 构建标准化错误事件
     */
    private String buildErrorEvent(String errorCode, String errorType,
            String message, String conversationId) {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("type", "error");
        event.put("errorCode", errorCode);
        event.put("errorType", errorType);
        event.put("message", message);
        if (conversationId != null) {
            event.put("conversationId", conversationId);
        }
        event.put("timestamp", System.currentTimeMillis());

        return "data: " + com.alibaba.fastjson.JSON.toJSONString(event) + "\n\n";
    }
}
