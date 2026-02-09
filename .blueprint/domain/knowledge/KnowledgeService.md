# KnowledgeService Blueprint

## 职责契约
- **做什么**: 管理知识库系统——数据集(KnowledgeDataset)的 CRUD、文档(KnowledgeDocument)的上传/解析/分块/向量化、语义检索(RAG)
- **不做什么**: 不负责文件的物理存储（那是 MinIO 适配器的职责）；不负责向量数据库操作（那是 VectorStore 端口的职责）；不负责 LLM 调用

## 核心聚合根

### KnowledgeDataset
- 知识库聚合根，管理文档集合
- 关联 userId 和 agentId

### KnowledgeDocument
- 文档实体，管理文档处理生命周期
- 状态: PENDING → PROCESSING → COMPLETED / FAILED
- 记录分块数量、处理进度

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createDataset | name, agentId, userId | KnowledgeDataset | 写DB | - |
| uploadDocument | datasetId, file | KnowledgeDocument | 存MinIO, 写DB, 触发异步处理 | 文件大小限制 |
| processDocument | documentId | void | 解析→分块→向量化→写Milvus | 异步执行 |
| retrieve | query, datasetId, topK | List<RetrievalResult> | 无 | 向量相似度检索 |
| deleteDocument | documentId | void | 删MinIO文件, 删Milvus向量, 软删DB | 级联清理 |

## 依赖拓扑
- **上游**: KnowledgeController, KnowledgeApplicationService, SchedulerService(hydrateMemory)
- **下游**: KnowledgeDatasetRepository(端口), KnowledgeDocumentRepository(端口), FileStorageService(端口/MinIO), VectorStore(端口/Milvus), DocumentReaderAdapter, TextSplitterAdapter

## 领域事件
- 发布: 无
- 监听: 无

## 设计约束
- 文档解析使用 Spring AI + Apache Tika，支持多种格式
- 文本分块使用 Spring AI TextSplitter
- 向量化使用配置的 Embedding 模型
- 原始文件存储在 MinIO，向量存储在 Milvus
- 文档处理是异步的，通过状态机跟踪进度
- 检索结果注入到工作流执行上下文（hydrateMemory 机制）

## 变更日志
- [初始] 从现有代码逆向生成蓝图
