## Metadata
- file: `.blueprint/application/KnowledgeApplicationService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: KnowledgeApplicationService
- 该文件用于描述 KnowledgeApplicationService 的职责边界与协作关系。

## 2) 核心方法
- `createDataset()`
- `uploadDocument()`
- `getDataset()`
- `listDocuments()`
- `deleteDocument()`

## 3) 具体方法
### 3.1 createDataset()
- 函数签名: `KnowledgeDataset createDataset(String name, String description, Long userId, Long agentId)`
- 入参:
  - `name`: 知识库名称
  - `description`: 描述
  - `userId`: 用户ID
  - `agentId`: 绑定的 Agent ID（可选）
- 出参: KnowledgeDataset 实体（创建的知识库）
- 功能含义: 创建知识库，生成 datasetId（UUID），构建 KnowledgeDataset 实体并持久化，返回创建的知识库。
- 链路作用: 在 KnowledgeController.createDataset() 中调用，执行知识库创建用例。

### 3.2 uploadDocument()
- 函数签名: `KnowledgeDocument uploadDocument(String datasetId, MultipartFile file, ChunkingConfig chunkingConfig)`
- 入参:
  - `datasetId`: 知识库ID
  - `file`: 上传的文件
  - `chunkingConfig`: 分块配置（可选）
- 出参: KnowledgeDocument 实体（创建的文档）
- 功能含义: 上传文档到知识库，文件安全校验，上传到 MinIO，构建 KnowledgeDocument 实体，保存文档记录（状态：PENDING），更新知识库统计，触发异步处理。
- 链路作用: 在 KnowledgeController.uploadDocument() 中调用，执行文档上传用例。

### 3.3 getDataset()
- 函数签名: `KnowledgeDataset getDataset(String datasetId, Long userId)`
- 入参:
  - `datasetId`: 知识库ID
  - `userId`: 当前用户ID（用于权限校验）
- 出参: KnowledgeDataset 实体（知识库对象）
- 功能含义: 查询知识库详情，检查所有权，返回知识库对象。
- 链路作用: 在 KnowledgeController.getDataset() 中调用，提供知识库详情查询接口。

### 3.4 listDocuments()
- 函数签名: `Page<KnowledgeDocument> listDocuments(String datasetId, Pageable pageable, Long userId)`
- 入参:
  - `datasetId`: 知识库ID
  - `pageable`: 分页参数
  - `userId`: 当前用户ID（用于权限校验）
- 出参: 文档分页结果
- 功能含义: 查询知识库的文档列表（分页），检查所有权，返回文档分页结果。
- 链路作用: 在 KnowledgeController.listDocuments() 中调用，提供文档列表查询接口。

### 3.5 deleteDocument()
- 函数签名: `void deleteDocument(String documentId, Long userId)`
- 入参:
  - `documentId`: 文档ID
  - `userId`: 当前用户ID（用于权限校验）
- 出参: 无（void）
- 功能含义: 删除文档，检查所有权，删除向量数据（通过 AsyncDocumentProcessor），删除 MinIO 文件，删除数据库记录，更新知识库统计。
- 链路作用: 在 KnowledgeController.deleteDocument() 中调用，执行文档删除用例。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 KnowledgeApplicationService.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
