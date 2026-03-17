# VectorStore.java 蓝图

## Metadata
- title: VectorStore
- type: interface
- summary: 向量存储端口，抽象长期记忆（LTM）和知识库的存储/检索能力，由 MilvusVectorStoreAdapter 实现

## 关键方法单元

### search
- location: VectorStore.search
- purpose: 按 agentId 检索长期记忆
- input: query（String）、agentId（Long）、topK（int）
- output: List<String>

### similaritySearch
- location: VectorStore.similaritySearch
- purpose: 使用 domain 层 SearchRequest 执行高级检索（支持 filterExpression 和 similarityThreshold）
- input: SearchRequest（domain 值对象）
- output: List<Document>（domain 值对象）

### searchKnowledgeByDataset
- location: VectorStore.searchKnowledgeByDataset
- purpose: 按 datasetId 检索知识库（便捷方法，default 实现抛 UnsupportedOperationException）
- input: datasetId（String）、query（String）、topK（int）
- output: List<String>

### addDocuments
- location: VectorStore.addDocuments
- purpose: 批量存储文档到知识库
- input: List<Document>
- output: void

### deleteByMetadata
- location: VectorStore.deleteByMetadata
- purpose: 按 metadata 过滤条件删除向量
- input: Map<String, Object> filter
- output: void

## 变更记录
- 2026-03-16: 初始蓝图生成
