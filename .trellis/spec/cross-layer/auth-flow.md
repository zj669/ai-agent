# Auth Flow

This flow documents login, token creation, frontend token persistence, request
interception, backend authentication, and SSE authorization behavior.

## Scope

- Login form and frontend adapter.
- Access token storage in local storage or session storage.
- Axios request and response interceptors.
- Backend login endpoint and application/domain authentication.
- JWT creation, validation, invalidation, and request-scoped user context.
- SSE authentication through custom fetch headers or query token fallback.

## Frontend Login Sequence

1. `LoginPage` submits email, password, and remember-me state through
   `authAdapter.login`:
   `ai-agent-foward/src/app/pages/LoginPage.tsx:33`.
2. `LoginPage` saves the returned access token through `saveAccessToken`:
   `ai-agent-foward/src/app/pages/LoginPage.tsx:42`.
3. `LoginPage` only redirects to a relative redirect path after login:
   `ai-agent-foward/src/app/pages/LoginPage.tsx:45`.
4. `authAdapter.login` posts to `/client/user/login`:
   `ai-agent-foward/src/shared/api/adapters/authAdapter.ts:35`.
5. `LoginRequest` contains `email` and `password` on the frontend:
   `ai-agent-foward/src/shared/api/adapters/authAdapter.ts:4`.
6. `LoginDataDTO` expects `token`, `refreshToken`, `expireIn`, `deviceId`, and
   `user`:
   `ai-agent-foward/src/shared/api/adapters/authAdapter.ts:19`.
7. The adapter stores `userInfo` in local storage after login:
   `ai-agent-foward/src/shared/api/adapters/authAdapter.ts:39`.
8. `saveAccessToken` stores the token in local storage when remember-me is
   enabled:
   `ai-agent-foward/src/app/auth.ts:12`.
9. `saveAccessToken` stores the token in session storage when remember-me is
   disabled:
   `ai-agent-foward/src/app/auth.ts:17`.
10. `isAuthenticated` checks both storage locations:
    `ai-agent-foward/src/app/auth.ts:3`.
11. `clearAccessToken` removes both token copies:
    `ai-agent-foward/src/app/auth.ts:7`.

## Frontend Request Interception

1. `httpClient` reads `accessToken` from local storage first and session
   storage second:
   `ai-agent-foward/src/shared/api/httpClient.ts:16`.
2. `httpClient` adds `Authorization: Bearer <token>` when a token exists:
   `ai-agent-foward/src/shared/api/httpClient.ts:20`.
3. The response interceptor clears token state and redirects on HTTP 401:
   `ai-agent-foward/src/shared/api/httpClient.ts:24`.
4. The response interceptor also handles application codes
   `TOKEN_EXPIRED` and `UNAUTHORIZED`:
   `ai-agent-foward/src/shared/api/httpClient.ts:31`.
5. Chat streaming uses fetch instead of Axios but still sends the bearer token:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:336`.
6. Resume streaming also sends the bearer token:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:365`.

## Backend Login Route

1. `UserController` is mapped under `/client/user`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:19`.
2. Login is `POST /client/user/login`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:43`.
3. Backend `LoginRequest` contains `email`, `password`, and optional
   `deviceId`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserRequests.java:34`.
4. `UserController.login` delegates to `UserApplicationService.login`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:47`.
5. `UserApplicationService.login` delegates credential validation to the domain
   authentication service:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java:68`.
6. `UserApplicationService.buildLoginResponse` creates device id, access token,
   refresh token, expiry, and user DTO:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/UserApplicationService.java:182`.
7. `UserLoginResponse` exposes `token`, `refreshToken`, `expireIn`, `deviceId`,
   and `user`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserLoginResponse.java:12`.
8. `Response<T>` wraps successful login responses with `code = 200`,
   `message = "success"`, `data`, and `success = true`:
   `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java:16`.

## Domain Authentication

1. `UserAuthenticationDomainService.login` starts by checking login rate
   limiting:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:123`.
2. It looks up the user by email:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:135`.
3. It verifies the password against the stored hash:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:141`.
4. It rejects disabled users:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:147`.
5. It records login success and returns the authenticated user:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:153`.

## Token Creation and Refresh

1. `JwtTokenService.createToken` signs access tokens with issuer, subject email,
   token type, and expiry:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/token/JwtTokenService.java:40`.
2. `JwtTokenService.invalidateToken` stores invalidated tokens in Redis until
   expiry:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/token/JwtTokenService.java:57`.
3. `JwtTokenService.validateToken` checks Redis blacklist state before JWT
   signature verification:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/token/JwtTokenService.java:78`.
4. `JwtTokenService.getUserIdFromToken` extracts the user id from token
   subject:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/token/JwtTokenService.java:102`.
5. Refresh endpoint is `POST /client/user/refresh`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:53`.
6. Refresh request contains `refreshToken` and validated `deviceId`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserRequests.java:46`.
7. `TokenRefreshResponse` exposes `accessToken`, `refreshToken`, `expiresIn`,
   and `tokenType`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/TokenRefreshResponse.java:15`.

## Request Authentication

1. `WebMvcConfig` installs `LoginInterceptor` for `/client/**` and `/api/**`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java:40`.
2. Login, registration, refresh, reset password, and metadata routes are
   excluded from auth interception:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java:45`.
3. `LoginInterceptor.preHandle` allows CORS `OPTIONS` requests:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:36`.
4. The interceptor has a debug-user shortcut:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:41`.
5. The interceptor extracts bearer tokens from the `Authorization` header:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:58`.
6. The interceptor also accepts query parameter `token` for SSE/EventSource
   style requests:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:67`.
7. Missing tokens return unauthorized before controller execution:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:75`.
8. Valid JWTs populate `UserContext`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:79`.
9. `afterCompletion` clears `UserContext`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:94`.
10. `JwtAuthStrategy` validates tokens with a parser configured from
    `${jwt.secret:defaultSecret}`:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/JwtAuthStrategy.java:16`.

## Authenticated Resource Examples

1. `/api/workflow/execution/start` is under `/api/**` and therefore
   intercepted:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:51`.
2. `/api/chat/conversations` is under `/api/**` and therefore intercepted:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:59`.
3. `/api/workflow/reviews/pending` is under `/api/**` and therefore
   intercepted:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:41`.
4. `UserController.info` reads the authenticated user from `UserContext`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:64`.
5. `ChatController.createConversation` reads user id from `UserContext`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:61`.

## Gotchas

1. The login route is `/client/user/login`, not `/api/user/login`.
2. Frontend `LoginRequest` does not include `deviceId`, but backend supports it
   as optional input.
3. The frontend stores `userInfo` in local storage independently from the access
   token persistence choice.
4. Fetch-based SSE streams must remember to set `Authorization` manually,
   because they do not use the Axios interceptor.
5. Query-token fallback exists for EventSource-style clients, but the current
   chat stream uses fetch headers.
6. `debug-user` bypasses normal auth and should be treated as an environment
   or development concern when assessing production behavior.
7. Token creation uses `JwtTokenService`, while request authentication passes
   through `JwtAuthStrategy`; if auth fails unexpectedly, verify that issuer,
   algorithm, and secret assumptions are compatible.
8. Auth exclusions in `WebMvcConfig` must be updated when adding public
   registration, email-code, token-refresh, or metadata endpoints.
