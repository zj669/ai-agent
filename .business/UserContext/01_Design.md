# 用户上下文传递机制 - 设计文档

## 1. 问题描述

**现状**：多个 Controller 中硬编码 `Long userId = 1L`

```java
// AgentController.java (5 处)
Long userId = 1L; // Placeholder

// KnowledgeController.java (2 处)  
Long userId = 1L;

// HumanReviewController.java (1 处)
Long userId = 1L; // Mock or from Security Context
```

**影响**：
- 无法识别真实用户
- 权限隔离无效
- 安全风险

---

## 2. 现有基础设施

| 组件 | 状态 | 位置 |
|------|------|------|
| `UserContext` | ✅ 已实现 | `ai-agent-shared/.../context/UserContext.java` |
| `ITokenService.getUserIdFromToken()` | ✅ 可用 | Domain 层 |
| `LoginInterceptor` | ⚠️ 未设置上下文 | Interface 层 |
| `AuthStrategy` (Debug/JWT) | ✅ 验证可用 | Interface 层 |

---

## 3. 技术方案

### 3.1 修改 LoginInterceptor

1. **在认证成功后设置 `UserContext`**
2. **添加 `afterCompletion()` 方法清理上下文**

```java
// 认证成功后
UserContext.setUserId(userId);

// 请求完成后
@Override
public void afterCompletion(...) {
    UserContext.clear();
}
```

### 3.2 修改 Controllers

替换所有 `Long userId = 1L` 为：

```java
Long userId = UserContext.getUserId();
if (userId == null) {
    return Response.error(401, "Unauthorized");
}
```

---

## 4. Proposed Changes

### [MODIFY] [LoginInterceptor.java](file:///D:/java/ai-agent/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java)

1. 注入 `ITokenService` 用于从 JWT 解析 userId
2. Debug 模式下直接使用 header 中的 userId
3. JWT 模式下调用 `tokenService.getUserIdFromToken()`
4. 认证成功后调用 `UserContext.setUserId()`
5. 新增 `afterCompletion()` 方法调用 `UserContext.clear()`

---

### [MODIFY] [AgentController.java](file:///D:/java/ai-agent/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/agent/web/AgentController.java)

替换 5 处 `Long userId = 1L` 为 `UserContext.getUserId()`

---

### [MODIFY] [KnowledgeController.java](file:///D:/java/ai-agent/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/web/KnowledgeController.java)

替换 2 处 `Long userId = 1L` 为 `UserContext.getUserId()`

---

### [MODIFY] [HumanReviewController.java](file:///D:/java/ai-agent/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java)

替换 1 处 `Long userId = 1L` 为 `UserContext.getUserId()`

---

## 5. Verification Plan

### 5.1 编译验证
```bash
mvn compile -DskipTests
```

### 5.2 手动测试（需用户执行）

1. **启用 Debug 模式**：设置 `AUTH_DEBUG_ENABLED=true`
2. **发送请求**：
   ```bash
   curl -H "X-Debug-User-Id: 123" http://localhost:8080/api/agent/list
   ```
3. **验证日志**：查看是否有 `Debug Authentication successful for UserID: 123`

---

## 6. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| ThreadLocal 内存泄漏 | 高 | `afterCompletion()` 必须调用 `clear()` |
| 未认证请求到达 Controller | 中 | 添加 null 检查 |
| 异步线程上下文丢失 | 低 | 当前功能不涉及异步 |

---

> **⛔ STOP POINT**: 设计文档已生成。请审核。（输入'通过'进入编码阶段）
