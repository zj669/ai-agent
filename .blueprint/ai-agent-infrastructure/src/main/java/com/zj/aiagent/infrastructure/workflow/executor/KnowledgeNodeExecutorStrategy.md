# KnowledgeNodeExecutorStrategy.java 蓝图

## Metadata
- title: KnowledgeNodeExecutorStrategy
- type: service
- summary: 知识库节点执行器策略，从 Milvus 向量库按 datasetId 检索知识片段并返回结果列表

## 关键方法单元

### executeAsync
- location: KnowledgeNodeExecutorStrategy.executeAsync
- purpose: 异步执行知识库检索节点，从节点配置中读取 datasetId/topK/strategy，从 resolvedInputs 中获取 query，调用 KnowledgeRetrievalService 执行检索
- input: Node（节点定义，含 config.properties）、resolvedInputs（已解析输入，期望含 query 或 user_input）、StreamPublisher（未使用）
- output: CompletableFuture<NodeExecutionResult>，成功时包含 Map("knowledge_list" → List<String>)
- core_steps:
  1. 从 node.config 读取 knowledge_dataset_id（必填）、search_strategy（未使用）、knowledge_top_k（默认 5）
  2. 从 resolvedInputs 获取 query，回退到 user_input
  3. 调用 knowledgeRetrievalService.retrieveByDataset(datasetId, query, topK)
  4. 返回 NodeExecutionResult.success(Map.of("knowledge_list", results))

### getSupportedType
- location: KnowledgeNodeExecutorStrategy.getSupportedType
- purpose: 声明支持 NodeType.KNOWLEDGE，供 NodeExecutorFactory 注册
- input: 无
- output: NodeType.KNOWLEDGE

## 已知问题
- search_strategy 读取后未传递给下游检索服务（死代码）
- resolvedInputs 中 query 为 null 时 String.valueOf 返回 "null" 字符串，不会被空检查拦截
- 未使用 StreamPublisher，不支持流式输出

## 变更记录
- 2026-03-16: 初始蓝图生成（排查知识库节点执行器问题时创建）
