# 系统端到端集成测试报告

## 测试概述

**测试日期**: 2026-02-10
**测试人员**: qa-engineer
**测试类型**: 端到端集成测试 (E2E)
**测试目标**: 验证系统各模块协同工作的完整性

## 测试范围

### 1. 用户完整流程
- 用户注册 → 登录 → 创建 Agent → 创建知识库 → 创建工作流 → 执行 → 查看结果

### 2. 跨模块集成
- 工作流调用 Agent
- 工作流检索知识库
- 人工审核流程
- SSE 实时推送

### 3. 并发场景
- 多用户同时操作
- 多工作流并发执行
- 并发知识库检索

### 4. 异常场景
- 网络中断恢复
- 服务重启恢复
- 数据库连接失败
- Redis 连接失败

### 5. 性能验证
- API 响应时间
- 工作流执行效率
- 并发支持能力

---

## 测试用例

### 场景 1: 用户完整业务流程 (E2E-001)

#### 测试步骤

**Step 1: 用户注册**
```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456",
    "username": "TestUser"
  }'
```
- **预期结果**: HTTP 200, 返回 userId
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 2: 用户登录**
```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456"
  }'
```
- **预期结果**: HTTP 200, 返回 JWT token
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 3: 创建 Agent**
```bash
TOKEN="<from_step2>"
curl -X POST http://localhost:8080/api/agent/create \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Service Agent",
    "description": "AI customer service assistant",
    "icon": "service.png"
  }'
```
- **预期结果**: HTTP 200, 返回 agentId
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 4: 配置 Agent 工作流图**
```bash
AGENT_ID="<from_step3>"
curl -X POST http://localhost:8080/api/agent/update \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": '${AGENT_ID}',
    "name": "Customer Service Agent",
    "graphJson": "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"},{\"id\":\"llm1\",\"type\":\"LLM\",\"config\":{\"model\":\"gpt-4\",\"prompt\":\"You are a helpful assistant\"}},{\"id\":\"end\",\"type\":\"END\"}],\"edges\":[{\"source\":\"start\",\"target\":\"llm1\"},{\"source\":\"llm1\",\"target\":\"end\"}]}",
    "version": 1
  }'
```
- **预期结果**: HTTP 200, Agent 更新成功
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 5: 发布 Agent**
```bash
curl -X POST http://localhost:8080/api/agent/publish \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"id": '${AGENT_ID}'}'
```
- **预期结果**: HTTP 200, Agent 状态变为 PUBLISHED
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 6: 创建知识库**
```bash
curl -X POST http://localhost:8080/api/knowledge/dataset \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Product FAQ",
    "description": "Product frequently asked questions"
  }'
```
- **预期结果**: HTTP 200, 返回 datasetId
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 7: 上传知识文档**
```bash
DATASET_ID="<from_step6>"
curl -X POST http://localhost:8080/api/knowledge/document/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@test_faq.txt" \
  -F "datasetId=${DATASET_ID}"
```
- **预期结果**: HTTP 200, 文档上传并向量化
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 8: 创建对话**
```bash
curl -X POST http://localhost:8080/api/chat/conversation \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "title": "Test Conversation"
  }'
```
- **预期结果**: HTTP 200, 返回 conversationId
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 9: 启动工作流执行**
```bash
CONVERSATION_ID="<from_step8>"
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "What is your return policy?"
  }'
```
- **预期结果**: HTTP 200, 返回 executionId
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 10: 监听 SSE 流式输出**
```bash
EXECUTION_ID="<from_step9>"
curl -N http://localhost:8080/api/workflow/execution/${EXECUTION_ID}/stream \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - 接收到 SSE 事件流
  - 包含节点执行状态更新
  - 包含 LLM 流式输出
  - 最终收到完成事件
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 11: 查询执行结果**
```bash
curl -X GET http://localhost:8080/api/workflow/execution/${EXECUTION_ID} \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - HTTP 200
  - status = SUCCEEDED
  - 包含完整的执行历史
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 12: 查询对话历史**
```bash
curl -X GET http://localhost:8080/api/chat/conversation/${CONVERSATION_ID}/messages \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - HTTP 200
  - 包含用户消息和 AI 回复
  - 消息顺序正确
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 场景 2: 工作流条件分支 (E2E-002)

#### 测试目标
验证条件节点的分支选择和未选分支的剪枝逻辑

#### 测试步骤

**Step 1: 创建带条件分支的 Agent**
```json
{
  "nodes": [
    {"id": "start", "type": "START"},
    {"id": "condition1", "type": "CONDITION", "config": {
      "mode": "EXPRESSION",
      "branches": [
        {
          "name": "VIP客户",
          "conditionGroups": [{
            "logicalOperator": "AND",
            "conditions": [{
              "variable": "{{userLevel}}",
              "operator": "EQUALS",
              "value": "VIP"
            }]
          }]
        },
        {
          "name": "普通客户",
          "conditionGroups": [{
            "logicalOperator": "AND",
            "conditions": [{
              "variable": "{{userLevel}}",
              "operator": "EQUALS",
              "value": "NORMAL"
            }]
          }]
        }
      ]
    }},
    {"id": "vip_service", "type": "LLM", "config": {"prompt": "VIP专属服务"}},
    {"id": "normal_service", "type": "LLM", "config": {"prompt": "标准服务"}},
    {"id": "end", "type": "END"}
  ],
  "edges": [
    {"source": "start", "target": "condition1"},
    {"source": "condition1", "target": "vip_service", "sourceHandle": "VIP客户"},
    {"source": "condition1", "target": "normal_service", "sourceHandle": "普通客户"},
    {"source": "vip_service", "target": "end"},
    {"source": "normal_service", "target": "end"}
  ]
}
```

**Step 2: 执行工作流 - VIP 分支**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "I need help",
    "variables": {"userLevel": "VIP"}
  }'
```
- **预期结果**:
  - condition1 选择 "VIP客户" 分支
  - vip_service 节点执行
  - normal_service 节点被 SKIPPED
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 3: 执行工作流 - 普通分支**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "I need help",
    "variables": {"userLevel": "NORMAL"}
  }'
