# PRD: 人工审核（Human-in-the-Loop）API

## 1. 背景与目标

### 1.1 业务背景
当前工作流系统支持全自动执行，但缺乏人机协同能力。根据 `05-人工审核模块需求.md`，关键业务节点需要支持：
- 人工审核/干预
- 结果修正与补充
- 审批通过后继续执行

### 1.2 目标
提供完整的人工审核 API，支持工作流在特定节点暂停、等待人工决策、并根据审核结果继续/终止执行。

---

## 2. 功能需求

### 2.1 核心功能
| 功能 | 优先级 | 描述 |
|------|--------|------|
| 恢复/审批执行 | P0 | 提交人工审核决策，唤醒暂停的工作流 |
| 获取待审核列表 | P0 | 查询当前用户（通常为 Agent 创建者）待处理的暂停任务（仅摘要） |
| 获取审核详情 | P0 | 加载完整的上下文数据供审核员决策 |
| 审核历史记录 | P1 | 查看某个执行的所有审核记录 |

### 2.2 用户故事
- **作为运营人员**，我希望在 Agent 生成敏感内容后（Post-Execution），能够人工审核并修改，然后继续执行后续节点。
- **作为高级用户**，我希望在发送高成本 API 请求前（Pre-Execution），确认并修改参数。
- **作为管理员**，我希望看到所有待审核的任务队列，按优先级处理。

---

## 3. API 设计

### 3.1 恢复/审批执行

**POST** `/api/workflow/execution/resume`

#### 请求体
```json
{
  "executionId": "exec-uuid-123",
  "version": 5,   // [新增] 乐观锁控制，必填。防止多人并发审核冲突。
  "decision": "APPROVE",  // enum: APPROVE, REJECT
  "nodeId": "node-001",
  "edits": {
    "output": "..." // 如果是 Post-Execution 暂停，修改 Output
    // "input": "..." // 如果是 Pre-Execution 暂停，修改 Input
  },
  "comment": "修改了敏感词汇"
}
```

#### 逻辑说明
- **APPROVE**:
    - 若 `edits` 存在，覆盖对应 Context 数据（Input 或 Output）。
    - 恢复执行。
- **REJECT**:
    - 记录审核日志。
    - 将执行状态标记为 `TERMINATED` (或 `FAILED`)。
    - 停止后续调度。

### 3.2 获取待审核任务列表 (Lightweight)

**GET** `/api/workflow/execution/pending-review`

*优化：移除 `context` 大字段，防止列表页响应过慢。*

#### 响应
```json
{
  "total": 5,
  "list": [
    {
      "executionId": "exec-uuid-123",
      "agentId": 1,
      "agentName": "客服助手",
      "nodeId": "node-001",
      "nodeName": "内容审核",
      "pausedAt": "2026-01-12T15:30:00Z",
      "triggerPhase": "AFTER_EXECUTION" // 指示是在执行前还是执行后暂停
    }
  ]
}
```

### 3.3 获取审核任务详情 (Details)

**GET** `/api/workflow/execution/{executionId}/pending-review-detail`

#### 响应
```json
{
  "executionId": "exec-uuid-123",
  "nodeId": "node-001",
  "triggerPhase": "AFTER_EXECUTION",
  "context": {
    "inputs": { ... },     // 节点的输入
    "outputs": { ... },    // 节点的输出（仅 AFTER_EXECUTION 有值）
    "globalMemory": { ... } // 需要参考的上下文
  }
}
```

### 3.4 审核历史记录

**GET** `/api/workflow/execution/{executionId}/review-history`

#### 响应
```json
{
  "executionId": "exec-uuid-123",
  "reviews": [
    {
      "nodeId": "node-001",
      "reviewer": "admin@example.com",
      "decision": "REJECT",
      "comment": "内容违规",
      "reviewedAt": "2026-01-12T15:35:00Z"
    }
  ]
}
```

---

## 4. 领域模型影响

### 4.1 执行状态与配置
需要在 `ExecutionStatus` 枚举中增加： `PAUSED_FOR_REVIEW`。

**节点配置扩展 (`HumanReviewConfig`)**:
```java
public class HumanReviewConfig {
    private boolean enabled;
    private TriggerPhase triggerPhase; // BEFORE_EXECUTION, AFTER_EXECUTION
    // ...
}
```

### 4.2 审核记录实体 (Immutable)
(同前：`entry_type` 或其他字段可能需要记录是 Input 修改还是 Output 修改，暂通过 `modified_content` JSON 结构自描述)

---

## 5. 技术实现建议

### 5.1 Controller 层
- 拆分 List 和 Detail 接口。

### 5.2 应用服务 (`SchedulerService`)
- **暂停逻辑扩展**：
    - `BEFORE_EXECUTION`: 在 `scheduleNode` 开始时检查。
    - `AFTER_EXECUTION`: 在 `onNodeComplete` (或 Future callback) 中检查。
- **恢复逻辑扩展** (`resumeExecution`)：
    - 根据 paused node 的 `triggerPhase` 处理 `edits`。
    - 如果是 `BEFORE`，更新 Inputs 并调用 `strategy.execute`。
    - 如果是 `AFTER`，更新 Outputs 并调用 `onNodeComplete` (继续后续流程)。
    - 处理 `REJECT` -> `cancelExecution` / `terminate`.

---

## 6. 验收标准

- [ ] `/pending-review` 接口响应时间 < 200ms (不含大字段)。
- [ ] 支持配置 `BEFORE_EXECUTION` (改参) 和 `AFTER_EXECUTION` (改结果) 两种模式。
- [ ] `REJECT` 操作导致工作流终止。
- [ ] 恢复后正确使用修改后的 Input/Output 继续运行。
