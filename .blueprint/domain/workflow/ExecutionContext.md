# ExecutionContext Blueprint

## 职责契约
- **做什么**: 作为工作流执行的"智能黑板"，承载全部上下文信息——基础数据、长期记忆(LTM)、短期记忆(STM)、环境感知(Awareness)；提供 SpEL 表达式解析和参数注入能力
- **不做什么**: 不负责记忆的检索和加载（那是 SchedulerService.hydrateMemory 的职责）；不负责持久化（通过 Checkpoint 快照实现）

## 数据结构

### 基础数据
| 字段 | 类型 | 说明 | 线程安全 |
|------|------|------|---------|
| inputs | ConcurrentHashMap\<String, Object\> | 全局输入参数（用户输入） | ✅ |
| nodeOutputs | ConcurrentHashMap\<String, Map\<String, Object\>\> | 节点输出 (nodeId → outputs) | ✅ |
| sharedState | ConcurrentHashMap\<String, Object\> | 共享状态数据（跨节点传递） | ✅ |

### 长期记忆 (LTM)
| 字段 | 类型 | 说明 |
|------|------|------|
| longTermMemories | List\<String\> | 启动时从 Milvus 向量库检索的系统级知识，所有节点共享 |

### 短期记忆 (STM)
| 字段 | 类型 | 说明 |
|------|------|------|
| chatHistory | List\<Map\<String, String\>\> | 会话历史消息 (role: USER/ASSISTANT, content)，启动时从 MySQL 加载 |

### 环境感知 (Awareness)
| 字段 | 类型 | 说明 |
|------|------|------|
| executionLog | StringBuilder | 动态执行日志，记录 "[nodeId-nodeName]: summary"，让 LLM 知道当前进度 |

## 接口摘要

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| setInputs | Map\<String, Object\> | void | 设置全局输入（创建新 ConcurrentHashMap） |
| setNodeOutput | nodeId, Map outputs | void | 存储节点输出 |
| getNodeOutput | nodeId | Map\<String, Object\> | 获取节点输出（不存在返回空 Map） |
| appendLog | nodeId, nodeName, summary | void | 追加执行日志 |
| getExecutionLogContent | - | String | 获取完整执行日志 |
| snapshot | - | ExecutionContext | 创建深拷贝快照（用于检查点） |

## 表达式解析（已移除）
- **重构说明**: SpEL 表达式解析逻辑已从 ExecutionContext 移除，迁移到 infrastructure 层的 ExpressionResolverPort 实现
- **原因**: 保持 domain 层纯净，避免依赖 Spring Expression Language
- **新位置**: `StructuredConditionEvaluator` 和其他需要表达式解析的 infrastructure 组件

## 依赖拓扑
- **上游**: Execution (持有), SchedulerService (水合记忆), NodeExecutorStrategy (读取/写入)
- **下游**: 无（纯值对象）

## 设计约束
- 所有 Map 字段使用 ConcurrentHashMap 保证并发安全
- snapshot() 创建深拷贝，用于 Checkpoint 持久化
- LTM 在 `hydrateMemory` 阶段一次性加载，执行过程中只读
- STM 在启动时加载，LLM 节点按需拼接到 Message Chain
- Awareness 日志在每个节点完成后追加，供后续 LLM 节点参考执行进度
- **纯值对象**: 无框架依赖，不包含业务逻辑，仅作为数据容器

## 变更日志
- [2026-02-10] **架构重构**: 移除 SpEL 依赖，删除 resolve() 和 resolveInputs() 方法，保持 domain 层纯净
- [2026-02-09] resolve() 增强异常处理：SpEL 解析失败时返回原始表达式并记录 WARN 日志
- [2026-02-08] 新建蓝图，记录智能黑板的完整结构和 LTM/STM/Awareness 三层记忆模型
