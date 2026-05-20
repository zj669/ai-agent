# Backend Interfaces Layer

## Purpose And Boundaries

The interfaces layer is the outer delivery shell for the backend. It translates
external protocols into application use cases and serializes use case results
back to clients.

Allowed responsibilities:

- Declare HTTP, SSE, and WebSocket delivery endpoints.
- Bind request data with Spring MVC annotations.
- Run entry-point validation with `@Valid`, `@Validated`, path variables, query
  parameters, and multipart parameters.
- Read the authenticated user from `UserContext` after the interceptor has
  populated it.
- Map incoming payloads into application commands or request DTOs.
- Call application services, domain ports used as read/query boundaries, or
  dedicated delivery adapters.
- Shape responses as `Response<T>`, `ResponseEntity<T>`, or `SseEmitter` where
  the endpoint contract requires streaming.
- Translate thrown exceptions through `GlobalExceptionHandler`.
- Manage delivery lifecycle resources such as SSE subscriptions, heartbeat
  tasks, and emitter cleanup.

Not allowed responsibilities:

- Do not place business decisions in controllers.
- Do not implement workflow scheduling, agent publishing, knowledge chunking,
  LLM selection, MCP execution, or persistence rules in controllers.
- Do not bypass application services or domain ports to mutate data directly.
- Do not inject raw Redis clients such as `RedisTemplate` into controllers.
- Do not create a second authentication path outside `LoginInterceptor`.
- Do not add `/api/user`; the user/auth route prefix is `/client/user`.

The current application entry point is
`ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java`. It
uses `@SpringBootApplication`, `@EnableScheduling`, and `@EnableAsync`.

## Controller Naming And Layout

Controller classes use the `*Controller` suffix and live under:

```text
ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/
```

Observed package layout:

```text
interfaces/
  agent/web/AgentController.java
  chat/ChatController.java
  common/advice/GlobalExceptionHandler.java
  common/config/WebMvcConfig.java
  common/config/HttpsConfig.java
  common/interceptor/LoginInterceptor.java
  dashboard/web/DashboardController.java
  knowledge/web/KnowledgeController.java
  llm/LlmConfigController.java
  mcp/McpServerController.java
  meta/MetadataController.java
  swarm/Swarm*Controller.java
  user/UserController.java
  workflow/WorkflowController.java
  workflow/HumanReviewController.java
  writing/WritingController.java
```

Layout rules:

- Keep REST/SSE delivery code in `interfaces/**`.
- Put cross-cutting web advice in `interfaces/common/advice`.
- Put web delivery configuration in `interfaces/common/config`.
- Put authentication interception in `interfaces/common/interceptor`.
- Module-specific request/response DTOs may live beside the controller under a
  local `dto` package.
- Keep application service orchestration in `ai-agent-application`, not in
  `interfaces`.

## Path Conventions

The standard authenticated business API prefix is `/api/**`.

The user/auth prefix is the important exception:

```text
/client/user
```

Do not document or add user/auth endpoints under `/api/user`.

Verified controller inventory:

| Controller | Verified base path or route family | Primary purpose |
| --- | --- | --- |
| `AgentController` | `/api/agent` | Agent create, update, publish, rollback, delete, list, detail, versions |
| `WorkflowController` | `/api/workflow/execution` | Workflow start, stream re-subscribe, stop, pause, execution detail, logs, context |
| `HumanReviewController` | `/api/workflow/reviews` | Pending reviews, review detail, resume, reject, review history |
| `ChatController` | `/api/chat` | Conversation CRUD, message history, SSE-backed message send |
| `KnowledgeController` | `/api/knowledge` | Dataset CRUD, document upload/list/detail/delete/retry, search |
| `UserController` | `/client/user` | Email code, register, login, refresh, profile, logout, password reset |
| `MetadataController` | `/api/meta` | Node template and node type metadata |
| `DashboardController` | `/api/dashboard` | Aggregated dashboard stats |
| `LlmConfigController` | `/api/llm-config` | User-scoped LLM provider config CRUD and connectivity test |
| `McpServerController` | `/api/mcp` | MCP server CRUD, connect/disconnect/status, tool discovery |
| `WritingController` | `/api/writing` | Writing workspace sessions and session overview projection |
| `SwarmWorkspaceController` | `/api/swarm/workspace` | Workspace CRUD and defaults |
| `SwarmAgentController` | `/api/swarm/workspace/{wid}/agents`, `/api/swarm/agent/{id}`, `/api/swarm/agents/interrupt-all` | Workspace agents and runtime stop |
| `SwarmGroupController` | `/api/swarm/workspace/{wid}/groups`, `/api/swarm/group/{gid}/messages` | Groups and group messages |
| `SwarmGraphController` | `/api/swarm/workspace/{wid}/graph`, `/api/swarm/workspace/{wid}/search` | Workspace graph and search |
| `SwarmSseController` | `/api/swarm/agent/{agentId}/stream`, `/api/swarm/workspace/{workspaceId}/ui-stream` | Swarm agent and UI SSE streams |

