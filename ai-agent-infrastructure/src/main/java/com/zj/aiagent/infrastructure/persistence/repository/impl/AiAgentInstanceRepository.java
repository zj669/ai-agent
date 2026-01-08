package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentInstanceMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentInstanceRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * AI智能体运行实例仓储实现
 */
@Slf4j
@Repository
public class AiAgentInstanceRepository implements IAiAgentInstanceRepository {

    @Resource
    private AiAgentInstanceMapper aiAgentInstanceMapper;

    @Override
    public AiAgentInstancePO findByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return null;
        }

        try {
            LambdaQueryWrapper<AiAgentInstancePO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiAgentInstancePO::getConversationId, conversationId)
                    .orderByDesc(AiAgentInstancePO::getCreateTime)
                    .last("LIMIT 1");

            return aiAgentInstanceMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("查询实例失败: conversationId={}", conversationId, e);
            return null;
        }
    }

    @Override
    public AiAgentInstancePO saveOrUpdate(AiAgentInstancePO instance) {
        if (instance == null) {
            throw new IllegalArgumentException("实例不能为空");
        }

        try {
            if (instance.getId() != null) {
                // 更新现有记录
                instance.setUpdateTime(LocalDateTime.now());
                aiAgentInstanceMapper.updateById(instance);
                log.debug("更新实例: id={}, conversationId={}", instance.getId(), instance.getConversationId());
            } else {
                // 插入新记录
                if (instance.getCreateTime() == null) {
                    instance.setCreateTime(LocalDateTime.now());
                }
                if (instance.getUpdateTime() == null) {
                    instance.setUpdateTime(LocalDateTime.now());
                }
                aiAgentInstanceMapper.insert(instance);
                log.debug("创建实例: id={}, conversationId={}", instance.getId(), instance.getConversationId());
            }
            return instance;
        } catch (Exception e) {
            log.error("保存或更新实例失败: conversationId={}", instance.getConversationId(), e);
            throw new RuntimeException("保存或更新实例失败", e);
        }
    }

    @Override
    public void clearCheckpointData(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            log.warn("清除检查点数据：conversationId 为空");
            return;
        }

        try {
            // todo 引入逻辑删除
            AiAgentInstancePO instance = findByConversationId(conversationId);
            if (instance != null) {
                instance.setRuntimeContextJson(null);
                instance.setCurrentNodeId(null);
                instance.setUpdateTime(LocalDateTime.now());
                aiAgentInstanceMapper.updateById(instance);
                log.debug("清除检查点数据: conversationId={}", conversationId);
            }
        } catch (Exception e) {
            log.error("清除检查点数据失败: conversationId={}", conversationId, e);
            throw new RuntimeException("清除检查点数据失败", e);
        }
    }
}
