# Bug 记录：主工作空间编译失败

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-05 |
| **模块** | 后端 `ai-agent-application` / `ai-agent-interfaces` |
| **严重度** | P0（编译无法通过，后端无法启动） |
| **发现方式** | 执行 `mvn compile` 时报错 |

---

## 问题描述

项目代码改动在 git worktree（`C:\Users\32183\.cursor\worktrees\ai-agent\nxs`）中完成，但主工作空间（`e:\WorkSpace\repo\ai-agent`）的部分配套文件未同步，导致以下两类编译错误。

### 错误一：`AiAgentApplication.java` 主类缺失

执行 `mvn spring-boot:run` 或 `mvn install` 时报错：

```
Unable to find main class, please add a 'mainClass' property
```

**原因**：`ai-agent-interfaces` 模块的 Spring Boot 应用入口类文件在主工作空间中不存在（可能从未提交或被误删），导致 Maven 打包时无法确定启动类。

### 错误二：`SwarmAgentRunner` 构造函数参数不匹配

编译 `ai-agent-application` 时报错：

```
无法将类 SwarmAgentRunner 中的构造器 SwarmAgentRunner 应用到给定类型
  需要: ...LlmProviderConfig
  找到: ...LlmProviderConfig,boolean  ← 新增了 isRoot 参数
```

相关方法签名不匹配：
- `SwarmTools.createAgent(...)` — 主工作空间中不存在此方法（仍是旧的 `create` 方法）
- `SwarmTools.executeWorkflow(...)` — 主工作空间中不存在此方法
- `SwarmPromptTemplate.buildRootPrompt(...)` — 主工作空间中不存在此方法
- `SwarmPromptTemplate.buildSubPrompt(...)` — 主工作空间中不存在此方法

**原因**：worktree 中新增的方法（区分 Root/Sub 工具集、Prompt 模板重构）未同步到主工作空间。

---

## 修复方案

### 修复一：创建 AiAgentApplication.java 主类

在 `ai-agent-interfaces/src/main/java/com/zj/aiagent/` 下创建启动类：

```java
package com.zj.aiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}
```

### 修复二：同步 SwarmPromptTemplate.java

重写 `SwarmPromptTemplate`，提供两套 Prompt：

- `buildRootPrompt(agentId, workspaceId, role, humanAgentId)`：协调者模板，列出全部 5 个工具，说明任务分解流程
- `buildSubPrompt(agentId, workspaceId, role, humanAgentId, parentAgentId)`：执行者模板，只有 `send` + `self` 工具，明确禁止创建子 Agent

### 修复三：同步 SwarmTools.java

更新 `SwarmTools`，移除废弃工具，新增：

- `createAgent(role, description, graphJson)` — 合并原 `create`，支持可选工作流图
- `executeWorkflow(agentId, input)` — 执行已发布 Agent 的工作流（当前返回 not_implemented）
- 移除 `sendGroupMessage`、`listGroups`

### 修复四：同步 SwarmAgentRuntimeService.java

在 `startAgent` 方法中，通过查询 `parentId` 对应 Agent 的 role 来判断当前 Agent 是否为 Root：

```java
boolean isRoot = false;
if (agent.getParentId() != null) {
    isRoot = agentRepository.findById(agent.getParentId())
            .map(parent -> "human".equals(parent.getRole()))
            .orElse(false);
}
// 传入 SwarmAgentRunner 构造函数
new SwarmAgentRunner(..., isRoot)
```

---

## 修复后验证

```
mvn compile       → BUILD SUCCESS（全部 6 个模块）
mvn install -DskipTests → BUILD SUCCESS
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local
    → Started AiAgentApplication in 10.246 seconds
```

---

## 涉及文件

| 文件 | 操作 |
|------|------|
| `ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java` | 新建 |
| `ai-agent-application/.../swarm/prompt/SwarmPromptTemplate.java` | 重写 |
| `ai-agent-application/.../swarm/runtime/SwarmTools.java` | 重写 |
| `ai-agent-application/.../swarm/SwarmAgentRuntimeService.java` | 修改（增加 isRoot 判断和传参） |
