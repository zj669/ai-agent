package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class AiAgentRepository implements IAiAgentRepository {

    @Resource
    private AiAgentMapper aiAgentMapper;

    @Override
    public AiAgentPO getById(Long id) {
        if (id == null) {
            return null;
        }
        return aiAgentMapper.selectById(id);
    }

    @Override
    public List<AiAgentPO> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<AiAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgentPO::getUserId, userId)
                .orderByDesc(AiAgentPO::getUpdateTime);
        return aiAgentMapper.selectList(wrapper);
    }

    @Override
    public void saveOrUpdate(AiAgentPO agent) {
        if (agent == null) {
            log.warn("Agent PO 为空，跳过保存");
            return;
        }

        if (agent.getId() == null) {
            // 新增
            aiAgentMapper.insert(agent);
            log.info("新增 Agent: id={}, name={}", agent.getId(), agent.getAgentName());
        } else {
            // 更新
            aiAgentMapper.updateById(agent);
            log.info("更新 Agent: id={}, name={}", agent.getId(), agent.getAgentName());
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            log.warn("Agent ID 为空，跳过删除");
            return;
        }
        aiAgentMapper.deleteById(id);
        log.info("删除 Agent: id={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("参数为空，跳过更新状态: id={}, status={}", id, status);
            return;
        }

        LambdaUpdateWrapper<AiAgentPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiAgentPO::getId, id)
                .set(AiAgentPO::getStatus, status);
        aiAgentMapper.update(null, wrapper);
        log.info("更新 Agent 状态: id={}, status={}", id, status);
    }
}
