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
INSERT INTO `node_template` (`type_code`, `name`, `description`, `icon`, `default_schema_policy`, `initial_schema`, `category`, `sort_order`, `status`) VALUES
('START', '开始节点', '工作流起始节点，接收用户输入', 'play', JSON_OBJECT('inputSchemaAdd', false, 'outputSchemaAdd', false, 'inputSchemaUpdate', false, 'outputSchemaUpdate', false), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY()), 'BASIC', 1, 1),
('END', '结束节点', '工作流终止节点，返回最终结果', 'stop', JSON_OBJECT('inputSchemaAdd', false, 'outputSchemaAdd', false, 'inputSchemaUpdate', false, 'outputSchemaUpdate', false), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY()), 'BASIC', 2, 1),
('LLM', '大模型节点', '调用大语言模型进行推理', 'brain', JSON_OBJECT('inputSchemaAdd', true, 'outputSchemaAdd', false, 'inputSchemaUpdate', true, 'outputSchemaUpdate', false), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY(JSON_OBJECT('key', 'llm_output', 'label', '大模型输出', 'type', 'string', 'system', true))), 'AI', 10, 1),
('TOOL', 'MCP工具节点', '调用MCP工具执行操作', 'tool', JSON_OBJECT('inputSchemaAdd', true, 'outputSchemaAdd', true, 'inputSchemaUpdate', true, 'outputSchemaUpdate', true), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY()), 'ACTION', 20, 1),
('CONDITION', '条件分支节点', '根据条件进行分支路由', 'git-branch', JSON_OBJECT('inputSchemaAdd', true, 'outputSchemaAdd', true, 'inputSchemaUpdate', true, 'outputSchemaUpdate', true), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY()), 'CONTROL', 30, 1),
('HTTP', 'HTTP请求节点', '发起HTTP请求调用外部API', 'globe', JSON_OBJECT('inputSchemaAdd', true, 'outputSchemaAdd', true, 'inputSchemaUpdate', true, 'outputSchemaUpdate', true), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY()), 'ACTION', 40, 1),
('KNOWLEDGE', '知识库节点', '查询知识库并返回命中列表', 'book-open', JSON_OBJECT('inputSchemaAdd', true, 'outputSchemaAdd', false, 'inputSchemaUpdate', true, 'outputSchemaUpdate', false), JSON_OBJECT('inputSchema', JSON_ARRAY(), 'outputSchema', JSON_ARRAY(JSON_OBJECT('key', 'knowledge_list', 'label', '知识列表', 'type', 'array', 'system', true))), 'AI', 50, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `icon` = VALUES(`icon`), `default_schema_policy` = VALUES(`default_schema_policy`), `initial_schema` = VALUES(`initial_schema`), `category` = VALUES(`category`), `sort_order` = VALUES(`sort_order`), `status` = VALUES(`status`);

-- 插入系统配置字段定义（节点配置项）
INSERT INTO `sys_config_field_def` (`field_key`, `field_label`, `field_type`, `options`, `default_value`, `placeholder`, `description`) VALUES
('http_url', '请求路径', 'text', NULL, NULL, 'https://api.example.com/v1/data', 'HTTP 请求完整地址'),
('http_method', '请求方式', 'select', JSON_ARRAY(JSON_OBJECT('label','GET','value','GET'), JSON_OBJECT('label','POST','value','POST'), JSON_OBJECT('label','PUT','value','PUT'), JSON_OBJECT('label','PATCH','value','PATCH'), JSON_OBJECT('label','DELETE','value','DELETE')), 'GET', NULL, 'HTTP 方法'),
('http_headers', '请求头', 'textarea', NULL, NULL, '{"Authorization":"Bearer xxx"}', 'JSON 格式请求头'),
('http_body', '请求体', 'textarea', NULL, NULL, '{"query":"hello"}', 'JSON 或文本请求体'),
('knowledge_dataset_id', '知识库ID', 'text', NULL, NULL, 'dataset-xxx', '指定要查询的知识库 ID'),
('knowledge_top_k', '召回数量', 'number', NULL, '5', '5', '知识库最多返回条数')
ON DUPLICATE KEY UPDATE `field_label` = VALUES(`field_label`), `field_type` = VALUES(`field_type`), `options` = VALUES(`options`), `default_value` = VALUES(`default_value`), `placeholder` = VALUES(`placeholder`), `description` = VALUES(`description`);

-- 插入节点模板与配置字段映射
INSERT INTO `node_template_config_mapping` (`node_template_id`, `field_def_id`, `group_name`, `sort_order`, `is_required`)
SELECT t.id, f.id, 'HTTP配置', 1, 1 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'http_url' WHERE t.type_code = 'HTTP'
UNION ALL
SELECT t.id, f.id, 'HTTP配置', 2, 1 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'http_method' WHERE t.type_code = 'HTTP'
UNION ALL
SELECT t.id, f.id, 'HTTP配置', 3, 0 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'http_headers' WHERE t.type_code = 'HTTP'
UNION ALL
SELECT t.id, f.id, 'HTTP配置', 4, 0 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'http_body' WHERE t.type_code = 'HTTP'
UNION ALL
SELECT t.id, f.id, '知识库配置', 1, 1 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'knowledge_dataset_id' WHERE t.type_code = 'KNOWLEDGE'
UNION ALL
SELECT t.id, f.id, '知识库配置', 2, 0 FROM `node_template` t JOIN `sys_config_field_def` f ON f.field_key = 'knowledge_top_k' WHERE t.type_code = 'KNOWLEDGE'
ON DUPLICATE KEY UPDATE `group_name` = VALUES(`group_name`), `sort_order` = VALUES(`sort_order`), `is_required` = VALUES(`is_required`);

-- ============================================================
-- 8. 蜂群协作模块（Swarm）
-- ============================================================

-- 蜂群工作空间表
CREATE TABLE IF NOT EXISTS `swarm_workspace` (
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

-- 蜂群工作空间Agent关联表
CREATE TABLE IF NOT EXISTS `swarm_workspace_agent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `agent_id` bigint(20) DEFAULT NULL COMMENT '关联的agent_info ID（人类节点为空）',
  `role` varchar(50) NOT NULL COMMENT '角色: human/assistant/自定义角色',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父Agent ID（谁创建的，指向本表id）',
  `llm_history` longtext COMMENT '该Agent在此workspace的LLM对话历史',
  `status` varchar(20) DEFAULT 'IDLE' COMMENT 'Agent状态: IDLE/BUSY/WAKING/STOPPED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群工作空间Agent关联表';

-- 蜂群IM群组表
CREATE TABLE IF NOT EXISTS `swarm_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `workspace_id` bigint(20) NOT NULL COMMENT '工作空间ID',
  `name` varchar(100) DEFAULT NULL COMMENT '群组名称（P2P群可为空）',
  `context_tokens` int(11) DEFAULT 0 COMMENT '上下文token数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群IM群组表';

-- 蜂群群成员表
CREATE TABLE IF NOT EXISTS `swarm_group_member` (
  `group_id` bigint(20) NOT NULL COMMENT '群组ID',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID（指向swarm_workspace_agent.id）',
  `last_read_message_id` bigint(20) DEFAULT 0 COMMENT '最后已读消息ID',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`group_id`, `agent_id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂群群成员表';

-- 蜂群IM消息表
CREATE TABLE IF NOT EXISTS `swarm_message` (
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
-- 9. LLM配置中心
-- ============================================================

-- LLM供应商配置表
CREATE TABLE IF NOT EXISTS `llm_provider_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint(20) NOT NULL COMMENT '创建者用户ID',
  `name` varchar(100) NOT NULL COMMENT '配置名称（如DeepSeek V3、GPT-4o）',
  `provider` varchar(50) NOT NULL COMMENT '供应商标识: openai/zhipu/ollama等',
  `base_url` varchar(500) NOT NULL COMMENT 'API基础URL',
  `api_key` varchar(500) DEFAULT NULL COMMENT 'API Key（MVP明文，后续AES加密）',
  `model` varchar(100) NOT NULL COMMENT '默认模型名',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否为默认配置: 0-否, 1-是',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM供应商配置表';

-- 完成
SELECT 'AI Agent database initialized successfully!' AS message;
