# PRD: API 一致性修正与优化

## 1. 背景与目标

### 1.1 问题描述
通过 APIFOX 文档与代码实现的对比，发现以下不一致和不规范问题：
1. HTTP 方法不符合 RESTful 规范（如 DELETE 使用 POST）
2. API 路径与文档不一致
3. 冗余接口（Debug vs Start）

### 1.2 目标
统一 API 设计规范，提升接口一致性和可维护性。

---

## 2. 修正清单

### 2.1 Agent 管理接口修正

#### 问题 1: DELETE 接口使用 POST
**现状**: `POST /api/agent/delete/{id}`  
**目标**: `DELETE /api/agent/{id}`

**影响**:
- Controller: `AgentController.deleteAgent()`
- 前端调用需同步修改

**优先级**: P1（规范性问题）

---

#### 问题 2: 路径不一致
**现状**: 
- 文档: `GET /api/agent/detail/{id}`
- 代码: `GET /api/agent/{id}`

**目标**: 统一为 `GET /api/agent/{id}`（更符合 RESTful）

**优先级**: P2（文档修正）

---

#### 问题 3: Debug 接口冗余
**现状**: `POST /api/agent/debug` 与 `/api/workflow/execution/start` 逻辑重复

**目标**: 删除 `/api/agent/debug`，在 `StartExecutionRequest` 中增加 `mode` 字段

**新设计**:
```java
public class StartExecutionRequest {
    private Long agentId;
    private Long userId;
    private String conversationId;
    private Integer versionId;
    private Map<String, Object> inputs;
    private ExecutionMode mode;  // NEW: STANDARD, DEBUG, DRY_RUN
}

public enum ExecutionMode {
    STANDARD,   // 常规执行
    DEBUG,      // Debug 模式（详细日志，不发送真实通知）
    DRY_RUN     // 干运行（不持久化）
}
```

**优先级**: P0（功能冗余）

---

### 2.2 用户接口修正

#### 问题 4: 修改用户信息使用 POST
**现状**: `POST /client/user/modify`  
**目标**: `PUT /client/user/profile` 或 `PATCH /client/user/profile`

**选择建议**:
- `PUT`: 全量更新（需传所有字段）
- `PATCH`: 部分更新（只传修改字段）

**推荐**: `PATCH /client/user/profile`（更灵活）

**优先级**: P1

---

### 2.3 新增接口补充

#### 工具元数据接口
**路径**: `GET /api/meta/tools`

**功能**: 获取系统支持的工具列表（供前端 Agent 画布使用）

**响应示例**:
```json
{
  "tools": [
    {
      "toolId": "google_search",
      "name": "Google 搜索",
      "description": "通过 Google 搜索获取信息",
      "icon": "https://example.com/icons/google.png",
      "inputSchema": {
        "type": "object",
        "properties": {
          "query": { "type": "string", "description": "搜索关键词" }
        },
        "required": ["query"]
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "results": { "type": "array" }
        }
      }
    }
  ]
}
```

**优先级**: P0（前端依赖）

---

#### 上下文调试接口
**路径**: `GET /api/workflow/execution/{executionId}/context`

**功能**: 获取执行上下文快照（LTM/STM/全局变量）

**响应示例**:
```json
{
  "executionId": "exec-123",
  "longTermMemories": [
    "用户上次提到喜欢蓝色",
    "公司产品定价为 999 元/年"
  ],
  "chatHistory": [
    { "role": "user", "content": "你好" },
    { "role": "assistant", "content": "你好！有什么可以帮您？" }
  ],
  "executionLog": "START → LLM-1 (完成) → HTTP-2 (执行中)",
  "globalVariables": {
    "userId": "12345",
    "sessionStart": "2026-01-12T15:00:00Z"
  }
}
```

**优先级**: P1（调试工具）

---

## 3. 实施计划

### 3.1 Phase 1: 规范性修正（P0-P1）
- [ ] 修改 `DELETE /agent/{id}` (2h)
- [ ] 合并 Debug 接口到 Start (4h)
- [ ] 修改 `PATCH /user/profile` (2h)

### 3.2 Phase 2: 新增接口（P0）
- [ ] 实现 `GET /meta/tools` (4h)
- [ ] 实现 `GET /execution/{id}/context` (3h)

### 3.3 Phase 3: 文档同步（P2）
- [ ] 更新 APIFOX 文档
- [ ] 更新前端 API 调用代码

---

## 4. 验收标准

- [ ] 所有 DELETE 操作使用 DELETE 方法
- [ ] Debug 功能通过 `mode` 参数实现，删除独立接口
- [ ] 用户修改接口符合 RESTful 规范
- [ ] 工具元数据接口返回正确的 Schema
- [ ] 上下文调试接口能返回完整的 `ExecutionContext` 快照

---

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 前端代码需同步修改 | 中 | 提前通知前端团队，提供迁移指南 |
| 现有调用方受影响 | 高 | 保留旧接口 2 个版本周期（废弃标记） |
| Debug 模式行为差异 | 低 | 详细设计 ExecutionMode 的行为差异文档 |
