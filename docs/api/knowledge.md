# 知识库模块 API 文档

## 概述

知识库模块负责数据集管理、文档上传与异步处理，以及基于 Milvus 的数据集内检索。

- Base URL: `/api/knowledge`
- 当前控制器: `KnowledgeController`
- 当前返回风格: 统一 `Response<T>`

## 认证

所有接口都要求登录态；本地调试可使用 `debug-user` 请求头。

## 数据模型

### KnowledgeDataset

| 字段 | 类型 | 说明 |
|------|------|------|
| `datasetId` | `String` | 知识库 ID |
| `name` | `String` | 名称 |
| `description` | `String` | 描述 |
| `userId` | `Long` | 所属用户 |
| `agentId` | `Long` | 绑定 Agent，可为空 |
| `documentCount` | `Integer` | 文档数 |
| `totalChunks` | `Integer` | 总分块数 |
| `createdAt` | `String` | 创建时间 |
| `updatedAt` | `String` | 更新时间 |

### KnowledgeDocument

| 字段 | 类型 | 说明 |
|------|------|------|
| `documentId` | `String` | 文档 ID |
| `datasetId` | `String` | 所属知识库 ID |
| `filename` | `String` | 文件名 |
| `fileUrl` | `String` | MinIO URL |
| `fileSize` | `Long` | 文件大小 |
| `contentType` | `String` | MIME 类型 |
| `status` | `String` | `PENDING / PROCESSING / COMPLETED / FAILED` |
| `totalChunks` | `Integer` | 总分块数 |
| `processedChunks` | `Integer` | 已处理分块数 |
| `errorMessage` | `String` | 失败原因 |
| `uploadedAt` | `String` | 上传时间 |
| `completedAt` | `String` | 完成时间 |

## 接口列表

### 1. 创建知识库

- 方法: `POST /api/knowledge/dataset`
- 请求体：

```json
{
  "name": "技术文档库",
  "description": "存储技术文档和 API 参考",
  "agentId": 123
}
```

- 返回: `Response<DatasetResp>`

### 2. 查询知识库列表

- 方法: `GET /api/knowledge/dataset/list`
- 返回: `Response<List<DatasetResp>>`

### 3. 查询知识库详情

- 方法: `GET /api/knowledge/dataset/{id}`
- 返回: `Response<DatasetResp>`

### 4. 删除知识库

- 方法: `DELETE /api/knowledge/dataset/{id}`
- 返回: `Response<Void>`

实现备注：
- 会删除文档记录、MinIO 文件和对应向量

### 5. 上传文档

- 方法: `POST /api/knowledge/document/upload`
- 请求类型: `multipart/form-data`
- 参数：
  - `file`
  - `datasetId`
  - `chunkStrategy`，默认 `FIXED`
  - `chunkSize`，固定分块时使用，默认 `500`
  - `chunkOverlap`，固定分块默认 `50`；语义分块可选
  - `maxChunkSize`，语义分块最大块大小
  - `minChunkSize`，语义分块最小块大小
  - `similarityThreshold`，语义分块相似度阈值，范围 `(0, 1]`
  - `mergeSmallChunks`，语义分块是否合并过小片段
- 返回: `Response<DocumentResp>`

当前支持的分块策略：

- `FIXED`
- `SEMANTIC`

兼容说明：

- 如果未传 `chunkStrategy`，系统默认按 `FIXED` 处理
- 旧调用方只传 `chunkSize/chunkOverlap` 仍可继续使用

请求示例：

`FIXED`

```text
file=<binary>
datasetId=xxx
chunkStrategy=FIXED
chunkSize=500
chunkOverlap=50
```

`SEMANTIC`

```text
file=<binary>
datasetId=xxx
chunkStrategy=SEMANTIC
minChunkSize=200
maxChunkSize=800
similarityThreshold=0.75
mergeSmallChunks=true
chunkOverlap=0
```

当前处理链路：
1. 文件上传至 MinIO
2. 创建文档记录，初始状态 `PENDING`
3. `AsyncDocumentProcessor` 异步解析、分块、向量化
4. 处理完成后变为 `COMPLETED`，失败则为 `FAILED`

