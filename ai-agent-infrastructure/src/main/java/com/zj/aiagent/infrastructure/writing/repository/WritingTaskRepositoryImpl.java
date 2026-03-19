package com.zj.aiagent.infrastructure.writing.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import com.zj.aiagent.domain.writing.repository.WritingTaskRepository;
import com.zj.aiagent.infrastructure.writing.mapper.WritingTaskMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingTaskPO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WritingTaskRepositoryImpl implements WritingTaskRepository {

    private final WritingTaskMapper mapper;

    @Override
    public void save(WritingTask task) {
        WritingTaskPO po = toPO(task);
        mapper.insert(po);
        task.setId(po.getId());
    }

    @Override
    public void update(WritingTask task) {
        mapper.updateById(toPO(task));
    }

    @Override
    public Optional<WritingTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<WritingTask> findByTaskUuid(String taskUuid) {
        LambdaQueryWrapper<WritingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WritingTaskPO::getTaskUuid, taskUuid).last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(
            this::toDomain
        );
    }

    @Override
    public List<WritingTask> findBySessionId(Long sessionId) {
        LambdaQueryWrapper<WritingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper
            .eq(WritingTaskPO::getSessionId, sessionId)
            .orderByDesc(WritingTaskPO::getCreatedAt);
        return mapper
            .selectList(wrapper)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WritingTask> findByWritingAgentId(Long writingAgentId) {
        LambdaQueryWrapper<WritingTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper
            .eq(WritingTaskPO::getWritingAgentId, writingAgentId)
            .orderByDesc(WritingTaskPO::getCreatedAt);
        return mapper
            .selectList(wrapper)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private WritingTaskPO toPO(WritingTask domain) {
        WritingTaskPO po = new WritingTaskPO();
        po.setId(domain.getId());
        po.setTaskUuid(domain.getTaskUuid());
        po.setSessionId(domain.getSessionId());
        po.setWritingAgentId(domain.getWritingAgentId());
        po.setSwarmAgentId(domain.getSwarmAgentId());
        po.setTaskType(domain.getTaskType());
        po.setTitle(domain.getTitle());
        po.setInstruction(domain.getInstruction());
        po.setInputPayloadJson(domain.getInputPayloadJson());
        po.setExpectedOutputSchemaJson(domain.getExpectedOutputSchemaJson());
        po.setStatus(domain.getStatus());
        po.setPriority(domain.getPriority());
        po.setCreatedBySwarmAgentId(domain.getCreatedBySwarmAgentId());
        po.setStartedAt(domain.getStartedAt());
        po.setFinishedAt(domain.getFinishedAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private WritingTask toDomain(WritingTaskPO po) {
        return WritingTask.builder()
            .id(po.getId())
            .taskUuid(po.getTaskUuid())
            .sessionId(po.getSessionId())
            .writingAgentId(po.getWritingAgentId())
            .swarmAgentId(po.getSwarmAgentId())
            .taskType(po.getTaskType())
            .title(po.getTitle())
            .instruction(po.getInstruction())
            .inputPayloadJson(po.getInputPayloadJson())
            .expectedOutputSchemaJson(po.getExpectedOutputSchemaJson())
            .status(po.getStatus())
            .priority(po.getPriority())
            .createdBySwarmAgentId(po.getCreatedBySwarmAgentId())
            .startedAt(po.getStartedAt())
            .finishedAt(po.getFinishedAt())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
