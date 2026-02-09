# KnowledgeApplicationService Blueprint

## 职责契约
- **做什么**: 知识库用例编排——协调文档上传、处理、检索等用例；管理异步文档处理流程
- **不做什么**: 不负责文件解析/分块/向量化的具体实现；不直接操作 MinIO 或 Milvus

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createDataset | CreateDatasetCmd | DatasetDTO | 写DB | @Transactional |
| uploadDocument | datasetId, MultipartFile | DocumentDTO | 存文件, 写DB, 触发异步处理 | 文件大小限制 |
| getDataset | datasetId | DatasetDTO | 无 | - |
| listDocuments | datasetId | List<DocumentDTO> | 无 | - |
| deleteDocument | documentId | void | 级联删除(文件+向量+DB) | @Transactional |

## 依赖拓扑
- **上游**: KnowledgeController
- **下游**: KnowledgeDataset(聚合根), KnowledgeDocument(实体), FileStorageService(端口), VectorStore(端口), DocumentReaderAdapter, TextSplitterAdapter

## 设计约束
- 文档处理是异步的，上传后立即返回，后台处理
- 删除操作需要级联清理 MinIO 文件和 Milvus 向量

## 变更日志
- [初始] 从现有代码逆向生成蓝图
