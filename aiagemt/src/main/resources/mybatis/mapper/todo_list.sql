CREATE TABLE IF NOT EXISTS `todo_list` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '对话ID',
  `task_content` text NOT NULL COMMENT '任务内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态(0:未完成, 1:进行中, 2:已完成)',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级(1:低, 2:中, 3:高)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';