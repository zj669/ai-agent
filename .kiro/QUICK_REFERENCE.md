# 🚀 Kiro 快速参考卡片

> 工业级开发环境的日常使用速查表

## 📋 编码前检查清单

在开始编写代码前，AI 会自动执行：

```
✓ 搜索现有代码（避免重复）
✓ 检查数据库 Schema（避免冲突）
✓ 确认使用封装的 Service（避免直接用 SDK）
✓ 遵循架构规范（DDD 分层）
```

## 🎯 常用命令

### 查看配置

```bash
# 查看所有 Steering 文件
ls .kiro/steering/

# 查看所有 Hooks
ls .kiro/hooks/

# 打开完整指南
code .kiro/INDUSTRIAL_DEV_GUIDE.md
```

### 管理 Hooks

```bash
# 方法 1：可视化管理
Ctrl+Shift+P → "Open Kiro Hook UI"

# 方法 2：直接编辑
code .kiro/hooks/java-file-save-check.json

# 方法 3：临时禁用（添加 "disabled": true）
```

### 管理 Powers

```bash
# 打开 Powers 配置面板
Ctrl+Shift+P → "Configure Powers"

# 询问 AI
"列出所有已安装的 Powers"
```

## 💡 AI 自动执行的检查

### 代码复用检查

```
📋 代码复用检查报告

🔍 搜索范围：
- [x] Application Service
- [x] Domain Repository  
- [x] Infrastructure Mapper
- [x] Interface Controller

📦 发现的相关代码：
...

💡 复用建议：
...

✅ 建议方案：...
```

### 数据库设计检查

```
📊 数据库设计检查报告

🔍 Schema 检查：
- [x] 已查看 ai_agent.sql
- [x] 已搜索相关表名
- [x] 已搜索相关 PO 类

📦 发现的相关表：
...

✅ 建议方案：...
```

## 🎣 自动触发的 Hooks

| 触发时机 | Hook | 作用 |
|---------|------|------|
| 保存 .java 文件 | java-file-save-check | 立即检查语法错误 |
| 修改 .sql 文件 | sql-file-review | 审查迁移文件规范 |
| Agent 完成后 | code-quality-check | 检查编译和规范 |

## 🚫 禁止的操作

### 1. 直接使用底层 SDK

```java
// ❌ 错误
@Autowired
private RedisTemplate redisTemplate;

// ✅ 正确
@Autowired
private RedisService redisService;
```

### 2. 创建重复的表

```sql
-- ❌ 错误：已有 workflow_node_execution_log
CREATE TABLE workflow_execution (...)

-- ✅ 正确：扩展现有表
ALTER TABLE workflow_node_execution_log 
ADD COLUMN ...
```

### 3. 不处理异常

```java
// ❌ 错误
return repository.findById(id).get();

// ✅ 正确
return repository.findById(id)
    .orElseThrow(() -> new NotFoundException("..."));
```

### 4. 使用错误的命令

```bash
# ❌ 错误：处理中文文件
Get-Content file.txt
Set-Content file.txt "内容"

# ✅ 正确：使用工具
readFile tool
fsWrite tool
python .kiro/scripts/file_operations.py
```

## 📝 常见场景

### 场景 1：添加新功能

```
你："添加用户登录日志功能"

AI 自动：
1. 搜索 UserService, LoginService
2. 检查 user_login_log 表
3. 给出复用建议
4. 编写代码
5. 检查编译
```

### 场景 2：修改数据库

```
你："给 user 表添加 last_login_time"

AI 自动：
1. 读取 ai_agent.sql
2. 确认 user 表存在
3. 创建 Flyway 迁移
4. 审查 SQL 规范
```

### 场景 3：保存文件

```
你：Ctrl+S 保存 UserService.java

自动触发：
1. java-file-save-check Hook
2. getDiagnostics 检查
3. 回复结果或修复错误
```

## 🔧 自定义配置

### 添加 Steering 文件

```bash
# 1. 创建文件
.kiro/steering/my-rule.md

# 2. 编写规范（Markdown）
# 3. 自动生效
```

### 添加 Hook

```json
{
  "name": "我的 Hook",
  "version": "1.0.0",
  "when": {
    "type": "fileEdited",
    "patterns": ["*.java"]
  },
  "then": {
    "type": "askAgent",
    "prompt": "执行检查..."
  }
}
```

### 条件包含规范

```markdown
---
inclusion: fileMatch
fileMatchPattern: '**/*.tsx'
---

# 只在读取 .tsx 文件时生效
```

## 📊 规范优先级

```
临时指令（对话）
    ↓ 最高优先级
    
工作区 Steering (.kiro/steering/)
    ↓ 覆盖全局规范
    
全局 Steering (~/.kiro/steering/)
    ↓ 基础规范
```

## 🆘 快速问题解决

| 问题 | 解决方案 |
|------|---------|
| Steering 不生效 | 检查文件格式，重启 Kiro |
| Hook 不触发 | 检查 JSON 格式，查看输出面板 |
| 编译错误 | AI 会自动用 getDiagnostics 检查 |
| 中文乱码 | 使用 file_operations.py 脚本 |
| 想临时禁用规范 | 添加 `inclusion: manual` |

## 💬 常用对话

```
# 查看配置
"列出所有 Steering 文件"
"列出所有 Hooks"
"列出所有 Powers"

# 编码请求
"添加 XXX 功能"
"修改 XXX 表"
"优化 XXX 性能"

# 检查请求
"检查编译错误"
"审查这段代码"
"这个设计是否符合规范？"
```

## 🎯 记住这些

1. **编码前先搜索** - 避免重复造轮子
2. **数据库先检查** - 避免表冲突
3. **使用封装 Service** - 不直接用 SDK
4. **异常必须处理** - 不用 .get()
5. **日志包含上下文** - userId, requestId
6. **中文用工具处理** - file_operations.py

## 📚 详细文档

- [完整使用指南](.kiro/INDUSTRIAL_DEV_GUIDE.md)
- [项目架构](.kiro/steering/structure.md)
- [代码复用](.kiro/steering/code-reuse-first.md)
- [代码质量](.kiro/steering/code-quality.md)
- [数据库规范](.kiro/steering/database-standards.md)

---

💡 **提示**：将此文件加入书签，随时查阅！