```
- **预期结果**:
  - condition1 选择 "普通客户" 分支
  - normal_service 节点执行
  - vip_service 节点被 SKIPPED
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 场景 3: 知识库检索集成 (E2E-003)

#### 测试目标
验证工作流中调用知识库检索的完整流程

#### 测试步骤

**Step 1: 准备知识库数据**
- 上传多个文档到知识库
- 等待向量化完成

**Step 2: 创建带检索节点的工作流**
```json
{
  "nodes": [
    {"id": "start", "type": "START"},
    {"id": "retrieval", "type": "TOOL", "config": {
      "toolType": "KNOWLEDGE_RETRIEVAL",
      "datasetId": "${DATASET_ID}",
      "topK": 3
    }},
    {"id": "llm", "type": "LLM", "config": {
      "prompt": "Based on the following context: {{retrieval.result}}, answer: {{userMessage}}"
    }},
    {"id": "end", "type": "END"}
  ],
  "edges": [
    {"source": "start", "target": "retrieval"},
    {"source": "retrieval", "target": "llm"},
    {"source": "llm", "target": "end"}
  ]
}
```

**Step 3: 执行工作流**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "What is the warranty period?"
  }'
```
- **预期结果**:
  - retrieval 节点成功检索相关文档
  - LLM 节点基于检索结果生成回答
  - 回答内容与知识库一致
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 场景 4: 人工审核流程 (E2E-004)

#### 测试目标
验证工作流暂停、人工审核、恢复的完整流程

#### 测试步骤

**Step 1: 创建带审核节点的工作流**
```json
{
  "nodes": [
    {"id": "start", "type": "START"},
    {"id": "llm1", "type": "LLM", "config": {"prompt": "Generate draft response"}},
    {"id": "review", "type": "HUMAN_REVIEW", "config": {
      "reviewPrompt": "Please review the draft response"
    }},
    {"id": "llm2", "type": "LLM", "config": {"prompt": "Finalize response"}},
    {"id": "end", "type": "END"}
  ],
  "edges": [
    {"source": "start", "target": "llm1"},
    {"source": "llm1", "target": "review"},
    {"source": "review", "target": "llm2"},
    {"source": "llm2", "target": "end"}
  ]
}
```

**Step 2: 启动工作流**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "Test message"
  }'
