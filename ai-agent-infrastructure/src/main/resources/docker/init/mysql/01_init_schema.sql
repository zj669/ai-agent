-- ============================================================
-- AI Agent Platform - MySQL 初始化脚本
-- 此脚本在容器首次启动时自动执行
-- ============================================================

-- 确保使用正确的数据库
USE ai_agent;

-- 设置字符集
SET NAMES utf8mb4;

-- ============================================================
-- 1. 用户模块
-- ============================================================

-- 用户信息表
CREATE TABLE IF NOT EXISTS `user_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码(加密)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `status` int(11) DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ============================================================
-- 2. 智能体模块
-- ============================================================

-- 智能体主表
CREATE TABLE IF NOT EXISTS `agent_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) NOT NULL COMMENT '智能体名称',
  `description` text COMMENT '智能体描述',
  `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
  `graph_json` json DEFAULT NULL COMMENT '工作流图JSON(草稿)',
  `status` int(11) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线',
  `published_version_id` bigint(20) DEFAULT NULL COMMENT '当前发布的版本ID',
  `version` int(11) DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` int(11) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体主表';

-- 智能体版本表
CREATE TABLE IF NOT EXISTS `agent_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本记录ID',
  `agent_id` bigint(20) NOT NULL COMMENT '智能体ID',
  `version` int(11) NOT NULL COMMENT '版本号',
  `graph_snapshot` json NOT NULL COMMENT '工作流图快照',
  `description` varchar(500) DEFAULT NULL COMMENT '版本描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_version` (`agent_id`, `version`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体版本表';

-- ============================================================
-- 3. 会话模块
-- ============================================================

-- 会话表
CREATE TABLE IF NOT EXISTS `conversations` (
  `id` varchar(36) NOT NULL COMMENT '会话ID (UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID',
  `agent_id` varchar(36) NOT NULL COMMENT '智能体ID',
  `title` varchar(200) DEFAULT NULL COMMENT '会话标题',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_agent` (`user_id`, `agent_id`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
  `id` varchar(36) NOT NULL COMMENT '消息ID (UUID)',
  `conversation_id` varchar(36) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色: USER, ASSISTANT, SYSTEM',
  `content` longtext COMMENT '消息内容',
  `thought_process` json DEFAULT NULL COMMENT '思维链过程 (JSON数组)',
  `citations` json DEFAULT NULL COMMENT '引用列表 (JSON数组)',
  `meta_data` json DEFAULT NULL COMMENT '元数据 (JSON对象)',
  `status` varchar(20) DEFAULT 'COMPLETED' COMMENT '消息状态: PENDING, STREAMING, COMPLETED, FAILED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_created` (`conversation_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ============================================================
-- 4. 知识库模块
-- ============================================================

-- 知识数据集表
CREATE TABLE IF NOT EXISTS `knowledge_dataset` (
  `dataset_id` varchar(36) NOT NULL COMMENT '知识库ID (UUID)',
  `name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` text COMMENT '知识库描述',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `agent_id` bigint(20) DEFAULT NULL COMMENT '绑定的Agent ID',
  `document_count` int(11) DEFAULT 0 COMMENT '文档数量',
  `total_chunks` int(11) DEFAULT 0 COMMENT '总分块数量',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dataset_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识数据集表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
  `document_id` varchar(36) NOT NULL COMMENT '文档ID (UUID)',
  `dataset_id` varchar(36) NOT NULL COMMENT '所属知识库ID',
  `filename` varchar(255) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT 'MinIO 文件URL',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `content_type` varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '处理状态: PENDING, PROCESSING, COMPLETED, FAILED',
  `total_chunks` int(11) DEFAULT NULL COMMENT '总分块数',
  `processed_chunks` int(11) DEFAULT 0 COMMENT '已处理分块数',
  `error_message` text COMMENT '错误信息',
  `chunk_size` int(11) DEFAULT 500 COMMENT '分块大小(tokens)',
  `chunk_overlap` int(11) DEFAULT 50 COMMENT '分块重叠(tokens)',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `completed_at` datetime DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (`document_id`),
  KEY `idx_dataset_id` (`dataset_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_document_dataset` FOREIGN KEY (`dataset_id`) 
    REFERENCES `knowledge_dataset` (`dataset_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文档表';

-- ============================================================
-- 5. 元数据模块
-- ============================================================

-- 节点模板表
CREATE TABLE IF NOT EXISTS `node_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `type_code` varchar(50) NOT NULL COMMENT '节点类型代码',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板描述',
  `icon` varchar(200) DEFAULT NULL COMMENT '图标标识',
  `default_schema_policy` json DEFAULT NULL COMMENT '默认Schema策略',
  `initial_schema` json DEFAULT NULL COMMENT '初始化Schema',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `status` int(11) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点模板表';

-- 系统配置字段定义表
CREATE TABLE IF NOT EXISTS `sys_config_field_def` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `field_key` varchar(50) NOT NULL COMMENT '字段键名',
  `field_label` varchar(100) NOT NULL COMMENT '字段标签',
  `field_type` varchar(20) NOT NULL COMMENT '字段类型',
  `options` json DEFAULT NULL COMMENT '选项值',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `placeholder` varchar(200) DEFAULT NULL COMMENT '输入提示',
  `description` text COMMENT '字段描述',
  `validation_rules` json DEFAULT NULL COMMENT '校验规则',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field_key` (`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置字段定义表';

-- 节点模板与配置字段映射表
CREATE TABLE IF NOT EXISTS `node_template_config_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `node_template_id` bigint(20) NOT NULL COMMENT '节点模板ID',
  `field_def_id` bigint(20) NOT NULL COMMENT '字段定义ID',
  `group_name` varchar(50) DEFAULT NULL COMMENT '配置分组名',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `override_default` varchar(255) DEFAULT NULL COMMENT '覆盖默认值',
  `is_required` int(11) DEFAULT 0 COMMENT '是否必填',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`node_template_id`),
  KEY `idx_field_def_id` (`field_def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点模板配置映射表';

-- ============================================================
-- 6. 工作流执行模块
-- ============================================================

-- 工作流节点执行日志表
CREATE TABLE IF NOT EXISTS `workflow_node_execution_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `execution_id` varchar(36) NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) NOT NULL COMMENT '节点ID',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) DEFAULT NULL COMMENT '节点类型',
  `render_mode` varchar(20) DEFAULT NULL COMMENT '渲染模式',
  `status` int(11) DEFAULT 0 COMMENT '执行状态',
  `inputs` json DEFAULT NULL COMMENT '输入参数',
  `outputs` json DEFAULT NULL COMMENT '输出结果',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_execution_node` (`execution_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点执行日志表';

-- 人工审核记录表
CREATE TABLE IF NOT EXISTS `workflow_human_review_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `execution_id` varchar(36) NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) NOT NULL COMMENT '节点ID',
  `reviewer_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `decision` varchar(20) DEFAULT NULL COMMENT '审核决定',
  `trigger_phase` varchar(20) NOT NULL COMMENT '触发阶段',
  `original_data` json DEFAULT NULL COMMENT '原始数据',
  `modified_data` json DEFAULT NULL COMMENT '修改后数据',
  `comment` text COMMENT '审核备注',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工审核记录表';

-- ============================================================
-- 7. 初始化数据
-- ============================================================

-- 插入节点模板数据
INSERT INTO `node_template` (`type_code`, `name`, `description`, `icon`, `category`, `sort_order`, `status`) VALUES
('START', '开始节点', '工作流起始节点，接收用户输入', 'play', 'BASIC', 1, 1),
('END', '结束节点', '工作流终止节点，返回最终结果', 'stop', 'BASIC', 2, 1),
('LLM', '大模型节点', '调用大语言模型进行推理', 'brain', 'AI', 10, 1),
('TOOL', 'MCP工具节点', '调用MCP工具执行操作', 'tool', 'ACTION', 20, 1),
('CONDITION', '条件分支节点', '根据条件进行分支路由', 'git-branch', 'CONTROL', 30, 1),
('HTTP', 'HTTP请求节点', '发起HTTP请求调用外部API', 'globe', 'ACTION', 40, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 完成
SELECT 'AI Agent database initialized successfully!' AS message;
