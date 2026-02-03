---
name: "agent-skills-guide"
displayName: "Agent Skills Guide"
description: "完整指南：如何发现、理解和使用 .agents/skills 目录下已安装的 AI agent 技能，包括技能文档阅读、脚本执行和转换为 Kiro Powers 的方法。"
keywords: ["skills", "agent-skills", "skill-management", "npx-skills", "skill-conversion"]
author: "Kiro Team"
---

# Agent Skills Guide

## 概述

本 Power 提供完整的指南，帮助你理解和使用项目中 `.agents/skills` 目录下已安装的 AI agent 技能。这些技能来自开源的 agent skills 生态系统，通过 `npx skills` CLI 安装。

**核心能力：**
- 发现和浏览已安装的技能
- 理解技能的功能和使用方法
- 执行技能中包含的脚本
- 将有用的技能转换为 Kiro Powers
- 搜索和安装新技能

## 什么是 Agent Skills？

Agent Skills 是为 AI 助手（如 Cursor、Cline、Claude 等）设计的模块化能力包，包含：

1. **SKILL.md** - 技能描述和使用指南
2. **scripts/** - 可执行的 Python/JavaScript 脚本（可选）
3. **references/** - 参考文档和资源（可选）
4. **assets/** - 模板、示例等资源（可选）

这些技能通过 `npx skills` CLI 管理，存储在 `.agents/skills/` 目录中。

## 已安装的技能

项目中当前已安装以下技能：

### 1. academic-research-writer
**功能：** 创建高质量学术研究文档，包含同行评审来源和 IEEE 格式引用

**适用场景：**
- 撰写研究论文、文献综述
- 技术报告、学位论文
- 会议论文、学术提案

**核心特性：**
- 学术严谨性和客观性
- 来源验证（仅使用同行评审来源）
- IEEE 标准引用格式
- 研究完整性保证

**包含资源：**
- `scripts/ieee_formatter.py` - IEEE 引用格式化工具
- `references/ACADEMIC-WRITING.md` - 学术写作规范
- `references/IEEE-CITATION-GUIDE.md` - IEEE 引用指南
- `references/SOURCE-VERIFICATION.md` - 来源验证指南

### 2. research-paper-writer
**功能：** 创建符合 IEEE/ACM 格式标准的正式学术研究论文

**适用场景：**
- 会议/期刊论文撰写
- 实验性研究论文
- 综述论文

**核心特性：**
- 标准学术论文结构
- IEEE/ACM 格式规范
- 学术写作风格
- 完整的引用管理

**包含资源：**
- `index.js` - 论文生成工具
- `references/ieee_formatting_specs.md` - IEEE 格式规范
- `references/acm_formatting_specs.md` - ACM 格式规范
- `references/writing_style_guide.md` - 写作风格指南
- `assets/full_paper_template.pdf` - IEEE 论文模板
- `assets/interim-layout.pdf` - ACM 论文模板

### 3. find-skills
**功能：** 帮助发现和安装 agent skills 生态系统中的技能

**适用场景：**
- 搜索特定功能的技能
- 浏览可用技能
- 安装新技能

**核心特性：**
- 交互式技能搜索
- 技能推荐
- 安装指导

## 使用已安装技能的方法

### 方法 1：阅读技能文档（最常用）

每个技能的 SKILL.md 包含完整的使用指南。在 Kiro 中，我可以直接读取这些文档：

```typescript
// 我会这样做：
readFile(".agents/skills/academic-research-writer/SKILL.md")
```

然后按照文档中的工作流和指南来帮助你完成任务。

**示例场景：**
```
用户："帮我写一篇关于机器学习的学术论文"

我的操作：
1. 读取 academic-research-writer 的 SKILL.md
2. 按照其中的工作流指导：
   - 理解需求
   - 研究规划
   - 来源发现和验证
   - 文档结构
   - 写作指南
   - IEEE 引用格式
3. 生成符合学术标准的论文
```

### 方法 2：执行技能脚本

如果技能包含可执行脚本，我可以直接运行它们：

**查看可用脚本：**
```bash
# 列出技能的脚本目录
ls .agents/skills/academic-research-writer/scripts/
```

**执行 Python 脚本：**
```bash
# 运行 IEEE 格式化工具
python .agents/skills/academic-research-writer/scripts/ieee_formatter.py [参数]
```

**执行 JavaScript 脚本：**
```bash
# 运行论文生成工具
node .agents/skills/research-paper-writer/index.js [参数]
```

**注意事项：**
- 确保已安装脚本所需的依赖（Python 包、npm 包等）
- 查看脚本文档了解所需参数
- 某些脚本可能需要配置环境变量

### 方法 3：转换为 Kiro Power（推荐用于常用技能）

对于经常使用的技能，可以将其转换为 Kiro Power，获得更好的集成：

**转换的好处：**
- 更好的文档结构（POWER.md + steering files）
- 可以包装脚本为 MCP 服务器（提供工具调用能力）
- 与 Kiro 深度集成
- 通过 Powers UI 统一管理

**转换步骤：**

1. **分析技能内容**
   ```bash
   # 查看技能结构
   tree .agents/skills/[skill-name]
   
   # 阅读 SKILL.md
   readFile(".agents/skills/[skill-name]/SKILL.md")
   ```

2. **创建 Power 目录结构**
   ```bash
   mkdir powers/[power-name]
   ```

3. **转换文档**
   - 将 SKILL.md 内容转换为 POWER.md
   - 添加 Power 元数据（frontmatter）
   - 重组为 Kiro 风格
   - 如果内容过多（>500 行），拆分为 steering 文件

4. **包装脚本为 MCP 服务器（可选）**
   
   如果技能有 Python/JS 脚本，可以包装为 MCP 服务器：
   
   ```python
   # powers/[power-name]/mcp-server/server.py
   from mcp.server import Server
   from mcp.server.stdio import stdio_server
   
   app = Server("skill-name")
   
   @app.tool()
   def skill_function(param: str) -> str:
       """工具描述"""
       # 调用原始脚本逻辑
       return result
   
   if __name__ == "__main__":
       stdio_server(app)
   ```

5. **配置 MCP（如果包装了脚本）**
   
   创建 `powers/[power-name]/mcp.json`：
   ```json
   {
     "mcpServers": {
       "skill-name": {
         "command": "python",
         "args": ["powers/[power-name]/mcp-server/server.py"],
         "disabled": false
       }
     }
   }
   ```

6. **测试和安装**
   - 使用 Powers UI 安装本地 Power
   - 测试功能是否正常
   - 迭代改进

**转换示例：academic-research-writer**

该技能可以转换为 Power，提供：
- `format_ieee_citation` 工具 - 格式化 IEEE 引用
- `validate_sources` 工具 - 验证学术来源
- 学术写作 steering 文件 - 详细的写作指南

## 搜索和安装新技能

### Skills CLI 基础

Skills CLI (`npx skills`) 是开源 agent skills 生态系统的包管理器。

**核心命令：**
```bash
# 搜索技能
npx skills find [query]

# 安装技能
npx skills add <package>

# 检查更新
npx skills check

# 更新所有技能
npx skills update
```

**浏览技能：** https://skills.sh/

### 常见技能分类

| 分类 | 示例查询 |
|------|---------|
| Web 开发 | react, nextjs, typescript, css, tailwind |
| 测试 | testing, jest, playwright, e2e |
| DevOps | deploy, docker, kubernetes, ci-cd |
| 文档 | docs, readme, changelog, api-docs |
| 代码质量 | review, lint, refactor, best-practices |
| 设计 | ui, ux, design-system, accessibility |
| 效率工具 | workflow, automation, git |

### 搜索技能示例

**场景 1：需要 React 性能优化帮助**
```bash
npx skills find react performance
```

**场景 2：需要 PR 审查工具**
```bash
npx skills find pr review
```

**场景 3：需要创建 changelog**
```bash
npx skills find changelog
```

### 安装技能

找到合适的技能后，使用以下命令安装：

```bash
# 安装到当前项目
npx skills add <owner/repo@skill>

# 全局安装（用户级别）
npx skills add <owner/repo@skill> -g -y
```

**示例：**
```bash
# 安装 Vercel React 最佳实践技能
npx skills add vercel-labs/agent-skills@vercel-react-best-practices
```

## 技能使用最佳实践

### 1. 优先阅读文档

在使用技能前，先阅读 SKILL.md 了解：
- 技能的核心功能
- 适用场景
- 工作流程
- 注意事项

### 2. 检查依赖

如果技能包含脚本，检查所需依赖：
- Python 包：查看 `requirements.txt` 或脚本导入
- npm 包：查看 `package.json`
- 系统依赖：查看文档说明

### 3. 测试脚本

在正式使用前，先测试脚本是否正常工作：
```bash
# 使用测试数据运行脚本
python .agents/skills/[skill-name]/scripts/[script].py --test
```

### 4. 版本管理

定期检查和更新技能：
```bash
# 检查可用更新
npx skills check

# 更新所有技能
npx skills update
```

### 5. 文档化使用

如果某个技能特别有用，考虑：
- 将其转换为 Kiro Power
- 或创建 steering 文件记录使用方法
- 分享给团队成员

## 技能目录结构

典型的技能目录结构：

```
.agents/skills/
├── academic-research-writer/
│   ├── SKILL.md                    # 技能文档（必需）
│   ├── scripts/                    # 可执行脚本（可选）
│   │   └── ieee_formatter.py
│   ├── references/                 # 参考文档（可选）
│   │   ├── ACADEMIC-WRITING.md
│   │   ├── IEEE-CITATION-GUIDE.md
│   │   └── SOURCE-VERIFICATION.md
│   └── assets/                     # 资源文件（可选）
│       └── research_paper_template.md
├── research-paper-writer/
│   ├── SKILL.md
│   ├── index.js
│   ├── package.json
│   ├── references/
│   └── assets/
└── find-skills/
    └── SKILL.md
```

## 与 Kiro Powers 的区别

| 特性 | Agent Skills | Kiro Powers |
|------|-------------|-------------|
| 来源 | 开源生态系统 | Kiro 官方或社区 |
| 安装方式 | `npx skills add` | Powers UI |
| 文档格式 | SKILL.md | POWER.md + steering/ |
| MCP 集成 | 无 | 可包含 MCP 服务器 |
| 工具调用 | 需手动执行脚本 | 可通过 MCP 工具调用 |
| 管理方式 | CLI 命令 | Powers UI |
| 适用范围 | 多个 AI 助手 | Kiro 专用 |

**选择建议：**
- **偶尔使用**：直接使用 Agent Skills
- **频繁使用**：转换为 Kiro Power
- **需要工具调用**：必须转换为 Power（包含 MCP）

## 故障排除

### 问题 1：找不到技能目录

**症状：** `.agents/skills/` 目录不存在

**解决方案：**
```bash
# 创建目录
mkdir .agents/skills

# 安装第一个技能
npx skills add <package>
```

### 问题 2：脚本执行失败

**症状：** 运行脚本时报错

**可能原因和解决方案：**

1. **缺少依赖**
   ```bash
   # Python 依赖
   pip install -r .agents/skills/[skill-name]/requirements.txt
   
   # npm 依赖
   cd .agents/skills/[skill-name]
   npm install
   ```

2. **Python 版本不兼容**
   ```bash
   # 检查 Python 版本
   python --version
   
   # 使用虚拟环境
   python -m venv venv
   source venv/bin/activate  # Linux/Mac
   venv\Scripts\activate     # Windows
   ```

3. **权限问题**
   ```bash
   # 添加执行权限（Linux/Mac）
   chmod +x .agents/skills/[skill-name]/scripts/[script].py
   ```

### 问题 3：技能文档不完整

**症状：** SKILL.md 缺少关键信息

**解决方案：**
1. 查看技能的 GitHub 仓库获取更多信息
2. 查看 `references/` 目录中的额外文档
3. 在 https://skills.sh/ 搜索技能查看完整描述

### 问题 4：技能更新后不工作

**症状：** 更新技能后功能异常

**解决方案：**
```bash
# 回滚到之前的版本
npx skills add <owner/repo@skill@version>

# 或重新安装
npx skills remove <skill-name>
npx skills add <owner/repo@skill>
```

## 进阶使用

### 创建自定义技能

如果现有技能不满足需求，可以创建自己的技能：

```bash
# 初始化新技能
npx skills init my-custom-skill

# 编辑 SKILL.md 添加文档
# 添加脚本到 scripts/ 目录
# 测试技能功能

# 分享技能（可选）
# 推送到 GitHub 仓库
```

### 组合多个技能

可以组合多个技能的功能：

**示例：学术写作工作流**
1. 使用 `find-skills` 搜索相关研究工具
2. 使用 `academic-research-writer` 撰写论文
3. 使用 `research-paper-writer` 格式化最终版本

### 贡献技能到生态系统

如果创建了有用的技能，可以分享给社区：

1. 在 GitHub 创建公开仓库
2. 按照 skills 规范组织目录结构
3. 编写完整的 SKILL.md 文档
4. 提交到 https://skills.sh/

## 相关资源

- **Skills 官网：** https://skills.sh/
- **Skills CLI 文档：** https://github.com/skills-sh/cli
- **Kiro Powers 文档：** 使用 `power-builder` Power
- **社区技能仓库：**
  - vercel-labs/agent-skills
  - ComposioHQ/awesome-claude-skills

## 总结

Agent Skills 为 AI 助手提供了丰富的能力扩展。在 Kiro 中：

1. **直接使用**：阅读 SKILL.md，按照指南工作
2. **执行脚本**：运行技能中的 Python/JS 脚本
3. **转换为 Power**：将常用技能转换为 Kiro Power，获得更好的集成

选择合适的使用方式，可以大大提升工作效率！

---

**技能目录：** `.agents/skills/`
**安装命令：** `npx skills add <package>`
**浏览技能：** https://skills.sh/