```
- **预期结果**:
  - llm1 执行完成
  - 工作流进入 PAUSED_FOR_REVIEW 状态
  - 审核任务进入队列
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 3: 查询待审核任务**
```bash
curl -X GET http://localhost:8080/api/workflow/reviews/pending \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - HTTP 200
  - 返回待审核任务列表
  - 包含 executionId 和审核内容
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 4: 提交审核结果**
```bash
REVIEW_ID="<from_step3>"
curl -X POST http://localhost:8080/api/workflow/reviews/${REVIEW_ID}/submit \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "feedback": "Looks good"
  }'
```
- **预期结果**:
  - HTTP 200
  - 工作流自动恢复执行
  - llm2 节点开始执行
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 5: 验证工作流完成**
```bash
curl -X GET http://localhost:8080/api/workflow/execution/${EXECUTION_ID} \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - status = SUCCEEDED
  - 所有节点状态正确
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 场景 5: 并发执行 (E2E-005)

#### 测试目标
验证系统支持多用户、多工作流并发执行

#### 测试步骤

**Step 1: 创建多个用户**
- 注册 10 个测试用户
- 每个用户登录获取 token

**Step 2: 每个用户创建 Agent 和工作流**
- 并发创建 10 个 Agent
- 每个 Agent 配置相同的工作流图

**Step 3: 并发启动工作流**
```bash
# 使用 GNU parallel 或脚本并发执行
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/workflow/execution/start \
    -H "Authorization: Bearer ${TOKEN[$i]}" \
    -H "Content-Type: application/json" \
    -d '{
      "agentId": '${AGENT_ID[$i]}',
      "conversationId": '${CONVERSATION_ID[$i]}',
      "userMessage": "Test concurrent execution"
    }' &
done
wait
```
- **预期结果**:
  - 所有请求成功返回
  - 10 个工作流并发执行
  - 无数据冲突
  - 无死锁
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 4: 验证执行结果**
- 查询每个工作流的执行状态
- 验证所有工作流都成功完成
- 检查 Redis 和数据库数据一致性

---

### 场景 6: 异常恢复 (E2E-006)

#### 测试目标
验证系统在异常情况下的恢复能力

#### 测试场景

**6.1: 工作流执行中重启服务**

**Step 1: 启动长时间工作流**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "Test message"
  }'
```

**Step 2: 工作流执行到一半时重启后端服务**
```bash
# 杀掉后端进程
pkill -f "spring-boot:run"

# 重新启动
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local &
```

**Step 3: 服务恢复后查询工作流状态**
```bash
curl -X GET http://localhost:8080/api/workflow/execution/${EXECUTION_ID} \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**:
  - 工作流状态保持一致（从 Redis checkpoint 恢复）
  - 可以继续执行或重新启动
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**6.2: Redis 连接中断**

**Step 1: 停止 Redis**
```bash
docker stop ai-agent-redis
```

**Step 2: 尝试启动工作流**
```bash
curl -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": '${AGENT_ID}',
    "conversationId": '${CONVERSATION_ID}',
    "userMessage": "Test message"
  }'
```
- **预期结果**: HTTP 500, 返回友好错误信息
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 3: 恢复 Redis**
```bash
docker start ai-agent-redis
```

**Step 4: 重试启动工作流**
- **预期结果**: HTTP 200, 工作流正常启动
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**6.3: 数据库连接中断**

**Step 1: 停止 MySQL**
```bash
docker stop ai-agent-mysql
```

**Step 2: 尝试查询 Agent 列表**
```bash
curl -X GET http://localhost:8080/api/agent/list \
  -H "Authorization: Bearer ${TOKEN}"
