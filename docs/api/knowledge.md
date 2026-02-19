# 知识库模块 API 文档

## 概述

知识库模块提供文档管理和向量检索功能，支持多种文档格式的上传、解析、分块和向量化存储。

**Base URL**: `/api/knowledge`

## 认证

所有接口需要用户登录，通过 `UserContext` 获取当前用户 ID。

**请求头格式**:
```
Authorization: Bearer {token}
```

**错误响应**:
- `401 Unauthorized`: Token 无效或已过期
- `403 Forbidden`: 无权限访问该资源

---

## 数据模型

### KnowledgeDataset (知识库)

| 字段 | 类型 | 说明 |
|------|------|------|
| datasetId | String | 知识库 ID (UUID) |
| name | String | 知识库名称 |
| description | String | 描述信息 |
| userId | Long | 所有者用户 ID |
| agentId | Long | 绑定的 Agent ID（可选） |
| documentCount | Integer | 文档数量 |
| totalChunks | Integer | 总分块数 |
| createdAt | String | 创建时间 (yyyy-MM-dd HH:mm:ss) |
| updatedAt | String | 更新时间 (yyyy-MM-dd HH:mm:ss) |

### KnowledgeDocument (文档)

| 字段 | 类型 | 说明 |
|------|------|------|
| documentId | String | 文档 ID (UUID) |
| datasetId | String | 所属知识库 ID |
| filename | String | 文件名 |
| fileUrl | String | MinIO 存储 URL |
| fileSize | Long | 文件大小（字节） |
| contentType | String | MIME 类型 |
| status | String | 处理状态: PENDING, PROCESSING, COMPLETED, FAILED |
| totalChunks | Integer | 总分块数 |
| processedChunks | Integer | 已处理分块数 |
| errorMessage | String | 错误信息（失败时） |
| uploadedAt | String | 上传时间 |
| completedAt | String | 完成时间 |

---

## API 接口

### 1. 创建知识库

**接口**: `POST /api/knowledge/dataset`

**请求体**:
```json
{
  "name": "技术文档库",
  "description": "存储技术文档和API参考",
  "agentId": 123
}
```

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| name | String | Body | 是 | 知识库名称 |
| description | String | Body | 否 | 描述信息 |
| agentId | Long | Body | 否 | 绑定的 Agent ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "datasetId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "技术文档库",
    "description": "存储技术文档和API参考",
    "userId": 1001,
    "agentId": 123,
    "documentCount": 0,
    "totalChunks": 0,
    "createdAt": "2026-02-10 14:30:00",
    "updatedAt": "2026-02-10 14:30:00"
  }
}
```

**错误码**:
- `401`: 未登录
- `400`: 参数验证失败

---

### 2. 查询知识库列表

**接口**: `GET /api/knowledge/dataset/list`

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "datasetId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "技术文档库",
      "description": "存储技术文档和API参考",
      "userId": 1001,
      "agentId": 123,
      "documentCount": 5,
      "totalChunks": 120,
      "createdAt": "2026-02-10 14:30:00",
      "updatedAt": "2026-02-10 15:45:00"
    }
  ]
}
```

**说明**: 仅返回当前用户创建的知识库

---

### 3. 查询知识库详情

**接口**: `GET /api/knowledge/dataset/{id}`

**路径参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | String | Path | 是 | 知识库 ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "datasetId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "技术文档库",
    "documentCount": 5,
    "totalChunks": 120
  }
}
```

**错误码**:
- `404`: 知识库不存在
- `403`: 无权访问（不是所有者）

---

### 4. 删除知识库

**接口**: `DELETE /api/knowledge/dataset/{id}`

**路径参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | String | Path | 是 | 知识库 ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**说明**:
- 级联删除所有文档、MinIO 文件和 Milvus 向量
- 使用分页删除，避免 OOM

**错误码**:
- `404`: 知识库不存在
- `403`: 无权删除（不是所有者）

---

### 5. 上传文档

**接口**: `POST /api/knowledge/document/upload`

**请求类型**: `multipart/form-data`

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| file | File | Form | 是 | 上传的文件 |
| datasetId | String | Form | 是 | 知识库 ID |
| chunkSize | Integer | Form | 否 | 分块大小，默认 500 tokens |
| chunkOverlap | Integer | Form | 否 | 分块重叠，默认 50 tokens |

**文件限制**:
- **支持格式**: PDF, DOC, DOCX, TXT, MD, CSV, XLS, XLSX
- **最大大小**: 50MB
- **安全检查**: 自动检测路径遍历攻击、MIME 类型验证

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": "660e8400-e29b-41d4-a716-446655440001",
    "datasetId": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "API文档.pdf",
    "fileSize": 2048576,
    "contentType": "application/pdf",
    "status": "PENDING",
    "totalChunks": 0,
    "processedChunks": 0,
    "uploadedAt": "2026-02-10 15:00:00"
  }
}
```

