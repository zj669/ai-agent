package com.zj.aiagent.infrastructure.dag.repository;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
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
}
