package com.zj.aiagent.infrastructure.dag.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.repository.IDagRepository;
import com.zj.aiagent.infrastructure.dag.converter.AgentConvert;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class DagRepository implements IDagRepository {
    @Resource
    private AiAgentMapper aiAgentMapper;

    @Resource
    private com.zj.aiagent.infrastructure.persistence.mapper.AiAgentInstanceMapper aiAgentInstanceMapper;

    @Override
    public AiAgent selectAiAgentByAgentId(String agentId) {
        if (agentId == null) {
            return null;
        }

        LambdaQueryWrapper<AiAgentPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiAgentPO::getId, agentId);

        AiAgentPO aiAgentPO = aiAgentMapper.selectOne(queryWrapper);
        return AgentConvert.toDomain(aiAgentPO);
    }

    @Override
    public AiAgent saveAgent(AiAgent agent) {
        log.info("保存Agent配置, agentId={}, agentName={}", agent.getAgentId(), agent.getAgentName());

        // 查询是否已存在
        LambdaQueryWrapper<AiAgentPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiAgentPO::getId, agent.getAgentId());
        AiAgentPO existingPO = aiAgentMapper.selectOne(queryWrapper);

        AiAgentPO aiAgentPO = AgentConvert.toPO(agent);

        if (existingPO != null) {
            // 更新现有记录
            aiAgentPO.setId(existingPO.getId());
            aiAgentMapper.updateById(aiAgentPO);
            log.info("更新Agent配置成功, id={}", aiAgentPO.getId());
        } else {
            // 插入新记录
            aiAgentMapper.insert(aiAgentPO);
            log.info("创建Agent配置成功, id={}", aiAgentPO.getId());
        }

        return AgentConvert.toDomain(aiAgentPO);
    }

    @Override
    public AiAgent selectAiAgentByAgentIdAndUserId(String agentId, Long userId) {
        log.debug("根据agentId和userId查询Agent: agentId={}, userId={}", agentId, userId);

        if (agentId == null || userId == null) {
            return null;
        }

        LambdaQueryWrapper<AiAgentPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiAgentPO::getId, agentId);
        queryWrapper.eq(AiAgentPO::getUserId, userId);

        AiAgentPO aiAgentPO = aiAgentMapper.selectOne(queryWrapper);
        return AgentConvert.toDomain(aiAgentPO);
    }

    @Override
    public java.util.List<String> findDistinctConversationIds(Long userId, Long agentId) {
        log.debug("查询用户历史会话ID: userId={}, agentId={}", userId, agentId);

        if (userId == null || agentId == null) {
            return java.util.Collections.emptyList();
        }

        // 查询所有符合条件的实例
        LambdaQueryWrapper<com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO::getAgentId, agentId)
                .orderByDesc(com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO::getCreateTime);

        java.util.List<com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO> instances = aiAgentInstanceMapper
                .selectList(queryWrapper);

        // 提取并去重 conversationId
        java.util.List<String> conversationIds = instances.stream()
                .map(com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO::getConversationId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        log.debug("查询到 {} 个不重复的会话ID", conversationIds.size());
        return conversationIds;
    }
}