### 6. 查询文档列表

- 方法: `GET /api/knowledge/document/list`
- 参数：
  - `datasetId`
  - `page`，默认 `0`
  - `size`，默认 `20`
- 返回: `Response<Page<DocumentResp>>`

### 7. 查询文档详情

- 方法: `GET /api/knowledge/document/{id}`
- 返回: `Response<DocumentResp>`

### 8. 删除文档

- 方法: `DELETE /api/knowledge/document/{id}`
- 返回: `Response<Void>`

实现备注：
- 当前会根据文档 metadata 删除 Milvus 中对应向量

### 9. 重试失败文档

- 方法: `POST /api/knowledge/document/{id}/retry`
- 返回: `Response<DocumentResp>`

### 10. 测试检索接口

- 方法: `POST /api/knowledge/search`
- 返回: `Response<List<String>>`

请求体：
```json
{
  "datasetId": "221593a6-a963-426b-9a62-a191bf075784",
  "query": "周杰是谁",
  "topK": 5
}
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    "周杰是西南科技大学的学生"
  ],
  "success": true
}
```

实现备注：
- 这是一个按 dataset 检索的调试接口
- 会先校验知识库归属，再执行向量检索
- 返回值是纯文本片段列表，不包含 score / document metadata

## 工作流内知识库检索

虽然 `/api/knowledge/search` 只有基本语义检索入口，但工作流中的知识库节点支持三种策略：

- `SEMANTIC`
- `KEYWORD`
- `HYBRID`

执行链路：

`KnowledgeNodeExecutorStrategy -> KnowledgeRetrievalServiceImpl -> MilvusVectorStoreAdapter`

## Milvus 元数据约定

### 当前标准键名

知识库向量 metadata 当前统一使用下划线风格：

- `dataset_id`
- `document_id`
- `agent_id`
- `chunk_index`
- `filename`

### 历史兼容

2026-03-18 起，检索和删除已兼容历史驼峰键名：

- `datasetId`
- `documentId`
- `agentId`
- `chunkIndex`

这意味着：
- 新写入统一按下划线
- 旧向量数据不需要立刻重灌，也能被搜索/删除命中

## 故障排查

### 场景 1：文档状态是 `COMPLETED`，但知识库节点返回 0 条

优先排查：
- `datasetId` 是否正确
- MySQL 中 `knowledge_document.status` 是否为 `COMPLETED`
- 当前 query 是否已正确传入知识库节点
- 是否踩到了历史 metadata 键名不一致问题

当前已知结论：
- 2026-03-18 前，异步入库写的是驼峰 metadata，检索 filter 用的是下划线 metadata，可能导致“文档已完成但永远搜不到”
- 该问题已在 `AsyncDocumentProcessor` 和 `MilvusVectorStoreAdapter` 中修复

### 场景 2：删除文档后，检索结果仍然出现旧内容

优先排查：
- 删除是否成功命中了对应 `document_id`
- 该批历史向量是否使用了驼峰 `documentId`
- 当前服务是否已重启到包含兼容删除逻辑的新版本

### 场景 3：本地改了代码但检索行为没变化

优先排查：
- 是否重新执行过：
  - `./mvnw clean install -pl ai-agent-interfaces -am -Dmaven.test.skip=true`
  - `./mvnw spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local -Dmaven.test.skip=true`

原因：
- 当前 `spring-boot:run` 依赖模块可能仍从本地 Maven 仓库加载旧 SNAPSHOT JAR

## 向量集合结构

当前知识库集合名：

- `agent_knowledge_base`

关键 metadata 字段：

| 字段名 | 说明 |
|--------|------|
| `dataset_id` | 知识库隔离 |
| `document_id` | 文档删除定位 |
| `agent_id` | Agent 维度隔离，可为空 |
| `chunk_index` | 分块序号 |
| `filename` | 原文件名 |

## 更新记录

- 2026-03-18:
  - 对齐 `/api/knowledge/*` 的当前响应契约
  - 补充 `retry` 接口
  - 记录 Milvus metadata 的标准键名与历史兼容策略
  - 补充“COMPLETED 但检索为空”的定位方法
