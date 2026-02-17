## Metadata
- file: `.blueprint/files/ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDocument.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: blueprint-team

# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDocument.java

## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDocument.java
- Type: .java

## Responsibility
- 承载对应领域/应用/基础设施的 Java 类型定义与业务职责实现。

## Key Symbols / Structure
- class KnowledgeDocument
- startProcessing()
- updateProgress(int processedChunks)
- setTotalChunksCount(int totalChunks)
- markCompleted()
- markFailed(String errorMessage)
- getProgressPercentage()

## Dependencies
- com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig
- com.zj.aiagent.domain.knowledge.valobj.DocumentStatus
- lombok.AllArgsConstructor
- lombok.Builder
- lombok.Data
- lombok.NoArgsConstructor

## Notes
- updated_at: 2026-02-15 07:36
- status: 正常
- 占位符内容已按源码职责自动回填。
