# Continue vs Spawn 决策落地 — Phase 追踪 + 上下文重叠评估

## Goal

让 Continue vs Spawn 决策矩阵从"提示词里的建议"变成"执行层真正支撑的行为"。

## Problem

当前 Swarm Agent 的 Continue vs Spawn 决策：
- **Phase** 硬编码为 `IMPLEMENTATION`，从不流转
- **无上下文追踪**，代码不记录 Worker 探索过哪些文件/模块
- **决策无支撑**，提示词里有矩阵，但运行时没有结构化数据供 LLM 参考

结果：AI 决策随机，执行混乱。

## Requirements

### R1: Phase 动态追踪

- `SwarmAgentRunner.currentPhase` 从硬编码改为**运行时动态追踪**
- Phase 流转：`RESEARCH → SYNTHESIS → IMPLEMENTATION → VERIFICATION`
- Coordinator 在不同 Phase 有不同的任务分发策略
- Phase 转换触发条件：
  - RESEARCH: 首次启动或接收调研任务
  - SYNTHESIS: Coordinator 收到所有 Worker 调研结果后
  - IMPLEMENTATION: Coordinator 开始派发实现任务
  - VERIFICATION: 实现任务完成，进入验证

### R2: 结构化任务上下文追踪

- 引入 `SwarmTaskContext` 值对象，记录 Worker 探索过的：
  - 文件路径列表（`exploredFiles`）
  - 相关模块列表（`exploredModules`）
  - 关键发现摘要（`findings`）
- `SwarmAgentRunner` 在每次 ReAct 循环后更新上下文
- 上下文通过消息传递回 Coordinator

### R3: Context Overlap API

- 新增 `SwarmContextAnalyzer` 服务，计算当前任务与 Worker 上下文的重叠度
- 重叠度评分维度：
  - 文件路径重叠率
  - 模块重叠率
  - 历史任务相关性
- 将重叠度评分注入 Coordinator 的 prompt，供 Continue vs Spawn 决策参考

### R4: Continue vs Spawn 决策支撑

- 在 Coordinator 的 prompt 中增加**结构化上下文块**：
  ```
  【当前任务上下文】
  - 任务类型: {taskType}
  - 探索过的文件: {exploredFiles}
  - 上下文重叠度: {overlapScore} (HIGH/MEDIUM/LOW)
  - 推荐策略: {recommendedStrategy} (Continue/Spawn)
  ```
- LLM 仍做最终决策，但有结构化数据支撑

## Technical Notes

### Phase 流转实现

在 `SwarmAgentRunner.java` 中：

1. 新增 `currentPhase` 字段，初始化为 `RESEARCH`
2. 新增 `transitionPhase(newPhase)` 方法，触发时更新并记录日志
3. Phase 转换条件通过 `SwarmPhaseTransitionService` 判断

### 上下文追踪实现

在 `SwarmAgentRunner.processTurn()` 末尾：

1. 解析 LLM 响应中的 `exploredFiles`、`findings` 等字段
2. 更新 `SwarmTaskContext` 值对象
3. 通过 `SwarmAgentEventBus` 广播上下文更新

### Context Overlap 计算

在 `SwarmContextAnalyzer.java`（新建）中：

```java
public record ContextOverlapScore(
    double fileOverlap,      // 0.0 - 1.0
    double moduleOverlap,    // 0.0 - 1.0
    double taskRelevance,    // 0.0 - 1.0
    String level,           // HIGH / MEDIUM / LOW
    String recommendedStrategy
) {}
```

### 文件变更

| 文件 | 操作 |
|------|------|
| `SwarmAgentRunner.java` | 修改 Phase 追踪、上下文收集 |
| `SwarmPromptSection.java` | 增加结构化上下文块模板 |
| `SwarmPromptService.java` | 增加 overlapScore 注入 |
| `SwarmTaskContext.java` | 新建 — 任务上下文值对象 |
| `SwarmContextAnalyzer.java` | 新建 — 上下文重叠度分析服务 |

## Acceptance Criteria

- [ ] Phase 从硬编码 `IMPLEMENTATION` 改为动态流转
- [ ] Worker 探索过的文件路径被记录并传递给 Coordinator
- [ ] Context Overlap API 返回结构化评分（HIGH/MEDIUM/LOW）
- [ ] Coordinator prompt 中包含上下文重叠度信息
- [ ] 相同文件上下文的子任务 → Continue（上下文复用）
- [ ] 无关任务 → Spawn（避免探索噪声）
