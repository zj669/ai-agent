# 🏭 工业级开发环境使用指南

## 📚 目录

1. [环境概述](#环境概述)
2. [Steering Files 使用](#steering-files-使用)
3. [Hooks 使用](#hooks-使用)
4. [Powers 使用](#powers-使用)
5. [完整工作流示例](#完整工作流示例)
6. [最佳实践](#最佳实践)

---

## 环境概述

你的工作区已配置为**工业级开发环境**，包含以下三层保障：

```
┌─────────────────────────────────────────────────┐
│  📋 Steering Files (规范层)                      │
│  持续生效的开发规范和架构原则                      │
├─────────────────────────────────────────────────┤
│  🎣 Hooks (自动化层)                             │
│  自动触发的检查和提醒                             │
├─────────────────────────────────────────────────┤
│  ⚡ Powers (能力扩展层)                          │
│  MCP 服务器集成的外部工具                         │
└─────────────────────────────────────────────────┘
```

---

## Steering Files 使用

### 已配置的 Steering 文件

| 文件 | 用途 | 何时生效 |
|------|------|---------|
| `product.md` | 产品概述和核心功能 | 始终 |
| `tech.md` | 技术栈和构建命令 | 始终 |
| `structure.md` | 架构规范和分层设计 | 始终 |
| `code-reuse-first.md` | 代码复用优先流程 | 始终 |
| `code-quality.md` | 代码质量标准 | 始终 |
| `database-standards.md` | 数据库设计规范 | 始终 |
| `command-restrictions.md` | 命令使用限制 | 始终 |

### Steering Files 的作用

**自动生效**：这些规范会自动注入到 AI 的上下文中，无需你每次提醒。

**示例场景**：

```
你说："帮我添加一个消息持久化功能"

AI 会自动：
1. 执行 code-reuse-first.md 的搜索流程
2. 检查 database-standards.md 的 Schema
3. 遵循 structure.md 的分层架构
4. 应用 code-quality.md 的编码规范
```

### 如何添加新的 Steering 文件

```bash
# 1. 在 .kiro/steering/ 目录创建 Markdown 文件
# 2. 文件会自动生效（默认 inclusion: always）

# 示例：创建前端规范
.kiro/steering/frontend-standards.md
```

### 高级用法：条件包含

```markdown
---
inclusion: fileMatch
fileMatchPattern: '**/*.tsx'
---

# React 组件规范

当读取 .tsx 文件时，此规范才会生效...
```

---

## Hooks 使用

### 已配置的 Hooks

| Hook | 触发时机 | 作用 |
|------|---------|------|
| `code-quality-check.json` | Agent 执行完成后 | 自动检查编译错误和代码规范 |
| `java-file-save-check.json` | 保存 Java 文件时 | 立即检查语法错误 |
| `sql-file-review.json` | 修改 SQL 文件时 | 审查迁移文件规范性 |

### Hook 的工作原理

```
你保存了 UserService.java
    ↓
触发 java-file-save-check Hook
    ↓
AI 自动执行 getDiagnostics
    ↓
发现错误 → 立即修复
没有错误 → 回复 "✅ 编译通过"
```

### 如何查看和管理 Hooks

**方法 1：使用 Explorer 视图**
```
1. 打开 VS Code 侧边栏
2. 找到 "Agent Hooks" 部分
3. 查看、启用、禁用 Hooks
```

**方法 2：使用命令面板**
```
1. Ctrl+Shift+P (Windows) 或 Cmd+Shift+P (Mac)
2. 输入 "Open Kiro Hook UI"
3. 可视化管理 Hooks
```

**方法 3：直接编辑 JSON 文件**
```bash
# Hooks 位置
.kiro/hooks/*.json

# 编辑后自动重新加载
```

### 如何创建自定义 Hook

**示例 1：前端文件保存时运行 ESLint**

```json
{
  "name": "前端代码检查",
  "version": "1.0.0",
  "description": "保存 TypeScript 文件时自动运行 ESLint",
  "when": {
    "type": "fileEdited",
    "patterns": ["app/frontend/src/**/*.ts", "app/frontend/src/**/*.tsx"]
  },
  "then": {
    "type": "askAgent",
    "prompt": "TypeScript 文件已保存，请运行 'npm run lint' 检查代码规范"
  }
}
```

**示例 2：提交消息时运行测试**

```json
{
  "name": "自动运行测试",
  "version": "1.0.0",
  "description": "在 Agent 完成后运行相关测试",
  "when": {
    "type": "agentStop"
  },
  "then": {
    "type": "askAgent",
    "prompt": "如果修改了业务逻辑代码，询问用户是否需要运行相关测试"
  }
}
```

### Hook 的限制

⚠️ **重要**：`runCommand` 类型的 Hook 只能用于 `promptSubmit` 和 `agentStop` 事件。

```json
// ✅ 正确
{
  "when": { "type": "agentStop" },
  "then": { "type": "runCommand", "command": "mvn compile" }
}

// ❌ 错误 - fileEdited 不能使用 runCommand
{
  "when": { "type": "fileEdited", "patterns": ["*.java"] },
  "then": { "type": "runCommand", "command": "mvn compile" }
}

// ✅ 正确 - 使用 askAgent 代替
{
  "when": { "type": "fileEdited", "patterns": ["*.java"] },
  "then": { "type": "askAgent", "prompt": "请运行 mvn compile 检查编译" }
}
```

---

## Powers 使用

### 什么是 Powers？

Powers 是 Kiro 的能力扩展系统，通过 MCP (Model Context Protocol) 集成外部工具。

### 已安装的 Powers

你可以通过以下方式查看：

```
# 方法 1：询问 AI
"列出所有已安装的 Powers"

# 方法 2：使用命令面板
Ctrl+Shift+P → "Configure Powers"
```

### 如何使用 Powers

**步骤 1：激活 Power**
```
你："我需要使用 Git 最佳实践"
AI："让我激活 git-best-practices Power..."
```

**步骤 2：AI 自动调用工具**
```
AI 会自动使用 Power 提供的工具完成任务
```

### 推荐安装的 Powers

对于你的项目，建议安装：

1. **Git Best Practices** - Git 工作流和提交规范
2. **Requirements Analyst** - 需求分析和 PRD 编写
3. **Database Tools** - 数据库管理和查询（如果有）

**安装方法**：
```
1. 打开命令面板：Ctrl+Shift+P
2. 输入 "Configure Powers"
3. 在面板中浏览和安装 Powers
```

---

## 完整工作流示例

### 场景 1：添加新功能

**你的请求**：
```
"我想添加一个查询用户所有对话的功能"
```

**AI 的执行流程**：

```
📋 步骤 1：代码复用检查（code-reuse-first.md）
🔍 搜索现有代码...
   - ConversationRepository ✅ 已存在
   - listByUserId 方法 ❌ 缺失

📊 步骤 2：数据库检查（database-standards.md）
🔍 检查 Schema...
   - conversation 表 ✅ 已存在
   - user_id 字段 ✅ 已存在
   - 索引 idx_user_id ✅ 已存在

💡 步骤 3：设计方案
   - 复用：ConversationRepository（扩展方法）
   - 复用：conversation 表（无需修改）
   - 新建：listByUserId 方法

✅ 步骤 4：询问确认
   "发现可以通过扩展 ConversationRepository 实现，
    需要修改 3 个文件（接口、实现、Mapper），约 20 行代码。
    是否继续？"

👨‍💻 步骤 5：编写代码
   [AI 开始编写代码...]

🎣 步骤 6：自动触发 Hook（agentStop）
   - 运行 getDiagnostics 检查语法
   - 确认编译通过
   - 总结修改内容

✅ 完成！
```

### 场景 2：修改数据库表

**你的请求**：
```
"给 workflow_execution 表添加一个 priority 字段"
```

**AI 的执行流程**：

```
📊 步骤 1：Schema 检查（database-standards.md）
🔍 读取 ai_agent.sql...
   - workflow_execution 表 ❌ 不存在！
   - workflow_node_execution_log 表 ✅ 存在

⚠️ 步骤 2：冲突检测
   "发现项目中没有 workflow_execution 表，
    但有 workflow_node_execution_log 表。
    您是想：
    A) 给 workflow_node_execution_log 添加 priority 字段
    B) 创建新的 workflow_execution 表
    请确认？"

👨‍💻 步骤 3：等待你的选择...

[假设你选择 A]

📝 步骤 4：创建迁移文件
   - 创建 V1_6__add_priority_to_execution_log.sql
   - 添加字段和索引
   - 更新 PO 类

🎣 步骤 5：自动触发 Hook（sql-file-review）
   - 检查文件命名规范 ✅
   - 检查是否使用 ALTER TABLE ✅
   - 检查索引设计 ✅

✅ 完成！
```

### 场景 3：保存 Java 文件

**你的操作**：
```
在 VS Code 中编辑 UserService.java
按 Ctrl+S 保存
```

**自动触发的流程**：

```
🎣 触发 Hook：java-file-save-check

🔍 AI 自动执行：
   1. getDiagnostics("UserService.java")
   2. 发现错误：缺少分号
   3. 自动修复
   4. 再次检查
   5. 回复："✅ 编译通过"

⏱️ 整个过程 < 5 秒
```

---

## 最佳实践

### 1. 充分利用 Steering Files

**✅ 推荐做法**：
- 将团队的编码规范写入 Steering 文件
- 将常见的架构决策记录下来
- 定期更新和完善规范

**示例**：
```markdown
# .kiro/steering/team-conventions.md

## 我们团队的特殊约定

1. 所有 Service 方法必须添加 @Transactional
2. 所有 Controller 必须返回统一的 Response<T>
3. 异常处理使用全局异常处理器
```

### 2. 合理配置 Hooks

**✅ 推荐的 Hook 配置**：

```
轻量级检查 → fileEdited Hook
  - 语法检查
  - 格式检查

重量级检查 → agentStop Hook
  - 编译检查
  - 测试运行
  - 代码审查
```

**❌ 避免的配置**：
- 不要在 fileEdited 中运行耗时操作（如完整编译）
- 不要创建过多的 Hook（会影响响应速度）

### 3. 善用 Powers

**场景 1：需求分析**
```
你："我需要设计一个用户权限系统"
AI："让我激活 requirements-analyst Power 来帮你..."
```

**场景 2：Git 工作流**
```
你："帮我创建一个 feature 分支并提交代码"
AI："让我激活 git-best-practices Power..."
```

### 4. 分层管理规范

```
全局规范（~/.kiro/steering/）
  ↓ 适用于所有项目
  
工作区规范（.kiro/steering/）
  ↓ 适用于当前项目
  
临时指令（直接对话）
  ↓ 一次性需求
```

### 5. 定期审查和优化

**每月检查清单**：
- [ ] Steering 文件是否需要更新？
- [ ] Hooks 是否还在正常工作？
- [ ] 是否有新的 Powers 可以安装？
- [ ] 团队成员是否了解这些工具？

---

## 快速参考

### 常用命令

```bash
# 查看 Steering 文件
ls .kiro/steering/

# 查看 Hooks
ls .kiro/hooks/

# 编辑 Steering 文件
code .kiro/steering/code-reuse-first.md

# 编辑 Hook
code .kiro/hooks/java-file-save-check.json
```

### 常见问题

**Q: Steering 文件修改后需要重启吗？**
A: 不需要，会自动重新加载。

**Q: Hook 不工作怎么办？**
A: 检查 JSON 格式是否正确，查看 Kiro 输出面板的错误信息。

**Q: 如何临时禁用某个 Hook？**
A: 在 Hook JSON 中添加 `"disabled": true`。

**Q: Powers 和 Hooks 有什么区别？**
A: Powers 提供外部工具能力，Hooks 提供自动化触发机制。

---

## 总结

你现在拥有一个**工业级的开发环境**，包含：

✅ **7 个 Steering 文件** - 覆盖架构、代码质量、数据库等方面
✅ **3 个自动化 Hooks** - 自动检查代码质量和规范
✅ **Powers 扩展能力** - 可按需安装更多工具

**下一步建议**：

1. 尝试提出一个编码需求，体验完整的工作流
2. 根据团队需求，添加自定义的 Steering 文件
3. 探索和安装更多有用的 Powers
4. 与团队分享这套配置

**记住**：这些工具是为了提高效率，而不是增加负担。根据实际需要灵活调整！

---

📚 **相关文档**：
- [Kiro 官方文档](https://docs.kiro.ai)
- [MCP 协议说明](https://modelcontextprotocol.io)
- [项目架构文档](.kiro/steering/structure.md)
