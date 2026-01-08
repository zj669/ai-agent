package com.zj.aiagent.interfaces.web.controller.client;

import cn.hutool.core.util.IdUtil;
import com.zj.aiagent.application.chat.ICharApplicationService;
import com.zj.aiagent.application.chat.command.ChatCommand;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.chat.CancelRequest;
import com.zj.aiagent.interfaces.web.dto.request.chat.ChatRequest;
import com.zj.aiagent.interfaces.web.dto.request.chat.ReviewRequest;
import com.zj.aiagent.interfaces.web.dto.response.agent.ExecutionContextResponse;
import com.zj.aiagent.interfaces.web.dto.response.chat.ChatHistoryResponse;
import com.zj.aiagent.shared.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

        // 调用应用服务查询历史
        List<com.zj.aiagent.domain.memory.dto.ChatHistoryDTO> dtoList = charApplicationService.queryHistory(userId,
                agentId, conversationId);

        // 转换为接口层 Response DTO
        List<ChatHistoryResponse> responseList = dtoList.stream()
                .map(this::toChatHistoryResponse)
                .collect(java.util.stream.Collectors.toList());

        return Response.success(responseList);
    }

    /**
     * ChatHistoryDTO → Chat HistoryResponse
     */
    private ChatHistoryResponse toChatHistoryResponse(com.zj.aiagent.domain.memory.dto.ChatHistoryDTO dto) {
        ChatHistoryResponse response = ChatHistoryResponse.builder()
                .role(dto.getRole())
                .content(dto.getContent())
                .timestamp(dto.getTimestamp())
                .error(dto.getError())
                .build();

        // 转换节点执行记录
        if (dto.getNodes() != null) {
            List<ChatHistoryResponse.NodeExecution> nodes = dto.getNodes().stream()
                    .map(node -> ChatHistoryResponse.NodeExecution.builder()
                            .nodeId(node.getNodeId())
                            .nodeName(node.getNodeName())
                            .status(node.getStatus())
                            .content(node.getContent())
                            .duration(node.getDuration())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            response.setNodes(nodes);
        }

        return response;
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
    public Response<ExecutionContextResponse> snapshot(@PathVariable("agentId") String agentId,
            @PathVariable("conversationId") String conversationId) {
        Long userId = UserContext.getUserId();

        // 调用应用服务获取快照
        com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot snapshot = charApplicationService
                .getSnapshot(userId, agentId, conversationId);

        if (snapshot == null) {
            return Response.success(null);
        }

        // 转换为 Response DTO
        ExecutionContextResponse.ExecutionContextResponseBuilder builder = ExecutionContextResponse.builder()
                .conversationId(snapshot.getExecutionId())
                .lastNodeId(snapshot.getLastNodeId())
                .status(snapshot.getStatus())
                .timestamp(snapshot.getTimestamp())
                .stateData(snapshot.getStateData());

        // 如果是暂停状态,提取人工介入信息
        if ("PAUSED".equals(snapshot.getStatus())) {
            builder.humanIntervention(buildHumanInterventionInfo(snapshot));
        }

        // 转换执行历史
        builder.executionHistory(buildExecutionHistory(snapshot));

        // 构建可编辑字段元数据
        builder.editableFields(buildEditableFields(snapshot));

        return Response.success(builder.build());
    }

    /**
     * 从快照中提取人工介入信息
     */
    private com.zj.aiagent.interfaces.web.dto.response.agent.HumanInterventionInfo buildHumanInterventionInfo(
            com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot snapshot) {

        java.util.Map<String, Object> stateData = snapshot.getStateData();
        if (stateData == null) {
            return null;
        }

        return com.zj.aiagent.interfaces.web.dto.response.agent.HumanInterventionInfo.builder()
                .nodeId(snapshot.getLastNodeId())
                .nodeName((String) stateData.getOrDefault("current_node_name", "未知节点"))
                .nodeType((String) stateData.getOrDefault("current_node_type", "UNKNOWN"))
                .checkMessage((String) stateData.getOrDefault("check_message", "请审核此内容"))
                .allowModifyOutput(true)
                .build();
    }

    /**
     * 从快照中提取执行历史
     */
    private java.util.List<com.zj.aiagent.interfaces.web.dto.response.agent.NodeExecutionRecord> buildExecutionHistory(
            com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot snapshot) {

        java.util.Map<String, Object> stateData = snapshot.getStateData();
        if (stateData == null) {
            return java.util.Collections.emptyList();
        }

        // 尝试从 stateData 中提取 execution_history
        Object executionHistoryObj = stateData.get("execution_history");
        if (executionHistoryObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> historyList = (java.util.List<java.util.Map<String, Object>>) executionHistoryObj;

            return historyList.stream()
                    .map(this::convertToNodeExecutionRecord)
                    .collect(java.util.stream.Collectors.toList());
        }

        return java.util.Collections.emptyList();
    }

    /**
     * 转换单个执行记录
     */
    private com.zj.aiagent.interfaces.web.dto.response.agent.NodeExecutionRecord convertToNodeExecutionRecord(
            java.util.Map<String, Object> record) {

        return com.zj.aiagent.interfaces.web.dto.response.agent.NodeExecutionRecord.builder()
                .nodeId((String) record.get("node_id"))
                .nodeName((String) record.get("node_name"))
                .nodeType((String) record.get("node_type"))
                .status((String) record.getOrDefault("status", "COMPLETED"))
                .startTime(getLongValue(record, "start_time"))
                .endTime(getLongValue(record, "end_time"))
                .duration(getLongValue(record, "duration"))
                .input(record.get("input"))
                .output(record.get("output"))
                .error((String) record.get("error"))
                .build();
    }

    /**
     * 安全获取 Long 值
     */
    private Long getLongValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 构建可编辑字段元数据
     * <p>
     * 动态检测 stateData 中实际存在的字段，为每个字段生成相应的元数据
     */
    private java.util.List<com.zj.aiagent.interfaces.web.dto.response.agent.EditableFieldMeta> buildEditableFields(
            com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot snapshot) {

        java.util.List<com.zj.aiagent.interfaces.web.dto.response.agent.EditableFieldMeta> fields = new java.util.ArrayList<>();

        java.util.Map<String, Object> stateData = snapshot.getStateData();
        if (stateData == null || stateData.isEmpty()) {
            return fields;
        }

        // 遍历 stateData 中的所有键，为每个字段生成元数据
        for (String key : stateData.keySet()) {
            Object value = stateData.get(key);

            // 跳过 null 值
            if (value == null) {
                continue;
            }

            // 根据字段名和值类型推断字段类型和描述
            String type = inferFieldType(key, value);
            String label = generateFieldLabel(key);
            String description = generateFieldDescription(key);

            fields.add(com.zj.aiagent.interfaces.web.dto.response.agent.EditableFieldMeta.builder()
                    .key(key)
                    .label(label)
                    .type(type)
                    .description(description)
                    .editable(true)
                    .build());
        }

        return fields;
    }

    /**
     * 推断字段类型
     */
    private String inferFieldType(String key, Object value) {
        // 根据字段名特殊处理
        if (key.contains("message") || key.contains("history") && value instanceof java.util.List) {
            // 检查是否是消息历史
            return "messages";
        }

        if (key.contains("input") || key.contains("prompt") || key.contains("query")) {
            return "text";
        }

        // 根据值类型推断
        if (value instanceof String) {
            return "text";
        } else if (value instanceof java.util.List || value instanceof java.util.Map) {
            return "json";
        } else if (value instanceof Number || value instanceof Boolean) {
            return "text";
        }

        return "json";
    }

    /**
     * 生成字段显示标签
     */
    private String generateFieldLabel(String key) {
        // 直接使用原始字段名
        return key;
    }

    /**
     * 生成字段描述
     */
    private String generateFieldDescription(String key) {
        // 预定义的常见字段描述
        java.util.Map<String, String> descMap = java.util.Map.ofEntries(
                java.util.Map.entry("user_input", "用户的原始输入内容"),
                java.util.Map.entry("execution_history", "各个节点的执行结果记录"),
                java.util.Map.entry("custom_variables", "工作流中的自定义变量"),
                java.util.Map.entry("message_history", "对话历史记录"),
                java.util.Map.entry("thought_history", "Agent 的思考过程记录"),
                java.util.Map.entry("action_history", "Agent 的行动记录"),
                java.util.Map.entry("loop_count", "当前循环执行次数"));

        return descMap.getOrDefault(key, "工作流状态数据");
    }

    @PostMapping("/snapshot/{agentId}/{conversationId}")
    @Operation(summary = "更新会话快照")
    public Response<Void> updateSnapshot(@PathVariable("agentId") String agentId,
            @PathVariable("conversationId") String conversationId,
            @RequestBody com.zj.aiagent.interfaces.web.dto.request.chat.UpdateSnapshotRequest request) {
        Long userId = UserContext.getUserId();

        log.info("更新快照请求: agentId={}, conversationId={}, nodeId={}",
                agentId, conversationId, request.getNodeId());

        // 调用应用服务更新快照
        charApplicationService.updateSnapshot(userId, agentId, conversationId,
                request.getNodeId(), request.getStateData());

        return Response.success();
    }

    /**
     * 取消 Agent 执行
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消 Agent 执行", description = "取消正在执行的 DAG")
    public Response<Void> cancelExecution(
            @Valid @RequestBody CancelRequest request) {
        try {
            log.info("收到取消执行请求: conversationId={}", request.getConversationId());

            // 构建命令
            com.zj.aiagent.application.agent.command.CancelExecutionCommand command = new com.zj.aiagent.application.agent.command.CancelExecutionCommand();
            command.setConversationId(request.getConversationId());

            // 调用应用服务取消执行
            charApplicationService.cancelExecution(command);

            return Response.success();

        } catch (Exception e) {
            log.error("取消执行失败", e);
            return Response.fail(e.getMessage());
        }
    }

}
