# 知识库业务域索引

本文件为知识库业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

KnowledgeDataset、KnowledgeDocument、向量存储（Milvus）、文档切片、语义检索（LTM）。

## 典型触发

- 创建/管理知识库数据集
- 文档上传、切片、向量化
- 向量搜索相关问题
- KnowledgeController 接口排查

## 核心约束

- 向量存储通过 `VectorStore` 端口接入 `MilvusVectorStoreAdapter`
- Protobuf 版本固定为 3.25.3（Milvus SDK 依赖），版本在父 POM 锁定，不要单独升级
- MinIO 用于文档对象存储
- 语义检索是 ExecutionContext LTM（长期记忆）的数据来源

## 代码入口（支撑事实）

- 接口层：`KnowledgeController.java`（路径 `/api/knowledge`）
- 领域层：`ai-agent-domain/src/main/java/.../knowledge/`
- 基础设施：`MilvusVectorStoreAdapter.java`

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 知识库功能开发 | `references/knowledge/feature-delivery.md` | 待创建 |
| 知识库排查 | `references/knowledge/triage.md` | 待创建 |
