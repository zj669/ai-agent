# 修复认证错误反馈与阻断点 - Design

## Boundaries

This task touches the authentication UX and the smallest backend route configuration needed to make documented auth flows reachable.

- Frontend pages:
  - `ai-agent-foward/src/app/pages/LoginPage.tsx`
  - `ai-agent-foward/src/app/pages/RegisterPage.tsx`
  - `ai-agent-foward/src/app/pages/ForgotPasswordPage.tsx`
  - `ai-agent-foward/src/modules/settings/pages/SettingsPage.tsx`
- Frontend shared API helpers:
  - `ai-agent-foward/src/shared/api/errorMapper.ts`
  - `ai-agent-foward/src/shared/api/adapters/authAdapter.ts`
- Backend route guard:
  - `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java`

## Data Flow

1. Backend returns unified `Response<T>` for auth endpoints.
2. Frontend API adapter receives Axios response.
3. Axios non-2xx errors are mapped by `mapApiError`.
4. Auth pages use a shared message helper to display mapped backend `message`; only unknown failures use local fallback text.
5. Refresh requests reach `UserController.refreshToken` without needing a valid access token. The refresh token and device id remain responsible for validation.

## Error Contract

Backend `GlobalExceptionHandler` currently returns:

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "success": false
}
```

Frontend should preserve `message` for user-facing toasts. It should not display a generic "登录失败，请重试" when the server supplied a precise reason.

## Compatibility

- Keep existing access token keys: `accessToken` in local or session storage.
- Keep existing `userInfo` local storage behavior.
- Do not change login success response fields.
- Do not add a new auth state store in this task.
- Do not change backend DTO names or route paths.
- Do not change `unwrapResponse`; GitNexus marks it `CRITICAL` risk because it is shared across many modules.

## Known Follow-Ups

- Access-token blacklist is not currently enforced by `LoginInterceptor`; that is a deeper backend auth hardening task.
- Frontend refresh-token persistence and automatic access-token renewal are not implemented in this task.
- Login failure rate limiting currently counts attempts before credential validation; that is not changed in this task unless required by tests.
