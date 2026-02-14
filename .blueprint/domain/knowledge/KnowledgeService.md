## Metadata
- file: `.blueprint/domain/knowledge/KnowledgeService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: KnowledgeService
- 该文件用于描述 KnowledgeService 的职责边界与协作关系。

## 2) 核心方法
- `createDataset()`
- `uploadDocument()`
- `processDocument()`
- `retrieve()`

## 3) 具体方法
### 3.1 createDataset()
- 函数签名: `KnowledgeDataset createDataset(String name, String description, Long userId, Long agentId)`（KnowledgeApplicationService）
- 入参:
  - `name`: 知识库名称
  - `description`: 描述
  - `userId`: 用户ID
  - `agentId`: 绑定的 Agent ID（可选）
- 出参: KnowledgeDataset 实体（创建的知识库）
- 功能含义: 创建知识库，生成 datasetId（UUID），构建 KnowledgeDataset 实体并持久化。
- 链路作用: 在 KnowledgeController.createDataset() 中调用，执行知识库创建逻辑。

### 3.2 uploadDocument()
- 函数签名: `KnowledgeDocument uploadDocument(String datasetId, MultipartFile file, ChunkingConfig chunkingConfig)`（KnowledgeApplicationService）
- 入参:
  - `datasetId`: 知识库ID
  - `file`: 上传的文件
  - `chunkingConfig`: 分块配置（可选）
- 出参: KnowledgeDocument 实体（创建的文档）
- 功能含义: 上传文档到知识库，文件安全校验，上传到 MinIO，构建 KnowledgeDocument 实体，保存文档记录（状态：PENDING），更新知识库统计，触发异步处理（解析、分块、向量化）。
- 链路作用: 在 KnowledgeController.uploadDocument() 中调用，执行文档上传逻辑。

### 3.3 processDocument()
- 函数签名: `void processDocumentAsync(KnowledgeDocument document)`（AsyncDocumentProcessor）
- 入参:
  - `document`: 文档实体
- 出参: 无（void，异步执行）
- 功能含义: 异步处理文档，解析文本内容，分块（根据 ChunkingConfig），生成 Embedding，存储到 Milvus，更新文档状态（PROCESSING → COMPLETED/FAILED）。
- 链路作用: 在 KnowledgeApplicationService.uploadDocument() 中调用，触发异步文档处理。

### 3.4 retrieve()
- 函数签名: `List<String> searchKnowledgeByDataset(String datasetId, String query, int topK)`（VectorStore）
- 入参:
  - `datasetId`: 知识库ID
  - `query`: 查询文本
  - `topK`: 返回结果数量
- 出参: 相关知识文本列表
- 功能含义: 根据 datasetId 和 query 检索知识库，自动添加 metadata filter，返回相似度最高的文本。
- 链路作用: 在工作流 LTM 加载或知识库检索场景中调用，提供知识检索能力。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 KnowledgeApplicationService.java 和 AsyncDocumentProcessor 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
