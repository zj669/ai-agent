package com.zj.aiagent.application.agent;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 应用服务实现
 *
 * @author zj
 * @since 2025-12-27
 */
@Slf4j
@Service
public class AgentApplicationService implements IAgentApplicationService {

    @Resource
    private IAiAgentRepository agentRepository;

    @Override
    public List<AiAgentPO> getUserAgentList(Long userId) {
        log.info("查询用户 Agent 列表: userId={}", userId);
        return agentRepository.findByUserId(userId);
    }

    @Override
    public AiAgentPO getAgentDetail(Long userId, Long agentId) {
        log.info("查询 Agent 详情: userId={}, agentId={}", userId, agentId);

        AiAgentPO agent = agentRepository.getById(agentId);

        if (agent == null) {
            throw new RuntimeException("Agent 不存在: " + agentId);
        }

        // 权限校验：只能查看自己的 Agent
        if (!agent.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该 Agent");
        }

        return agent;
    }

    @Override
    public String saveAgent(Long userId, String agentId, String agentName,
            String description, String graphJson, Integer status) {
        log.info("保存 Agent: userId={}, agentName={}", userId, agentName);

        AiAgentPO agent;

        if (agentId != null && !agentId.isEmpty()) {
            // 更新现有 Agent
            Long agentIdLong = Long.parseLong(agentId);
            agent = agentRepository.getById(agentIdLong);

            if (agent == null) {
                throw new RuntimeException("Agent 不存在: " + agentIdLong);
            }

            // 权限校验：只能修改自己的 Agent
            if (!agent.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改该 Agent");
            }

            // 更新字段
            agent.setAgentName(agentName);
            agent.setDescription(description);
            agent.setGraphJson(graphJson);
            if (status != null) {
                agent.setStatus(status);
            }
            agent.setUpdateTime(LocalDateTime.now());

        } else {
            // 创建新 Agent
            agent = AiAgentPO.builder()
                    .userId(userId)
                    .agentName(agentName)
                    .description(description)
                    .graphJson(graphJson)
                    .status(status != null ? status : 0) // 默认草稿
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
        }

        agentRepository.saveOrUpdate(agent);

        return String.valueOf(agent.getId());
    }

    @Override
    public void deleteAgent(Long userId, Long agentId) {
        log.info("删除 Agent: userId={}, agentId={}", userId, agentId);

        AiAgentPO agent = agentRepository.getById(agentId);

        if (agent == null) {
            throw new RuntimeException("Agent 不存在: " + agentId);
        }

        // 权限校验：只能删除自己的 Agent
        if (!agent.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该 Agent");
        }

        agentRepository.deleteById(agentId);
    }

    @Override
    public void publishAgent(Long userId, Long agentId) {
        log.info("发布 Agent: userId={}, agentId={}", userId, agentId);

        AiAgentPO agent = agentRepository.getById(agentId);

        if (agent == null) {
            throw new RuntimeException("Agent 不存在: " + agentId);
        }

        // 权限校验：只能发布自己的 Agent
        if (!agent.getUserId().equals(userId)) {
            throw new RuntimeException("无权发布该 Agent");
        }

        // 更新状态为已发布 (1)
        agentRepository.updateStatus(agentId, 1);
    }
}
