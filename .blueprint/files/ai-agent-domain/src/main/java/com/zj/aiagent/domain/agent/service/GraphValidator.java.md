## Metadata
- file: `.blueprint/files/ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/service/GraphValidator.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: blueprint-team

# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/service/GraphValidator.java

## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/service/GraphValidator.java
- Type: .java

## Responsibility
- 承载对应领域/应用/基础设施的 Java 类型定义与业务职责实现。

## Key Symbols / Structure
- class GraphValidator
- validate(String graphJson)
- findStartNodeId(JsonNode nodes)
- buildAdjacencyList(JsonNode nodes, JsonNode edges)
- checkConnectivity(String startId, JsonNode nodes, Map<String, List<String>> adj)
- checkCycles(JsonNode nodes, Map<String, List<String>> adj)
- hasCycle(String curr, Map<String, List<String>> adj, Set<String> visited, Set<String> recStack)

## Dependencies
- com.fasterxml.jackson.databind.JsonNode
- com.fasterxml.jackson.databind.ObjectMapper
- lombok.extern.slf4j.Slf4j
- org.springframework.stereotype.Service

## Notes
- updated_at: 2026-02-15 07:36
- status: 正常
- 占位符内容已按源码职责自动回填。
