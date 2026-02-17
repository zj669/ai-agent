# Blueprint Mirror: ai-agent-application/src/test/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceTest.java

## Metadata
- file: `ai-agent-application/src/test/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceTest.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/test/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationServiceTest.java
- Type: .java

## Responsibility
- `KnowledgeApplicationService` 单元测试，验证应用编排行为与依赖交互。

## Key Symbols / Structure
- 测试分组
  - `CreateDatasetTests`
  - `QueryDatasetTests`
  - `DeleteDatasetTests`
  - `UploadDocumentTests`
  - `QueryDocumentTests`
  - `DeleteDocumentTests`
- 关键验证点
  - 数据集创建字段初始化
  - 查询不存在资源抛异常
  - 上传后触发 `asyncDocumentProcessor.processDocumentAsync`
  - 删除文档时联动向量删除、文件删除与统计回写

## Dependencies
- JUnit5, Mockito, AssertJ
- Mock 组件：`KnowledgeDatasetRepository`, `KnowledgeDocumentRepository`, `FileStorageService`, `AsyncDocumentProcessor`

## Notes
- 状态: 正常
- 使用 `ReflectionTestUtils` 注入 `bucketName`。
