# 登录模块验证和开发计划

> **创建时间**: 2026-02-10  
> **目标**: 全面验证登录模块功能，修复前后端不匹配问题，实现自动 Token 刷新机制

---

## 📊 现状分析

### ✅ 已实现功能

#### 后端实现（完整度：95%）
- ✅ 用户注册（邮箱验证码）
- ✅ 用户登录（邮箱+密码）
- ✅ JWT Token 生成和验证
- ✅ Refresh Token 多设备支持
- ✅ Token 黑名单机制
- ✅ 登录失败限流（5次/15分钟）
- ✅ 邮箱验证码限流（1次/分钟）
- ✅ 密码强度验证（最少8位）
- ✅ 用户信息修改
- ✅ 密码重置
- ✅ 用户登出

**核心文件**:
- `UserController.java` - REST API 接口
- `UserApplicationService.java` - 应用层服务
- `UserAuthenticationDomainService.java` - 领域服务
- `JwtTokenService.java` - Token 管理
- `User.java` - 用户聚合根

#### 前端实现（完整度：90%）
- ✅ 登录页面 UI（Ant Design）
- ✅ 注册页面 UI（Tab 切换）
- ✅ 表单验证（邮箱、密码）
- ✅ 验证码发送（60秒倒计时）
- ✅ Zustand 状态管理
- ✅ Token 本地存储
- ✅ 路由守卫（ProtectedRoute）
- ✅ 登录跳转（redirect 参数）
- ✅ 错误提示（友好的中文提示）

**核心文件**:
- `LoginPage.tsx` - 登录/注册页面
- `authStore.ts` - 认证状态管理
- `authService.ts` - API 调用服务
- `ProtectedRoute.tsx` - 路由守卫
- `auth.ts` - 类型定义

---

## ⚠️ 发现的问题

### 1. 前后端接口不匹配 🔴 高优先级

**问题描述**:
- 后端 `UserLoginResponse` 包含 `refreshToken` 和 `deviceId` 字段
- 前端 `LoginResponse` 类型定义缺少这两个字段
- 前端 `authStore` 未存储和管理 `refreshToken` 和 `deviceId`

**影响**:
- 无法使用 Refresh Token 功能
- 无法支持多设备登录
- Token 过期后只能重新登录

**修复方案**:
```typescript
// 1. 更新 auth.ts 类型定义
export interface LoginResponse {
  token: string;
  refreshToken: string;  // 新增
  expireIn: number;
  deviceId: string;       // 新增
  user: User;
}

// 2. 更新 authStore.ts 状态管理
interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;  // 新增
  deviceId: string | null;       // 新增
  tokenExpireAt: number | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
```

### 2. Token 刷新机制缺失 🔴 高优先级

**问题描述**:
- 后端已实现 `/refresh` 接口
- 前端未实现自动刷新逻辑
- `checkTokenExpiration` 只检查不刷新

**影响**:
- Token 过期后用户体验差
- 需要频繁重新登录

**修复方案**:
- 实现 Axios 请求拦截器，自动添加 `Authorization` header
- 实现 Axios 响应拦截器，捕获 401 错误
- Token 过期前 5 分钟自动刷新
- 刷新失败后跳转登录页

### 3. Axios 拦截器缺失 🔴 高优先级

**问题描述**:
- 未自动添加 `Authorization` header
- 未处理 401 自动跳转登录
- 未处理 Token 过期自动刷新

**影响**:
- 每个 API 调用都需要手动添加 Token
- 401 错误处理不统一
- 用户体验差

**修复方案**:
- 创建 `axios.ts` 配置文件
- 实现请求拦截器（添加 Token）
- 实现响应拦截器（处理 401、自动刷新）

### 4. 数据库表结构验证 🟡 中优先级

**问题描述**:
- `user_info` 表的 `status` 字段定义为 `0-禁用, 1-正常`
- 需要验证 Domain 层 `UserStatus` 枚举是否一致

**修复方案**:
- 检查 `UserStatus.java` 枚举定义
- 确保前后端状态码一致

---

## 🎯 开发计划

### 阶段 1: 修复前后端接口不匹配 ⏱️ 30分钟

#### 任务 1.1: 更新前端类型定义
- [ ] 修改 `auth.ts`，添加 `refreshToken` 和 `deviceId` 字段
- [ ] 添加 `TokenRefreshRequest` 和 `TokenRefreshResponse` 类型
- [ ] 验证类型定义与后端一致

