package com.zj.aiagent.application.chat;

import com.zj.aiagent.application.chat.command.ChatCommand;

import java.util.List;

public interface ICharApplicationService {
    void chat(ChatCommand command);

    List<String> queryHistoryId(Long userId, String agentId);

    void review(Long userId, String conversationId, String nodeId,  Boolean approved,  String agentId);
}
