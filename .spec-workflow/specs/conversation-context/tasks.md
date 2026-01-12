# Tasks: Conversation Context Implementation

- [x] 1. Domain Layer: Define Conversation and Message Entities
  - File: ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/entity/Conversation.java
  - File: ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/entity/Message.java
  - Implementation: Define Conversation aggregate root and Message entity with all required fields (including JSON fields for ThoughtStep, Citations).
  - Define Value Objects: ThoughtStep (Recursive), Citation, MessageStatus.
  - Define Repository Interface: ConversationRepository.
  - _Leverage: ddd-design.md_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Java DDD Expert | Task: Implement Domain Entities and Repository Interface for Conversation Context. Ensure Checkpoint/ThoughtStep JSON compatibility structure. | Success: Entities and Repository interface defined._

- [x] 2. Infrastructure Layer: Implement Persistence
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/entity/ConversationDO.java
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/entity/MessageDO.java
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/repository/JpaConversationRepository.java
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/converter/ThoughtProcessJsonConverter.java
  - Implementation: Create JPA Data Objects (DO), Repository implementation, and JSON Converters.
  - **Database Index**: Explicitly define composite index `idx_conversation_created` on `(conversation_id, created_at)` in MessageDO using `@Table(indexes = ...)` or DDL.
  - _Leverage: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java (for reference)_
  - _Requirements: 3.1_
  - _Prompt: Role: Spring Data JPA Expert | Task: Implement Infrastructure persistence for Conversation and Message. Ensure composite index for performance. | Success: Repository implementation ready._

- [x] 3. Application Layer: Implement ChatApplicationService
  - File: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java
  - Implementation:
    - `initAssistantMessage`: Create PENDING message.
    - `finalizeMessage`: Update content and status.
    - `appendUserMessage`: Save user input.
    - `getHistory`: Retrieve messages with paging.
  - _Requirements: 1.2, 1.3, 2.3_
  - _Prompt: Role: Spring Service Developer | Task: Implement ChatApplicationService with streaming support (init/finalize) and history retrieval. | Success: Service business logic implemented._

- [x] 4. Application Layer: Event Listeners
  - File: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/ExecutionCompletedListener.java
  - File: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/AutoTitleListener.java
  - Implementation:
    - Listen to `ExecutionCompletedEvent` to finalize Assistant message.
    - Logic for Auto-generating title (stub or call LLM service).
    - **Async Processing**: Apply `@Async` to `AutoTitleListener` to prevent blocking the main thread during LLM generation.
  - _Requirements: 4.1_
  - _Prompt: Role: Spring Event Specialist | Task: Implement Event Listeners. Use @Async for title generation. | Success: Listeners correctly trigger Service methods._

- [x] 5. Interface Layer: Implement ChatController
  - File: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java
  - File: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/ConversationResponse.java
  - File: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/MessageResponse.java
  - Implementation: REST APIs for Conversation CRUD and History.
  - **DTO Pattern**: Define DTOs (e.g., `MessageResponse`) to map Entity fields (like formatting date, simplifying structure) for the frontend.
  - _Requirements: 1.2_
  - _Prompt: Role: API Developer | Task: Implement ChatController endpoints with proper DTOs. | Success: API endpoints verifiable via HTTP._

- [x] 6. Verification
  - Verification Strategy: Use `curl` or unit tests to simulate:
    1. Create Conversation.
    2. User sends message (`appendUserMessage`).
    3. Simulate Workflow execution (Init -> Finalize).
    4. Verify Message content (JSON ThoughtProcess).
    5. Verify Auto-Title trigger.
