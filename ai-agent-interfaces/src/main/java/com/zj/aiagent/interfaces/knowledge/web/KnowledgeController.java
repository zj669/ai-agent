package com.zj.aiagent.interfaces.knowledge.web;

import com.zj.aiagent.application.knowledge.KnowledgeApplicationService;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDataset;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingStrategy;
import com.zj.aiagent.interfaces.knowledge.dto.KnowledgeDTO;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库管理 Controller
 * 提供知识库、文档的 CRUD 以及检索功能
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeApplicationService knowledgeApplicationService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(
            ZoneId.systemDefault()
        );

    // ========== 知识库管理 ==========

    /**
     * 创建知识库
     */
    @PostMapping("/dataset")
    public Response<KnowledgeDTO.DatasetResp> createDataset(
        @Validated @RequestBody KnowledgeDTO.DatasetCreateReq req
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        KnowledgeDataset dataset = knowledgeApplicationService.createDataset(
            req.getName(),
            req.getDescription(),
            userId,
            req.getAgentId()
        );

        return Response.success(toDatasetResp(dataset));
    }

    /**
     * 查询知识库列表
     */
    @GetMapping("/dataset/list")
    public Response<List<KnowledgeDTO.DatasetResp>> listDatasets() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        List<KnowledgeDataset> datasets =
            knowledgeApplicationService.listDatasetsByUser(userId);

        List<KnowledgeDTO.DatasetResp> respList = datasets
            .stream()
            .map(this::toDatasetResp)
            .collect(Collectors.toList());

        return Response.success(respList);
    }

    /**
     * 查询知识库详情
     */
    @GetMapping("/dataset/{id}")
    public Response<KnowledgeDTO.DatasetResp> getDataset(
        @PathVariable("id") String datasetId
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        KnowledgeDataset dataset = knowledgeApplicationService.getDataset(
            datasetId,
            userId
        );
        return Response.success(toDatasetResp(dataset));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/dataset/{id}")
    public Response<Void> deleteDataset(@PathVariable("id") String datasetId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        knowledgeApplicationService.deleteDataset(datasetId, userId);
        return Response.success();
    }

    // ========== 文档管理 ==========

    /**
     * 上传文档
     */
    @PostMapping("/document/upload")
    public Response<KnowledgeDTO.DocumentResp> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("datasetId") String datasetId,
        @RequestParam(
            value = "chunkStrategy",
            required = false
        ) String chunkStrategy,
        @RequestParam(
            value = "chunkSize",
            defaultValue = "500"
        ) Integer chunkSize,
        @RequestParam(
            value = "chunkOverlap",
            defaultValue = "50"
        ) Integer chunkOverlap,
        @RequestParam(
            value = "maxChunkSize",
            required = false
        ) Integer maxChunkSize,
        @RequestParam(
            value = "minChunkSize",
            required = false
        ) Integer minChunkSize,
        @RequestParam(
            value = "similarityThreshold",
            required = false
        ) Double similarityThreshold,
        @RequestParam(
            value = "mergeSmallChunks",
            required = false
        ) Boolean mergeSmallChunks
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        log.info(
            "接收文档上传请求: datasetId={}, filename={}, size={}",
            datasetId,
            file.getOriginalFilename(),
            file.getSize()
        );

        ChunkingConfig config = ChunkingConfig.builder()
            .strategy(ChunkingStrategy.fromValue(chunkStrategy))
            .chunkSize(chunkSize)
            .chunkOverlap(chunkOverlap)
            .maxChunkSize(maxChunkSize)
            .minChunkSize(minChunkSize)
            .similarityThreshold(similarityThreshold)
            .mergeSmallChunks(mergeSmallChunks)
            .build()
            .normalized();
        config.validate();

        KnowledgeDocument document = knowledgeApplicationService.uploadDocument(
            datasetId,
            file,
            config,
            userId
        );

        return Response.success(toDocumentResp(document));
    }

    /**
     * 查询文档列表
     */
    @GetMapping("/document/list")
    public Response<Page<KnowledgeDTO.DocumentResp>> listDocuments(
        @RequestParam("datasetId") String datasetId,
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        Page<KnowledgeDocument> documentPage =
            knowledgeApplicationService.listDocuments(
                datasetId,
                PageRequest.of(page, size),
                userId
            );

        Page<KnowledgeDTO.DocumentResp> respPage = documentPage.map(
            this::toDocumentResp
        );

        return Response.success(respPage);
    }

    /**
     * 查询文档详情
     */
    @GetMapping("/document/{id}")
    public Response<KnowledgeDTO.DocumentResp> getDocument(
        @PathVariable("id") String documentId
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        KnowledgeDocument document = knowledgeApplicationService.getDocument(
            documentId,
            userId
        );
        return Response.success(toDocumentResp(document));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/document/{id}")
    public Response<Void> deleteDocument(
        @PathVariable("id") String documentId
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        knowledgeApplicationService.deleteDocument(documentId, userId);
        return Response.success();
    }

    /**
     * 重试处理失败文档
     */
    @PostMapping("/document/{id}/retry")
    public Response<KnowledgeDTO.DocumentResp> retryDocument(
        @PathVariable("id") String documentId
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        KnowledgeDocument document = knowledgeApplicationService.retryDocument(
            documentId,
            userId
        );
        return Response.success(toDocumentResp(document));
    }

    // ========== 知识检索 ==========

    /**
     * 测试检索
     */
    @PostMapping("/search")
    public Response<List<String>> search(
        @Validated @RequestBody KnowledgeDTO.SearchReq req
    ) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        // 先校验知识库归属，避免跨用户检索
        knowledgeApplicationService.getDataset(req.getDatasetId(), userId);

        log.info(
            "知识检索: datasetId={}, query={}, topK={}",
            req.getDatasetId(),
            req.getQuery().length() > 50
                ? req.getQuery().substring(0, 50) + "..."
                : req.getQuery(),
            req.getTopK()
        );

        List<String> results = knowledgeRetrievalService.retrieveByDataset(
            req.getDatasetId(),
            req.getQuery(),
            req.getTopK()
        );

        return Response.success(results);
    }

    // ========== 转换方法 ==========

    private KnowledgeDTO.DatasetResp toDatasetResp(KnowledgeDataset dataset) {
        KnowledgeDTO.DatasetResp resp = new KnowledgeDTO.DatasetResp();
        resp.setDatasetId(dataset.getDatasetId());
        resp.setName(dataset.getName());
        resp.setDescription(dataset.getDescription());
        resp.setUserId(dataset.getUserId());
        resp.setAgentId(dataset.getAgentId());
        resp.setDocumentCount(dataset.getDocumentCount());
        resp.setTotalChunks(dataset.getTotalChunks());
        resp.setCreatedAt(
            dataset.getCreatedAt() != null
                ? FORMATTER.format(dataset.getCreatedAt())
                : null
        );
        resp.setUpdatedAt(
            dataset.getUpdatedAt() != null
                ? FORMATTER.format(dataset.getUpdatedAt())
                : null
        );
        return resp;
    }

    private KnowledgeDTO.DocumentResp toDocumentResp(
        KnowledgeDocument document
    ) {
        KnowledgeDTO.DocumentResp resp = new KnowledgeDTO.DocumentResp();
        resp.setDocumentId(document.getDocumentId());
        resp.setDatasetId(document.getDatasetId());
        resp.setFilename(document.getFilename());
        resp.setFileUrl(document.getFileUrl());
        resp.setFileSize(document.getFileSize());
        resp.setContentType(document.getContentType());
        resp.setStatus(
            document.getStatus() != null ? document.getStatus().name() : null
        );
        resp.setTotalChunks(document.getTotalChunks());
        resp.setProcessedChunks(document.getProcessedChunks());
        resp.setErrorMessage(document.getErrorMessage());
        resp.setChunkStrategy(
            document.getChunkingConfig() != null &&
                document.getChunkingConfig().getStrategy() != null
                ? document.getChunkingConfig().getStrategy().name()
                : ChunkingStrategy.FIXED.name()
        );
        resp.setUploadedAt(
            document.getUploadedAt() != null
                ? FORMATTER.format(document.getUploadedAt())
                : null
        );
        resp.setCompletedAt(
            document.getCompletedAt() != null
                ? FORMATTER.format(document.getCompletedAt())
                : null
        );
        return resp;
    }
}