```
- **预期结果**: HTTP 500, 返回友好错误信息
- **实际结果**: 待测试
- **状态**: ⏳ Pending

**Step 3: 恢复 MySQL**
```bash
docker start ai-agent-mysql
```

**Step 4: 重试查询**
- **预期结果**: HTTP 200, 正常返回数据
- **实际结果**: 待测试
- **状态**: ⏳ Pending

---

### 场景 7: 性能验证 (E2E-007)

#### 测试目标
验证系统在正常负载下的性能表现

#### 测试指标

**7.1: API 响应时间**

| API 端点 | 预期响应时间 | 实际响应时间 | 状态 |
|---------|------------|------------|------|
| POST /api/user/login | < 200ms | 待测试 | ⏳ |
| GET /api/agent/list | < 300ms | 待测试 | ⏳ |
| POST /api/agent/create | < 500ms | 待测试 | ⏳ |
| POST /api/workflow/execution/start | < 1s | 待测试 | ⏳ |
| GET /api/workflow/execution/{id} | < 300ms | 待测试 | ⏳ |

**7.2: 工作流执行效率**

| 工作流类型 | 节点数 | 预期执行时间 | 实际执行时间 | 状态 |
|----------|-------|------------|------------|------|
| 简单流程 (START→LLM→END) | 3 | < 5s | 待测试 | ⏳ |
| 条件分支 | 5 | < 8s | 待测试 | ⏳ |
| 知识库检索 | 4 | < 10s | 待测试 | ⏳ |
| 复杂流程 (10+ 节点) | 10+ | < 30s | 待测试 | ⏳ |

**7.3: 并发支持能力**

| 并发用户数 | 成功率 | 平均响应时间 | P95 响应时间 | 状态 |
|----------|-------|------------|------------|------|
| 10 | > 99% | < 2s | < 5s | ⏳ |
| 50 | > 95% | < 5s | < 10s | ⏳ |
| 100 | > 90% | < 10s | < 20s | ⏳ |

---

## 测试执行计划

### 阶段 1: 环境准备
- [x] 检查 Docker 服务
- [ ] 启动后端服务
- [ ] 准备测试数据
- [ ] 准备测试脚本

### 阶段 2: 基础流程测试
- [ ] 执行 E2E-001 (用户完整流程)
- [ ] 验证各模块协同工作
- [ ] 记录测试结果

### 阶段 3: 高级功能测试
- [ ] 执行 E2E-002 (条件分支)
- [ ] 执行 E2E-003 (知识库检索)
- [ ] 执行 E2E-004 (人工审核)

### 阶段 4: 并发与异常测试
- [ ] 执行 E2E-005 (并发执行)
- [ ] 执行 E2E-006 (异常恢复)

### 阶段 5: 性能测试
- [ ] 执行 E2E-007 (性能验证)
- [ ] 收集性能指标
- [ ] 生成性能报告

---

## 测试结果汇总

**总场景数**: 7
**通过**: 0
**失败**: 0
**阻塞**: 0
**待执行**: 7

**通过率**: 0%

---

## 发现的问题

### 高优先级 (P0)
无

### 中优先级 (P1)
无

### 低优先级 (P2)
无

---

## 性能分析

### 响应时间分布
待测试

### 资源使用情况
待测试

### 瓶颈分析
待测试

---

## 测试结论

**状态**: 🔄 进行中

**下一步**:
1. 等待后端服务启动
2. 执行基础流程测试
3. 逐步完成所有测试场景
4. 根据测试结果更新本报告

---

## 附录

### 测试环境信息

```yaml
Backend:
  Version: 1.0.0-SNAPSHOT
  Java: 21
  Spring Boot: 3.4.9
  Profile: local

Database:
  MySQL: 8.0
  Port: 13306
  Database: ai_agent

Cache:
  Redis: 7.x
  Port: 6379

Vector Store:
  Milvus: 2.x
  Port: 19530

Object Storage:
  MinIO: latest
  API Port: 9000
  Console Port: 9001
```

### 测试工具

- **API 测试**: curl, Postman
- **并发测试**: GNU parallel, Apache JMeter
- **性能监控**: Prometheus, Grafana
- **日志分析**: Kibana, Loki

### 测试数据

```sql
-- 测试用户
INSERT INTO user_account (email, username, password_hash) VALUES
('test1@example.com', 'TestUser1', '$2a$10$...'),
('test2@example.com', 'TestUser2', '$2a$10$...');

-- 测试 Agent
INSERT INTO agent_info (user_id, name, description, status, version) VALUES
(1, 'Test Agent 1', 'Test Description', 'PUBLISHED', 1),
(2, 'Test Agent 2', 'Test Description', 'DRAFT', 1);

-- 测试知识库
INSERT INTO knowledge_dataset (user_id, name, description) VALUES
(1, 'Test Dataset', 'Test Description');
```

### 自动化测试脚本

```bash
#!/bin/bash
# e2e-test.sh

# 配置
BASE_URL="http://localhost:8080"
TEST_EMAIL="test@example.com"
TEST_PASSWORD="Test123456"

# 1. 注册用户
echo "Step 1: Register user..."
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/user/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\",\"username\":\"TestUser\"}")
echo "Register response: ${REGISTER_RESPONSE}"

# 2. 登录
echo "Step 2: Login..."
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/user/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\"}")
TOKEN=$(echo ${LOGIN_RESPONSE} | jq -r '.data.token')
echo "Token: ${TOKEN}"

# 3. 创建 Agent
echo "Step 3: Create agent..."
AGENT_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/agent/create \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Agent","description":"Test Description","icon":"icon.png"}')
AGENT_ID=$(echo ${AGENT_RESPONSE} | jq -r '.data')
echo "Agent ID: ${AGENT_ID}"

# ... 继续其他步骤
```
