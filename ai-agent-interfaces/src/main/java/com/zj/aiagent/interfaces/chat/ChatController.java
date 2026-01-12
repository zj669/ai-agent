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
    public String createConversation(@RequestParam String userId, @RequestParam String agentId) {
        return chatApplicationService.createConversation(userId, agentId);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public PageResult<ConversationResponse> getConversations(
            @RequestParam String userId,
            @RequestParam String agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        PageResult<Conversation> result = chatApplicationService.getConversationHistory(userId, agentId, pageable);

        List<ConversationResponse> list = result.getList().stream()
                .map(ConversationResponse::from)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), result.getPages(), list);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        List<Message> messages = chatApplicationService.getMessages(conversationId, pageable);

        return messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public void deleteConversation(@PathVariable String conversationId) {
        chatApplicationService.deleteConversation(conversationId);
    }
}
