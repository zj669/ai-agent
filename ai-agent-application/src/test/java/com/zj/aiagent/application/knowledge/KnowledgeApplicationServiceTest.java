package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDataset;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.port.FileStorageService;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDatasetRepository;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * KnowledgeApplicationService 单元测试
 * 
 * 测试策略：使用 Mockito 模拟所有依赖，验证业务编排逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("知识库应用服务测试")
class KnowledgeApplicationServiceTest {

    @Mock
    private KnowledgeDatasetRepository datasetRepository;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AsyncDocumentProcessor asyncDocumentProcessor;

    @InjectMocks
    private KnowledgeApplicationService knowledgeApplicationService;

    private static final String BUCKET_NAME = "knowledge-files";
    private static final Long USER_ID = 1L;
    private static final Long AGENT_ID = 100L;

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段
        ReflectionTestUtils.setField(knowledgeApplicationService, "bucketName", BUCKET_NAME);
    }

    // ========== 知识库管理测试 ==========

    @Nested
    @DisplayName("创建知识库")
    class CreateDatasetTests {

        @Test
        @DisplayName("成功创建知识库")
        void shouldCreateDatasetSuccessfully() {
            // Given
            String name = "产品文档库";
            String description = "存放产品手册";

            given(datasetRepository.save(any(KnowledgeDataset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            KnowledgeDataset result = knowledgeApplicationService.createDataset(
                    name, description, USER_ID, AGENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDatasetId()).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getAgentId()).isEqualTo(AGENT_ID);
            assertThat(result.getDocumentCount()).isEqualTo(0);
            assertThat(result.getTotalChunks()).isEqualTo(0);

            verify(datasetRepository).save(any(KnowledgeDataset.class));
        }

        @Test
        @DisplayName("创建知识库时 agentId 可为空")
        void shouldCreateDatasetWithNullAgentId() {
            // Given
            given(datasetRepository.save(any(KnowledgeDataset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            KnowledgeDataset result = knowledgeApplicationService.createDataset(
                    "测试库", null, USER_ID, null);

            // Then
            assertThat(result.getAgentId()).isNull();
        }
    }

    @Nested
    @DisplayName("查询知识库")
    class QueryDatasetTests {

        @Test
        @DisplayName("按用户ID查询知识库列表")
        void shouldListDatasetsByUser() {
            // Given
            KnowledgeDataset dataset = createMockDataset("ds-1");
            given(datasetRepository.findByUserId(USER_ID))
                    .willReturn(List.of(dataset));

            // When
            List<KnowledgeDataset> result = knowledgeApplicationService.listDatasetsByUser(USER_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDatasetId()).isEqualTo("ds-1");
        }

        @Test
        @DisplayName("查询知识库详情 - 存在")
        void shouldGetDatasetWhenExists() {
            // Given
            String datasetId = "ds-123";
            KnowledgeDataset dataset = createMockDataset(datasetId);
            given(datasetRepository.findById(datasetId))
                    .willReturn(Optional.of(dataset));

            // When
            KnowledgeDataset result = knowledgeApplicationService.getDataset(datasetId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDatasetId()).isEqualTo(datasetId);
        }

        @Test
        @DisplayName("查询知识库详情 - 不存在时抛异常")
        void shouldThrowExceptionWhenDatasetNotFound() {
            // Given
            String datasetId = "not-exist";
            given(datasetRepository.findById(datasetId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> knowledgeApplicationService.getDataset(datasetId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("知识库不存在");
        }
    }

    @Nested
    @DisplayName("删除知识库")
    class DeleteDatasetTests {

        @Test
        @DisplayName("删除知识库及其所有文档")
        void shouldDeleteDatasetAndAllDocuments() {
            // Given
            String datasetId = "ds-to-delete";
            KnowledgeDataset dataset = createMockDataset(datasetId);
            KnowledgeDocument document = createMockDocument("doc-1", datasetId);

            given(datasetRepository.findById(datasetId))
                    .willReturn(Optional.of(dataset));
            given(documentRepository.findByDatasetId(eq(datasetId), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(document)));

            // When
            knowledgeApplicationService.deleteDataset(datasetId);

            // Then
            verify(asyncDocumentProcessor).deleteDocumentVectors("doc-1");
            verify(fileStorageService).delete(eq(BUCKET_NAME), anyString());
            verify(documentRepository).deleteById("doc-1");
            verify(datasetRepository).deleteById(datasetId);
        }
    }

    // ========== 文档管理测试 ==========

    @Nested
    @DisplayName("上传文档")
    class UploadDocumentTests {

        @Test
        @DisplayName("成功上传文档并触发异步处理")
        void shouldUploadDocumentAndTriggerAsyncProcessing() throws Exception {
            // Given
            String datasetId = "ds-1";
            KnowledgeDataset dataset = createMockDataset(datasetId);
            MultipartFile mockFile = mock(MultipartFile.class);

            given(mockFile.getOriginalFilename()).willReturn("test.pdf");
            given(mockFile.getSize()).willReturn(1024L);
            given(mockFile.getContentType()).willReturn("application/pdf");
            given(mockFile.getInputStream()).willReturn(new ByteArrayInputStream(new byte[0]));

            given(datasetRepository.findById(datasetId))
                    .willReturn(Optional.of(dataset));
            given(fileStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong()))
                    .willReturn("http://localhost:9000/knowledge-files/ds-1/doc-1/test.pdf");
            given(documentRepository.save(any(KnowledgeDocument.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(datasetRepository.save(any(KnowledgeDataset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ChunkingConfig config = ChunkingConfig.builder()
                    .chunkSize(500)
                    .chunkOverlap(50)
                    .build();

            // When
            KnowledgeDocument result = knowledgeApplicationService.uploadDocument(
                    datasetId, mockFile, config);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDocumentId()).isNotNull();
            assertThat(result.getDatasetId()).isEqualTo(datasetId);
            assertThat(result.getFilename()).isEqualTo("test.pdf");

            // 验证异步处理被触发
            verify(asyncDocumentProcessor).processDocumentAsync(any(KnowledgeDocument.class));
            // 验证知识库统计更新
            verify(datasetRepository, times(1)).save(any(KnowledgeDataset.class));
        }

        @Test
        @DisplayName("上传文档时使用默认分块配置")
        void shouldUseDefaultChunkingConfigWhenNull() throws Exception {
            // Given
            String datasetId = "ds-1";
            KnowledgeDataset dataset = createMockDataset(datasetId);
            MultipartFile mockFile = mock(MultipartFile.class);

            given(mockFile.getOriginalFilename()).willReturn("test.md");
            given(mockFile.getSize()).willReturn(512L);
            given(mockFile.getContentType()).willReturn("text/markdown");
            given(mockFile.getInputStream()).willReturn(new ByteArrayInputStream(new byte[0]));

            given(datasetRepository.findById(datasetId)).willReturn(Optional.of(dataset));
            given(fileStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong()))
                    .willReturn("http://localhost:9000/knowledge-files/ds-1/doc-1/test.md");
            given(documentRepository.save(any(KnowledgeDocument.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(datasetRepository.save(any(KnowledgeDataset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            KnowledgeDocument result = knowledgeApplicationService.uploadDocument(
                    datasetId, mockFile, null); // null config

            // Then
            assertThat(result.getChunkingConfig()).isNotNull();
        }
    }

    @Nested
    @DisplayName("查询文档")
    class QueryDocumentTests {

        @Test
        @DisplayName("分页查询文档列表")
        void shouldListDocumentsWithPagination() {
            // Given
            String datasetId = "ds-1";
            KnowledgeDocument doc1 = createMockDocument("doc-1", datasetId);
            KnowledgeDocument doc2 = createMockDocument("doc-2", datasetId);
            Page<KnowledgeDocument> page = new PageImpl<>(List.of(doc1, doc2));

            given(documentRepository.findByDatasetId(eq(datasetId), any(PageRequest.class)))
                    .willReturn(page);

            // When
            Page<KnowledgeDocument> result = knowledgeApplicationService.listDocuments(
                    datasetId, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("查询文档详情")
        void shouldGetDocument() {
            // Given
            String documentId = "doc-123";
            KnowledgeDocument document = createMockDocument(documentId, "ds-1");
            given(documentRepository.findById(documentId))
                    .willReturn(Optional.of(document));

            // When
            KnowledgeDocument result = knowledgeApplicationService.getDocument(documentId);

            // Then
            assertThat(result.getDocumentId()).isEqualTo(documentId);
        }

        @Test
        @DisplayName("文档不存在时抛异常")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            given(documentRepository.findById("not-exist"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> knowledgeApplicationService.getDocument("not-exist"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("文档不存在");
        }
    }

    @Nested
    @DisplayName("删除文档")
    class DeleteDocumentTests {

        @Test
        @DisplayName("删除文档并更新知识库统计")
        void shouldDeleteDocumentAndUpdateDatasetStats() {
            // Given
            String documentId = "doc-to-delete";
            String datasetId = "ds-1";
            KnowledgeDocument document = createMockDocument(documentId, datasetId);
            document.setTotalChunks(10);
            KnowledgeDataset dataset = createMockDataset(datasetId);
            dataset.setTotalChunks(20);

            given(documentRepository.findById(documentId))
                    .willReturn(Optional.of(document));
            given(datasetRepository.findById(datasetId))
                    .willReturn(Optional.of(dataset));

            // When
            knowledgeApplicationService.deleteDocument(documentId);

            // Then
            verify(asyncDocumentProcessor).deleteDocumentVectors(documentId);
            verify(fileStorageService).delete(eq(BUCKET_NAME), anyString());
            verify(documentRepository).deleteById(documentId);

            // 验证知识库统计更新
            ArgumentCaptor<KnowledgeDataset> captor = ArgumentCaptor.forClass(KnowledgeDataset.class);
            verify(datasetRepository).save(captor.capture());
            // 20 - 10 = 10
            assertThat(captor.getValue().getTotalChunks()).isEqualTo(10);
        }
    }

    // ========== Helper Methods ==========

    private KnowledgeDataset createMockDataset(String datasetId) {
        return KnowledgeDataset.builder()
                .datasetId(datasetId)
                .name("Test Dataset")
                .description("Test Description")
                .userId(USER_ID)
                .agentId(AGENT_ID)
                .documentCount(1)
                .totalChunks(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private KnowledgeDocument createMockDocument(String documentId, String datasetId) {
        return KnowledgeDocument.builder()
                .documentId(documentId)
                .datasetId(datasetId)
                .filename("test.pdf")
                .fileUrl("http://localhost:9000/" + BUCKET_NAME + "/" + datasetId + "/" + documentId + "/test.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .status(DocumentStatus.COMPLETED)
                .totalChunks(5)
                .processedChunks(5)
                .chunkingConfig(ChunkingConfig.builder().chunkSize(500).chunkOverlap(50).build())
                .uploadedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
    }
}
