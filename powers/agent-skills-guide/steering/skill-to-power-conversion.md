# Skills 到 Powers 转换详细工作流

本指南提供将 Agent Skills 转换为 Kiro Powers 的完整步骤。

## 何时转换？

**转换的时机：**
- ✅ 技能经常使用（每周多次）
- ✅ 技能包含有价值的脚本
- ✅ 希望通过 MCP 工具调用脚本
- ✅ 需要更好的文档组织
- ✅ 想要团队共享

**不需要转换：**
- ❌ 偶尔使用的技能
- ❌ 只需要阅读文档的技能
- ❌ 简单的参考指南

## 转换类型

### 类型 1：纯文档型 Power（Knowledge Base Power）

**适用于：** 技能只包含文档，没有脚本

**特点：**
- 只需要 POWER.md
- 不需要 mcp.json
- 快速转换

**示例技能：**
- find-skills（技能搜索指南）
- 最佳实践文档
- 参考指南

### 类型 2：带工具的 Power（Guided MCP Power）

**适用于：** 技能包含可执行脚本

**特点：**
- 需要 POWER.md + mcp.json
- 脚本包装为 MCP 服务器
- 提供工具调用能力

**示例技能：**
- academic-research-writer（包含 ieee_formatter.py）
- research-paper-writer（包含 index.js）

## 转换步骤

### 步骤 1：分析技能结构

**检查技能内容：**

```bash
# 查看技能目录结构
tree .agents/skills/[skill-name]

# 或使用 ls
ls -R .agents/skills/[skill-name]
```

**记录以下信息：**
- [ ] SKILL.md 的长度（行数）
- [ ] 是否有 scripts/ 目录
- [ ] 是否有 references/ 目录
- [ ] 是否有 assets/ 目录
- [ ] 脚本的编程语言（Python/JavaScript）
- [ ] 脚本的依赖（requirements.txt/package.json）

**阅读技能文档：**

```typescript
readFile(".agents/skills/[skill-name]/SKILL.md")
```

**理解：**
- 技能的核心功能
- 主要工作流
- 脚本的作用
- 适用场景

### 步骤 2：设计 Power 结构

**决策 1：是否需要 MCP 服务器？**

```
有可执行脚本？
├─ 是 → Guided MCP Power（需要 mcp.json）
└─ 否 → Knowledge Base Power（只需 POWER.md）
```

**决策 2：是否需要 steering 文件？**

```
SKILL.md 内容长度？
├─ < 500 行 → 单个 POWER.md
└─ > 500 行 → POWER.md + steering/
```

**规划目录结构：**

**纯文档型：**
```
powers/[power-name]/
└── POWER.md
```

**带工具型：**
```
powers/[power-name]/
├── POWER.md
├── mcp.json
└── mcp-server/
    ├── server.py (或 server.js)
    └── requirements.txt (或 package.json)
```

**大型文档：**
```
powers/[power-name]/
├── POWER.md
└── steering/
    ├── workflow-1.md
    ├── workflow-2.md
    └── reference.md
```

### 步骤 3：创建 Power 目录

```bash
# 创建 Power 目录
mkdir powers/[power-name]

# 如果需要 MCP 服务器
mkdir powers/[power-name]/mcp-server

# 如果需要 steering 文件
mkdir powers/[power-name]/steering
```

### 步骤 4：转换 POWER.md

**4.1 创建 Frontmatter**

从 SKILL.md 的 frontmatter 提取信息，转换为 POWER.md 格式：

```yaml
---
name: "[power-name]"
displayName: "[Display Name]"
description: "[从 SKILL.md 的 description 改写，最多 3 句话]"
keywords: ["[从 SKILL.md 提取或新增]"]
author: "[原作者或你的名字]"
---
```

**注意：**
- `name` 使用 kebab-case
- `displayName` 使用 Title Case
- `description` 简洁明了，最多 3 句话
- `keywords` 5-7 个相关关键词

**4.2 转换文档内容**

**基本结构：**