#### 任务 1.2: 更新 authStore 状态管理
- [ ] 添加 `refreshToken` 和 `deviceId` 状态
- [ ] 更新 `login` 和 `register` 方法，存储新字段
- [ ] 更新 `logout` 方法，清理新字段
- [ ] 更新 `initializeAuth` 方法，恢复新字段

#### 任务 1.3: 更新 authService
- [ ] 添加 `refreshToken` 方法
- [ ] 更新 `logout` 方法，传递 `deviceId`

---

### 阶段 2: 实现 Axios 拦截器 ⏱️ 45分钟

#### 任务 2.1: 创建 Axios 配置文件
- [ ] 创建 `src/utils/axios.ts`
- [ ] 配置 baseURL 和默认 headers
- [ ] 导出配置好的 axios 实例

#### 任务 2.2: 实现请求拦截器
- [ ] 自动添加 `Authorization: Bearer {token}` header
- [ ] 处理 Token 不存在的情况

#### 任务 2.3: 实现响应拦截器
- [ ] 捕获 401 错误
- [ ] 尝试使用 Refresh Token 刷新
- [ ] 刷新成功后重试原请求
- [ ] 刷新失败后跳转登录页
- [ ] 处理其他错误（统一错误提示）

#### 任务 2.4: 实现自动刷新机制
- [ ] Token 过期前 5 分钟自动刷新
- [ ] 使用定时器或请求时检查
- [ ] 防止并发刷新（加锁）

#### 任务 2.5: 更新 authService
- [ ] 使用新的 axios 实例
- [ ] 移除手动添加 Token 的代码

---

### 阶段 3: 验证后端功能 ⏱️ 30分钟

#### 任务 3.1: 启动后端服务
- [ ] 启动 Docker 服务（MySQL, Redis）
- [ ] 检查数据库连接
- [ ] 启动 Spring Boot 应用
- [ ] 验证应用启动成功

#### 任务 3.2: 测试注册接口
- [ ] 发送验证码 `POST /client/user/email/sendCode`
- [ ] 验证邮件发送（检查日志）
- [ ] 注册用户 `POST /client/user/email/register`
- [ ] 验证数据库记录

#### 任务 3.3: 测试登录接口
- [ ] 登录 `POST /client/user/login`
- [ ] 验证返回 Token 和 Refresh Token
- [ ] 验证 Token 格式和内容
- [ ] 验证 Redis 中的 Refresh Token

#### 任务 3.4: 测试 Token 刷新接口
- [ ] 刷新 Token `POST /client/user/refresh`
- [ ] 验证返回新的 Token
- [ ] 验证旧 Token 失效

#### 任务 3.5: 测试登出接口
- [ ] 登出 `POST /client/user/logout`
- [ ] 验证 Token 加入黑名单
- [ ] 验证 Refresh Token 删除

#### 任务 3.6: 测试限流机制
- [ ] 测试邮箱验证码限流（1次/分钟）
- [ ] 测试登录失败限流（5次/15分钟）

---

### 阶段 4: 验证前端功能 ⏱️ 30分钟

#### 任务 4.1: 启动前端服务
- [ ] 安装依赖 `npm install`
- [ ] 启动开发服务器 `npm run dev`
- [ ] 访问 http://localhost:5173

#### 任务 4.2: 测试登录页面 UI
- [ ] 验证页面布局正确
- [ ] 验证登录/注册 Tab 切换
- [ ] 验证表单字段显示

#### 任务 4.3: 测试表单验证
- [ ] 测试邮箱格式验证
- [ ] 测试密码长度验证（最少8位）
- [ ] 测试必填字段验证
- [ ] 测试验证码倒计时

#### 任务 4.4: 测试状态管理
- [ ] 验证 authStore 初始化
- [ ] 验证 localStorage 存储
- [ ] 验证状态更新

#### 任务 4.5: 测试路由守卫
- [ ] 未登录访问受保护页面，跳转登录
- [ ] 登录后跳转回原页面
- [ ] 登出后清理状态

---

### 阶段 5: 前后端联调 ⏱️ 45分钟

#### 任务 5.1: 配置代理
- [ ] 配置 Vite 代理，转发 `/client` 到后端
- [ ] 验证代理配置生效

#### 任务 5.2: 测试完整注册流程
- [ ] 输入邮箱，发送验证码
- [ ] 验证倒计时显示
- [ ] 输入验证码和密码，提交注册
- [ ] 验证注册成功，自动登录
- [ ] 验证跳转到 Dashboard

#### 任务 5.3: 测试完整登录流程
- [ ] 输入邮箱和密码，提交登录
- [ ] 验证登录成功
- [ ] 验证 Token 存储
- [ ] 验证跳转到 Dashboard
- [ ] 验证用户信息显示