Current server and management paths:

- Spring Boot listens on port `8080`.
- Actuator web endpoints use `/actuator`.
- Exposed actuator IDs include `health`, `info`, `metrics`, `prometheus`,
  `env`, and `configprops`.

## REST Controller Pattern

Controllers are Spring MVC components:

```java
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentApplicationService agentApplicationService;

    @PostMapping("/create")
    public Response<Long> createAgent(
            @Validated(AgentRequest.Create.class)
            @RequestBody AgentRequest.SaveAgentRequest req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }

        AgentCommand.CreateAgentCmd cmd = new AgentCommand.CreateAgentCmd();
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setIcon(req.getIcon());

        return Response.success(agentApplicationService.createAgent(cmd));
    }
}
```

Rules from the example:

- Declare the class-level prefix with `@RequestMapping` when the controller owns
  a coherent route family.
- Use method-level `@GetMapping`, `@PostMapping`, `@PutMapping`,
  `@DeleteMapping`, or `@PatchMapping` for individual endpoints.
- Use constructor injection through `@RequiredArgsConstructor`.
- Obtain user identity from `UserContext`, not from request payloads.
- Convert web request DTOs into application commands before calling use cases.
- Return `Response.success(...)` for the normal JSON envelope where the module
  follows the shared response contract.
- Keep only request mapping, validation, command construction, and response
  shaping in the controller.

`Response<T>` is the shared JSON envelope:

```java
public class Response<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;
}
```

Most JSON controllers use `Response<T>`. Workflow debug and human review
endpoints also use `ResponseEntity<T>` where they need explicit status control,
empty bodies, or raw DTOs. SSE endpoints return `SseEmitter`.

## Validation And Request Mapping

Use Jakarta validation for request bodies and fields that define the external
contract:

```java
@PostMapping(value = "/start", produces = "text/event-stream;charset=UTF-8")
public SseEmitter startExecution(
    @Valid @RequestBody StartExecutionRequest request,
    jakarta.servlet.http.HttpServletResponse response
) {
    ...
}

@Data
public static class StartExecutionRequest {
    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotNull(message = "userId is required")
    private Long userId;
}
```

Use `@RequestParam` for pagination, filters, multipart metadata, and simple
command parameters. Use `@PathVariable` for resource identifiers in the route.
For multipart uploads, bind the file with `@RequestParam("file")
MultipartFile`.

Validation failures are translated by `GlobalExceptionHandler` into HTTP 400
with a shared `Response<Void>` body.

## SSE And Streaming

Workflow and chat streaming use the same Redis Pub/Sub bridge.

Producer side:

```text
NodeExecutorStrategy
  -> StreamPublisher
  -> RedisSseStreamPublisher
  -> RedisSsePublisher
  -> IRedisService.publish("workflow:channel:{executionId}", json)
```

Controller subscriber side:

```text
WorkflowController or ChatController
  -> create SseEmitter
  -> subscribe RedisSseListener to "workflow:channel:{executionId}"
  -> forward each SseEventPayload as SseEmitter.event()
  -> send ping heartbeats
  -> remove Redis listener and cancel heartbeat on completion, timeout, or error
```

Canonical workflow SSE pattern:

```java
@PostMapping(value = "/start", produces = "text/event-stream;charset=UTF-8")
public SseEmitter startExecution(
    @Valid @RequestBody StartExecutionRequest request,
    jakarta.servlet.http.HttpServletResponse response
) {
    response.setCharacterEncoding("UTF-8");

    String executionId = UUID.randomUUID().toString();
    SseEmitter emitter = createExecutionEmitter(executionId, true);

    CompletableFuture.runAsync(() -> {
        schedulerService.startExecution(
            executionId,
            request.getAgentId(),
            request.getUserId(),
            request.getConversationId(),
            request.getVersionId(),
            request.getInputs(),
            request.getMode()
        );
    });

    return emitter;
}
```

Redis subscription and cleanup pattern:

```java
SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
String channel = "workflow:channel:" + executionId;

RedisSseListener listener = new RedisSseListener(objectMapper, payload -> {
    String eventName = payload.getEventType() != null
        ? payload.getEventType().name().toLowerCase()
        : "message";
    emitter.send(SseEmitter.event()
        .name(eventName)
        .data(payload, MediaType.APPLICATION_JSON));
});

redisMessageListenerContainer.addMessageListener(
    listener,
    new ChannelTopic(channel)
);

var heartbeatTask = workflowTaskScheduler.scheduleAtFixedRate(
    () -> emitter.send(SseEmitter.event().name("ping").data("pong")),
    15,
    15,
    TimeUnit.SECONDS
);

Runnable cleanUp = () -> {
    redisMessageListenerContainer.removeMessageListener(listener);
    heartbeatTask.cancel(true);
};
emitter.onCompletion(cleanUp);
emitter.onTimeout(cleanUp);
emitter.onError(e -> cleanUp.run());
```

