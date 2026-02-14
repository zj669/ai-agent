# 代码风格与约定

## Java
- 4 空格缩进。
- 包根：`com.zj.aiagent`。
- 类名：PascalCase；方法/字段：camelCase；常量：UPPER_SNAKE_CASE。
- 分层约束：interfaces -> application -> domain -> infrastructure（按项目文档要求避免越层依赖）。

## 前端 TypeScript/React
- 2 空格缩进。
- 组件/页面：PascalCase（如 `DashboardPage.tsx`）。
- Hook：`useXxx.ts`；服务：`xxxService.ts`。
- 状态管理：Zustand。
- API 访问：统一通过 `apiClient.ts`（Axios 实例 + 拦截器）。

## 蓝图协议
- 代码变更前先看 `.blueprint/_overview.md`。
- 涉及架构/接口变更时先更新蓝图，再改代码。
- 蓝图与代码不一致时以蓝图为准（项目约定）。

## 安全与配置
- 不提交真实密钥。
- 使用环境变量（DB_*, REDIS_*, JWT_SECRET, MINIO_*）。
- 按 profile 管理配置：`application-local.yml`、`application-prod.yml`。