package com.zj.aiagent.infrastructure.context.memory;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import com.zj.aiagent.infrastructure.context.memory.convert.ChatMessageConverter;
import com.zj.aiagent.infrastructure.persistence.entity.AgentExecutionLogPO;
import com.zj.aiagent.infrastructure.persistence.entity.ChatMessagePO;
import com.zj.aiagent.infrastructure.persistence.mapper.AgentExecutionLogMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.ChatMessageMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于数据库的聊天历史存储实现
 * <p>
 * 使用 MySQL 存储对话消息和节点执行日志
 * </p>
 * <p>
 * 通过 memory.storage=database 配置启用
 * </p>
 */
@Slf4j
@Repository
@Primary
@ConditionalOnProperty(name = "memory.storage", havingValue = "database", matchIfMissing = true)
@AllArgsConstructor
public class DatabaseChatHistoryRepository implements ChatHistoryRepository {

        private final ChatMessageMapper chatMessageMapper;
        private final AgentExecutionLogMapper executionLogMapper;
        private final ChatMessageConverter converter;

        @Override
        public void save(String executionId, ChatMessage message) {
                ChatMessagePO po = converter.toPO(message);
                po.setConversationId(executionId);

                chatMessageMapper.insert(po);
                // 回填生成的ID
                message.setId(po.getId());

                log.debug("[{}] [Database] 保存消息: role={}, id={}",
                                executionId, message.getRole(), po.getId());
        }

        @Override
        public List<ChatMessage> load(String executionId, int maxMessages) {
                List<ChatMessagePO> poList = chatMessageMapper
                                .selectByConversationId(executionId, maxMessages);

                List<ChatMessage> result = poList.stream()
                                .map(converter::toEntity)
                                .collect(Collectors.toList());

                log.debug("[{}] [Database] 加载对话历史: {} 条", executionId, result.size());
                return result;
        }

        @Override
        public void clear(String executionId) {
                int messageCount = chatMessageMapper.deleteByConversationId(executionId);
                int logCount = executionLogMapper.deleteByConversationId(executionId);

                log.info("[{}] [Database] 清除对话历史: {} 条消息, {} 条执行日志",
                                executionId, messageCount, logCount);
        }

        @Override
        public List<ChatMessage> loadWithNodeExecutions(String conversationId, int maxMessages) {
                // 1. 查询消息列表
                List<ChatMessagePO> messagePOs = chatMessageMapper
                                .selectByConversationId(conversationId, maxMessages);

                if (messagePOs.isEmpty()) {
                        log.debug("[{}] [Database] 无历史消息", conversationId);
                        return Collections.emptyList();
                }

                // 2. 提取所有 instanceId（避免 N+1 查询）
                List<Long> instanceIds = messagePOs.stream()
                        .sorted(Comparator.comparing(ChatMessagePO::getCreateTime))
                        .map(ChatMessagePO::getInstanceId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());

                // 3. 批量查询节点执行日志
                Map<Long, List<AgentExecutionLogPO>> executionMap = new HashMap<>();
                if (!instanceIds.isEmpty()) {
                        List<AgentExecutionLogPO> executions = executionLogMapper
                                        .selectByInstanceIds(instanceIds);

                        // 按 instanceId 分组
                        executionMap = executions.stream()
                                        .collect(Collectors.groupingBy(AgentExecutionLogPO::getInstanceId));

                        log.debug("[{}] [Database] 批量加载节点执行日志: {} 个实例, {} 条记录",
                                        conversationId, instanceIds.size(), executions.size());
                }

                // 4. 组装结果
                List<ChatMessage> result = new ArrayList<>();
                for (ChatMessagePO po : messagePOs) {
                        ChatMessage message = converter.toEntity(po);

                        // 关联节点执行记录
                        if (po.getInstanceId() != null) {
                                List<AgentExecutionLogPO> logs = executionMap
                                                .getOrDefault(po.getInstanceId(), Collections.emptyList());

                                if (!logs.isEmpty()) {
                                        List<NodeExecutionRecord> nodeExecutions = logs.stream()
                                                        .map(converter::toNodeExecutionRecord)
                                                        .collect(Collectors.toList());
                                        message.setNodeExecutions(nodeExecutions);
                                }
                        }

                        result.add(message);
                }

                log.debug("[{}] [Database] 加载完整消息（含节点详情）: {} 条", conversationId, result.size());
                return result;
        }

        @Override
        public void saveNodeExecution(String conversationId, Long instanceId, NodeExecutionRecord record) {
                AgentExecutionLogPO po = converter.toExecutionLogPO(record);
                po.setConversationId(conversationId);
                po.setInstanceId(instanceId);
                po.setAgentId(record.getAgentId());

                executionLogMapper.insert(po);
                record.setId(po.getId());

                log.debug("[{}] [Database] 保存节点执行日志: nodeId={}, status={}, id={}",
                                conversationId, record.getNodeId(), record.getExecuteStatus(), po.getId());
        }

        @Override
        public List<NodeExecutionRecord> loadNodeExecutions(Long instanceId) {
                List<AgentExecutionLogPO> poList = executionLogMapper
                                .selectByInstanceId(instanceId);

                List<NodeExecutionRecord> result = poList.stream()
                                .map(converter::toNodeExecutionRecord)
                                .collect(Collectors.toList());

                log.debug("[Database] 加载实例 {} 的节点执行日志: {} 条", instanceId, result.size());
                return result;
        }

        @Override
        public List<String> queryConversationIds(Long userId, String agentId) {
                List<String> conversationIds = chatMessageMapper
                                .selectConversationIdsByUserAndAgent(userId, agentId);

                log.debug("[Database] 查询用户 {} 在 Agent {} 的会话ID: {} 个",
                                userId, agentId, conversationIds.size());
                return conversationIds;
        }
}
