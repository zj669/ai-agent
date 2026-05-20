# Frontend API And SSE

The frontend uses Axios for ordinary HTTP APIs, streaming `fetch` for workflow
execution streams, and browser `EventSource` for swarm UI/agent streams.

## HTTP Client Setup

The shared Axios client lives at:

- `ai-agent-foward/src/shared/api/httpClient.ts`

The client is created by `createHttpClient(baseURL = '/')`:

```ts
const instance = axios.create({
  baseURL,
  timeout: 10000
})
```

The default exported client is:

```ts
export const httpClient = createHttpClient('/')
```

`src/shared/api/client.ts` re-exports the same client as `apiClient`:

```ts
export { createHttpClient as createApiClient, httpClient as apiClient } from './httpClient'
```

Use `apiClient` or an injected `ApiClientLike` in adapters. Do not create
feature-local Axios instances unless a backend endpoint requires different
transport behavior.

## Vite Proxy

The frontend dev server proxy is configured in `ai-agent-foward/vite.config.ts`.

Observed proxy targets:

- `/api` -> `http://localhost:8080`
- `/client` -> `http://localhost:8080`

Because the Axios base URL is `/`, adapters should use backend paths such as
`/api/agent/list` and `/client/user/login`. Do not hard-code
`http://localhost:8080` in frontend source.

## Request Interceptor

`httpClient.ts` reads the access token from:

- `localStorage.getItem('accessToken')`
- `sessionStorage.getItem('accessToken')`

When a token exists, the request interceptor sets:

```ts
Authorization: Bearer <token>
```

This means normal Axios adapters should not manually repeat Authorization
headers. Central token injection belongs in the shared client.

## Response Interceptor

The response interceptor maps thrown Axios errors through:

- `ai-agent-foward/src/shared/api/errorMapper.ts`

Mapped auth failures have normalized codes:

- `TOKEN_EXPIRED`
- `UNAUTHORIZED`

When either code is observed, the interceptor removes `accessToken` from both
local storage and session storage. It then redirects to `/login?redirect=<path>`
unless the user is already on `/login` or `/register`.

Feature code should catch errors as `unknown`, derive a user-facing message, and
avoid assuming raw Axios error shape.

## Standard Response Envelope

The standard backend envelope helper lives at:

- `ai-agent-foward/src/shared/api/response.ts`

It defines:

```ts
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
```

Adapters that receive `ApiResponse<T>` should call `unwrapResponse(response)`
and return the `data` value.

Observed adapters following this pattern:

- `src/shared/api/adapters/agentAdapter.ts`
- `src/shared/api/adapters/authAdapter.ts`
- `src/shared/api/adapters/metadataAdapter.ts`
- `src/shared/api/adapters/chatAdapter.ts` for standard chat endpoints

## Non-Envelope Endpoints

Not every backend endpoint returns `ApiResponse<T>`.

Observed examples in `chatAdapter.ts`:

- `getExecution` calls `/api/workflow/execution/${executionId}` and returns
  `response.data` directly.
- `getReviewDetail` calls `/api/workflow/reviews/${executionId}` and returns
  `response.data` directly.
- `resumeExecution` and `rejectExecution` post to review endpoints and do not
  unwrap a standard response.

Always inspect the backend or existing adapter before adding `unwrapResponse`.
Review endpoints are known exceptions.

## Adapter Pattern

Adapters live under `src/shared/api/adapters`.

An adapter should:

1. Define request payload interfaces near the API function.
2. Define DTO interfaces matching backend response shape.
3. Accept `client: ApiClientLike = apiClient` when test injection is useful.
4. Use `ApiResponse<T>` only for endpoints that return the standard envelope.
5. Return typed data, not raw Axios responses, unless the caller explicitly
   needs headers or status.

Module services can wrap adapters when they map backend DTOs into UI-specific
state. Example:

- `src/modules/workflow/api/workflowService.ts` wraps agent adapter calls and
  parses `graphJson`.
- `src/modules/chat/api/chatService.ts` maps message DTOs into `ChatMessage`.

## Workflow SSE Consumption

Workflow execution streaming for chat is implemented in:

- `ai-agent-foward/src/modules/chat/api/chatService.ts`

The stream starts with a `fetch` request:

```ts
fetch('/api/workflow/execution/start', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'debug-user': '1',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  },
  body: JSON.stringify({ ... }),
  signal,
})
```