**处理流程**:
1. 文件上传到 MinIO
2. 创建文档记录（状态: PENDING）
3. 触发异步处理（解析 → 分块 → 向量化 → 存储到 Milvus）
4. 状态变更: PENDING → PROCESSING → COMPLETED / FAILED

**错误码**:
- `400`: 文件验证失败（类型不支持、大小超限、文件名非法）
- `403`: 无权上传到该知识库
- `500`: 上传失败

---

### 6. 查询文档列表

**接口**: `GET /api/knowledge/document/list`

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| datasetId | String | Query | 是 | 知识库 ID |
| page | Integer | Query | 否 | 页码（从 0 开始），默认 0 |
| size | Integer | Query | 否 | 每页数量，默认 20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "documentId": "660e8400-e29b-41d4-a716-446655440001",
        "datasetId": "550e8400-e29b-41d4-a716-446655440000",
        "filename": "API文档.pdf",
        "fileSize": 2048576,
        "status": "COMPLETED",
        "totalChunks": 25,
        "processedChunks": 25,
        "uploadedAt": "2026-02-10 15:00:00",
        "completedAt": "2026-02-10 15:02:30"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### 7. 查询文档详情

**接口**: `GET /api/knowledge/document/{id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | String | 文档 ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": "660e8400-e29b-41d4-a716-446655440001",
    "filename": "API文档.pdf",
    "status": "COMPLETED",
    "totalChunks": 25,
    "processedChunks": 25,
    "errorMessage": null
  }
}
```

**错误码**:
- `404`: 文档不存在
- `403`: 无权访问

---

### 8. 删除文档

**接口**: `DELETE /api/knowledge/document/{id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | String | 文档 ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**说明**:
- 级联删除 MinIO 文件和 Milvus 向量
- 更新知识库统计信息

**错误码**:
- `404`: 文档不存在
- `403`: 无权删除

---

### 9. 知识检索（测试接口）

**接口**: `POST /api/knowledge/search`

**请求体**:
```json
{
  "datasetId": "550e8400-e29b-41d4-a716-446655440000",
  "query": "如何使用 REST API",
  "topK": 5
}
```

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| datasetId | String | Body | 是 | 知识库 ID |
| query | String | Body | 是 | 查询文本 |
| topK | Integer | Body | 否 | 返回结果数量，默认 5 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    "REST API 使用指南：首先需要获取 API Token...",
    "API 认证方式：支持 Bearer Token 和 API Key 两种方式...",
    "常见错误码说明：401 表示未授权，403 表示无权限..."
  ]
}
```

**说明**:
- 使用 Milvus 向量相似度检索
- 自动过滤出属于指定知识库的文档
- 结果按相似度降序排列

---

## 性能优化建议

### 1. Milvus 索引配置

推荐配置（在 `MilvusConfig` 中）:

```java
// HNSW 索引（高性能）
IndexParam indexParam = IndexParam.builder()
    .indexType(IndexType.HNSW)
    .metricType(MetricType.COSINE)
    .extraParams(Map.of(
        "M", 16,              // 连接数
        "efConstruction", 200 // 构建时搜索深度
    ))
    .build();
```

### 2. 相似度阈值

建议在 `KnowledgeRetrievalServiceImpl` 中添加相似度阈值过滤：

```java
SearchRequest request = SearchRequest.builder()
    .query(query)
    .topK(topK)
    .filterExpression("datasetId == '" + datasetId + "'")
    .similarityThreshold(0.7)  // 添加阈值
    .build();
```

### 3. 批量向量化

在 `AsyncDocumentProcessor` 中，建议批量调用 `vectorStore.addDocuments()` 而非逐个调用 `store()`：

```java
// 收集所有 chunks
List<Document> domainDocuments = chunks.stream()
    .map(chunk -> Document.builder()
        .content(chunk.getText())
        .metadata(enrichedMetadata)
        .build())
    .collect(Collectors.toList());

// 批量存储
vectorStore.addDocuments(domainDocuments);
```