```markdown
# [Display Name]

## 概述

[2-3 段落说明：
- 这个 Power 做什么
- 为什么有用
- 核心能力]

## 入门指南（Onboarding）

### 前置条件
[列出所需的：
- 系统要求
- 依赖
- 账号或凭证]

### 安装
[如果有脚本，说明如何安装依赖]

### 配置
[如果需要配置，说明步骤]

## 常用工作流

[从 SKILL.md 提取主要工作流，每个工作流一个小节]

### 工作流 1：[名称]
[描述 + 步骤 + 示例]

### 工作流 2：[名称]
[描述 + 步骤 + 示例]

## 故障排除

[从 SKILL.md 提取常见问题和解决方案]

## 最佳实践

[从 SKILL.md 提取最佳实践]

## 配置

[如果是 Guided MCP Power，说明 MCP 配置]

---

[底部信息：包名、命令等]
```

**4.3 内容改写技巧**

- **保持原意**：不改变核心内容
- **重新组织**：按 Kiro Power 的结构重组
- **添加上下文**：为 Kiro 用户添加相关说明
- **简化语言**：使用更直接的表达
- **添加示例**：补充实际使用示例

### 步骤 5：创建 MCP 服务器（如果需要）

**5.1 分析脚本功能**

```bash
# 查看脚本内容
cat .agents/skills/[skill-name]/scripts/[script].py
```

**识别：**
- 脚本的输入参数
- 脚本的输出
- 脚本的主要功能
- 可以拆分为哪些工具

**5.2 设计 MCP 工具**

**原则：**
- 一个脚本可以拆分为多个工具
- 每个工具做一件事
- 工具名称清晰描述功能
- 参数类型明确

**示例设计：**

```
原脚本：ieee_formatter.py
功能：格式化 IEEE 引用

拆分为工具：
1. format_journal_article - 格式化期刊文章引用
2. format_conference_paper - 格式化会议论文引用
3. format_book - 格式化书籍引用
4. validate_citation - 验证引用格式
```

**5.3 实现 MCP 服务器**

**Python 示例：**

```python
# powers/[power-name]/mcp-server/server.py

from mcp.server import Server
from mcp.server.stdio import stdio_server
import sys
import os

# 添加原始脚本路径到 sys.path
skill_scripts_path = os.path.join(os.path.dirname(__file__), '../../../.agents/skills/[skill-name]/scripts')
sys.path.insert(0, skill_scripts_path)

# 导入原始脚本的功能
from original_script import some_function

app = Server("[power-name]")

@app.tool()
def tool_name(param1: str, param2: int = 10) -> str:
    """
    工具描述
    
    Args:
        param1: 参数 1 描述
        param2: 参数 2 描述（可选，默认 10）
    
    Returns:
        结果描述
    """
    try:
        # 调用原始脚本功能
        result = some_function(param1, param2)
        return result
    except Exception as e:
        return f"错误: {str(e)}"

@app.tool()
def another_tool(input_text: str) -> dict:
    """
    另一个工具描述
    
    Args:
        input_text: 输入文本
    
    Returns:
        包含结果的字典
    """
    # 实现逻辑
    return {"status": "success", "data": "..."}

if __name__ == "__main__":
    stdio_server(app)
```

**JavaScript 示例：**

```javascript
// powers/[power-name]/mcp-server/server.js

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

// 导入原始脚本
import { someFunction } from '../../../.agents/skills/[skill-name]/index.js';

const server = new Server({
  name: "[power-name]",
  version: "1.0.0"
});

server.setRequestHandler("tools/list", async () => ({
  tools: [
    {
      name: "tool_name",
      description: "工具描述",
      inputSchema: {
        type: "object",
        properties: {
          param1: {
            type: "string",
            description: "参数 1 描述"
          },
          param2: {
            type: "number",
            description: "参数 2 描述",
            default: 10
          }
        },
        required: ["param1"]
      }
    }
  ]
}));

server.setRequestHandler("tools/call", async (request) => {
  const { name, arguments: args } = request.params;
  
  if (name === "tool_name") {
    try {
      const result = await someFunction(args.param1, args.param2);
      return {
        content: [{ type: "text", text: result }]
      };
    } catch (error) {
      return {
        content: [{ type: "text", text: `错误: ${error.message}` }],
        isError: true
      };
    }
  }
  
  throw new Error(`未知工具: ${name}`);
});

const transport = new StdioServerTransport();
await server.connect(transport);
```

