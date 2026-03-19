USE ai_agent;

SET @description_column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'swarm_workspace_agent'
    AND COLUMN_NAME = 'description'
);

SET @description_column_ddl := IF(
  @description_column_exists = 0,
  'ALTER TABLE `swarm_workspace_agent` ADD COLUMN `description` text COMMENT ''子Agent的能力边界和职责描述'' AFTER `role`',
  'SELECT ''swarm_workspace_agent.description already exists'''
);

PREPARE stmt FROM @description_column_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
