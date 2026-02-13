## ⚠️ 强制协议 (Mandatory Protocols)

### 蓝图优先协议 (Blueprint-First Protocol)

**蓝图是唯一的真理源 (Source of Truth)，代码只是蓝图的投影。**

本项目维护 `.blueprint/` 目录，包含每个核心模块的架构摘要。任何代码修改必须遵循三步思考链：

1. **Step 1 - 蓝图索引**: 先读 `.blueprint/_overview.md`，定位相关 Blueprint 文件，分析涉及哪些模块
2. **Step 2 - 架构推演**: 检查职责契约、依赖拓扑，更新 `.blueprint/` 文件，展示变更摘要
3. **Step 3 - 代码投影**: 蓝图确认后才修改代码，实现必须严格遵循 Blueprint 定义

**例外**: 纯 Bug 修复（不涉及接口变更）可简化为 Step1 + Step3；纯格式调整可直接修改。

蓝图维护规则：
- 蓝图与代码必须同步，修改代码时同步更新 Blueprint
- 蓝图优先级高于代码，不一致时以蓝图为准
- 新模块必须先建蓝图，再写代码
- 每个蓝图文件控制在 50-100 行

### 代码复用协议 (Code Reuse Protocol)

**编码前必做：架构对齐检查。**

在生成任何代码前，必须执行以下搜索：
- **查组件**: 搜索已封装的 Service（如 RedisService），禁止直接使用底层 SDK（如 RedisTemplate）
- **查实体**: 搜索已有 Entity/PO 和数据库表，禁止创建功能重叠的表
- **查逻辑**: 搜索是否有已存在的同类业务方法

**必须输出架构对齐简报**:
```markdown
**架构对齐检查**
- **发现可用组件**：（例如：已找到 RedisService，将放弃 RedisTemplate）
- **发现关联实体**：（例如：已找到 WorkflowNodeExecution，将基于此扩展，而非建新表）
- **复用策略**：[完全复用 / 扩展现有 / 新建关联]
```

若发现现有组件与需求部分重叠：优先扩展，禁止绕过。若改动可能破坏现有逻辑，必须列出差异并确认。

---

# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module backend with a separate React frontend.

- Backend modules:
  - `ai-agent-shared`: shared utilities and common abstractions.
  - `ai-agent-domain`: core domain models and business rules.
  - `ai-agent-application`: application services and orchestration.
  - `ai-agent-infrastructure`: persistence, Redis, Milvus, MinIO, external adapters.
  - `ai-agent-interfaces`: REST/Web entry points and Spring Boot startup module.
- Frontend:
  - Primary UI code: `ai-agent-foward/src` (`components`, `pages`, `services`, `hooks`, `stores`).
  - `app/frontend` exists as scaffold metadata but currently has no active source files.
- Tests follow Maven layout under `*/src/test/java`.

## Build, Test, and Development Commands
- Start infrastructure services:
  - `cd ai-agent-infrastructure/src/main/resources/docker`
  - `docker-compose up -d`
- Run backend locally:
  - `mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local`
- Build backend package:
  - `mvn clean package -DskipTests`
- Run backend tests:
  - `mvn test`
  - Example (single module): `mvn -pl ai-agent-infrastructure test`
- Run frontend:
  - `cd ai-agent-foward && npm install && npm run dev`
- Build frontend:
  - `cd ai-agent-foward && npm run build`

## Coding Style & Naming Conventions
- Java: 4-space indentation, package root `com.zj.aiagent`, class names `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- TypeScript/React (`ai-agent-foward`): 2-space indentation, components/pages in `PascalCase` (for example `DashboardPage.tsx`), hooks as `useXxx.ts`, services as `xxxService.ts`.
- Keep layers separated (interfaces -> application -> domain -> infrastructure). Do not bypass domain/application boundaries.

## Testing Guidelines
- Backend uses `spring-boot-starter-test` (JUnit 5) across modules and `jqwik` property-based tests in infrastructure.
- Test file names: `*Test.java`; property-based suites: `*PropertyTest.java`.
- Focus tests on domain behavior, workflow execution, and integration-sensitive adapters.
- Frontend has no committed automated test suite yet; at minimum, run `npm run build` and manually verify changed screens.

## Commit & Pull Request Guidelines
- Follow Conventional Commit style seen in history: `feat:`, `fix:`, `refactor:`, `chore:`, `test:` (optional scope, for example `feat(workflow): ...`).
- Keep each commit focused on one concern and one module group.
- PRs should include:
  - change summary and affected modules,
  - linked issue/task ID,
  - validation evidence (`mvn test`/build output),
  - UI screenshots for frontend-visible changes.

## Security & Configuration Tips
- Never commit real secrets. Keep credentials in environment variables (`DB_*`, `REDIS_*`, `JWT_SECRET`, `MINIO_*`).
- Use profile-specific configs (`application-local.yml`, `application-prod.yml`) with placeholders only in version control.
