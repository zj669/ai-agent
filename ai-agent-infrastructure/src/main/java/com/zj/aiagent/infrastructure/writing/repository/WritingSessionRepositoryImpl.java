package com.zj.aiagent.infrastructure.writing.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.repository.WritingSessionRepository;
import com.zj.aiagent.infrastructure.writing.mapper.WritingSessionMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingSessionPO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WritingSessionRepositoryImpl implements WritingSessionRepository {

    private final WritingSessionMapper mapper;

    @Override
    public void save(WritingSession session) {
        WritingSessionPO po = toPO(session);
        mapper.insert(po);
        session.setId(po.getId());
    }

    @Override
    public void update(WritingSession session) {
        mapper.updateById(toPO(session));
    }

    @Override
    public Optional<WritingSession> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<WritingSession> findByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<WritingSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingSessionPO::getWorkspaceId, workspaceId)
            .orderByDesc(WritingSessionPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private WritingSessionPO toPO(WritingSession domain) {
        WritingSessionPO po = new WritingSessionPO();
        po.setId(domain.getId());
        po.setWorkspaceId(domain.getWorkspaceId());
        po.setRootAgentId(domain.getRootAgentId());
        po.setHumanAgentId(domain.getHumanAgentId());
        po.setDefaultGroupId(domain.getDefaultGroupId());
        po.setTitle(domain.getTitle());
        po.setGoal(domain.getGoal());
        po.setConstraintsJson(domain.getConstraintsJson());
        po.setStatus(domain.getStatus());
        po.setCurrentDraftId(domain.getCurrentDraftId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private WritingSession toDomain(WritingSessionPO po) {
        return WritingSession.builder()
            .id(po.getId())
            .workspaceId(po.getWorkspaceId())
            .rootAgentId(po.getRootAgentId())
            .humanAgentId(po.getHumanAgentId())
            .defaultGroupId(po.getDefaultGroupId())
            .title(po.getTitle())
            .goal(po.getGoal())
            .constraintsJson(po.getConstraintsJson())
            .status(po.getStatus())
            .currentDraftId(po.getCurrentDraftId())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
