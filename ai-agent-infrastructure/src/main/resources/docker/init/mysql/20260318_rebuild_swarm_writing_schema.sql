-- ============================================================
-- AI Agent Platform - Swarm/Writing Schema 重建脚本
-- 方案A：直接删除旧的 swarm_* / writing_* 表，并按新设计同名重建
-- 注意：该脚本会清空上述表中的全部历史数据
-- ============================================================

USE ai_agent;

SET NAMES utf8mb4;

-- ============================================================
-- 1. 按依赖顺序删除旧表
-- ============================================================

DROP TABLE IF EXISTS `writing_draft`;
DROP TABLE IF EXISTS `writing_result`;
DROP TABLE IF EXISTS `writing_task`;
DROP TABLE IF EXISTS `writing_agent`;
DROP TABLE IF EXISTS `writing_session`;

DROP TABLE IF EXISTS `swarm_group_member`;
DROP TABLE IF EXISTS `swarm_message`;
DROP TABLE IF EXISTS `swarm_group`;
DROP TABLE IF EXISTS `swarm_workspace_agent`;
DROP TABLE IF EXISTS `swarm_workspace`;

-- ============================================================
-- 2. 重建 swarm 运行时表
-- ============================================================

CREATE TABLE `swarm_workspace` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工作空间ID',
  `name` varchar(100) NOT NULL COMMENT '工作空间名称',
  `user_id` bigint(20) NOT NULL COMMENT '创建者用户ID',
  `default_model` varchar(100) DEFAULT NULL COMMENT '默认模型（MVP从yml读，后续支持覆盖）',
  `llm_config_id` bigint(20) DEFAULT NULL COMMENT '关联的LLM配置ID',
  `max_rounds_per_turn` int(11) DEFAULT 10 COMMENT 'Agent单次唤醒最大LLM调用轮次（死循环防护）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群工作空间表';

CREATE TABLE `swarm_workspace_agent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `agent_id` bigint(20) DEFAULT NULL COMMENT '关联的agent_info ID（人类节点为空）',
  `role` varchar(50) NOT NULL COMMENT '角色: human/assistant/自定义角色',
  `description` text COMMENT '子Agent的能力边界和职责描述',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父Agent ID（谁创建的，指向本表id）',
  `llm_history` longtext COMMENT '该Agent在此workspace的LLM对话历史',
  `status` varchar(20) DEFAULT 'IDLE' COMMENT 'Agent状态: IDLE/BUSY/WAKING/STOPPED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群工作空间Agent关联表';

CREATE TABLE `swarm_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `name` varchar(100) DEFAULT NULL COMMENT '群组名称（P2P群可为空）',
  `context_tokens` int(11) DEFAULT 0 COMMENT '上下文token数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群IM群组表';

CREATE TABLE `swarm_group_member` (
  `group_id` bigint(20) NOT NULL COMMENT '群组ID',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID（指向swarm_workspace_agent.id）',
  `last_read_message_id` bigint(20) DEFAULT 0 COMMENT '最后已读消息ID',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`group_id`, `agent_id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群群成员表';

CREATE TABLE `swarm_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `group_id` bigint(20) NOT NULL COMMENT '群组ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID（指向swarm_workspace_agent.id）',
  `content_type` varchar(20) DEFAULT 'text' COMMENT '内容类型: text等',
  `content` longtext COMMENT '消息内容',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群IM消息表';

-- ============================================================
-- 3. 重建 writing 业务表
-- ============================================================

CREATE TABLE `writing_session` (
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

CREATE TABLE `writing_agent` (
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

CREATE TABLE `writing_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '写作任务ID',
  `task_uuid` varchar(64) NOT NULL COMMENT '写作任务业务唯一标识，供AI/前端/日志使用',
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
  UNIQUE KEY `uk_task_uuid` (`task_uuid`),
  KEY `idx_session_status` (`session_id`, `status`),
  KEY `idx_writing_agent_status` (`writing_agent_id`, `status`),
  KEY `idx_swarm_agent_id` (`swarm_agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态多智能体写作任务表';

CREATE TABLE `writing_result` (
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

CREATE TABLE `writing_draft` (
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
