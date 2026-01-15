package com.zj.aiagent.infrastructure.knowledge.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.DocumentStatus;
import com.zj.aiagent.infrastructure.knowledge.mapper.KnowledgeDocumentMapper;
import com.zj.aiagent.infrastructure.knowledge.po.KnowledgeDocumentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * KnowledgeDocument Repository 实现 (MyBatis-Plus)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MySQLKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentMapper mapper;

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        KnowledgeDocumentPO po = toPO(document);

        if (mapper.selectById(po.getDocumentId()) == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }

        return toDomain(po);
    }

    @Override
    public Optional<KnowledgeDocument> findById(String documentId) {
        KnowledgeDocumentPO po = mapper.selectById(documentId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public org.springframework.data.domain.Page<KnowledgeDocument> findByDatasetId(String datasetId,
            Pageable pageable) {
        // 创建 MyBatis-Plus 分页对象
        Page<KnowledgeDocumentPO> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<KnowledgeDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocumentPO::getDatasetId, datasetId);

        // 执行分页查询
        IPage<KnowledgeDocumentPO> poPage = mapper.selectPage(page, wrapper);

        // 转换为 Domain 对象
        List<KnowledgeDocument> documents = poPage.getRecords()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        // 返回 Spring Data Page
        return new PageImpl<>(documents, pageable, poPage.getTotal());
    }

    @Override
    public void deleteById(String documentId) {
        mapper.deleteById(documentId);
    }

    // ========== PO <=> Domain Entity Converters ==========

    private KnowledgeDocumentPO toPO(KnowledgeDocument domain) {
        if (domain == null) {
            return null;
        }

        KnowledgeDocumentPO po = new KnowledgeDocumentPO();
        po.setDocumentId(domain.getDocumentId());
        po.setDatasetId(domain.getDatasetId());
        po.setFilename(domain.getFilename());
        po.setFileUrl(domain.getFileUrl());
        po.setFileSize(domain.getFileSize());
        po.setContentType(domain.getContentType());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : DocumentStatus.PENDING.name());
        po.setTotalChunks(domain.getTotalChunks());
        po.setProcessedChunks(domain.getProcessedChunks() != null ? domain.getProcessedChunks() : 0);
        po.setErrorMessage(domain.getErrorMessage());

        // ChunkingConfig
        if (domain.getChunkingConfig() != null) {
            po.setChunkSize(domain.getChunkingConfig().getChunkSize());
            po.setChunkOverlap(domain.getChunkingConfig().getChunkOverlap());
        } else {
            po.setChunkSize(500);
            po.setChunkOverlap(50);
        }

        po.setUploadedAt(toLocalDateTime(domain.getUploadedAt()));
        po.setCompletedAt(toLocalDateTime(domain.getCompletedAt()));

        return po;
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentPO po) {
        if (po == null) {
            return null;
        }

        KnowledgeDocument domain = new KnowledgeDocument();
        domain.setDocumentId(po.getDocumentId());
        domain.setDatasetId(po.getDatasetId());
        domain.setFilename(po.getFilename());
        domain.setFileUrl(po.getFileUrl());
        domain.setFileSize(po.getFileSize());
        domain.setContentType(po.getContentType());
        domain.setStatus(po.getStatus() != null ? DocumentStatus.valueOf(po.getStatus()) : DocumentStatus.PENDING);
        domain.setTotalChunks(po.getTotalChunks());
        domain.setProcessedChunks(po.getProcessedChunks());
        domain.setErrorMessage(po.getErrorMessage());

        // ChunkingConfig
        ChunkingConfig config = ChunkingConfig.builder()
                .chunkSize(po.getChunkSize() != null ? po.getChunkSize() : 500)
                .chunkOverlap(po.getChunkOverlap() != null ? po.getChunkOverlap() : 50)
                .build();
        domain.setChunkingConfig(config);

        domain.setUploadedAt(toInstant(po.getUploadedAt()));
        domain.setCompletedAt(toInstant(po.getCompletedAt()));

        return domain;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
