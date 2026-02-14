## Metadata
- file: `.blueprint/domain/memory/VectorStorePort.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: VectorStorePort
- 该文件用于描述 VectorStorePort 的职责边界与协作关系。

## 2) 核心方法
- `similaritySearch()`
- `addDocuments()`
- `searchKnowledgeByDataset()`
- `deleteByMetadata()`

## 3) 具体方法
### 3.1 similaritySearch()
- 函数签名: `List<Document> similaritySearch(SearchRequest request)`
- 入参:
  - `request`: 领域层 SearchRequest 对象（包含 query、topK、metadata filter 等）
- 出参: Document 列表（包含 content 和 metadata）
- 功能含义: 使用 SearchRequest 进行高级检索，支持 Metadata Filter 过滤条件（如 datasetId、documentId），返回相似度最高的文档。
- 链路作用: 在知识库检索场景中调用，提供精准的向量搜索能力。

### 3.2 addDocuments()
- 函数签名: `void addDocuments(List<Document> documents)`
- 入参:
  - `documents`: 领域层 Document 列表（包含 content 和 metadata）
- 出参: 无（void）
- 功能含义: 批量存储文档到向量库，自动进行 Embedding 和索引构建。
- 链路作用: 在 AsyncDocumentProcessor 中调用，将分块后的文档存储到 Milvus。

### 3.3 searchKnowledgeByDataset()
- 函数签名: `List<String> searchKnowledgeByDataset(String datasetId, String query, int topK)`
- 入参:
  - `datasetId`: 知识库ID（用于范围隔离）
  - `query`: 查询文本
  - `topK`: 返回结果数量
- 出参: 相关知识文本列表
- 功能含义: 根据 datasetId 和 query 检索知识库（便捷方法），自动添加 metadata filter。
- 链路作用: 在工作流 LTM 加载时调用，提供知识库检索能力。

### 3.4 deleteByMetadata()
- 函数签名: `void deleteByMetadata(Map<String, Object> filter)`
- 入参:
  - `filter`: Metadata 过滤条件（如 {"documentId": "doc_123"}）
- 出参: 无（void）
- 功能含义: 根据 Metadata 删除向量，用于文档删除时清理向量数据。
- 链路作用: 在 AsyncDocumentProcessor.deleteDocumentVectors() 中调用，清理文档向量。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 VectorStore.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
