# 修复认证错误反馈与阻断点 - Implementation Plan

## Checklist

- [x] Load frontend/backend specs before code changes.
- [x] Run GitNexus impact analysis for edited symbols before modifying them.
- [x] Add a shared frontend error-message helper so auth pages can display mapped backend `message` without changing `unwrapResponse`.
- [x] Update auth adapter with reset-password support if missing.
- [x] Update login/register/forgot-password/settings pages to surface specific errors and align reset-password payloads.
- [x] Update backend interceptor exclusions so `/client/user/refresh` is public.
- [x] Add or adjust frontend tests for login error display, registration send-code failure, forgot-password reset, and settings reset payload.
- [x] Add or adjust backend test coverage for refresh route exclusion.
- [x] Run focused frontend tests.
- [x] Run focused backend tests where practical.
- [x] Run `gitnexus_detect_changes` before final report.

## Validation Commands

```bash
cd ai-agent-foward && npm test -- --run src/modules/auth/__tests__/rememberMe.test.tsx src/app/pages/__tests__/register.send-code-flow.test.tsx
cd ai-agent-foward && npm test -- --run src/app/pages/__tests__/forgot-password.test.tsx src/modules/settings/pages/__tests__/settings.auth.test.tsx
./mvnw -pl ai-agent-interfaces -Dtest=WebMvcConfigTest test
```

If exact test file names differ after implementation, run the nearest focused equivalents.

## Risky Files

- `WebMvcConfig.java`: route exclusions affect public/protected API boundaries.
- `errorMapper.ts`: shared API error normalization is used by the Axios interceptor; keep changes additive and low-risk.
- Auth pages: user-facing behavior and route redirects should remain stable.

## Rollback Points

- Frontend error-surfacing changes can be reverted independently from `WebMvcConfig`.
- `/client/user/refresh` whitelist can be reverted independently if product decides refresh must require an unexpired access token.
