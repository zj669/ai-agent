package com.zj.aiagent.application.dag;

import cn.hutool.core.util.IdUtil;
import com.zj.aiagent.application.dag.command.ChatCommand;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.service.DagExecuteService;
import com.zj.aiagent.domain.agent.dag.service.DagLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {
    private final DagExecuteService dagExecuteService;
    private final DagLoaderService dagLoaderService;

    public void chat(ChatCommand command) {
        log.info("执行对话, user: {}", command.getUserMessage());
        DagGraph dagGraph = dagLoaderService.loadDagByAgentId(command.getAgentId());
        String conversationId = command.getConversationId();
        if(conversationId == null){
            conversationId = String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
        }
        dagExecuteService.executeDag(dagGraph, conversationId, command.getUserMessage(), command.getEmitter());
    }
}
