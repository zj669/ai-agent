# Agent 管理模块集成测试报告

## 测试概述

**测试日期**: 2026-02-10
**测试人员**: qa-engineer
**测试范围**: Agent CRUD、版本管理、发布回滚
**测试环境**:
- Backend: Spring Boot 3.4.9 (Java 21)
- Database: MySQL 8.0 (Port 13306)
- Redis: 6.379
- Profile: local

## 测试用例清单

### 1. Agent CRUD 操作 (10个用例)

#### TC-001: 创建 Agent - 成功场景
- **前置条件**: 用户已登录
- **测试步骤**:
  1. POST `/api/agent/create`
  2. Body: `{"name": "Test Agent", "description": "Test Description", "icon": "icon.png"}`
- **预期结果**:
  - HTTP 200
  - 返回 agentId
  - 数据库中创建记录，status=DRAFT, version=1
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-002: 创建 Agent - 未登录
- **前置条件**: 无 token
- **测试步骤**: POST `/api/agent/create`
- **预期结果**: HTTP 401 Unauthorized
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-003: 创建 Agent - 参数校验失败
- **前置条件**: 用户已登录
- **测试步骤**: POST `/api/agent/create` with empty name
- **预期结果**: HTTP 400 Bad Request
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-004: 查询 Agent 列表
- **前置条件**: 用户已创建多个 Agent
- **测试步骤**: GET `/api/agent/list`
- **预期结果**:
  - HTTP 200
  - 返回当前用户的所有 Agent
  - 不包含其他用户的 Agent
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-005: 查询 Agent 详情
- **前置条件**: Agent 存在
- **测试步骤**: GET `/api/agent/{id}`
- **预期结果**:
  - HTTP 200
  - 返回完整的 Agent 信息（包括 graphJson）
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-006: 查询不存在的 Agent
- **前置条件**: Agent ID 不存在
- **测试步骤**: GET `/api/agent/99999`
- **预期结果**: HTTP 404 Not Found
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-007: 更新 Agent - 成功场景
- **前置条件**: Agent 存在且为 DRAFT 状态
- **测试步骤**:
  1. POST `/api/agent/update`
  2. Body: `{"id": 1, "name": "Updated Name", "version": 1, "graphJson": "{...}"}`
- **预期结果**:
  - HTTP 200
  - 数据库中更新记录
  - version 保持不变（DRAFT 状态下）
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-008: 更新 Agent - 乐观锁冲突
- **前置条件**: Agent 存在
- **测试步骤**: POST `/api/agent/update` with wrong version
- **预期结果**: HTTP 409 Conflict (ConcurrentModificationException)
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-009: 更新 Agent - 权限校验
- **前置条件**: Agent 属于其他用户
- **测试步骤**: POST `/api/agent/update` with other user's agent
- **预期结果**: HTTP 403 Forbidden
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-010: 删除 Agent - 强制删除
- **前置条件**: Agent 存在
- **测试步骤**: DELETE `/api/agent/{id}/force`
- **预期结果**:
  - HTTP 200
  - 数据库中逻辑删除（deleted=1）
  - 所有版本一并删除
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 2. 版本管理 (10个用例)