**5.4 创建依赖文件**

**Python - requirements.txt：**

```txt
# MCP SDK
mcp

# 原始脚本的依赖（从 .agents/skills/[skill-name] 复制）
dependency1>=1.0.0
dependency2>=2.0.0
```

**JavaScript - package.json：**

```json
{
  "name": "[power-name]-mcp-server",
  "version": "1.0.0",
  "type": "module",
  "dependencies": {
    "@modelcontextprotocol/sdk": "^0.5.0"
  }
}
```

### 步骤 6：创建 mcp.json（如果需要）

```json
{
  "mcpServers": {
    "[power-name]": {
      "command": "python",
      "args": ["powers/[power-name]/mcp-server/server.py"],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

**或 JavaScript：**

```json
{
  "mcpServers": {
    "[power-name]": {
      "command": "node",
      "args": ["powers/[power-name]/mcp-server/server.js"],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

**注意：**
- 使用相对路径（从工作区根目录）
- 如果需要环境变量，添加 `env` 字段
- 如果需要特定工作目录，添加 `cwd` 字段

### 步骤 7：创建 Steering 文件（如果需要）

**何时创建：**
- POWER.md 超过 500 行
- 有多个独立的工作流
- 有大量参考文档

**如何拆分：**

1. **按工作流拆分**
   ```
   steering/
   ├── workflow-1.md
   ├── workflow-2.md
   └── workflow-3.md
   ```

2. **按主题拆分**
   ```
   steering/
   ├── getting-started.md
   ├── advanced-usage.md
   ├── troubleshooting.md
   └── reference.md
   ```

3. **按功能拆分**
   ```
   steering/
   ├── citation-formatting.md
   ├── source-verification.md
   └── writing-guidelines.md
   ```

**Steering 文件内容：**

```markdown
# [Steering 文件标题]

[从 POWER.md 或 SKILL.md 提取的详细内容]

## [小节 1]

[详细说明]

## [小节 2]

[详细说明]

## 示例

[完整示例]

## 注意事项

[重要提示]
```

**在 POWER.md 中列出 Steering 文件：**

```markdown
## Available Steering Files

本 Power 包含以下详细指南：

- **workflow-1** - 工作流 1 的详细步骤
- **workflow-2** - 工作流 2 的详细步骤
- **reference** - 完整参考文档

使用 `readSteering` action 访问这些指南。
```

### 步骤 8：测试 Power

**8.1 安装依赖（如果有 MCP 服务器）**

```bash
# Python
cd powers/[power-name]/mcp-server
pip install -r requirements.txt

# JavaScript
cd powers/[power-name]/mcp-server
npm install
```

**8.2 测试 MCP 服务器（如果有）**

```bash
# Python
python powers/[power-name]/mcp-server/server.py

# JavaScript
node powers/[power-name]/mcp-server/server.js
```

**验证：**
- 服务器能正常启动
- 没有导入错误
- 工具定义正确

**8.3 在 Kiro 中安装 Power**

1. 打开 Kiro Powers UI
2. 点击 "Add Custom Power"
3. 选择 "Local Directory"
4. 输入完整路径：`D:\java\ai-agent\powers\[power-name]`
5. 点击 "Add"

**8.4 测试 Power 功能**

1. **激活 Power**
   ```
   kiroPowers action="activate" powerName="[power-name]"
   ```

2. **检查文档**
   - 阅读 POWER.md 内容
   - 检查 steering 文件列表
   - 验证工具列表（如果有 MCP）

3. **测试工具**（如果有 MCP）
   ```
   kiroPowers action="use" 
     powerName="[power-name]"
     serverName="[power-name]"
     toolName="[tool-name]"
     arguments={...}
   ```

4. **测试工作流**
   - 按照 POWER.md 中的工作流测试
   - 验证所有步骤都能正常工作
   - 检查错误处理

### 步骤 9：迭代改进

**常见问题和修复：**

**问题 1：MCP 服务器无法启动**
- 检查依赖是否安装
- 检查导入路径是否正确
- 查看错误日志

**问题 2：工具调用失败**
- 验证参数类型
- 检查错误处理
- 添加日志输出

**问题 3：文档不清晰**
- 添加更多示例
- 补充说明
- 重组结构

**问题 4：性能问题**
- 优化脚本逻辑
- 添加缓存
- 减少不必要的计算

### 步骤 10：文档化和分享

**10.1 完善文档**

- [ ] POWER.md 包含所有必要信息
- [ ] 所有工具都有清晰的描述
- [ ] 包含完整的示例
- [ ] 故障排除部分完整
- [ ] 最佳实践明确

**10.2 添加 README（可选）**

在 Power 目录创建 README.md：

```markdown
# [Power Name]

转换自 Agent Skill: [skill-name]

## 安装

[安装说明]

## 使用

[快速开始]

## 原始技能

- 来源: [GitHub URL]
- 版本: [version]
- 许可: [license]
```

**10.3 分享 Power**

如果 Power 有价值，可以分享：

1. **团队内分享**
   - 提交到团队的 Git 仓库
   - 文档化安装步骤

2. **公开分享**
   - 推送到 GitHub 公开仓库
   - 提交到 Kiro Powers 推荐列表

## 转换示例

### 示例 1：纯文档型转换

**原技能：** find-skills

**转换步骤：**
1. 创建 `powers/find-skills-guide/`
2. 转换 SKILL.md 为 POWER.md
3. 添加 Kiro 特定的说明
4. 测试和安装

**结果：** Knowledge Base Power，只有 POWER.md

### 示例 2：带工具型转换

**原技能：** academic-research-writer

**转换步骤：**
1. 创建 `powers/academic-research-writer/`
2. 转换 SKILL.md 为 POWER.md
3. 包装 `ieee_formatter.py` 为 MCP 服务器
4. 创建 mcp.json
5. 测试工具调用
6. 迭代改进

**结果：** Guided MCP Power，包含 POWER.md + mcp.json + MCP 服务器

## 最佳实践

### 1. 保持原始技能

不要删除 `.agents/skills/` 中的原始技能：
- 作为参考
- 可能需要更新
- 其他 AI 助手可能使用

### 2. 版本管理

在 POWER.md 中记录：
- 原始技能的版本
- 转换日期
- 修改历史

### 3. 定期同步

如果原始技能更新：
- 检查更新内容
- 评估是否需要更新 Power
- 同步重要改进

### 4. 测试充分

在分享前：
- 完整测试所有功能
- 验证文档准确性
- 收集反馈

### 5. 文档清晰

- 使用清晰的语言
- 提供完整示例
- 包含故障排除
- 说明限制和注意事项

## 工具和资源

### 有用的命令

```bash
# 查看技能结构
tree .agents/skills/[skill-name]

# 统计文档行数
wc -l .agents/skills/[skill-name]/SKILL.md

# 查找 Python 依赖
grep "import" .agents/skills/[skill-name]/scripts/*.py

# 查找 npm 依赖
cat .agents/skills/[skill-name]/package.json
```

### 参考资源

- **Power Builder：** 使用 `power-builder` Power 获取详细指导
- **MCP 文档：** https://kiro.dev/docs/mcp/
- **Skills 生态：** https://skills.sh/

## 总结

转换 Agent Skills 为 Kiro Powers 的关键步骤：

1. ✅ 分析技能结构和内容
2. ✅ 决定 Power 类型（Knowledge Base 或 Guided MCP）
3. ✅ 转换文档为 POWER.md
4. ✅ 包装脚本为 MCP 服务器（如需要）
5. ✅ 创建 mcp.json（如需要）
6. ✅ 测试所有功能
7. ✅ 迭代改进
8. ✅ 文档化和分享

通过转换，你可以获得：
- 更好的文档组织
- MCP 工具调用能力
- 与 Kiro 的深度集成
- 统一的管理界面

开始转换你最常用的技能吧！
