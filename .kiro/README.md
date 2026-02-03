# 🏭 Kiro 工业级开发环境配置

> 本项目已配置完整的工业级开发环境，包含代码规范、自动化检查和能力扩展。

## 📁 目录结构

```
.kiro/
├── README.md                          # 本文件
├── INDUSTRIAL_DEV_GUIDE.md           # 详细使用指南
├── steering/                          # 开发规范（自动生效）
│   ├── product.md                    # 产品概述
│   ├── tech.md                       # 技术栈和构建命令
│   ├── structure.md                  # 架构规范
│   ├── code-reuse-first.md          # 代码复用优先流程
│   ├── code-quality.md              # 代码质量标准
│   ├── database-standards.md        # 数据库设计规范
│   └── command-restrictions.md      # 命令使用限制
├── hooks/                            # 自动化 Hooks
│   ├── code-quality-check.json      # 代码质量检查
│   ├── java-file-save-check.json    # Java 文件保存检查
│   └── sql-file-review.json         # SQL 文件审查
└── scripts/                          # 工具脚本
    └── file_operations.py           # 文件操作工具（UTF-8 支持）
```

## 🚀 快速开始

### 1. 了解你的环境

你的开发环境包含三层保障：

```
📋 Steering Files (规范层)
   ↓ 定义"做什么"和"怎么做"
   
🎣 Hooks (自动化层)
   ↓ 定义"何时触发"
   
⚡ Powers (能力扩展层)
   ↓ 定义"外部工具"
```

### 2. 体验自动化工作流

**试试这个**：
```
你："帮我添加一个查询用户对话列表的功能"

AI 会自动：
1. 搜索现有代码（code-reuse-first.md）
2. 检查数据库 Schema（database-standards.md）
3. 遵循架构规范（structure.md）
4. 应用代码质量标准（code-quality.md）
5. 完成后自动检查编译错误（Hook）
```

### 3. 查看详细文档

```bash
# 阅读完整的使用指南
code .kiro/INDUSTRIAL_DEV_GUIDE.md
```

## 📋 Steering Files 说明

### 核心规范文件

| 文件 | 作用 | 关键特性 |
|------|------|---------|
| `code-reuse-first.md` | 代码复用优先 | 编码前强制搜索现有代码 |
| `code-quality.md` | 代码质量标准 | 禁止反模式、异常处理、日志规范 |
| `database-standards.md` | 数据库设计 | Schema 检查、迁移规范、PO 设计 |
| `structure.md` | 架构规范 | DDD 分层、模块职责、命名规范 |
| `tech.md` | 技术栈 | 依赖版本、构建命令、环境配置 |
| `product.md` | 产品概述 | 核心功能、技术特点、目标用户 |
| `command-restrictions.md` | 命令限制 | 禁用命令、UTF-8 编码处理 |

### Steering Files 的工作原理

**自动注入**：这些规范会自动注入到 AI 的上下文中，无需每次提醒。

**示例**：
```java
// 你写：
@Autowired
private RedisTemplate redisTemplate;

// AI 会提醒：
// ❌ 不应直接使用 RedisTemplate
// ✅ 应该使用项目封装的 RedisService
```

## 🎣 Hooks 说明

### 已配置的 Hooks

| Hook | 触发时机 | 作用 |
|------|---------|------|
| `code-quality-check` | Agent 完成后 | 检查编译错误、代码规范 |
| `java-file-save-check` | 保存 .java 文件 | 立即检查语法错误 |
| `sql-file-review` | 修改 SQL 文件 | 审查迁移文件规范 |

### 如何管理 Hooks

**方法 1：可视化管理**
```
Ctrl+Shift+P → "Open Kiro Hook UI"
```

**方法 2：直接编辑**
```bash
# 编辑 Hook 配置
code .kiro/hooks/java-file-save-check.json

# 临时禁用 Hook（添加 "disabled": true）
{
  "name": "Java 文件保存检查",
  "disabled": true,
  ...
}
```

**方法 3：查看 Hook 列表**
```
在 VS Code 侧边栏 → "Agent Hooks" 部分
```

## ⚡ Powers 使用

### 推荐安装的 Powers

对于本项目，建议安装：

1. **git-best-practices** - Git 工作流和提交规范
2. **requirements-analyst** - 需求分析和 PRD 编写
3. **power-builder** - 创建自定义 Powers

### 如何安装 Powers

```
1. Ctrl+Shift+P
2. 输入 "Configure Powers"
3. 在面板中浏览和安装
```

### 如何使用 Powers

```
你："我需要分析这个需求并生成 PRD"
AI："让我激活 requirements-analyst Power..."
```

## 🛠️ 实用工具

### 文件操作脚本

**位置**：`.kiro/scripts/file_operations.py`

**用途**：处理工作区外的文件（完美支持中文 UTF-8）

**使用方法**：
```bash
# 读取文件
python .kiro/scripts/file_operations.py read "~/path/to/file.md"

# 写入文件
python .kiro/scripts/file_operations.py write "~/path/to/file.md" "内容"

# 追加文件
python .kiro/scripts/file_operations.py append "~/path/to/file.md" "追加内容"
```

## 📊 工作流示例

### 场景 1：添加新功能

