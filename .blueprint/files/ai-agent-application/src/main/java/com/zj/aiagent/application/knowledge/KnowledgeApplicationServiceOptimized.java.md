# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceOptimized.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceOptimized.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceOptimized.java
- Type: .java

## Responsibility
- Knowledge 应用服务优化版本：强化权限校验、上传安全与失败文档重试流程。

## Key Symbols / Structure
- `createDataset(...)`, `listDatasetsByUser(...)`
- `getDataset(datasetId, userId)` + `getDatasetInternal(datasetId)`
- `deleteDataset(datasetId, userId)`
- `uploadDocument(datasetId, file, chunkingConfig, userId)`
- `listDocuments(datasetId, pageable, userId)`
- `getDocument(documentId, userId)`
- `deleteDocument(documentId, userId)`
- `retryDocument(documentId, userId)`
- 辅助：`validateOwnership`, `deleteDocumentInternal`, `extractObjectName`

## Dependencies
- 与 `KnowledgeApplicationService` 相同：仓储 + 文件存储 + 异步处理器
- 额外强调 `FileValidator` 与 `DocumentStatus.FAILED` 校验

## Notes
- 状态: 正常
- Bean 名称为 `knowledgeApplicationServiceOptimized`。
