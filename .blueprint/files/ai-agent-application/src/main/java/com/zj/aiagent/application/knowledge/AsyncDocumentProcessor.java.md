# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java
- Type: .java

## Responsibility
- 异步编排文档处理流水线：下载文件、解析、分块、向量化存储、进度回写与失败兜底。

## Key Symbols / Structure
- `processDocumentAsync(KnowledgeDocument document)`
  - 文档置为 `PROCESSING`
  - MinIO 下载原文件
  - `SpringAIDocumentReaderAdapter` 解析
  - `SpringAITextSplitterAdapter` 按 `chunkSize/chunkOverlap` 分块
  - 循环调用 `VectorStore.store(...)` 存储向量
  - 周期性更新 `processedChunks`
  - 成功后 `markCompleted()` 并更新数据集 chunk 统计
  - 异常时 `markFailed(errorMessage)`
- `deleteDocumentVectors(String documentId)`
  - 按 metadata `documentId` 删除向量

## Dependencies
- `FileStorageService`
- `KnowledgeDocumentRepository`, `KnowledgeDatasetRepository`
- `SpringAIDocumentReaderAdapter`, `SpringAITextSplitterAdapter`
- `VectorStore`

## Notes
- 状态: 正常
- 通过 `@Async` 解耦上传 API 与向量化耗时流程。