The Redis publisher must remain outside controllers:

```java
@Component
@RequiredArgsConstructor
public class RedisSsePublisher {
    private final IRedisService redisService;
    private final ObjectMapper objectMapper;

    public void publish(SseEventPayload payload) {
        String channel = "workflow:channel:" + payload.getExecutionId();
        String message = objectMapper.writeValueAsString(payload);
        redisService.publish(channel, message);
    }
}
```

`ChatController.sendMessage` follows the same channel naming convention and
starts workflow execution asynchronously after emitting a `connected` event.

`SwarmSseController` is a separate SSE family. It subscribes to
`SwarmAgentEventBus` and `SwarmUIEventBus` instead of Redis channels, returns
`SseEmitter(0L)`, and unsubscribes the event bus callback on completion,
timeout, or error.

SSE rules:

- Always set the response content type to `text/event-stream` for streaming
  endpoints.
- Use UTF-8 explicitly for workflow streams.
- Send a `connected` event when a stream is established if the client needs the
  generated execution ID.
- Send `ping` heartbeats for long-running workflow/chat streams.
- Close the emitter on terminal workflow events after the final `END` node or
  when the payload indicates there is no node type.
- Treat client disconnects as normal lifecycle events, not business failures.

## WebSocket Delivery

The current WebSocket/STOMP configuration is in:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java
```

It declares:

- `@Configuration`
- `@EnableWebSocketMessageBroker`
- simple broker prefixes `/topic` and `/queue`
- application destination prefix `/app`
- SockJS endpoint `/ws`

The observed WebSocket publisher is
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/WebSocketMessageService.java`.
It uses `SimpMessagingTemplate` to send conversation title updates to:

```text
/topic/conversation/{conversationId}/title
```

No inbound `@MessageMapping` handlers were found in the interfaces source
during this spec pass. Treat WebSocket as server-push delivery unless an inbound
handler is added later and documented with the same route inventory discipline
as REST controllers.

## Error Translation

Global error translation lives in:

```text
ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java
```

Observed mappings:

| Exception | HTTP status | Response behavior |
| --- | --- | --- |
| `AsyncRequestNotUsableException` | already streaming | Void handler, debug log only, no JSON body |
| `AsyncRequestTimeoutException` | `503 SERVICE_UNAVAILABLE` | Void handler, debug log only |
| `AuthenticationException` with invalid credentials, disabled user, invalid refresh token | `401 UNAUTHORIZED` | `Response.error(status, message)` |
| `AuthenticationException` with rate limiting | `429 TOO_MANY_REQUESTS` | `Response.error(status, message)` |
| other `AuthenticationException` | `400 BAD_REQUEST` | `Response.error(status, message)` |
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | field messages joined with `; ` |
| `ConstraintViolationException` | `400 BAD_REQUEST` | validation message |
| `IllegalArgumentException` | `400 BAD_REQUEST` | exception message |
| `IllegalStateException` | `400 BAD_REQUEST` | exception message |
| `OptimisticLockingFailureException` | `409 CONFLICT` | fixed review conflict message |
| `SecurityException` | `403 FORBIDDEN` | exception message |
| `Exception` | `500 INTERNAL_SERVER_ERROR` | generic server error message |

Controller rules:

- Throw `IllegalArgumentException` for malformed client input that cannot be
  expressed with bean validation.
- Throw `IllegalStateException` for invalid state transitions visible at the API
  boundary.
- Throw `SecurityException` when authenticated context exists but the user is
  not allowed to perform the action.
- Let broken SSE connections reach the async handlers; do not try to write a
  JSON error envelope after the response stream has started.
- Do not catch broad exceptions in normal REST endpoints unless the endpoint
  must complete an SSE emitter or release streaming resources.

## Spring Configuration Beans

Configuration classes are split between root backend configuration and
interfaces-specific web configuration.

Root configuration package:

```text
ai-agent-interfaces/src/main/java/com/zj/aiagent/config/
```

Observed classes:

- `ThreadPoolConfig` creates the shared `ThreadPoolExecutor` from
  `ThreadPoolConfigProperties`.
- `ThreadPoolConfigProperties` binds `thread.pool.executor.config`.
- `RestClientConfig` creates the primary Apache HttpClient backed
  `RestClient.Builder`.
