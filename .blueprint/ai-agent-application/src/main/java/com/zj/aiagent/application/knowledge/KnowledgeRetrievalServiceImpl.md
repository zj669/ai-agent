# KnowledgeRetrievalServiceImpl.java 蓝图

## Metadata
- title: KnowledgeRetrievalServiceImpl
- type: service
- summary: 知识检索服务实现，实现 domain 层 KnowledgeRetrievalService 接口，委托 VectorStore 执行向量相似度检索

## 关键方法单元

### retrieve
- location: KnowledgeRetrievalServiceImpl.retrieve
- purpose: 按 agentId 检索知识（用于 LLM 节点内置 RAG 和 SchedulerService 内存注入）
- input: agentId（Long，权限隔离）、query（String）、topK（int）
- output: List<String> 知识片段文本列表，异常时返回空列表
- core_steps:
  1. 构建 SearchRequest，filterExpression = "agentId == {agentId}"
  2. 调用 vectorStore.similaritySearch(request)
  3. 提取 Document.getText() 返回

### retrieveByDataset
- location: KnowledgeRetrievalServiceImpl.retrieveByDataset
- purpose: 按 datasetId 检索知识（用于 KnowledgeNodeExecutorStrategy 知识库节点）
- input: datasetId（String）、query（String）、topK（int）
- output: List<String> 知识片段文本列表，异常时返回空列表
- core_steps:
  1. 构建 SearchRequest，filterExpression = "datasetId == '{datasetId}'"
  2. 调用 vectorStore.similaritySearch(request)
  3. 提取 Document.getText() 返回

## 已知问题
- retrieveByDataset 的 filterExpression 使用驼峰 "datasetId"，但 MilvusVectorStoreAdapter.searchKnowledgeByDataset 使用下划线 "dataset_id"，两者不一致
- retrieve 的 filterExpression 使用驼峰 "agentId"，但 MilvusVectorStoreAdapter.search 使用下划线 "agent_id"，同样不一致
- retrieveByDataset 走 similaritySearch 路径而非专用的 searchKnowledgeByDataset 方法

## 变更记录
- 2026-03-16: 初始蓝图生成（排查知识库节点执行器问题时创建）
