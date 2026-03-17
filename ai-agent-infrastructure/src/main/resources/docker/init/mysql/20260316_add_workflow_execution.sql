USE ai_agent;

CREATE TABLE IF NOT EXISTS `workflow_execution` (
  `execution_id` varchar(36) NOT NULL COMMENT '执行ID (UUID)',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(36) DEFAULT NULL COMMENT '关联的会话ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态: RUNNING, COMPLETED, FAILED, CANCELLED',
  `mode` varchar(20) NOT NULL DEFAULT 'STANDARD' COMMENT '执行模式: STANDARD, DEBUG, DRY_RUN',
  `input_data` json DEFAULT NULL COMMENT '输入数据',
  `output_data` json DEFAULT NULL COMMENT '输出数据',
  `error_message` text COMMENT '错误信息',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '执行时长(毫秒)',
  PRIMARY KEY (`execution_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_status` (`status`),
  KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流执行主表';