- `WebClientConfig` creates the primary Reactor `WebClient.Builder`.
- `AuthDebugProperties` binds `auth.debug`.
- `MybatisPlusConfig` registers pagination and optimistic locking interceptors.
- `DataSourceConfig`, `EmbeddingModelConfig`, and `AutoFillConfig` provide
  data source, embedding, and persistence support configuration.

Interfaces web configuration package:

```text
ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/
```

Observed classes:

- `WebMvcConfig` registers authentication interception, CORS, and scheduler
  beans.
- `HttpsConfig` is active only under the `prod` profile and configures Tomcat
  HTTP to HTTPS redirect behavior when the server port is `8080`.

Important `WebMvcConfig` beans:

```java
@Bean("workflowTaskScheduler")
public ScheduledExecutorService workflowTaskScheduler() {
    return Executors.newScheduledThreadPool(10);
}

@Bean("heartbeatScheduler")
public ScheduledExecutorService heartbeatScheduler() {
    return Executors.newScheduledThreadPool(2);
}
```

Use these named schedulers for workflow and chat SSE heartbeats. Do not create a
new scheduler in a controller.

## Auth And Security

Authentication is enforced by:

```text
ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java
ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java
```

`WebMvcConfig` applies `LoginInterceptor` to:

```text
/client/**
/api/**
```

Current exclusions:

```text
/client/user/login
/client/user/register
/client/user/send-code
/client/user/email/sendCode
/client/user/email/register
/client/user/resetPassword
/api/meta/**
```

`LoginInterceptor` behavior:

- Allows `OPTIONS` requests for CORS preflight.
- Supports development/test debug authentication through `auth.debug` and its
  configured header name.
- Authenticates JWT from `Authorization: Bearer <token>`.
- Authenticates JWT from query parameter `token` for SSE/EventSource clients
  that cannot set headers.
- Sets `UserContext.setUserId(userId)` after successful authentication.
- Clears `UserContext` in `afterCompletion` to prevent ThreadLocal leakage.
- Returns HTTP `401` directly when no strategy authenticates the request.

Security rules:

- Controllers should trust `UserContext` only after interception.
- Public routes must be explicitly excluded in `WebMvcConfig`.
- SSE routes that need browser `EventSource` support may accept `token` query
  parameters through the interceptor. Do not parse tokens manually in the
  controller.
- Debug authentication must remain controlled by `auth.debug` and must not be
  enabled by controller code.

## Anti-Patterns

Do not add these patterns:

- Business logic inside controllers, especially workflow graph decisions,
  version publishing rules, human review state transitions, knowledge chunking,
  or LLM provider selection.
- Direct repository or persistence mutation from controllers when an
  application service already owns the use case.
- Raw `RedisTemplate`, `StringRedisTemplate`, `RedissonClient`, or direct Redis
  publish calls in controllers. Workflow/chat SSE publishing goes through
  `RedisSsePublisher` and `IRedisService`; controllers only subscribe and
  forward events.
- New user/auth endpoints under `/api/user`. The verified prefix is
  `/client/user`.
- New unauthenticated `/api/**` routes without an explicit `WebMvcConfig`
  exclusion and a documented reason.
- Catch-all `catch (Exception)` blocks around normal REST endpoints that hide
  the global exception mapping.
- JSON error envelopes written after an SSE stream has begun.
- Creating new `ScheduledExecutorService` instances in controllers instead of
  using the named scheduler beans.
- Duplicating response envelopes instead of using `Response<T>`.
- Returning raw maps for stable API contracts when a DTO already exists.
- Logging Authorization headers, tokens, refresh tokens, API keys, or provider
  secrets.

## Change Checklist

Before changing the interfaces layer:

- Locate the existing controller or configuration class with code search or
  GitNexus.
- Verify the route prefix against the actual Spring annotations.
- Check whether the endpoint is JSON, SSE, or WebSocket delivery.
- Reuse existing application services, commands, DTOs, and response envelopes.
- Confirm whether the route is authenticated by `LoginInterceptor`.
- Confirm whether the change requires a `WebMvcConfig` exclusion.
- For streaming, confirm cleanup removes listeners and cancels heartbeats.
- For Redis-backed SSE, publish through `RedisSsePublisher` or an existing
  domain port implementation, not from the controller.
- For response shape changes, verify frontend consumers and API docs that read
  the route.

## Evidence Used For This Spec

This spec was derived from the current code and architecture references:

- `CLAUDE.md`
- `README.md`
- `docs/PROJECT_QUICK_CONTEXT.md`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/**/*Controller.java`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/HttpsConfig.java`
- `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSseListener.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/WebSocketMessageService.java`
- `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java`
- `ai-agent-interfaces/src/main/resources/application.yml`

