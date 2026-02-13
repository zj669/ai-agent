package com.zj.aiagent.interfaces.chat;

import com.zj.aiagent.application.chat.ChatApplicationService;
import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.interfaces.chat.dto.ConversationResponse;
import com.zj.aiagent.interfaces.chat.dto.MessageResponse;
import com.zj.aiagent.shared.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天接口
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    /**
     * 创建会话
     */
    @PostMapping("/conversations")
    public String createConversation(
            @RequestParam(required = true) String userId,
            @RequestParam(required = true) String agentId) {
        return chatApplicationService.createConversation(userId, agentId);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public PageResult<ConversationResponse> getConversations(
            @RequestParam(required = true) String userId,
            @RequestParam(required = true) String agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 明确指定排序：按 updatedAt 倒序
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        PageResult<Conversation> result = chatApplicationService.getConversationHistory(userId, agentId, pageable);

        List<ConversationResponse> list = result.getList().stream()
                .map(ConversationResponse::from)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), result.getPages(), list);
    }

    /**
     * 获取会话消息历史
     * 注意：需要验证用户权限
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = true) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "asc") String order) {

        // 构建分页和排序参数
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "createdAt"));

        // 使用带权限校验的方法
        List<Message> messages = chatApplicationService.getMessagesWithAuth(conversationId, userId, pageable);

        return messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 删除会话
     * 注意：需要验证用户权限
     */
    @DeleteMapping("/conversations/{conversationId}")
    public void deleteConversation(
            @PathVariable String conversationId,
            @RequestParam(required = true) String userId) {
        chatApplicationService.deleteConversationWithAuth(conversationId, userId);
    }
}
