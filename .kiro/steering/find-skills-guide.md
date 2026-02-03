---
inclusion: manual
---

# Skills 生态系统集成指南

## 概述
本指南帮助理解和使用开源 agent skills 生态系统中的技能。

## Skills CLI 基础

Skills CLI (`npx skills`) 是开源 agent skills 生态系统的包管理器。

**核心命令**：
- `npx skills find [query]` - 搜索技能
- `npx skills add <package>` - 安装技能
- `npx skills check` - 检查更新
- `npx skills update` - 更新所有技能

**浏览技能**: https://skills.sh/

## 常见技能分类

| 分类 | 示例查询 |
|------|---------|
| Web 开发 | react, nextjs, typescript, css, tailwind |
| 测试 | testing, jest, playwright, e2e |
| DevOps | deploy, docker, kubernetes, ci-cd |
| 文档 | docs, readme, changelog, api-docs |
| 代码质量 | review, lint, refactor, best-practices |
| 设计 | ui, ux, design-system, accessibility |
| 效率工具 | workflow, automation, git |

## 使用场景

当用户询问：
- "如何做 X"（X 可能有现成技能）
- "有没有技能可以..."
- "能帮我找个工具做..."

执行搜索：
```bash
npx skills find [相关关键词]
```

## 已安装技能的使用

项目中已安装的技能位于：
- `.agents/skills/` - 通用技能
- `.claude/skills/` - Claude 特定技能
- `.cursor/skills/` - Cursor 特定技能

这些技能包含：
1. **SKILL.md** - 技能描述和使用指南
2. **scripts/** - Python/JavaScript 脚本（如果有）
3. **references/** - 参考文档（如果有）

## 脚本执行

如果技能包含脚本，可以直接执行：

```bash
# Python 脚本
python .agents/skills/[skill-name]/scripts/[script-name].py [args]

# JavaScript 脚本
node .agents/skills/[skill-name]/scripts/[script-name].js [args]
```

## 示例：使用已安装的技能

### academic-research-writer
```bash
# 使用 IEEE 格式化工具
python .agents/skills/academic-research-writer/scripts/ieee_formatter.py
```

### research-paper-writer
```bash
# 使用论文生成工具
node .agents/skills/research-paper-writer/index.js
```

## 转换为 Kiro Power

如果某个技能特别有用，可以转换为 Kiro Power：

1. 创建 Power 目录结构
2. 将 SKILL.md 转换为 POWER.md
3. 如果有脚本，考虑包装为 MCP 服务器
4. 添加 steering files 提供工作流指导

## 注意事项

- Skills 是为其他 AI 助手设计的，可能需要适配
- 脚本依赖需要手动安装（npm/pip）
- 优先使用 Kiro 原生 Powers，它们集成更好
