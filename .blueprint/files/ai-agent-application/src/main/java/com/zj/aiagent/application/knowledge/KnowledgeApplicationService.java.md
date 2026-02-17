# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java
- Type: .java

## Responsibility
- 知识库应用编排服务：管理数据集与文档生命周期，协调仓储、对象存储与异步处理。

## Key Symbols / Structure
- 数据集
  - `createDataset(name, description, userId, agentId)`
  - `listDatasetsByUser(userId)`
  - `getDataset(datasetId)` / `getDataset(datasetId, userId)`
  - `deleteDataset(datasetId)` / `deleteDataset(datasetId, userId)`
- 文档
  - `uploadDocument(datasetId, file, chunkingConfig)`
  - `uploadDocument(datasetId, file, chunkingConfig, userId)`
  - `listDocuments(datasetId, pageable)` / `listDocuments(datasetId, pageable, userId)`
  - `getDocument(documentId)` / `getDocument(documentId, userId)`
  - `deleteDocument(documentId)` / `deleteDocument(documentId, userId)`
  - `retryDocument(documentId, userId)`
- 辅助
  - `validateOwnership(...)`
  - `deleteDocumentInternal(...)`
  - `extractObjectName(fileUrl)`

## Dependencies
- `KnowledgeDatasetRepository`, `KnowledgeDocumentRepository`
- `FileStorageService`, `AsyncDocumentProcessor`
- Domain: `KnowledgeDataset`, `KnowledgeDocument`, `ChunkingConfig`, `DocumentStatus`

## Notes
- 状态: 正常
- 上传流程包含 `FileValidator.validate(file)` 与异步向量化触发。
