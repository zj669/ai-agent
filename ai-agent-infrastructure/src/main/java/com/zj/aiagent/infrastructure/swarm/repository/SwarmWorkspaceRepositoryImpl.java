package com.zj.aiagent.infrastructure.swarm.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.infrastructure.swarm.mapper.SwarmWorkspaceMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmWorkspacePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SwarmWorkspaceRepositoryImpl implements SwarmWorkspaceRepository {

    private final SwarmWorkspaceMapper mapper;

    @Override
    public void save(SwarmWorkspace workspace) {
        SwarmWorkspacePO po = toPO(workspace);
        mapper.insert(po);
        workspace.setId(po.getId());
    }

    @Override
    public Optional<SwarmWorkspace> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SwarmWorkspace> findByUserId(Long userId) {
        LambdaQueryWrapper<SwarmWorkspacePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmWorkspacePO::getUserId, userId).orderByDesc(SwarmWorkspacePO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void update(SwarmWorkspace workspace) {
        mapper.updateById(toPO(workspace));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private SwarmWorkspacePO toPO(SwarmWorkspace domain) {
        SwarmWorkspacePO po = new SwarmWorkspacePO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setUserId(domain.getUserId());
        po.setDefaultModel(domain.getDefaultModel());
        po.setLlmConfigId(domain.getLlmConfigId());
        po.setMaxRoundsPerTurn(domain.getMaxRoundsPerTurn());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private SwarmWorkspace toDomain(SwarmWorkspacePO po) {
        return SwarmWorkspace.builder()
                .id(po.getId())
                .name(po.getName())
                .userId(po.getUserId())
                .defaultModel(po.getDefaultModel())
                .llmConfigId(po.getLlmConfigId())
                .maxRoundsPerTurn(po.getMaxRoundsPerTurn())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
