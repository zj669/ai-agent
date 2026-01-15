# 知识库管理 API - 开发任务清单

## 📋 任务总览

本清单将知识库管理功能拆解为 **原子性任务**，严格遵循 DDD 分层原则（Domain → Infrastructure → Application → Interface），确保每个任务独立可验证。

**预计总耗时**: ~12-15 小时

---

## Phase 0: 依赖准备

### Task 0.1: 添加 MinIO 依赖
**路径**: `pom.xml`  
**工作内容**:
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```
**预计耗时**: 5 分钟

---

### Task 0.2: 添加 Spring AI 依赖
**路径**: `pom.xml`  
**工作内容**:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```
**预计耗时**: 5 分钟

---

### Task 0.3: 配置异步线程池
**路径**: `ai-agent-interfaces/src/main/resources/application.yml`  
**工作内容**:
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 10
        queue-capacity: 100

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: knowledge-files
```
**预计耗时**: 10 分钟

---

## Phase 1: Domain Layer（领域层）

### Task 1.1: 创建 DocumentStatus 枚举
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/valobj/DocumentStatus.java`  
**工作内容**:
```java
public enum DocumentStatus {
    PENDING,      // 已上传，等待处理
    PROCESSING,   // 正在解析向量化
    COMPLETED,    // 完成
    FAILED        // 失败
}
```
**预计耗时**: 5 分钟

---

### Task 1.2: 创建 ChunkingConfig 值对象
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/valobj/ChunkingConfig.java`  
**工作内容**:
```java
@Data
@Builder
public class ChunkingConfig {
    private Integer chunkSize = 500;
    private Integer chunkOverlap = 50;
}
```
**预计耗时**: 5 分钟

---

### Task 1.3: 创建 KnowledgeDataset 聚合根
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDataset.java`  
**工作内容**:
- 定义字段：`datasetId`, `name`, `description`, `userId`, `agentId`, `documentCount`, `totalChunks`
- 领域行为：`addDocument()`, `removeDocument()`, `buildMetadataFilter()`
**预计耗时**: 20 分钟

---

### Task 1.4: 创建 KnowledgeDocument 聚合根
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDocument.java`  
**工作内容**:
- 定义字段：`documentId`, `datasetId`, `filename`, `fileUrl`, `status`, `totalChunks`, `processedChunks`, `errorMessage`
- 领域行为：`startProcessing()`, `updateProgress()`, `markCompleted()`, `markFailed()`
**预计耗时**: 20 分钟

---

### Task 1.5: 创建 KnowledgeDatasetRepository 接口
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/repository/KnowledgeDatasetRepository.java`  
**工作内容**:
```java
public interface KnowledgeDatasetRepository {
    KnowledgeDataset save(KnowledgeDataset dataset);
    Optional<KnowledgeDataset> findById(String datasetId);
    List<KnowledgeDataset> findByUserId(Long userId);
    void deleteById(String datasetId);
}
```
**预计耗时**: 10 分钟

---

### Task 1.6: 创建 KnowledgeDocumentRepository 接口
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/repository/KnowledgeDocumentRepository.java`  
**工作内容**:
```java
public interface KnowledgeDocumentRepository {
    KnowledgeDocument save(KnowledgeDocument document);
    Optional<KnowledgeDocument> findById(String documentId);
    Page<KnowledgeDocument> findByDatasetId(String datasetId, Pageable pageable);
    void deleteById(String documentId);
}
```
**预计耗时**: 10 分钟

---

### Task 1.7: 创建 FileStorageService 接口（Port）
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/port/FileStorageService.java`  
**工作内容**:
```java
public interface FileStorageService {
    String upload(String bucketName, String objectName, InputStream inputStream, long size);
    InputStream download(String bucketName, String objectName);
    void delete(String bucketName, String objectName);
}
```
**预计耗时**: 10 分钟

---

### Task 1.8: 创建 KnowledgeRetrievalService 领域服务接口
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/service/KnowledgeRetrievalService.java`  
**工作内容**:
```java
public interface KnowledgeRetrievalService {
    List<String> retrieve(Long agentId, String query, int topK);
    List<String> retrieveByDataset(String datasetId, String query, int topK);
}
```
**预计耗时**: 10 分钟

---

## Phase 2: Infrastructure Layer（基础设施层）

### Task 2.1: 实现 MinIOFileStorageService
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/MinIOFileStorageService.java`  
**工作内容**:
- 注入 `MinioClient` Bean
- 实现 `upload()`, `download()`, `delete()`
- 异常处理和日志记录
**预计耗时**: 45 分钟

