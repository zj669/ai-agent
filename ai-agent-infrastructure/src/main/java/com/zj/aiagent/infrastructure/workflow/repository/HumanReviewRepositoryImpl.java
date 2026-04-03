package com.zj.aiagent.infrastructure.workflow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import com.zj.aiagent.domain.workflow.port.HumanReviewRepository;
import com.zj.aiagent.infrastructure.workflow.mapper.HumanReviewMapper;
import com.zj.aiagent.infrastructure.workflow.po.HumanReviewPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HumanReviewRepositoryImpl implements HumanReviewRepository {

    private final HumanReviewMapper humanReviewMapper;

    @Override
    public void save(HumanReviewRecord record) {
        HumanReviewPO po = toPO(record);
        humanReviewMapper.insert(po);
        record.setId(po.getId().toString());
    }

    @Override
    public List<HumanReviewRecord> findByExecutionId(String executionId) {
        return humanReviewMapper.selectList(new LambdaQueryWrapper<HumanReviewPO>()
                .eq(HumanReviewPO::getExecutionId, executionId)
                .orderByDesc(HumanReviewPO::getReviewedAt))
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<HumanReviewRecord> findReviewHistory(Long userId, int offset, int limit) {
        LambdaQueryWrapper<HumanReviewPO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(HumanReviewPO::getReviewerId, userId);
        }
        wrapper.orderByDesc(HumanReviewPO::getReviewedAt)
                .last("LIMIT " + offset + ", " + limit);

        return humanReviewMapper.selectList(wrapper)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    private HumanReviewPO toPO(HumanReviewRecord entity) {
        HumanReviewPO po = new HumanReviewPO();
        if (entity.getId() != null) {
            try {
                po.setId(Long.parseLong(entity.getId()));
            } catch (NumberFormatException e) {
                // ignore or handle if using UUID vs AutoInc
            }
        }
        po.setExecutionId(entity.getExecutionId());
        po.setNodeId(entity.getNodeId());
        po.setReviewerId(entity.getReviewerId());
        po.setDecision(entity.getDecision());
        po.setTriggerPhase(entity.getTriggerPhase());
        po.setOriginalData(entity.getOriginalData());
        po.setModifiedData(entity.getModifiedData());
        po.setComment(entity.getComment());
        po.setReviewedAt(entity.getReviewedAt());
        return po;
    }

    private HumanReviewRecord toEntity(HumanReviewPO po) {
        return HumanReviewRecord.builder()
                .id(po.getId() != null ? po.getId().toString() : null)
                .executionId(po.getExecutionId())
                .nodeId(po.getNodeId())
                .reviewerId(po.getReviewerId())
                .decision(po.getDecision())
                .triggerPhase(po.getTriggerPhase())
                .originalData(po.getOriginalData())
                .modifiedData(po.getModifiedData())
                .comment(po.getComment())
                .reviewedAt(po.getReviewedAt())
                .build();
    }
}
