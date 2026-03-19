-- ============================================================
-- 为 writing_task 增加 task_uuid 业务唯一标识
-- 用途：
-- 1. 让 AI/前端/日志只依赖一个稳定标识回填任务结果
-- 2. 避免 sessionId / writingAgentId / swarmAgentId 混用导致回写失败
-- 说明：该脚本设计为幂等，可重复执行
-- ============================================================

USE ai_agent;

SET NAMES utf8mb4;

SET @has_task_uuid_column := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'writing_task'
    AND COLUMN_NAME = 'task_uuid'
);

SET @add_column_sql := IF(
  @has_task_uuid_column = 0,
  'ALTER TABLE `writing_task` ADD COLUMN `task_uuid` varchar(64) DEFAULT NULL COMMENT ''写作任务业务唯一标识，供AI/前端/日志使用'' AFTER `id`',
  'SELECT 1'
);
PREPARE stmt_add_column FROM @add_column_sql;
EXECUTE stmt_add_column;
DEALLOCATE PREPARE stmt_add_column;

UPDATE `writing_task`
SET `task_uuid` = CONCAT('wtask_', REPLACE(UUID(), '-', ''))
WHERE `task_uuid` IS NULL OR `task_uuid` = '';

ALTER TABLE `writing_task`
  MODIFY COLUMN `task_uuid` varchar(64) NOT NULL COMMENT '写作任务业务唯一标识，供AI/前端/日志使用';

SET @has_task_uuid_index := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'writing_task'
    AND INDEX_NAME = 'uk_task_uuid'
);

SET @add_index_sql := IF(
  @has_task_uuid_index = 0,
  'ALTER TABLE `writing_task` ADD UNIQUE KEY `uk_task_uuid` (`task_uuid`)',
  'SELECT 1'
);
PREPARE stmt_add_index FROM @add_index_sql;
EXECUTE stmt_add_index;
DEALLOCATE PREPARE stmt_add_index;
