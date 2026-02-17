## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/web/KnowledgeController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: KnowledgeController.java
- 知识库模块接口控制器，覆盖知识库 CRUD、文档上传与重试、向量检索测试，并负责 domain 对象到 DTO 的转换。

## 2) 核心方法
- `createDataset(DatasetCreateReq req)`
- `uploadDocument(MultipartFile file, String datasetId, Integer chunkSize, Integer chunkOverlap)`
- `search(SearchReq req)`
- `toDatasetResp(KnowledgeDataset dataset)`
- `toDocumentResp(KnowledgeDocument document)`

## 3) 具体方法
### 3.1 uploadDocument(...)
- 函数签名: `uploadDocument(MultipartFile file, String datasetId, Integer chunkSize, Integer chunkOverlap) -> Response<DocumentResp>`
- 入参: 文档文件、知识库 ID、分块参数
- 出参: 上传后的文档响应
- 功能含义: 构建 `ChunkingConfig` 后调用应用服务入库并触发处理流程。
- 链路作用: 文件上传 -> 文档入库/切片处理 -> 前端轮询状态查询。

## 4) 变更记录
- 2026-02-15: 基于源码回填知识库控制器接口矩阵与 DTO 转换职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
