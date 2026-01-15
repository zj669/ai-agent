package com.zj.aiagent.infrastructure.knowledge.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDataset;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDatasetRepository;
import com.zj.aiagent.infrastructure.knowledge.mapper.KnowledgeDatasetMapper;
import com.zj.aiagent.infrastructure.knowledge.po.KnowledgeDatasetPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * KnowledgeDataset Repository 实现 (MyBatis-Plus)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MySQLKnowledgeDatasetRepository implements KnowledgeDatasetRepository {

    private final KnowledgeDatasetMapper mapper;

    @Override
    public KnowledgeDataset save(KnowledgeDataset dataset) {
        KnowledgeDatasetPO po = toPO(dataset);

        if (mapper.selectById(po.getDatasetId()) == null) {
            // 新增
            mapper.insert(po);
        } else {
            // 更新
            mapper.updateById(po);
        }

        return toDomain(po);
    }

    @Override
    public Optional<KnowledgeDataset> findById(String datasetId) {
        KnowledgeDatasetPO po = mapper.selectById(datasetId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<KnowledgeDataset> findByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeDatasetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDatasetPO::getUserId, userId);

        return mapper.selectList(wrapper)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String datasetId) {
        mapper.deleteById(datasetId);
    }

    // ========== PO <=> Domain Entity Converters ==========

    private KnowledgeDatasetPO toPO(KnowledgeDataset domain) {
        if (domain == null) {
            return null;
        }

        KnowledgeDatasetPO po = new KnowledgeDatasetPO();
        po.setDatasetId(domain.getDatasetId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setUserId(domain.getUserId());
        po.setAgentId(domain.getAgentId());
        po.setDocumentCount(domain.getDocumentCount() != null ? domain.getDocumentCount() : 0);
        po.setTotalChunks(domain.getTotalChunks() != null ? domain.getTotalChunks() : 0);
        po.setCreatedAt(toLocalDateTime(domain.getCreatedAt()));
        po.setUpdatedAt(toLocalDateTime(domain.getUpdatedAt()));

        return po;
    }

    private KnowledgeDataset toDomain(KnowledgeDatasetPO po) {
        if (po == null) {
            return null;
        }

        KnowledgeDataset domain = new KnowledgeDataset();
        domain.setDatasetId(po.getDatasetId());
        domain.setName(po.getName());
        domain.setDescription(po.getDescription());
        domain.setUserId(po.getUserId());
        domain.setAgentId(po.getAgentId());
        domain.setDocumentCount(po.getDocumentCount());
        domain.setTotalChunks(po.getTotalChunks());
        domain.setCreatedAt(toInstant(po.getCreatedAt()));
        domain.setUpdatedAt(toInstant(po.getUpdatedAt()));

        return domain;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : LocalDateTime.now();
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
