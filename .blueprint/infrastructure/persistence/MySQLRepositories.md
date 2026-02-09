# MySQL Repositories Blueprint

## 职责契约
- **做什么**: 实现领域层定义的 Repository 接口，通过 MyBatis Plus 操作 MySQL 数据库
- **不做什么**: 不包含业务逻辑；不直接被 Controller 调用

## 实现清单

| Repository 接口 (Domain) | 实现类 (Infrastructure) | 对应表 |
|--------------------------|------------------------|--------|
| AgentRepository | (MyBatis实现) | agent_info, agent_version |
| ConversationRepository | MybatisConversationRepository | conversations |
| (Message持久化) | (集成在Conversation实现中) | messages |
| KnowledgeDatasetRepository | MySQLKnowledgeDatasetRepository | knowledge_dataset |
| KnowledgeDocumentRepository | MySQLKnowledgeDocumentRepository | knowledge_document |
| HumanReviewRepository | HumanReviewRepositoryImpl | workflow_human_review_record |
| WorkflowNodeExecutionLogRepository | (MyBatis实现) | workflow_node_execution_log |
| IUserRepository | (MyBatis实现) | user_info |

## PO 与 Entity 映射规则
- PO 类使用 `@TableName` 注解映射表名
- 字段自动驼峰转下划线映射
- 逻辑删除使用 `@TableLogic` 注解
- 审计字段使用 `@TableField(fill = FieldFill.INSERT)` 自动填充

## 依赖拓扑
- **上游**: 领域层 Repository 接口（通过 Spring DI 注入）
- **下游**: MyBatis Plus Mapper 接口, MySQL 数据库

## 设计约束
- 所有 Repository 实现必须在 infrastructure 层
- PO 与 Entity 的转换在 Repository 实现中完成
- 使用 MyBatis Plus 的 LambdaQueryWrapper 构建查询
- 批量操作使用 `insertBatch` / `updateBatch`

## 变更日志
- [初始] 从现有代码逆向生成蓝图
