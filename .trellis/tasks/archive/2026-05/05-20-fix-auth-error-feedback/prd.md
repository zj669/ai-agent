# 修复认证错误反馈与阻断点

## Goal

认证相关页面在后端返回明确错误时，应向用户展示可理解的具体原因，而不是统一显示“登录失败 / 注册失败 / 重置失败”。同时修复当前已确认会阻断认证流程的前后端契约问题，保证登录、注册、重置密码、刷新 Token 的基础路径可用。

## Requirements

- 登录页调用 `/client/user/login` 失败时，展示后端返回的错误消息，例如账号密码错误、用户禁用、登录频率限制、参数校验失败等，不再统一吞成“登录失败，请重试”。
- 注册页发送验证码、提交注册失败时，展示具体错误消息；发送验证码失败后不能让用户误以为验证码已可用。
- 忘记密码页必须完成真实重置密码流程：发送验证码、输入验证码、新密码、确认密码并调用 `/client/user/resetPassword`。
- 设置页安全设置中的重置密码请求必须与后端 `ResetPasswordRequest` 契约一致，提交 `confirmPassword`，并与后端最小 8 位密码规则一致。
- `/client/user/refresh` 必须符合文档描述的公开刷新接口行为，不能因为 access token 失效而被登录拦截器挡住。
- 认证相关错误提示应复用统一 API 错误映射，不在每个页面硬编码模糊兜底。
- 保持现有 token 存储、路由守卫和登录成功跳转行为不变，除非为修复上述阻断点所必需。

## Acceptance Criteria

- [ ] 登录接口返回 400 / 401 / 403 / 429 等错误时，登录页展示具体后端 `message`，没有 `message` 时才使用兜底提示。
- [ ] 注册发送验证码失败时仍停留在邮箱步骤，用户不会被推进到“验证码已发送至...”状态。
- [ ] 注册提交失败时展示具体错误；成功后仍自动登录并进入 `/dashboard`。
- [ ] 忘记密码页支持完整提交 `email/code/newPassword/confirmPassword`，成功后提示并允许返回登录。
- [ ] 设置页重置密码提交 `confirmPassword`，前端校验最小 8 位，与后端一致。
- [ ] `/client/user/refresh` 不需要有效 access token 即可到达 Controller，由 refresh token + deviceId 自身完成校验。
- [ ] 相关前端单元测试覆盖具体错误提示、注册验证码失败不前进、忘记密码完整提交、设置页 confirmPassword 契约。
- [ ] 后端测试覆盖 refresh 路由不被登录拦截器保护，或至少通过配置/路由测试证明白名单已更新。

## Confirmed Facts

- 登录接口实际路径是 `/client/user/login`，Controller 位于 `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java`。
- 前端登录页位于 `ai-agent-foward/src/app/pages/LoginPage.tsx`，当前 catch 分支只有本地兜底文案。
- Axios 错误统一映射位于 `ai-agent-foward/src/shared/api/errorMapper.ts`，`mapApiError` 已能读取后端 `message`。
- `unwrapResponse` 当前只返回 `response.data.data`，不会把业务 `code != 200` 的响应转成错误。
- `/client/user/refresh` 在 Controller 中存在，但 `WebMvcConfig` 的登录拦截排除列表当前没有排除该路径。
- 后端 `ResetPasswordRequest` 要求 `confirmPassword`，设置页当前未传该字段。
- 忘记密码页当前只发送验证码，没有调用重置密码接口。

## Out Of Scope

- 本任务不重构 JWT 签名策略与 token 黑名单校验链路。
- 本任务不实现完整 refresh token 自动续期和 refresh token 前端持久化策略。
- 本任务不调整登录限流算法语义，除非实现过程中发现它直接阻断错误提示验收。

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
