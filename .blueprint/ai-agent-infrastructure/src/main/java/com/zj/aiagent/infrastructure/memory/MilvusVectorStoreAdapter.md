# MilvusVectorStoreAdapter.java 蓝图

## Metadata
- title: MilvusVectorStoreAdapter
- type: service
- summary: Milvus 向量存储适配器，实现 domain 层 VectorStore 端口，管理双集合（knowledgeStore 知识库 + memoryStore 长期记忆），负责 domain 值对象与 Spring AI 类型的转换

## 关键方法单元

### search
- location: MilvusVectorStoreAdapter.search
- purpose: 从 agent_chat_memory 集合检索长期记忆（LTM）
- input: query（String）、agentId（Long）、topK（int）
- output: List<String> 记忆文本列表
- core_steps:
  1. 构建 Spring AI SearchRequest，filterExpression = "agent_id == {agentId}"
  2. 调用 memoryStore.similaritySearch(request)
  3. 提取 Document.getText() 返回

### similaritySearch
- location: MilvusVectorStoreAdapter.similaritySearch
- purpose: 使用 domain 层 SearchRequest 执行知识库检索（被 KnowledgeRetrievalServiceImpl 调用）
- input: SearchRequest（domain 值对象，含 query/topK/filterExpression/similarityThreshold）
- output: List<Document>（domain 值对象）
- core_steps:
  1. 将 domain SearchRequest 转换为 Spring AI SearchRequest（toSpringAiSearchRequest）
  2. 调用 knowledgeStore.similaritySearch
  3. 将 Spring AI Document 转换为 domain Document（toDomainDocument）

### searchKnowledgeByDataset
- location: MilvusVectorStoreAdapter.searchKnowledgeByDataset
- purpose: 按 datasetId 检索知识库（便捷方法，直接构建 "dataset_id == '{datasetId}'" 过滤）
- input: datasetId（String）、query（String）、topK（int）
- output: List<String> 知识片段文本列表
- core_steps:
  1. 构建 Spring AI SearchRequest，filterExpression = "dataset_id == '{datasetId}'"
  2. 调用 knowledgeStore.similaritySearch
  3. 提取 getText() 返回

### addDocuments
- location: MilvusVectorStoreAdapter.addDocuments
- purpose: 批量存储文档到知识库集合
- input: List<Document>（domain 值对象）
- output: void
- core_steps:
  1. 将 domain Document 转换为 Spring AI Document
  2. 调用 knowledgeStore.add

### deleteByMetadata
- location: MilvusVectorStoreAdapter.deleteByMetadata
- purpose: 按 metadata 过滤条件删除向量（用于文档删除时清理向量）
- input: Map<String, Object> filter
- output: void
- core_steps:
  1. 构建 Milvus filter 表达式（buildFilterExpression）
  2. 用通配查询 "*" + topK=1000 检索匹配文档
  3. 提取 Document IDs 批量删除

### store / storeBatch
- location: MilvusVectorStoreAdapter.store / storeBatch
- purpose: 存储内容到长期记忆集合（agent_chat_memory）
- input: agentId + content + metadata / agentId + contents
- output: void

## 关键设计
- 双集合隔离：knowledgeStore（@Qualifier("knowledgeVectorStore")）和 memoryStore（@Qualifier("memoryVectorStore")）
- @ConditionalOnProperty(prefix = "milvus", name = "enabled")：Milvus 未启用时不注册此 Bean
- metadata key 使用下划线命名（agent_id、dataset_id），与 Milvus 字段命名一致

## 变更记录
- 2026-03-16: 初始蓝图生成（排查知识库节点执行器问题时创建）
