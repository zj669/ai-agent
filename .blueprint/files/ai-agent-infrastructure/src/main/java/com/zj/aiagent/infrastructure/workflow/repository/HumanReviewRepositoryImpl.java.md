## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/HumanReviewRepositoryImpl.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/HumanReviewRepositoryImpl.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HumanReviewRepositoryImpl
- MyBatis-Plus 实现 `HumanReviewRepository`，承担人工审核记录的持久化与分页查询。
- 负责 Domain `HumanReviewRecord` 与 PO `HumanReviewPO` 的双向转换。

## 2) 核心方法
- `save(HumanReviewRecord record)`
- `findByExecutionId(String executionId)`
- `findReviewHistory(Long userId, Pageable pageable)`
- `toPO(HumanReviewRecord entity)`
- `toEntity(HumanReviewPO po)`

## 3) 具体方法
### 3.1 save(...)
- 函数签名: `public void save(HumanReviewRecord record)`
- 入参: 审核记录领域对象
- 出参: 无（会回填 record.id）
- 功能含义: 转 PO 后 insert，并将数据库自增 ID 写回领域对象。
- 链路作用: 人工审核提交后持久化主入口。

### 3.2 findReviewHistory(...)
- 函数签名: `public Page<HumanReviewRecord> findReviewHistory(Long userId, Pageable pageable)`
- 入参: 审核人ID、分页参数
- 出参: Spring Page 结果
- 功能含义: 使用 MyBatis Page 查询并映射为领域对象分页。
- 链路作用: 审核历史页面/API 的数据来源。

## 4) 变更记录
- 2026-02-15: 回填人工审核仓储蓝图语义，补齐分页链路说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
