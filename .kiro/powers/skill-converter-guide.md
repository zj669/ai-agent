# Skills 到 Powers 转换指南

## 为什么转换？

Kiro Powers 比传统 Skills 更强大：
- 可以包含 MCP 服务器（提供工具调用）
- 更好的文档结构（POWER.md + steering files）
- 与 Kiro 深度集成

## 转换步骤

### 1. 分析现有 Skill

检查技能包含的内容：
```bash
# 查看技能结构
tree .agents/skills/[skill-name]

# 阅读 SKILL.md
cat .agents/skills/[skill-name]/SKILL.md
```

### 2. 创建 Power 结构

```
.kiro/powers/[power-name]/
├── POWER.md              # 主文档（从 SKILL.md 转换）
├── steering/             # 工作流指南
│   └── getting-started.md
├── mcp-server/           # 如果有脚本，包装为 MCP
│   ├── server.py
│   └── requirements.txt
└── references/           # 参考文档
    └── ...
```

### 3. 转换文档

将 SKILL.md 转换为 POWER.md：
- 添加 Power 元数据
- 重组为 Kiro 风格
- 添加使用示例

### 4. 包装脚本（可选）

如果技能有 Python/JS 脚本，考虑包装为 MCP 服务器：

```python
# mcp-server/server.py
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

### 5. 配置 MCP

在 `.kiro/settings/mcp.json` 中添加：

```json
{
  "mcpServers": {
    "skill-name": {
      "command": "python",
      "args": [".kiro/powers/skill-name/mcp-server/server.py"],
      "disabled": false
    }
  }
}
```

## 示例：转换 academic-research-writer

该技能包含：
- IEEE 格式化脚本
- 学术写作指南
- 引用验证工具

转换后的 Power 可以提供：
- `format_ieee_citation` 工具
- `validate_sources` 工具
- 学术写作 steering 文件

## 快速使用现有技能

不需要完整转换，可以：

1. **读取文档**：
```bash
# 使用 readFile 工具
readFile(".agents/skills/[skill-name]/SKILL.md")
```

2. **执行脚本**：
```bash
# 使用 executePwsh 工具
python .agents/skills/[skill-name]/scripts/[script].py
```

3. **创建 Steering**：
将 SKILL.md 复制到 `.kiro/steering/` 作为参考

## 推荐策略

- **偶尔使用**：直接读取 SKILL.md 和执行脚本
- **频繁使用**：转换为 Steering 文件
- **核心功能**：完整转换为 Power（包含 MCP）
