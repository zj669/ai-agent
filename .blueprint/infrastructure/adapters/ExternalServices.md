## Metadata
- file: `.blueprint/infrastructure/adapters/ExternalServices.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ExternalServices
- 该文件描述外部服务适配器的职责边界，实现领域层定义的 Port 接口，负责对接第三方服务（MinIO 对象存储、Milvus 向量数据库、Spring AI 文档处理）。封装技术细节，提供领域层所需的抽象接口。

## 2) 核心方法
- `download()`
- `delete()`
- `similaritySearch()`
- `addDocuments()`
- `read()`

## 3) 具体方法
### 3.1 download()
- 函数签名: `InputStream download(String objectKey)` (MinioStorageAdapter)
- 入参: `objectKey` 对象存储路径（格式：knowledge/{datasetId}/{filename}）
- 出参: `InputStream` 文件流，用于读取文件内容
- 功能含义: 从 MinIO 下载文件，用于知识库文档处理和文件检索
- 链路作用: KnowledgeApplicationService.processDocument() → StoragePort.download() → MinioClient.getObject() → 返回文件流

### 3.2 delete()
- 函数签名: `void delete(String objectKey)` (MinioStorageAdapter)
- 入参: `objectKey` 对象存储路径
- 出参: 无（副作用：删除 MinIO 对象）
- 功能含义: 删除 MinIO 中的文件，用于知识库文档删除和清理
- 链路作用: KnowledgeApplicationService.deleteDocument() → StoragePort.delete() → MinioClient.removeObject() → MinIO 删除对象

### 3.3 similaritySearch()
- 函数签名: `List<String> similaritySearch(String query, Long agentId, int topK)` (MilvusVectorStoreAdapter)
- 入参: `query` 查询文本，`agentId` 智能体 ID（用于过滤），`topK` 返回结果数量
- 出参: `List<String>` 相似文档文本列表（按相似度排序）
- 功能含义: 向量相似度搜索，从 Milvus 检索相关文档，支持知识库检索（agent_knowledge_base）和长期记忆检索（agent_chat_memory）
- 链路作用: SchedulerService.hydrateMemory() → VectorStore.search() → MilvusVectorStoreAdapter.similaritySearch() → Spring AI VectorStore.similaritySearch() → Milvus 查询

### 3.4 addDocuments()
- 函数签名: `void addDocuments(List<Document> documents)` (MilvusVectorStoreAdapter)
- 入参: `documents` 文档列表（包含 text、metadata{agent_id, dataset_id}）
- 出参: 无（副作用：插入向量到 Milvus）
- 功能含义: 批量添加文档到向量数据库，自动生成 embedding 并存储，用于知识库构建
- 链路作用: KnowledgeApplicationService.processDocument() → VectorStore.addDocuments() → Spring AI VectorStore.add() → EmbeddingModel.embed() → Milvus 插入

### 3.5 read()
- 函数签名: `List<org.springframework.ai.document.Document> read(Resource resource)` (SpringAIDocumentReaderAdapter)
- 入参: `resource` Spring Resource 对象（支持 PDF、TXT、DOCX 等）
- 出参: `List<Document>` 解析后的文档对象列表（包含文本和元数据）
- 功能含义: 读取并解析文档内容，使用 Spring AI 的 DocumentReader（PdfDocumentReader/TextReader），支持多种文件格式
- 链路作用: KnowledgeApplicationService.processDocument() → DocumentReaderPort.read() → Spring AI DocumentReader.get() → 返回文档列表 → TextSplitter 分块


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 MilvusVectorStoreAdapter 和 MinioStorageAdapter 实现。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
