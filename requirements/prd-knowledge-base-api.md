# PRD: 知识库管理 API

## 1. 背景与目标

### 1.1 业务背景
当前系统已实现长期记忆（LTM）的**读取**能力（通过 `VectorStore.search()`），但缺乏**写入和管理**能力。用户无法：
- 上传知识库文档（PDF/Markdown/TXT）
- 查看文档解析进度
- 删除过时的知识

### 1.2 目标
提供完整的知识库生命周期管理 API，支持文档上传、向量化、检索、删除。

---

## 2. 功能需求

### 2.1 核心功能
| 功能 | 优先级 | 描述 |
|------|--------|------|
| 创建知识库 | P0 | 创建独立的知识库（数据集） |
| 上传文档 | P0 | 上传文件并触发向量化 |
| 文档列表 | P0 | 查看上传的文档及状态 |
| 删除文档 | P0 | 删除文档及向量索引 |
| 搜索知识库 | P1 | 直接通过 API 测试检索效果 |

### 2.2 用户故事
- **作为产品经理**，我希望为 Agent 上传公司产品手册，让它能回答产品相关问题。
- **作为开发者**，我希望查看文档的向量化进度，确保知识库可用。

---

## 3. API 设计

### 3.1 创建知识库

**POST** `/api/knowledge/dataset`

#### 请求体
```json
{
  "name": "产品手册",
  "description": "2024年产品文档合集",
  "agentId": 1,  // 可选，绑定到特定 Agent
  "collectionName": "product_manual_2024"  // Milvus Collection
}
```

#### 响应
```json
{
  "datasetId": "dataset-123",
  "collectionName": "product_manual_2024",
  "createdAt": "2026-01-12T15:00:00Z"
}
```

---

### 3.2 上传文档

**POST** `/api/knowledge/document/upload`

#### 请求 (multipart/form-data)
- `file`: 文件（支持 PDF/DOCX/MD/TXT，最大 10MB）
- `datasetId`: 知识库 ID
- `chunkSize`: 分块大小（可选，默认 500）
- `chunkOverlap`: 重叠大小（可选，默认 50）

#### 响应
```json
{
  "documentId": "doc-456",
  "filename": "产品手册.pdf",
  "status": "PROCESSING",  // enum: PROCESSING, COMPLETED, FAILED
  "totalChunks": 0,
  "processedChunks": 0,
  "uploadedAt": "2026-01-12T15:05:00Z"
}
```

---

### 3.3 文档列表

**GET** `/api/knowledge/document/list`

#### Query 参数
- `datasetId`: 知识库 ID
- `status`: 过滤状态（可选）
- `page`, `size`: 分页

#### 响应
```json
{
  "total": 15,
  "list": [
    {
      "documentId": "doc-456",
      "filename": "产品手册.pdf",
      "status": "COMPLETED",
      "totalChunks": 120,
      "processedChunks": 120,
      "uploadedAt": "2026-01-12T15:05:00Z",
      "completedAt": "2026-01-12T15:10:00Z"
    }
  ]
}
```

---

### 3.4 删除文档

**DELETE** `/api/knowledge/document/{documentId}`

#### 响应
```json
{
  "success": true,
  "message": "Document and embeddings deleted"
}
```

---

### 3.5 搜索知识库（测试接口）

**POST** `/api/knowledge/search`

#### 请求体
```json
{
  "datasetId": "dataset-123",
  "query": "产品价格是多少",
  "topK": 5
}
```

#### 响应
```json
{
  "results": [
    {
      "content": "产品定价为 999 元/年...",
      "score": 0.92,
      "metadata": {
        "documentId": "doc-456",
        "filename": "产品手册.pdf",
        "page": 12
      }
    }
  ]
}
```

---

## 4. 领域模型

### 4.1 知识库（Dataset）实体
```java
@Entity
public class KnowledgeDataset {
    private String id;
    private String name;
    private String description;
    private Long agentId;  // 可选，绑定到特定 Agent
    private String collectionName;  // Milvus Collection
    private Instant createdAt;
}
```

### 4.2 文档（Document）实体
```java
@Entity
public class KnowledgeDocument {
    private String id;
    private String datasetId;
    private String filename;
    private DocumentStatus status;  // PROCESSING, COMPLETED, FAILED
    private Integer totalChunks;
    private Integer processedChunks;
    private String errorMessage;
    private Instant uploadedAt;
    private Instant completedAt;
}
```

---

## 5. 技术实现建议

### 5.1 文档解析流程
1. 用户上传文件 → 保存到对象存储（如 MinIO/OSS）
2. 异步任务读取文件内容
3. 根据文件类型选择解析器（PDF: PDFBox, DOCX: Apache POI）
4. 分块（Chunking）→ 调用 `EmbeddingModel` 生成向量
5. 批量写入 Milvus（`VectorStore.storeBatch()`）
6. 更新文档状态

### 5.2 Controller 层
创建 `KnowledgeController`。

### 5.3 应用服务
- `KnowledgeApplicationService`
  - `uploadDocument(MultipartFile file, String datasetId)`
  - `getDocumentList(String datasetId, Pageable pageable)`
  - `deleteDocument(String documentId)`

### 5.4 基础设施层
- `DocumentParser` 接口及实现（PDF/DOCX/Markdown）
- `ChunkingStrategy` 接口（固定长度/句子分割）

---

## 6. 验收标准

- [ ] 支持上传 PDF/Markdown/TXT 文件
- [ ] 文档解析成功后，向量能写入 Milvus
- [ ] 通过搜索 API 能检索到上传的内容
- [ ] 删除文档后，向量索引同步删除
- [ ] 异步任务失败时，状态标记为 FAILED 并记录错误

---

## 7. 后续扩展

- 支持更多文件格式（DOCX, HTML, CSV）
- 支持 URL 导入（爬取网页）
- 文档分块策略可配置（句子级/段落级）
- 知识库版本管理（支持更新和回滚）