```
你的请求：
"添加一个用户登录日志记录功能"

AI 的执行流程：
1. 📋 搜索现有代码（UserService, LoginService）
2. 📊 检查数据库（user_login_log 表是否存在）
3. 💡 给出复用建议
4. 👨‍💻 编写代码
5. 🎣 自动检查编译错误
6. ✅ 完成
```

### 场景 2：修改数据库

```
你的请求：
"给 user 表添加 last_login_time 字段"

AI 的执行流程：
1. 📊 读取 ai_agent.sql 检查 Schema
2. 🔍 确认 user 表存在
3. 📝 创建 Flyway 迁移文件
4. 🎣 自动审查 SQL 规范
5. ✅ 完成
```

### 场景 3：保存文件

```
你的操作：
在 VS Code 中编辑 UserService.java
按 Ctrl+S 保存

自动触发：
1. 🎣 java-file-save-check Hook
2. 🔍 getDiagnostics 检查语法
3. ✅ 回复 "编译通过" 或自动修复错误
```

## 🎯 最佳实践

### 1. 充分利用 Steering Files

**✅ 推荐**：
- 将团队约定写入 Steering 文件
- 定期更新和完善规范
- 使用中文注释（代码用英文）

**示例**：
```markdown
# .kiro/steering/team-conventions.md

## 我们团队的特殊约定

1. 所有 API 必须返回统一的 Response<T>
2. 异常处理使用全局异常处理器
3. 日志必须包含 userId 和 requestId
```

### 2. 合理配置 Hooks

**✅ 推荐**：
```
轻量级检查 → fileEdited Hook
  - 语法检查
  - 格式检查

重量级检查 → agentStop Hook
  - 完整编译
  - 测试运行
```

**❌ 避免**：
- 不要在 fileEdited 中运行耗时操作
- 不要创建过多的 Hook

### 3. 善用 Powers

**场景示例**：
```
需求分析 → requirements-analyst Power
Git 工作流 → git-best-practices Power
创建 Power → power-builder Power
```

### 4. 分层管理规范

```
全局规范（~/.kiro/steering/）
  ↓ 适用于所有项目
  
工作区规范（.kiro/steering/）
  ↓ 适用于当前项目（优先级更高）
  
临时指令（直接对话）
  ↓ 一次性需求
```

## 🔧 自定义配置

### 添加新的 Steering 文件

```bash
# 1. 创建文件
code .kiro/steering/my-custom-rule.md

# 2. 编写规范（使用 Markdown）
# 3. 保存后自动生效
```

### 创建自定义 Hook

```bash
# 1. 创建 JSON 文件
code .kiro/hooks/my-custom-hook.json

# 2. 配置 Hook
{
  "name": "我的自定义 Hook",
  "version": "1.0.0",
  "description": "描述",
  "when": {
    "type": "fileEdited",
    "patterns": ["*.java"]
  },
  "then": {
    "type": "askAgent",
    "prompt": "执行某些检查..."
  }
}

# 3. 保存后自动生效
```

### 条件包含的 Steering 文件

```markdown
---
inclusion: fileMatch
fileMatchPattern: '**/*.tsx'
---

# React 组件规范

只有在读取 .tsx 文件时，此规范才会生效...
```

## 📚 相关文档

- **[📖 文档索引](../DOCUMENTATION_INDEX.md)** - 所有项目文档的完整索引
- [完整使用指南](.kiro/INDUSTRIAL_DEV_GUIDE.md)
- [项目架构规范](.kiro/steering/structure.md)
- [技术栈说明](.kiro/steering/tech.md)
- [代码复用流程](.kiro/steering/code-reuse-first.md)

## 🆘 常见问题

### Q: Steering 文件修改后需要重启吗？
A: 不需要，会自动重新加载。

### Q: Hook 不工作怎么办？
A: 检查 JSON 格式，查看 Kiro 输出面板的错误信息。

### Q: 如何临时禁用某个规范？
A: 在 Steering 文件开头添加：
```markdown
---
inclusion: manual
---
```

### Q: 如何查看当前生效的规范？
A: 询问 AI："列出当前生效的所有 Steering 文件"

### Q: Powers 和 Hooks 有什么区别？
A: 
- **Powers**：提供外部工具能力（如 Git、数据库工具）
- **Hooks**：提供自动化触发机制（如保存文件时检查）

## 🎉 总结

你现在拥有：

✅ **7 个 Steering 文件** - 覆盖架构、代码质量、数据库等
✅ **3 个自动化 Hooks** - 自动检查代码质量
✅ **Powers 扩展能力** - 可按需安装工具
✅ **完整的文档** - 详细的使用指南

**下一步**：

1. 阅读 [完整使用指南](.kiro/INDUSTRIAL_DEV_GUIDE.md)
2. 尝试提出一个编码需求，体验完整工作流
3. 根据团队需求，添加自定义规范
4. 探索和安装更多 Powers

**记住**：这些工具是为了提高效率，而不是增加负担。根据实际需要灵活调整！

---

📝 **最后更新**：2026-02-03
🔗 **项目地址**：[AI Agent Platform](https://github.com/your-repo)
📧 **问题反馈**：通过 Issues 提交
