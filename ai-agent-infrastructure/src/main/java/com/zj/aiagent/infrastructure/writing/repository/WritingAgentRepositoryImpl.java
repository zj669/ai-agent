package com.zj.aiagent.infrastructure.writing.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.writing.entity.WritingAgent;
import com.zj.aiagent.domain.writing.repository.WritingAgentRepository;
import com.zj.aiagent.infrastructure.writing.mapper.WritingAgentMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingAgentPO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WritingAgentRepositoryImpl implements WritingAgentRepository {

    private final WritingAgentMapper mapper;

    @Override
    public void save(WritingAgent agent) {
        WritingAgentPO po = toPO(agent);
        mapper.insert(po);
        agent.setId(po.getId());
    }

    @Override
    public void update(WritingAgent agent) {
        mapper.updateById(toPO(agent));
    }

    @Override
    public Optional<WritingAgent> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<WritingAgent> findBySessionIdAndSwarmAgentId(Long sessionId, Long swarmAgentId) {
        LambdaQueryWrapper<WritingAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingAgentPO::getSessionId, sessionId)
            .eq(WritingAgentPO::getSwarmAgentId, swarmAgentId)
            .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<WritingAgent> findBySessionId(Long sessionId) {
        LambdaQueryWrapper<WritingAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingAgentPO::getSessionId, sessionId)
            .orderByAsc(WritingAgentPO::getSortOrder)
            .orderByAsc(WritingAgentPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private WritingAgentPO toPO(WritingAgent domain) {
        WritingAgentPO po = new WritingAgentPO();
        po.setId(domain.getId());
        po.setSessionId(domain.getSessionId());
        po.setSwarmAgentId(domain.getSwarmAgentId());
        po.setRole(domain.getRole());
        po.setDescription(domain.getDescription());
        po.setSkillTagsJson(domain.getSkillTagsJson());
        po.setStatus(domain.getStatus());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private WritingAgent toDomain(WritingAgentPO po) {
        return WritingAgent.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .swarmAgentId(po.getSwarmAgentId())
            .role(po.getRole())
            .description(po.getDescription())
            .skillTagsJson(po.getSkillTagsJson())
            .status(po.getStatus())
            .sortOrder(po.getSortOrder())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
