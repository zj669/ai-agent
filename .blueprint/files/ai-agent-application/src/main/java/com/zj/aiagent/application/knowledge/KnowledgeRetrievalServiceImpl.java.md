# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java
- Type: .java

## Responsibility
- 实现领域接口 `KnowledgeRetrievalService`，提供知识检索能力供调度链路做 LTM 注入。

## Key Symbols / Structure
- `retrieve(Long agentId, String query, int topK)`
  - 构造 `SearchRequest`（`filterExpression: agentId == ...`）
  - 调用 `VectorStore.similaritySearch`
  - 返回文本片段列表
- `retrieveByDataset(String datasetId, String query, int topK)`
  - 使用 `datasetId` 过滤表达式检索

## Dependencies
- Domain service interface: `KnowledgeRetrievalService`
- Memory port: `VectorStore`
- VO: `SearchRequest`, `Document`

## Notes
- 状态: 正常
- 检索异常时返回空列表，避免阻断主链路。
