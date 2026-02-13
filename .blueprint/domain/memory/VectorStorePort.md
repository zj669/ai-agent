# VectorStore Port Blueprint

## 职责契约
- **做什么**: 定义向量存储的抽象接口，支持长期记忆(LTM)和知识库的存储/检索
- **不做什么**: 不依赖任何框架类型（如 Spring AI），使用 domain 层自己的值对象

## 核心值对象

### Document
领域层的文档值对象，替代 Spring AI 的 Document

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 文档唯一标识 |
| content | String | 文档内容 |
| metadata | Map\<String, Object\> | 元数据（如 agent_id, dataset_id, timestamp） |
| embedding | List\<Double\> | 向量嵌入（可选，由 infrastructure 层生成） |

### SearchRequest
领域层的搜索请求值对象，替代 Spring AI 的 SearchRequest

| 字段 | 类型 | 说明 |
|------|------|------|
| query | String | 查询文本 |
| topK | int | 返回结果数量 |
| filterExpression | String | 过滤表达式（如 "agent_id == 123"） |
| similarityThreshold | Double | 相似度阈值（可选） |

## 接口摘要

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| search | query, agentId, topK | List\<String\> | 搜索长期记忆（LTM） |
| store | agentId, content, metadata | void | 存储单条记忆 |
| storeBatch | agentId, contents | void | 批量存储记忆 |
| similaritySearch | SearchRequest | List\<Document\> | 高级检索（支持过滤） |
| searchKnowledgeByDataset | datasetId, query, topK | List\<String\> | 按数据集检索知识库 |
| addDocuments | List\<Document\> | void | 批量添加文档 |
| deleteByMetadata | Map filter | void | 按元数据删除文档 |

## 依赖拓扑
- **上游**: SchedulerService (记忆水合), KnowledgeApplicationService (知识库管理)
- **下游**: 无（纯接口定义）
- **实现**: MilvusVectorStoreAdapter (infrastructure 层)

## 设计约束
- 使用 domain 层自己的 Document 和 SearchRequest 值对象
- 不依赖 Spring AI 或任何框架类型
- infrastructure 层负责类型转换（domain Document ↔ Spring AI Document）

## 变更日志
- [2026-02-10] 新建蓝图，定义纯净的 VectorStore 端口接口和值对象
