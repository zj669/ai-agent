# KnowledgeApplicationService Blueprint

## 职责契约
- **做什么**: 知识库用例编排——协调文档上传、处理、检索等用例；管理异步文档处理流程
- **不做什么**: 不负责文件解析/分块/向量化的具体实现；不直接操作 MinIO 或 Milvus

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createDataset | CreateDatasetCmd | DatasetDTO | 写DB | @Transactional, 权限验证 |
| uploadDocument | datasetId, MultipartFile | DocumentDTO | 存文件, 写DB, 触发异步处理 | 文件类型白名单, 大小限制(50MB), 权限验证 |
| getDataset | datasetId | DatasetDTO | 无 | 权限验证 |
| listDocuments | datasetId, pageable | Page<DocumentDTO> | 无 | 权限验证, 分页 |
| deleteDocument | documentId | void | 级联删除(文件+向量+DB) | @Transactional, 权限验证 |
| deleteDataset | datasetId | void | 级联删除所有文档 | @Transactional, 权限验证 |
| retryDocument | documentId | void | 重新触发异步处理 | 仅FAILED状态可重试 |

## 依赖拓扑
- **上游**: KnowledgeController
- **下游**: KnowledgeDataset(聚合根), KnowledgeDocument(实体), FileStorageService(端口), VectorStore(端口), DocumentReaderAdapter, TextSplitterAdapter

## 设计约束
- 文档处理是异步的，上传后立即返回，后台处理
- 删除操作需要级联清理 MinIO 文件和 Milvus 向量
- **安全性约束**:
  - 文件类型白名单: pdf, doc, docx, txt, md, csv, xlsx
  - 文件大小限制: 50MB
  - 文件名安全检查: 禁止路径遍历字符 (../, ..\)
  - 权限验证: 所有操作需验证用户是否拥有该资源
- **性能约束**:
  - 异步处理使用独立线程池 (core=4, max=8, queue=100)
  - 向量化批量操作，每批最多100个chunk
  - Milvus 检索使用相似度阈值过滤 (threshold=0.7)

## 变更日志
- [2026-02-10] 新增安全性和性能约束，添加 retryDocument 接口
- [初始] 从现有代码逆向生成蓝图