#### TC-011: 发布 Agent - 首次发布
- **前置条件**: Agent 为 DRAFT 状态
- **测试步骤**: POST `/api/agent/publish` with `{"id": 1}`
- **预期结果**:
  - HTTP 200
  - status 变为 PUBLISHED
  - 创建版本快照（agent_version 表）
  - version 保持为 1
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-012: 发布 Agent - 重复发布
- **前置条件**: Agent 已为 PUBLISHED 状态
- **测试步骤**: POST `/api/agent/publish`
- **预期结果**: HTTP 400 Bad Request (已发布状态不能再次发布)
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-013: 修改已发布的 Agent
- **前置条件**: Agent 为 PUBLISHED 状态
- **测试步骤**: POST `/api/agent/update` with graphJson changes
- **预期结果**:
  - HTTP 200
  - status 变为 DRAFT
  - version 自增（version=2）
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-014: 查询版本历史
- **前置条件**: Agent 有多个版本
- **测试步骤**: GET `/api/agent/{id}/versions`
- **预期结果**:
  - HTTP 200
  - 返回所有版本列表（按版本号倒序）
  - 包含版本号、创建时间、graphSnapshot
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-015: 回滚到历史版本
- **前置条件**: Agent 有多个版本
- **测试步骤**: POST `/api/agent/rollback` with `{"id": 1, "targetVersion": 1}`
- **预期结果**:
  - HTTP 200
  - graphJson 恢复到目标版本
  - status 变为 DRAFT
  - version 保持当前值（不回退）
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-016: 回滚到不存在的版本
- **前置条件**: Agent 存在
- **测试步骤**: POST `/api/agent/rollback` with non-existent version
- **预期结果**: HTTP 404 Not Found
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-017: 删除指定版本
- **前置条件**: Agent 有多个版本
- **测试步骤**: DELETE `/api/agent/{id}/versions/{version}`
- **预期结果**:
  - HTTP 200
  - 版本记录被逻辑删除
  - 不影响当前 Agent 状态
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-018: 删除当前使用的版本
- **前置条件**: Agent 当前版本为 v2
- **测试步骤**: DELETE `/api/agent/{id}/versions/2`
- **预期结果**: HTTP 400 Bad Request (不能删除当前版本)
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-019: 版本快照完整性
- **前置条件**: Agent 发布后修改
- **测试步骤**:
  1. 发布 Agent (v1)
  2. 修改 graphJson
  3. 发布 (v2)
  4. 查询版本历史
- **预期结果**:
  - v1 和 v2 的 graphSnapshot 不同
  - 每个版本的快照完整保存
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-020: 并发发布冲突
- **前置条件**: Agent 为 DRAFT 状态
- **测试步骤**:
  1. 两个请求同时发布同一个 Agent
- **预期结果**:
  - 一个成功，一个失败（乐观锁）
  - 数据一致性保持
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 3. 权限与安全 (5个用例)

#### TC-021: 跨用户访问 - 查询
- **前置条件**: Agent 属于用户 A
- **测试步骤**: 用户 B 查询 Agent A 的详情
- **预期结果**: HTTP 403 Forbidden
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-022: 跨用户访问 - 修改
- **前置条件**: Agent 属于用户 A
- **测试步骤**: 用户 B 尝试修改 Agent A
- **预期结果**: HTTP 403 Forbidden
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-023: 跨用户访问 - 删除
- **前置条件**: Agent 属于用户 A
- **测试步骤**: 用户 B 尝试删除 Agent A
- **预期结果**: HTTP 403 Forbidden
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-024: Token 过期
- **前置条件**: 使用过期的 JWT token
- **测试步骤**: GET `/api/agent/list`
- **预期结果**: HTTP 401 Unauthorized
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-025: SQL 注入防护
- **前置条件**: 用户已登录
- **测试步骤**: POST `/api/agent/create` with SQL injection payload in name
- **预期结果**:
  - 参数被正确转义
  - 不执行恶意 SQL
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 4. 数据一致性 (4个用例)

#### TC-026: Agent 与 Version 关联
- **前置条件**: Agent 发布后删除
- **测试步骤**:
  1. 创建并发布 Agent
  2. 强制删除 Agent
  3. 查询版本历史
- **预期结果**:
  - Agent 被逻辑删除
  - 版本记录也被级联删除
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-027: 事务回滚
- **前置条件**: 数据库连接正常
- **测试步骤**:
  1. 模拟发布过程中数据库异常
- **预期结果**:
  - Agent 状态不变
  - 版本记录未创建
  - 事务完全回滚
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-028: 并发更新冲突
- **前置条件**: Agent 存在
- **测试步骤**:
  1. 两个请求同时更新同一个 Agent
- **预期结果**:
  - 一个成功，一个失败（乐观锁）
  - 成功的更新被保存
- **实际结果**: 待测试
- **状态**: ⏳ Pending

#### TC-029: 逻辑删除验证
- **前置条件**: Agent 被删除
- **测试步骤**:
  1. 删除 Agent
  2. 查询 Agent 列表
  3. 直接查询数据库
- **预期结果**:
  - API 不返回已删除的 Agent
  - 数据库中 deleted=1
  - 数据未物理删除
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

## 测试执行计划

### 阶段 1: 环境准备
- [x] 检查 Docker 服务状态
- [ ] 启动后端服务
- [ ] 验证数据库连接
- [ ] 准备测试数据

### 阶段 2: 基础 CRUD 测试
- [ ] 执行 TC-001 ~ TC-010
- [ ] 记录测试结果
- [ ] 截图保存

