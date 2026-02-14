## Metadata
- file: `.blueprint/infrastructure/persistence/MySQLRepositories.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: MySQLRepositories
- 该文件描述 MySQL 持久化仓储实现的职责边界，实现领域层定义的 Repository 接口，负责聚合根的持久化、查询、版本管理。使用 MyBatis Plus 进行数据库操作，处理 PO 与领域对象的转换，支持乐观锁和事务管理。

## 2) 核心方法
- `save()`
- `findById()`
- `findByUserIdAndAgentId()`
- `saveVersion()`
- `saveReviewRecord()`

## 3) 具体方法
### 3.1 save()
- 函数签名: `void save(Agent agent)` (AgentRepositoryImpl)
- 入参: `agent` 聚合根对象（包含 id、name、description、graphJson、version 等）
- 出参: 无（副作用：更新数据库，回写 id 和 version 到 agent 对象）
- 功能含义: 保存或更新智能体聚合根，新建时插入并回写 ID，更新时使用乐观锁（version 字段），失败抛出 OptimisticLockingFailureException
- 链路作用: AgentApplicationService → Agent.updateDraft() → AgentRepository.save() → AgentMapper.insert/updateById() → MySQL agent_info 表

### 3.2 findById()
- 函数签名: `Optional<Agent> findById(Long id)` (AgentRepositoryImpl)
- 入参: `id` 智能体主键
- 出参: `Optional<Agent>` 包含完整聚合根对象（包括 graphJson、modelConfig），不存在返回 empty
- 功能含义: 根据主键查询智能体聚合根，从 AgentPO 转换为领域对象，包含 JSON 字段反序列化
- 链路作用: AgentApplicationService.getAgentDetail() → AgentRepository.findById() → AgentMapper.selectById() → toDomain() 转换

### 3.3 findByUserIdAndAgentId()
- 函数签名: `Optional<Conversation> findByUserIdAndAgentId(Long userId, Long agentId)` (ConversationRepositoryImpl)
- 入参: `userId` 用户 ID，`agentId` 智能体 ID
- 出参: `Optional<Conversation>` 会话聚合根（包含 messages 列表），不存在返回 empty
- 功能含义: 查询用户与智能体的会话记录，用于加载 STM（短期记忆），包含关联的 Message 列表
- 链路作用: SchedulerService.hydrateMemory() → ConversationRepository.findByUserIdAndAgentId() → ConversationMapper.selectOne() + MessageMapper.selectList()

### 3.4 saveVersion()
- 函数签名: `void saveVersion(AgentVersion version)` (AgentRepositoryImpl)
- 入参: `version` 版本对象（包含 agentId、version、graphJson、publishTime）
- 出参: 无（副作用：插入版本记录，回写 id）
- 功能含义: 保存智能体版本快照，用于版本历史和回滚，插入 agent_version 表
- 链路作用: Agent.publish() → AgentRepository.saveVersion() → AgentVersionMapper.insert() → MySQL agent_version 表

### 3.5 saveReviewRecord()
- 函数签名: `void saveReviewRecord(HumanReview review)` (HumanReviewRepositoryImpl)
- 入参: `review` 人工审核记录（包含 executionId、nodeId、status、reviewerId、comment）
- 出参: 无（副作用：插入审核记录）
- 功能含义: 保存人工审核结果，记录审核状态（PENDING/APPROVED/REJECTED）和审核意见
- 链路作用: HumanReviewApplicationService.submitReview() → HumanReviewRepository.saveReviewRecord() → HumanReviewMapper.insert() → MySQL human_review 表


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 AgentRepositoryImpl 和相关仓储实现。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
