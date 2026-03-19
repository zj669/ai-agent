package com.zj.aiagent.infrastructure.writing.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.writing.entity.WritingDraft;
import com.zj.aiagent.domain.writing.repository.WritingDraftRepository;
import com.zj.aiagent.infrastructure.writing.mapper.WritingDraftMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingDraftPO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WritingDraftRepositoryImpl implements WritingDraftRepository {

    private final WritingDraftMapper mapper;

    @Override
    public void save(WritingDraft draft) {
        WritingDraftPO po = toPO(draft);
        mapper.insert(po);
        draft.setId(po.getId());
    }

    @Override
    public Optional<WritingDraft> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<WritingDraft> findLatestBySessionId(Long sessionId) {
        LambdaQueryWrapper<WritingDraftPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingDraftPO::getSessionId, sessionId)
            .orderByDesc(WritingDraftPO::getVersionNo)
            .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<WritingDraft> findBySessionId(Long sessionId) {
        LambdaQueryWrapper<WritingDraftPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingDraftPO::getSessionId, sessionId)
            .orderByDesc(WritingDraftPO::getVersionNo);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private WritingDraftPO toPO(WritingDraft domain) {
        WritingDraftPO po = new WritingDraftPO();
        po.setId(domain.getId());
        po.setSessionId(domain.getSessionId());
        po.setVersionNo(domain.getVersionNo());
        po.setTitle(domain.getTitle());
        po.setContent(domain.getContent());
        po.setSourceResultIdsJson(domain.getSourceResultIdsJson());
        po.setStatus(domain.getStatus());
        po.setCreatedBySwarmAgentId(domain.getCreatedBySwarmAgentId());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private WritingDraft toDomain(WritingDraftPO po) {
        return WritingDraft.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .versionNo(po.getVersionNo())
            .title(po.getTitle())
            .content(po.getContent())
            .sourceResultIdsJson(po.getSourceResultIdsJson())
            .status(po.getStatus())
            .createdBySwarmAgentId(po.getCreatedBySwarmAgentId())
            .createdAt(po.getCreatedAt())
            .build();
    }
}
