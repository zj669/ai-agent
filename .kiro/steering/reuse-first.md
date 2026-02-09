---
inclusion: always
---

# 架构一致性与复用优先 (Architecture Alignment)

## 核心原则

1. **封装组件优先**：禁止直接使用底层 SDK（如 RedisTemplate、RestTemplate），必须优先查找并使用项目已封装的 Service（如 RedisService、HttpService）。
2. **业务实体对齐**：在新建持久化逻辑前，必须检查现有数据库 Schema。严禁创建功能重叠的表（如已有 WorkflowNodeExecution，禁止新建 WorkflowExecution）。

## 强制执行流程（编码前必做）

### 1. 深度上下文检索

在生成任何代码前，必须执行以下 `grepSearch` 操作：

- **查组件**：搜索常见工具类的封装。示例：`grepSearch "Service" in infra/`。
- **查实体**：搜索业务关键字（如 "Workflow"、"Order"）。查找已有的 Entity/PO 和数据库表结构。
- **查逻辑**：搜索是否有已存在的同类业务方法。

### 2. 冲突与重叠检查

若发现现有组件/表与新需求部分重叠：

- **禁止绕过**：不允许因为现有组件不完全匹配而直接写底层实现。
- **优先扩展**：优先在现有 Service/Entity 上进行扩展。
- **确认机制**：若改动可能破坏现有逻辑，必须列出差异并询问："发现已有 X 实体/服务，是否在基础上扩展或建立关联？"

## 强制输出格式

在编写代码前，回复以下简报：

```markdown
**架构对齐检查**
- **发现可用组件**：（例如：已找到 RedisService，将放弃 RedisTemplate）
- **发现关联实体**：（例如：已找到 WorkflowNodeExecution，将基于此扩展，而非建新表）
- **复用策略**：[完全复用 / 扩展现有 / 新建关联]
```