#### 任务 5.4: 测试 Token 刷新
- [ ] 手动修改 Token 过期时间（测试用）
- [ ] 发起 API 请求
- [ ] 验证自动刷新 Token
- [ ] 验证请求成功

#### 任务 5.5: 测试登出流程
- [ ] 点击登出按钮
- [ ] 验证 Token 清理
- [ ] 验证跳转到登录页
- [ ] 验证无法访问受保护页面

#### 任务 5.6: 测试错误处理
- [ ] 测试错误邮箱/密码
- [ ] 测试验证码错误
- [ ] 测试网络错误
- [ ] 验证错误提示友好

---

### 阶段 6: E2E 测试 ⏱️ 30分钟

#### 任务 6.1: 边界场景测试
- [ ] 测试空表单提交
- [ ] 测试无效邮箱格式
- [ ] 测试弱密码
- [ ] 测试验证码过期
- [ ] 测试重复注册

#### 任务 6.2: 异常处理测试
- [ ] 测试后端服务停止
- [ ] 测试数据库连接失败
- [ ] 测试 Redis 连接失败
- [ ] 测试邮件服务失败

#### 任务 6.3: 安全性验证
- [ ] 测试 SQL 注入防护
- [ ] 测试 XSS 防护
- [ ] 测试 CSRF 防护
- [ ] 测试密码加密存储
- [ ] 测试 Token 签名验证

#### 任务 6.4: 性能测试
- [ ] 测试并发登录
- [ ] 测试限流机制
- [ ] 测试 Token 刷新性能

---

## 📝 验证清单

### 后端验证
- [ ] 所有接口返回正确的状态码
- [ ] 所有接口返回正确的数据格式
- [ ] Token 生成和验证正确
- [ ] Refresh Token 多设备支持正常
- [ ] 限流机制生效
- [ ] 数据库记录正确
- [ ] Redis 缓存正确

### 前端验证
- [ ] UI 显示正确
- [ ] 表单验证生效
- [ ] 状态管理正确
- [ ] 路由守卫生效
- [ ] 错误提示友好
- [ ] Token 自动刷新
- [ ] 登出清理完整

### 联调验证
- [ ] 完整注册流程通过
- [ ] 完整登录流程通过
- [ ] Token 刷新流程通过
- [ ] 登出流程通过
- [ ] 错误处理正确
- [ ] 边界场景处理正确

---

## 🚀 执行顺序

1. **阶段 1**: 修复前后端接口不匹配（必须先完成）
2. **阶段 2**: 实现 Axios 拦截器（必须先完成）
3. **阶段 3 + 4**: 并行验证后端和前端功能
4. **阶段 5**: 前后端联调
5. **阶段 6**: E2E 测试

---

## 📊 预计时间

| 阶段 | 预计时间 | 优先级 |
|------|---------|--------|
| 阶段 1: 修复接口不匹配 | 30分钟 | 🔴 高 |
| 阶段 2: 实现拦截器 | 45分钟 | 🔴 高 |
| 阶段 3: 验证后端 | 30分钟 | 🔴 高 |
| 阶段 4: 验证前端 | 30分钟 | 🔴 高 |
| 阶段 5: 前后端联调 | 45分钟 | 🔴 高 |
| 阶段 6: E2E 测试 | 30分钟 | 🟡 中 |
| **总计** | **3小时30分钟** | |

---

## 🎯 成功标准

1. ✅ 前后端接口完全匹配
2. ✅ Token 自动刷新机制正常工作
3. ✅ 完整登录流程无错误
4. ✅ 所有边界场景处理正确
5. ✅ 错误提示友好且准确
6. ✅ 安全性验证通过
7. ✅ 性能测试通过

---

## 📌 注意事项

1. **数据库准备**: 确保 MySQL 和 Redis 服务正常运行
2. **邮件服务**: 验证邮件服务配置正确（或使用 Mock）
3. **环境变量**: 检查 `.env` 文件配置
4. **端口冲突**: 确保 8080（后端）和 5173（前端）端口可用
5. **浏览器缓存**: 测试时清理浏览器缓存和 localStorage
6. **日志监控**: 实时查看后端日志，快速定位问题

---

## 🔗 相关文档

- [后端 API 文档](../app/APIDocumentation.md)
- [前端开发规范](../AGENTS.md#react-typescript-代码规范)
- [数据库 Schema](../ai-agent-infrastructure/src/main/resources/db/ai_agent.sql)
- [架构概览](../.blueprint/_overview.md)

---

**计划创建时间**: 2026-02-10  
**计划创建者**: Sisyphus AI Agent  
**计划状态**: 待执行
