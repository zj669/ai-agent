package com.zj.aiagent.infrastructure.writing.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.repository.WritingResultRepository;
import com.zj.aiagent.infrastructure.writing.mapper.WritingResultMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingResultPO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WritingResultRepositoryImpl implements WritingResultRepository {

    private final WritingResultMapper mapper;

    @Override
    public void save(WritingResult result) {
        WritingResultPO po = toPO(result);
        mapper.insert(po);
        result.setId(po.getId());
    }

    @Override
    public Optional<WritingResult> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<WritingResult> findBySessionId(Long sessionId) {
        LambdaQueryWrapper<WritingResultPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingResultPO::getSessionId, sessionId)
            .orderByDesc(WritingResultPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<WritingResult> findByTaskId(Long taskId) {
        LambdaQueryWrapper<WritingResultPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingResultPO::getTaskId, taskId)
            .orderByDesc(WritingResultPO::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private WritingResultPO toPO(WritingResult domain) {
        WritingResultPO po = new WritingResultPO();
        po.setId(domain.getId());
        po.setSessionId(domain.getSessionId());
        po.setTaskId(domain.getTaskId());
        po.setWritingAgentId(domain.getWritingAgentId());
        po.setSwarmAgentId(domain.getSwarmAgentId());
        po.setResultType(domain.getResultType());
        po.setSummary(domain.getSummary());
        po.setContent(domain.getContent());
        po.setStructuredPayloadJson(domain.getStructuredPayloadJson());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private WritingResult toDomain(WritingResultPO po) {
        return WritingResult.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .taskId(po.getTaskId())
            .writingAgentId(po.getWritingAgentId())
            .swarmAgentId(po.getSwarmAgentId())
            .resultType(po.getResultType())
            .summary(po.getSummary())
            .content(po.getContent())
            .structuredPayloadJson(po.getStructuredPayloadJson())
            .createdAt(po.getCreatedAt())
            .build();
    }
}
