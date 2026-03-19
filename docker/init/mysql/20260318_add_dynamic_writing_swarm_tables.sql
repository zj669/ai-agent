-- ============================================================
-- 为已有数据库补齐动态写作协作相关表
-- 用途：
-- 1. 老库升级时补齐 writing_* 表
-- 2. 与 01_init_schema.sql 保持一致，避免首次初始化和增量升级结构漂移
-- 说明：该脚本是幂等的，可重复执行
-- ============================================================

USE ai_agent;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `writing_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '写作会话ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '关联的swarm workspace ID',
  `root_agent_id` bigint(20) NOT NULL COMMENT '主Agent ID（指向swarm_workspace_agent.id）',
  `human_agent_id` bigint(20) NOT NULL COMMENT '人类Agent ID（指向swarm_workspace_agent.id）',
  `default_group_id` bigint(20) DEFAULT NULL COMMENT '主对话群ID（指向swarm_group.id）',
  `title` varchar(200) DEFAULT NULL COMMENT '本次写作任务标题',
  `goal` text COMMENT '写作目标描述',
  `constraints_json` json DEFAULT NULL COMMENT '写作约束（JSON对象/数组）',
  `status` varchar(20) DEFAULT 'PLANNING' COMMENT '会话状态: PLANNING/RUNNING/DRAFTING/REVIEWING/COMPLETED/FAILED',
  `current_draft_id` bigint(20) DEFAULT NULL COMMENT '当前草稿ID（指向writing_draft.id）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_root_agent_id` (`root_agent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作会话表';

CREATE TABLE IF NOT EXISTS `writing_agent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '写作Agent记录ID',
  `session_id` bigint(20) NOT NULL COMMENT '写作会话ID',
  `swarm_agent_id` bigint(20) NOT NULL COMMENT '关联的swarm agent ID（指向swarm_workspace_agent.id）',
  `role` varchar(100) NOT NULL COMMENT '写作角色名称',
  `description` text COMMENT '能力边界和职责描述',
  `skill_tags_json` json DEFAULT NULL COMMENT '技能标签列表（JSON数组）',
  `status` varchar(20) DEFAULT 'IDLE' COMMENT '状态: IDLE/ASSIGNED/RUNNING/DONE/FAILED',
  `sort_order` int(11) DEFAULT 0 COMMENT '展示排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_swarm_agent` (`session_id`, `swarm_agent_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作Agent表';

CREATE TABLE IF NOT EXISTS `writing_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '写作任务ID',
  `session_id` bigint(20) NOT NULL COMMENT '写作会话ID',
  `writing_agent_id` bigint(20) NOT NULL COMMENT '分配的写作Agent ID',
  `swarm_agent_id` bigint(20) NOT NULL COMMENT '分配的swarm agent ID（指向swarm_workspace_agent.id）',
  `task_type` varchar(50) DEFAULT 'WRITING' COMMENT '任务类型: OUTLINE/CHARACTER/WORLD/CHAPTER/REVISION/REVIEW/WRITING',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `instruction` text COMMENT '任务说明',
  `input_payload_json` json DEFAULT NULL COMMENT '任务输入载荷',
  `expected_output_schema_json` json DEFAULT NULL COMMENT '期望输出结构',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '任务状态: PENDING/RUNNING/DONE/FAILED/CANCELLED',
  `priority` int(11) DEFAULT 0 COMMENT '优先级',
  `created_by_swarm_agent_id` bigint(20) DEFAULT NULL COMMENT '创建任务的swarm agent ID',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_status` (`session_id`, `status`),
  KEY `idx_writing_agent_status` (`writing_agent_id`, `status`),
  KEY `idx_swarm_agent_id` (`swarm_agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作任务表';

CREATE TABLE IF NOT EXISTS `writing_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '写作结果ID',
  `session_id` bigint(20) NOT NULL COMMENT '写作会话ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联的任务ID',
  `writing_agent_id` bigint(20) NOT NULL COMMENT '写作Agent ID',
  `swarm_agent_id` bigint(20) NOT NULL COMMENT 'swarm agent ID（指向swarm_workspace_agent.id）',
  `result_type` varchar(50) DEFAULT 'TEXT' COMMENT '结果类型: PLAN/OUTLINE/CHARACTER_PROFILE/SCENE_DRAFT/REVIEW_NOTE/TEXT',
  `summary` varchar(500) DEFAULT NULL COMMENT '结果摘要',
  `content` longtext COMMENT '结果正文',
  `structured_payload_json` json DEFAULT NULL COMMENT '结构化结果内容',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_writing_agent_id` (`writing_agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作结果表';

CREATE TABLE IF NOT EXISTS `writing_draft` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '草稿ID',
  `session_id` bigint(20) NOT NULL COMMENT '写作会话ID',
  `version_no` int(11) NOT NULL COMMENT '草稿版本号',
  `title` varchar(200) DEFAULT NULL COMMENT '草稿标题',
  `content` longtext COMMENT '草稿正文',
  `source_result_ids_json` json DEFAULT NULL COMMENT '来源结果ID列表',
  `status` varchar(20) DEFAULT 'DRAFT' COMMENT '草稿状态: DRAFT/FINAL/ARCHIVED',
  `created_by_swarm_agent_id` bigint(20) DEFAULT NULL COMMENT '创建草稿的swarm agent ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_version` (`session_id`, `version_no`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作草稿表';