### 阶段 3: 版本管理测试
- [ ] 执行 TC-011 ~ TC-020
- [ ] 验证版本快照
- [ ] 测试并发场景

### 阶段 4: 安全与权限测试
- [ ] 执行 TC-021 ~ TC-025
- [ ] 验证权限隔离
- [ ] 测试注入防护

### 阶段 5: 数据一致性测试
- [ ] 执行 TC-026 ~ TC-029
- [ ] 验证事务完整性
- [ ] 检查数据库状态

---

## 测试结果汇总

**总用例数**: 29
**通过**: 0
**失败**: 0
**阻塞**: 29 (后端服务未启动)
**待执行**: 29

**通过率**: 0%

### 单元测试结果

**已执行单元测试**:

1. **AgentTest** - ✅ 全部通过
   - 测试数: 7
   - 通过: 7
   - 失败: 0
   - 涵盖: 所有权验证、乐观锁、版本回滚、克隆功能

2. **GraphValidatorTest** - ✅ 全部通过
   - 测试数: 7
   - 通过: 7
   - 失败: 0
   - 涵盖: 工作流图验证逻辑

3. **UserAuthenticationDomainServiceTest** - ❌ 部分失败
   - 测试数: 4
   - 通过: 0
   - 失败: 3
   - 错误: 1
   - 问题: NullPointerException，预期抛出 AuthenticationException

---

## 发现的问题

### 高优先级 (P0)
无

### 中优先级 (P1)

**P1-001: 后端服务无法启动**
- **描述**: Maven 网络连接问题，无法下载 spring-boot-maven-plugin 依赖
- **影响**: 所有集成测试被阻塞
- **状态**: 待修复
- **建议**: 使用 IDE 启动或修复 Maven 网络配置

**P1-002: UserAuthenticationDomainServiceTest 失败**
- **描述**: 用户认证测试抛出 NullPointerException
- **影响**: 用户认证功能可能存在缺陷
- **状态**: 待修复
- **详情**:
  - 文件: `UserAuthenticationDomainServiceTest.java`
  - 预期: AuthenticationException
  - 实际: NullPointerException

### 低优先级 (P2)

**P2-001: 测试代码编译错误（已修复）**
- **描述**:
  - `SchedulerServiceTest.java:130` - 调用不存在的 `resolveInputs()` 方法
  - `UserApplicationServiceTest.java:212,222` - `logout()` 方法参数不匹配
- **状态**: ✅ 已修复
- **修复时间**: 2026-02-10

---

## 测试结论

**状态**: ⏸️ 部分完成（阻塞中）

**已完成**:
- ✅ 修复测试代码编译错误
- ✅ 执行 Agent 单元测试（7/7 通过）
- ✅ 执行图验证测试（7/7 通过）
- ✅ 创建完整测试文档（29个集成测试用例）

**阻塞原因**:
- Maven 网络问题导致后端服务无法启动
- 无法执行需要运行服务的集成测试

**下一步**:
1. 修复 Maven 网络配置或使用 IDE 启动服务
2. 修复 UserAuthenticationDomainServiceTest 失败
3. 执行完整的 Agent 集成测试（29个用例）
4. 根据测试结果更新本报告

**测试覆盖率**:
- 单元测试: 部分完成（Agent 和 Graph 验证通过）
- 集成测试: 未开始（等待服务启动）
- E2E 测试: 未开始（等待服务启动）

---

## 附录

### 测试环境配置

```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/ai_agent
    username: root
    password: root123456

  data:
    redis:
      host: localhost
      port: 6379
```

### 测试脚本示例

```bash
# 创建 Agent
curl -X POST http://localhost:8080/api/agent/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "Test Agent",
    "description": "Test Description",
    "icon": "icon.png"
  }'

# 查询 Agent 列表
curl -X GET http://localhost:8080/api/agent/list \
  -H "Authorization: Bearer ${TOKEN}"

# 发布 Agent
curl -X POST http://localhost:8080/api/agent/publish \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"id": 1}'
```

### 数据库验证 SQL

```sql
-- 查询 Agent 状态
SELECT id, name, status, version, deleted FROM agent_info WHERE id = 1;

-- 查询版本历史
SELECT agent_id, version, create_time FROM agent_version WHERE agent_id = 1 ORDER BY version DESC;

-- 验证逻辑删除
SELECT COUNT(*) FROM agent_info WHERE deleted = 1;
```
