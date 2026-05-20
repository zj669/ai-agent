# Knowledge Retrieval Flow

This flow documents how workflow execution reads long-term memory and knowledge
base content across scheduler, node strategy, vector-store abstraction, Milvus
adapter, and disabled-vector-store fallback.

## Scope

- Long-term memory hydration before workflow node scheduling.
- Short-term chat history hydration from persisted conversations.
- LLM node RAG enrichment through `KnowledgeRetrievalService`.
- Explicit knowledge node retrieval by dataset id.
- Milvus-backed semantic, keyword, and hybrid retrieval.
- No-op vector store behavior when Milvus is disabled.

## Memory Hydration Before Execution

1. The scheduler calls `hydrateMemory` before `Execution.start`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:293`.
2. `hydrateMemory` extracts a user query from initial inputs:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:315`.
3. `extractUserQuery` accepts `inputMessage`, `input`, `query`, and `message`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:374`.
4. If a query and agent id exist, the scheduler calls
   `vectorStore.search(userQuery, agentId)`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:321`.
5. Returned long-term memories are stored on `ExecutionContext`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:328`.
6. Memory hydration failures are caught, logged, and ignored:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:329`.
7. If a conversation id exists, recent messages are loaded from the
   conversation repository:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:342`.
8. The scheduler stores recent messages in `ExecutionContext.chatHistory`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:360`.
9. Conversation-memory hydration failures are also caught, logged, and ignored:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:365`.
10. `ExecutionContext` owns long-term memories and chat history fields:
    `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java:55`.

## LLM Node RAG Enrichment

1. `LlmNodeExecutorStrategy` receives `KnowledgeRetrievalService` as a
   dependency:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:57`.
2. The scheduler injects `__context__` and `__agentId__` before strategy
   execution:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:867`.
3. The LLM strategy reads `ExecutionContext` and `agentId` from those inputs:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:153`.
4. The LLM strategy builds system prompt, user prompt, and message chain before
   calling the model:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:158`.
5. The strategy retrieves knowledge by agent id when RAG is enabled in node
   config:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:299`.
6. The RAG path calls `knowledgeRetrievalService.retrieve(agentId, userInput,
   ragTopK)`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:313`.
7. Retrieved knowledge chunks are appended to the system prompt:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:320`.
8. RAG failures are logged and ignored so the LLM node can continue:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:328`.
9. Model streaming publishes deltas to the same node stream publisher:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:185`.

## Explicit Knowledge Node

1. `KnowledgeNodeExecutorStrategy` is the runtime executor for knowledge nodes:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:28`.
2. It reads `knowledge_dataset_id`, `search_strategy`, and `knowledge_top_k`
   from node config:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:37`.
3. It reads the query from resolved node inputs:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:42`.
4. It fails the node when dataset id is blank:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:45`.
5. It fails the node when query is blank:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:54`.
6. It defaults `topK` to 5 and strategy to `SEMANTIC`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:62`.
7. It calls `retrieveByDataset(datasetId, query, topK, strategy)`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:75`.
8. It returns results under output key `knowledge_list`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:85`.
9. Retrieval exceptions fail the node:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/KnowledgeNodeExecutorStrategy.java:88`.

## Application Service Contract

1. `KnowledgeRetrievalService.retrieve(Long agentId, String query, int topK)`
   is the agent-scoped retrieval contract:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/service/KnowledgeRetrievalService.java:14`.
2. `KnowledgeRetrievalService.retrieveByDataset` is the dataset-scoped
   retrieval contract:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/service/KnowledgeRetrievalService.java:33`.
3. `KnowledgeRetrievalServiceImpl.retrieve` builds a vector search request with
   filter expression `agent_id == {agentId}`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:39`.
4. Agent-scoped retrieval calls `vectorStore.similaritySearch`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:52`.
5. Agent-scoped retrieval maps documents to text content:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:57`.
6. Agent-scoped retrieval returns an empty list on failure:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:63`.
7. Dataset-scoped retrieval switches between keyword, hybrid, and semantic
   search:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:71`.
8. Dataset-scoped retrieval returns an empty list on failure:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeRetrievalServiceImpl.java:95`.

## Vector Store Contract

1. `VectorStore.search` is the long-term memory retrieval method:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:28`.
2. `VectorStore.searchKnowledge` is the agent knowledge retrieval method:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:42`.
3. `VectorStore.similaritySearch` accepts structured search requests:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:64`.
4. `VectorStore.searchKnowledgeByDataset` is the dataset semantic retrieval
   method:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:70`.
5. `VectorStore.keywordSearchByDataset` and `hybridSearchByDataset` have
   default fallbacks unless overridden:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:78`.
6. `VectorStore` also defines document add and delete contracts:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java:94`.

## Milvus Adapter Behavior

1. `MilvusVectorStoreAdapter` depends on separate memory and knowledge Spring AI
   vector stores:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:32`.
2. Long-term memory search uses the memory store:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:52`.
3. Long-term memory search filters on `agent_id`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:66`.
4. Agent knowledge search uses the knowledge store:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:89`.
5. Agent knowledge search filters on `agent_id`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:103`.
6. `similaritySearch` converts Spring AI documents to domain documents:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:247`.
7. Dataset semantic search filters on `dataset_id`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:280`.
8. Keyword search ranks semantic candidates with `KeywordScorer` and falls back
   to semantic results:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:327`.
9. Hybrid search combines semantic similarity and keyword score:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:369`.

## Milvus Configuration and Disabled Mode

1. Milvus beans are conditional on `milvus.enabled=true`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java:15`.
2. The default Milvus host is `localhost`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java:38`.
3. The default Milvus port is `19530`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java:42`.
4. The default knowledge collection is `agent_knowledge_base`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java:46`.
5. The default memory collection is `agent_chat_memory`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java:50`.
6. `NoOpVectorStoreAdapter` is active when Milvus is disabled or missing:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/NoOpVectorStoreAdapter.java:20`.
7. The no-op adapter returns empty long-term memory results:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/NoOpVectorStoreAdapter.java:31`.
8. The no-op adapter returns empty knowledge results:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/NoOpVectorStoreAdapter.java:40`.

## Gotchas

1. Memory hydration is best-effort; missing Milvus does not block workflow
   execution.
2. Explicit knowledge nodes fail when required dataset or query inputs are
   absent, unlike memory hydration and LLM RAG which continue on retrieval
   failure.
3. Agent-scoped RAG and dataset-scoped knowledge nodes use different metadata
   filters.
4. The LLM node appends retrieved knowledge to the system prompt, not to a
   separate tool-call channel.
5. Keyword and hybrid retrieval are implemented in the Milvus adapter on top of
   semantic candidates, not as independent database indexes.
6. `knowledge_top_k` parsing failures fall back to 5.
7. `search_strategy` parsing failures fall back to semantic behavior through
   service-level error handling.
8. Any metadata key rename must update document ingestion, vector filters, and
   this flow together.