---

### Task 2.2: 配置 MinioClient Bean
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/MinIOConfig.java`  
**工作内容**:
```java
@Configuration
public class MinIOConfig {
    @Bean
    public MinioClient minioClient(@Value("${minio.endpoint}") String endpoint,
                                     @Value("${minio.access-key}") String accessKey,
                                     @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```
**预计耗时**: 15 分钟

---

### Task 2.3: 创建 SpringAIDocumentReaderAdapter
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/SpringAIDocumentReaderAdapter.java`  
**工作内容**:
- 封装 `TikaDocumentReader`
- 实现 `readDocuments(Resource resource)` 方法
**预计耗时**: 20 分钟

---

### Task 2.4: 创建 SpringAITextSplitterAdapter
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/SpringAITextSplitterAdapter.java`  
**工作内容**:
- 封装 `TokenTextSplitter`
- 实现 `split(List<Document> documents, int chunkSize, int overlap)`
**预计耗时**: 20 分钟

---

### Task 2.5: 扩展 VectorStore 支持 Spring AI SearchRequest
**路径**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java`  
**工作内容**:
- 新增方法：`List<String> search(SearchRequest request)`
- 保留原有的 `search(String query, Long agentId, int topK)` 作为便捷方法
- 新增方法：`void add(List<Document> documents)` （支持批量存储带 Metadata）

> **✅ 架构优化**: 直接复用 Spring AI 的 `SearchRequest` 和 `Filter.Expression`，避免手写字符串拼接。

**预计耗时**: 15 分钟

---

### Task 2.6: 实现 MilvusVectorStore 的 Spring AI SearchRequest 支持
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreImpl.java`  
**工作内容**:
- 实现 `search(SearchRequest request)` 方法
- **使用 Spring AI FilterExpressionBuilder**：
  ```java
  // 示例：构建过滤条件
  Filter.Expression filter = Filter.builder()
      .eq("agentId", agentId)
      .eq("datasetId", datasetId)
      .build();
  
  SearchRequest searchRequest = SearchRequest.builder()
      .query(queryText)
      .topK(topK)
      .filterExpression(filter)
      .build();
  ```
- 实现 `add(List<Document> documents)` 方法，存储时附加 Metadata
- 处理 Milvus 的 Filter Expression 转换

> **✅ 关键优化**: 使用 `FilterExpressionBuilder` 而非字符串拼接，提高安全性和兼容性。

**预计耗时**: 1 小时

---

### Task 2.7: 创建 MySQL 实体类 KnowledgeDatasetPO
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/po/KnowledgeDatasetPO.java`  
**工作内容**:
```java
@Entity
@Table(name = "knowledge_dataset")
public class KnowledgeDatasetPO {
    @Id
    private String datasetId;
    private String name;
    private String description;
    private Long userId;
    private Long agentId;
    // ...
}
```
**预计耗时**: 15 分钟

---

### Task 2.8: 创建 MySQL 实体类 KnowledgeDocumentPO
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/po/KnowledgeDocumentPO.java`  
**工作内容**:
```java
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocumentPO {
    @Id
    private String documentId;
    private String datasetId;
    // ...
}
```
**预计耗时**: 15 分钟

---

### Task 2.9: 实现 MySQLKnowledgeDatasetRepository
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/MySQLKnowledgeDatasetRepository.java`  
**工作内容**:
- 注入 JpaRepository
- 实现 Domain Repository 接口
- 实现 PO ↔ Domain Entity 转换
**预计耗时**: 30 分钟

---

### Task 2.10: 实现 MySQLKnowledgeDocumentRepository
**路径**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/MySQLKnowledgeDocumentRepository.java`  
**工作内容**:
- 注入 JpaRepository
- 实现 Domain Repository 接口
- 实现分页查询
**预计耗时**: 30 分钟

---

## Phase 3: Application Layer（应用层）

### Task 3.1: 创建 KnowledgeApplicationService
**路径**: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java`  
**工作内容**:
- `createDataset(CreateDatasetCommand cmd)`
- `getDatasetList(Long userId)`
- `deleteDataset(String datasetId)`
- `uploadDocument(MultipartFile file, String datasetId, ChunkingConfig config)`
- `getDocumentList(String datasetId, Pageable pageable)`
- `deleteDocument(String documentId)`
**预计耗时**: 1.5 小时

---

### Task 3.2: 创建 AsyncDocumentProcessor
**路径**: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java`  
**工作内容**:
- 使用 `@Async` 注解
- `processDocumentAsync(String documentId)` 方法
- 调用 Spring AI Adapter 解析和分块
- 调用 VectorStore 存储向量
- 更新文档状态和进度
- 异常处理和失败标记
**预计耗时**: 2 小时

---

### Task 3.3: 实现 KnowledgeRetrievalServiceImpl
**路径**: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java`  
**工作内容**:
- 实现 `retrieve(Long agentId, String query, int topK)`
- 构造 Metadata Filter：`{ "agentId": xxx }`
- 调用 `vectorStore.searchWithFilter()`
**预计耗时**: 30 分钟

---

### Task 3.4: 配置 @EnableAsync
**路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java`  
**工作内容**:
```java
@SpringBootApplication
@EnableAsync
public class AiAgentApplication {
    // ...
}
```
**预计耗时**: 5 分钟

---

## Phase 4: Interface Layer（接口层）

### Task 4.1: 创建 DTOs
**路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/dto/`  
**工作内容**:
- `DatasetCreateRequest`
- `DatasetDTO`
- `DocumentUploadResponse`
- `DocumentListDTO`
- `SearchRequest`
- `SearchResultDTO`
**预计耗时**: 30 分钟

---

### Task 4.2: 创建 KnowledgeController
**路径**: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/KnowledgeController.java`  
**工作内容**:
- `POST /api/knowledge/dataset` - 创建知识库
- `GET /api/knowledge/dataset/list` - 查询知识库列表
- `DELETE /api/knowledge/dataset/{id}` - 删除知识库
- `POST /api/knowledge/document/upload` - 上传文档
- `GET /api/knowledge/document/list` - 文档列表
- `GET /api/knowledge/document/{id}` - 文档详情
- `DELETE /api/knowledge/document/{id}` - 删除文档
- `POST /api/knowledge/search` - 测试检索
**预计耗时**: 1.5 小时

---

## Phase 5: 数据库脚本

### Task 5.1: 创建数据库表 DDL
**路径**: `ai-agent-infrastructure/src/main/resources/db/migration/`  
**工作内容**:
```sql
CREATE TABLE knowledge_dataset (
    dataset_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT,
    agent_id BIGINT,
    document_count INT DEFAULT 0,
    total_chunks INT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE knowledge_document (
    document_id VARCHAR(64) PRIMARY KEY,
    dataset_id VARCHAR(64) NOT NULL,
    filename VARCHAR(255),
    file_url VARCHAR(512),
    file_size BIGINT,
    content_type VARCHAR(64),
    status VARCHAR(32),
    total_chunks INT,
    processed_chunks INT,
    error_message TEXT,
    uploaded_at TIMESTAMP,
    completed_at TIMESTAMP
);
```
**预计耗时**: 20 分钟

---

## Phase 6: 编译与验证

### Task 6.1: 编译验证
**命令**: `mvn clean compile -DskipTests > .business/logs/Compile_Knowledge_{Timestamp}.log 2>&1`  
**预计耗时**: 5 分钟

---

### Task 6.2: 单元测试（可选）
**路径**: `ai-agent-application/src/test/java/com/zj/aiagent/application/knowledge/`  
**工作内容**:
- `KnowledgeApplicationServiceTest`
- `AsyncDocumentProcessorTest`
**预计耗时**: 1 小时

---

## 依赖关系图

```
Phase 0 (依赖准备)
   ↓
Phase 1 (Domain Layer)
   ↓
Phase 2 (Infrastructure Layer) ← 依赖 Phase 1
   ├── MinIO 实现
   ├── Spring AI Adapter
   ├── VectorStore 扩展
   └── MySQL Repository
   ↓
Phase 3 (Application Layer) ← 依赖 Phase 1, 2
   ├── KnowledgeApplicationService
   ├── AsyncDocumentProcessor
   └── KnowledgeRetrievalServiceImpl
   ↓
Phase 4 (Interface Layer) ← 依赖 Phase 3
   └── KnowledgeController
   ↓
Phase 5 (数据库脚本)
   ↓
Phase 6 (编译与验证)
```

---

## 总结

- **总任务数**: 36 个原子任务
- **预计总耗时**: ~12-15 小时
- **关键路径**: Phase 2（Infrastructure Layer）耗时最长
- **并行机会**: Domain 层的多个实体和接口可并行开发

---

> **⛔ STOP POINT**: 任务拆解完成。请确认任务是否合理？（输入 '开始执行' 进入编码阶段）