This is not Axios because the code reads `response.body.getReader()` and parses
server-sent event blocks from a streaming response.

The stream resume endpoint is:

```text
GET /api/workflow/execution/{executionId}/stream
```

It uses the same `fetch` streaming parser.

## Workflow SSE Parser

`consumeExecutionStream` reads chunks from the stream, appends decoded text to
a buffer, splits blocks on `\n\n`, and passes each block into `parseSseBlock`.

`parseSseBlock` supports:

- `event:` lines
- one or more `data:` lines
- `pong`
- JSON data
- raw text fallback when JSON parsing fails

`handleSseEvent` maps server events to UI callbacks:

- `connected` -> `onConnected`
- `start` -> `onNodeStart`
- `update` -> `onDelta`, `onThought`, or `onPaused`
- `finish` -> `onNodeFinish` and sometimes `onFinish`
- `execution_complete` -> `onFinish`
- `error` -> `onError`
- `workflow_paused` -> `onPaused`

When handling workflow streams, preserve this event mapping unless the backend
event contract changes.

## Stream Runtime State

`chatService.ts` tracks `hasAssistantDelta` and `hasFinished` in a local
`StreamRuntimeState`.

This prevents duplicate final messages and avoids firing finish callbacks more
than needed. If backend stream semantics change, update this runtime state logic
with tests in `src/modules/chat/api/__tests__/chatService.test.ts`.

## Swarm EventSource Streams

Swarm SSE uses browser `EventSource`, not streaming `fetch`.

Observed implementation:

- `src/modules/swarm/api/swarmService.ts`
- `src/modules/swarm/hooks/useUIStream.ts`

`subscribeAgentStream(agentId, onEvent)` opens:

```text
/api/swarm/agent/{agentId}/stream?token=<encoded-token>
```

`subscribeUIStream(workspaceId, onEvent)` opens:

```text
/api/swarm/workspace/{workspaceId}/ui-stream?token=<encoded-token>
```

The token is read from session storage first, then local storage, and is passed
as a query parameter because native `EventSource` cannot set custom headers.

## Swarm Event Names

`subscribeUIStream` registers these event names:

- `ui.agent.created`
- `ui.message.created`
- `ui.agent.llm.start`
- `ui.agent.llm.done`
- `ui.agent.stream.start`
- `ui.agent.stream.chunk`
- `ui.agent.stream.done`
- `ui.agent.tool_call.start`
- `ui.agent.tool_call.done`
- `ui.agent.waiting`
- `ui.agent.waiting.done`
- `ui.agent.task-notification`

`subscribeAgentStream` registers:

- default `onmessage`
- `agent.stream`
- `agent.done`
- `agent.error`
- `agent.task-notification`

Hook code must close the EventSource in the effect cleanup.

## Auth Token Handling

Token persistence helpers live in `src/app/auth.ts`.

Observed behavior:

- `saveAccessToken(token, remember = true)` stores the token in local storage
  when remembered.
- It stores the token in session storage when not remembered.
- It removes the token from the opposite storage to avoid conflicting values.
- `clearAccessToken()` removes the token from both storages.

Normal HTTP requests rely on the Axios interceptor.

Streaming `fetch` and `EventSource` must manually include token information
because they bypass Axios interceptors.

## Client Error Handling

Use `mapApiError` for Axios errors through the shared client. It normalizes:

- request timeout to `TIMEOUT`
- `401` to `TOKEN_EXPIRED` or `UNAUTHORIZED`
- `403` to `FORBIDDEN`
- `429` to `RATE_LIMITED`
- missing response status to `NETWORK_ERROR`
- fallback to `UNKNOWN_ERROR`

Feature pages should display user-facing messages through AntD `message`,
local error banners, or a shared feedback helper. Do not leak raw backend error
objects into rendered UI.

## API Anti-Patterns

Avoid these patterns:

1. Creating new Axios clients in feature modules.
2. Duplicating token injection in normal Axios adapters.
3. Using Axios for streaming endpoints that need `ReadableStream`.
4. Forgetting to close `EventSource` instances in hook cleanup.
5. Calling `unwrapResponse` on endpoints that return raw DTOs.
6. Hard-coding backend host names in source files.
7. Swallowing stream errors without calling the relevant callback.
8. Parsing SSE blocks ad hoc in page components.
9. Passing raw `any` API responses into components.
10. Adding query-token SSE without checking whether the backend expects it.
