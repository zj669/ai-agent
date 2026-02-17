## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: UserController.java
- 用户域接口控制器，覆盖邮箱验证码、注册、登录、刷新、登出、资料查询/修改、重置密码等账户生命周期接口。

## 2) 核心方法
- `sendEmailCode(SendEmailCodeRequest request)`
- `registerByEmail(RegisterByEmailRequest request)`
- `login(LoginRequest request, HttpServletRequest httpRequest)`
- `refreshToken(TokenRefreshRequest request)`
- `getUserInfo()`
- `logout(LogoutRequest request)`
- `getClientIp(HttpServletRequest request)`

## 3) 具体方法
### 3.1 login(LoginRequest request, HttpServletRequest httpRequest)
- 函数签名: `login(UserRequests.LoginRequest request, HttpServletRequest httpRequest) -> Response<UserLoginResponse>`
- 入参: 登录请求与 HTTP 请求上下文
- 出参: 登录响应（含 token 信息）
- 功能含义: 解析客户端 IP，调用应用服务登录并返回标准响应。
- 链路作用: 账号登录入口 -> 风控/IP 透传 -> Token 颁发。

## 4) 变更记录
- 2026-02-15: 基于源码回填用户控制器接口矩阵与登录链路。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