---

## 安全性说明

### 1. 文件上传安全

- ✅ 文件类型白名单验证
- ✅ 文件大小限制（50MB）
- ✅ 路径遍历攻击防护
- ✅ MIME 类型验证

### 2. 权限控制

- ✅ 所有操作验证用户所有权
- ✅ 知识库隔离（用户只能访问自己的知识库）
- ✅ 文档隔离（通过知识库所有权传递）

### 3. 数据隔离

- ✅ Milvus 使用 Metadata Filter 隔离不同知识库的向量
- ✅ MinIO 使用目录结构隔离文件：`{datasetId}/{documentId}/{filename}`

---

## 错误处理

### 通用错误码

| HTTP 状态码 | 说明 | 处理建议 |
|------------|------|----------|
| 400 | Bad Request | 检查请求参数格式和内容 |
| 401 | Unauthorized | Token 无效或已过期，重新登录 |
| 403 | Forbidden | 无权访问，确认资源所有权 |
| 404 | Not Found | 资源不存在，检查 ID 是否正确 |
| 500 | Internal Server Error | 服务器内部错误，查看日志或联系管理员 |

### 文档处理失败

如果文档状态为 `FAILED`，可以通过以下方式排查：

1. 查看 `errorMessage` 字段获取错误信息
2. 检查文件格式是否支持
3. 检查 Milvus 和 MinIO 服务是否正常
4. 查看应用日志：`AsyncDocumentProcessor` 的错误日志

---

## 使用示例

### 完整流程示例

```bash
# 1. 创建知识库
curl -X POST http://localhost:8080/api/knowledge/dataset \
  -H "Content-Type: application/json" \
  -d '{
    "name": "技术文档库",
    "description": "存储技术文档"
  }'

# 响应: { "data": { "datasetId": "xxx" } }

# 2. 上传文档
curl -X POST http://localhost:8080/api/knowledge/document/upload \
  -F "file=@API文档.pdf" \
  -F "datasetId=xxx" \
  -F "chunkSize=500" \
  -F "chunkOverlap=50"

# 响应: { "data": { "documentId": "yyy", "status": "PENDING" } }

# 3. 查询文档处理状态
curl -X GET http://localhost:8080/api/knowledge/document/yyy

# 响应: { "data": { "status": "COMPLETED", "totalChunks": 25 } }

# 4. 检索知识
curl -X POST http://localhost:8080/api/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "xxx",
    "query": "如何使用 API",
    "topK": 5
  }'

# 响应: { "data": ["相关文本片段1", "相关文本片段2", ...] }
```

---

## 附录

### 支持的文件格式

| 格式 | 扩展名 | MIME 类型 | 说明 |
|------|--------|-----------|------|
| PDF | .pdf | application/pdf | 使用 Apache Tika 解析 |
| Word | .doc | application/msword | 使用 Apache Tika 解析 |
| Word | .docx | application/vnd.openxmlformats-officedocument.wordprocessingml.document | 使用 Apache Tika 解析 |
| 文本 | .txt | text/plain | 直接读取 |
| Markdown | .md | text/markdown | 直接读取 |
| CSV | .csv | text/csv | 使用 Apache Tika 解析 |
| Excel | .xls | application/vnd.ms-excel | 使用 Apache Tika 解析 |
| Excel | .xlsx | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | 使用 Apache Tika 解析 |

### 分块策略

- **默认分块大小**: 500 tokens
- **默认重叠**: 50 tokens
- **分块器**: Spring AI TokenTextSplitter
- **目的**: 保持语义完整性，提高检索准确度

### Milvus Collection 结构

**Collection 名称**: `agent_knowledge_base`

**字段**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR | 向量 ID |
| vector | FLOAT_VECTOR | 文本向量（维度取决于 Embedding 模型） |
| text | VARCHAR | 原始文本内容 |
| datasetId | VARCHAR | 知识库 ID（用于过滤） |
| documentId | VARCHAR | 文档 ID（用于删除） |
| agentId | INT64 | Agent ID（可选，用于过滤） |
| filename | VARCHAR | 文件名 |
| chunkIndex | INT64 | 分块索引 |

---

**文档版本**: v1.0
**最后更新**: 2026-02-10
**维护者**: 后端开发工程师4号
