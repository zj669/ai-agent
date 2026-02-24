package com.zj.aiagent.infrastructure.swarm.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.valobj.SwarmAgentStatus;
import com.zj.aiagent.infrastructure.swarm.mapper.SwarmAgentMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmWorkspaceAgentPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SwarmAgentRepositoryImpl implements SwarmAgentRepository {

    private final SwarmAgentMapper mapper;

    @Override
    public void save(SwarmAgent agent) {
        SwarmWorkspaceAgentPO po = toPO(agent);
        mapper.insert(po);
        agent.setId(po.getId());
    }

    @Override
    public Optional<SwarmAgent> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SwarmAgent> findByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<SwarmWorkspaceAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmWorkspaceAgentPO::getWorkspaceId, workspaceId)
                .orderByAsc(SwarmWorkspaceAgentPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(Long id, String status) {
        LambdaUpdateWrapper<SwarmWorkspaceAgentPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SwarmWorkspaceAgentPO::getId, id)
                .set(SwarmWorkspaceAgentPO::getStatus, status);
        mapper.update(null, wrapper);
    }

    @Override
    public void updateLlmHistory(Long id, String llmHistory) {
        LambdaUpdateWrapper<SwarmWorkspaceAgentPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SwarmWorkspaceAgentPO::getId, id)
                .set(SwarmWorkspaceAgentPO::getLlmHistory, llmHistory);
        mapper.update(null, wrapper);
    }

    @Override
    public void deleteByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<SwarmWorkspaceAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmWorkspaceAgentPO::getWorkspaceId, workspaceId);
        mapper.delete(wrapper);
    }

    private SwarmWorkspaceAgentPO toPO(SwarmAgent domain) {
        SwarmWorkspaceAgentPO po = new SwarmWorkspaceAgentPO();
        po.setId(domain.getId());
        po.setWorkspaceId(domain.getWorkspaceId());
        po.setAgentId(domain.getAgentId());
        po.setRole(domain.getRole());
        po.setParentId(domain.getParentId());
        po.setLlmHistory(domain.getLlmHistory());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : "IDLE");
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private SwarmAgent toDomain(SwarmWorkspaceAgentPO po) {
        return SwarmAgent.builder()
                .id(po.getId())
                .workspaceId(po.getWorkspaceId())
                .agentId(po.getAgentId())
                .role(po.getRole())
                .parentId(po.getParentId())
                .llmHistory(po.getLlmHistory())
                .status(SwarmAgentStatus.fromCode(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .build();
    }
}
