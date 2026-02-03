# 数据库设计规范 (Database Design Standards)

## 核心原则

**在创建任何数据库相关代码前，必须先检查现有 Schema！**

## 强制检查流程

### 1. Schema 检查（必须执行）

在创建新表或修改表结构前，必须：

```bash
# 1. 查看完整的数据库 Schema
readFile: ai-agent-infrastructure/src/main/resources/db/ai_agent.sql

# 2. 搜索相关的表
grepSearch: "CREATE TABLE.*workflow" in ai-agent-infrastructure/src/main/resources/db/

# 3. 搜索相关的 PO 类
grepSearch: "class.*PO" in ai-agent-infrastructure/src/main/java/
```

### 2. 表命名检查

**禁止创建功能重叠的表！**

常见重叠场景：
- ❌ 已有 `workflow_node_execution_log`，禁止创建 `workflow_execution`
- ❌ 已有 `conversation`，禁止创建 `chat_session`
- ❌ 已有 `message`，禁止创建 `chat_message`

**正确做法**：
- ✅ 扩展现有表（添加字段）
- ✅ 建立关联关系（外键）
- ✅ 创建真正独立的新业务表

## 表设计规范

### 1. 命名规范

```sql
-- 表名：小写下划线分隔
CREATE TABLE user_account (...)      -- ✅
CREATE TABLE UserAccount (...)       -- ❌
CREATE TABLE user-account (...)      -- ❌

-- 字段名：小写下划线分隔
user_id BIGINT                       -- ✅
userId BIGINT                        -- ❌
```

### 2. 必需字段

每个表必须包含：

```sql
CREATE TABLE example_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 主键
    
    -- 业务字段
    ...
    
    -- 审计字段（必需）
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 0-未删除 1-已删除',
    
    -- 索引
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例表';
```

### 3. 字段类型选择

| 数据类型 | 使用场景 | 示例 |
|---------|---------|------|
| BIGINT | ID、数值 | `user_id BIGINT` |
| VARCHAR | 短文本 | `name VARCHAR(100)` |
| TEXT | 长文本 | `content TEXT` |
| DATETIME | 时间戳 | `create_time DATETIME` |
| TINYINT | 布尔/枚举 | `status TINYINT` |
| DECIMAL | 金额 | `amount DECIMAL(10,2)` |
| JSON | 复杂对象 | `config JSON` |

### 4. 索引设计

```sql
-- 单列索引
INDEX idx_user_id (user_id)

-- 复合索引（注意顺序）
INDEX idx_user_status (user_id, status, create_time)

-- 唯一索引
UNIQUE INDEX uk_email (email)

-- 全文索引
FULLTEXT INDEX ft_content (content)
```

**索引原则**：
- WHERE 条件字段必须加索引
- 高频查询字段优先
- 复合索引遵循最左前缀原则
- 避免过多索引（影响写入性能）

## 数据库迁移规范

### 1. Flyway 迁移文件

**位置**：`ai-agent-infrastructure/src/main/resources/db/migration/`

**命名格式**：`V{version}__{description}.sql`

```
V1_0__init_schema.sql              # 初始化
V1_1__add_user_table.sql           # 添加用户表
V1_2__add_email_to_user.sql        # 修改用户表
V1_3__create_index_on_user.sql     # 添加索引
```

### 2. 迁移文件内容

```sql
-- V1_4__add_assistant_message_id_to_execution.sql

-- 添加字段
ALTER TABLE workflow_execution 
ADD COLUMN assistant_message_id VARCHAR(64) COMMENT 'Assistant 消息 ID';

-- 添加索引
CREATE INDEX idx_assistant_message_id 
ON workflow_execution(assistant_message_id);

-- 数据迁移（如需要）
UPDATE workflow_execution 
SET assistant_message_id = CONCAT('msg_', id) 
WHERE assistant_message_id IS NULL;
```

### 3. 迁移原则

- ✅ 每个迁移文件只做一件事
- ✅ 迁移文件一旦提交不可修改
- ✅ 使用 ALTER TABLE 而非 DROP + CREATE
- ✅ 考虑数据迁移的性能影响
- ❌ 禁止在迁移中删除数据

## PO 类设计规范

### 1. PO 类与表对应

```java
// ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/user/po/UserPO.java

@Data
@TableName("user_account")  // 对应表名
public class UserPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String email;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic  // 逻辑删除
    private Integer deleted;
}
```

### 2. 字段映射规则

```java
// 数据库字段：user_id (下划线)
// Java 字段：userId (驼峰)
// MyBatis Plus 自动映射

// 特殊映射使用 @TableField
@TableField("user_name")
private String userName;
```

## 常见问题检查清单

### 创建新表前

- [ ] 是否查看了 `ai_agent.sql` 完整 Schema？
- [ ] 是否搜索了相关的表名？
- [ ] 是否确认没有功能重叠的表？
- [ ] 是否考虑了扩展现有表？
- [ ] 是否设计了合理的索引？
- [ ] 是否包含了审计字段？

### 修改表结构前

- [ ] 是否使用 Flyway 迁移文件？
- [ ] 是否考虑了数据兼容性？
- [ ] 是否影响现有查询性能？
- [ ] 是否需要数据迁移脚本？

### 创建 PO 类前

- [ ] 是否搜索了现有的 PO 类？
- [ ] 是否与表结构完全对应？
- [ ] 是否使用了正确的注解？
- [ ] 是否配置了逻辑删除？

## 输出格式

在创建数据库相关代码前，必须输出：

```markdown
📊 **数据库设计检查报告**

🔍 **Schema 检查**：
- [x] 已查看 ai_agent.sql
- [x] 已搜索相关表名
- [x] 已搜索相关 PO 类

📦 **发现的相关表**：
1. workflow_node_execution_log - 工作流节点执行日志
2. execution - 执行记录（Redis 临时存储）

💡 **设计决策**：
- 复用：workflow_node_execution_log（添加 parent_execution_id 字段）
- 新建：无需新建表
- 迁移：创建 V1_5__add_parent_execution_id.sql

✅ **建议方案**：扩展现有表，避免重复

是否继续？
```

## 性能优化建议

### 1. 查询优化

```sql
-- ❌ Bad - 全表扫描
SELECT * FROM message WHERE content LIKE '%关键词%';

-- ✅ Good - 使用索引
SELECT * FROM message WHERE user_id = 123 AND create_time > '2024-01-01';
```

### 2. 批量操作

```java
// ❌ Bad - 循环插入
for (Message msg : messages) {
    messageMapper.insert(msg);
}

// ✅ Good - 批量插入
messageMapper.insertBatch(messages);
```

### 3. 分页查询

```java
// ✅ 使用 MyBatis Plus 分页
Page<UserPO> page = new Page<>(1, 10);
userMapper.selectPage(page, queryWrapper);
```

## 数据安全

### 1. 敏感数据加密

```sql
-- 密码字段
password VARCHAR(255) NOT NULL COMMENT '加密后的密码'

-- API Key 字段
api_key VARCHAR(255) NOT NULL COMMENT '加密后的 API Key'
```

### 2. 软删除

```sql
-- 使用逻辑删除，不物理删除数据
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除 1-已删除'
```

### 3. 审计日志

```sql
-- 记录操作人和操作时间
create_by BIGINT COMMENT '创建人 ID',
update_by BIGINT COMMENT '更新人 ID'
```
