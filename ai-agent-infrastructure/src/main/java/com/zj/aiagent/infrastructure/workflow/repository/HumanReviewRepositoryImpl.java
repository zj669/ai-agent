package com.zj.aiagent.infrastructure.workflow.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public org.springframework.data.domain.Page<HumanReviewRecord> findReviewHistory(Long userId,
            org.springframework.data.domain.Pageable pageable) {
        Page<HumanReviewPO> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize()); // MyBatis Plus is
                                                                                                     // 1-indexed

        LambdaQueryWrapper<HumanReviewPO> wrapper = new LambdaQueryWrapper<>();
        // 如果需要筛选特定reviewer，可以加条件。这里假设查询该用户参与的审核
        if (userId != null) {
            wrapper.eq(HumanReviewPO::getReviewerId, userId);
        }
        wrapper.orderByDesc(HumanReviewPO::getReviewedAt);

        IPage<HumanReviewPO> resultPage = humanReviewMapper.selectPage(page, wrapper);

        List<HumanReviewRecord> content = resultPage.getRecords().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                content, pageable, resultPage.getTotal());
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
                .id(po.getId().toString())
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
