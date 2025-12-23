-- 创建配置字段定义表
CREATE TABLE `ai_config_field_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_type` varchar(50) NOT NULL COMMENT '配置类型 (MODEL, HUMAN_INTERVENTION等)',
  `field_name` varchar(50) NOT NULL COMMENT '字段名称',
  `field_label` varchar(100) NOT NULL COMMENT '字段标签',
  `field_type` varchar(20) NOT NULL COMMENT '字段类型 (text, number, boolean, select, textarea)',
  `required` tinyint(1) DEFAULT 0 COMMENT '是否必填',
  `description` text COMMENT '字段描述',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `options` text COMMENT '可选项 (JSON数组,用于select类型)',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_field` (`config_type`, `field_name`),
  KEY `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置字段定义表';

-- ============================================================
-- MODEL 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('MODEL', 'baseUrl', 'API基础URL', 'text', 0, '模型API的基础URL地址', NULL, NULL, 1),
('MODEL', 'apiKey', 'API密钥', 'password', 0, '访问模型API的密钥', NULL, NULL, 2),
('MODEL', 'modelName', '模型名称', 'text', 0, '使用的模型名称', NULL, NULL, 3),
('MODEL', 'temperature', '温度参数', 'number', 0, '控制输出随机性，范围0.0-2.0', '0.7', NULL, 4),
('MODEL', 'maxTokens', '最大Token数', 'number', 0, '生成文本的最大token数量', NULL, NULL, 5),
('MODEL', 'topP', 'Top P参数', 'number', 0, '核采样参数，范围0.0-1.0', NULL, NULL, 6),
('MODEL', 'frequencyPenalty', '频率惩罚', 'number', 0, '降低重复内容的惩罚系数', NULL, NULL, 7),
('MODEL', 'presencePenalty', '存在惩罚', 'number', 0, '鼓励生成新内容的惩罚系数', NULL, NULL, 8);

-- ============================================================
-- ADVISOR 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('ADVISOR', 'advisorId', 'Advisor ID', 'text', 1, 'Advisor的唯一标识', NULL, NULL, 1),
('ADVISOR', 'advisorType', 'Advisor类型', 'select', 1, 'Advisor类型', NULL, '["MEMORY","RAG","TOOL","CUSTOM"]', 2),
('ADVISOR', 'config', '配置参数', 'json', 0, 'Advisor的配置参数，JSON格式', NULL, NULL, 3);

-- ============================================================
-- MCP_TOOL 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('MCP_TOOL', 'mcpId', 'MCP工具ID', 'text', 1, 'MCP工具的唯一标识', NULL, NULL, 1),
('MCP_TOOL', 'mcpName', 'MCP工具名称', 'text', 1, 'MCP工具的名称', NULL, NULL, 2),
('MCP_TOOL', 'mcpType', 'MCP工具类型', 'select', 1, 'MCP工具类型', NULL, '["FILE_SYSTEM","CODE_EXECUTOR","WEB_SEARCH","DATABASE","API_CLIENT"]', 3);

-- ============================================================
-- USER_PROMPT 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('USER_PROMPT', 'userPrompt', '用户提示词', 'textarea', 0, '自定义的用户提示词内容', NULL, NULL, 1),
('USER_PROMPT', 'systemPrompt', '系统提示词', 'textarea', 0, '系统提示词内容', NULL, NULL, 2),
('USER_PROMPT', 'userPromptTemplate', '用户提示词模板', 'textarea', 0, '支持变量的用户提示词模板', NULL, NULL, 3);

-- ============================================================
-- TIMEOUT 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('TIMEOUT', 'timeout', '超时时间', 'number', 0, '节点执行超时时间(毫秒)', NULL, NULL, 1);

-- ============================================================
-- MEMORY 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('MEMORY', 'enabled', '启用记忆', 'boolean', 0, '是否启用记忆功能', 'false', NULL, 1),
('MEMORY', 'type', '记忆类型', 'select', 0, '记忆的类型', NULL, '["VECTOR_STORE","CHAT_MEMORY","SUMMARY_MEMORY","BUFFER_MEMORY"]', 2),
('MEMORY', 'retrieveSize', '检索大小', 'number', 0, '从记忆中检索的条目数量', '10', NULL, 3),
('MEMORY', 'conversationId', '会话ID', 'text', 0, '会话ID，支持占位符${conversationId}', NULL, NULL, 4);

-- ============================================================
-- HUMAN_INTERVENTION 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('HUMAN_INTERVENTION', 'enabled', '启用人工介入', 'boolean', 0, '是否启用人工介入功能', 'false', NULL, 1),
('HUMAN_INTERVENTION', 'timing', '介入时机', 'select', 0, '选择在节点执行前还是执行后暂停', 'AFTER', '["BEFORE","AFTER"]', 2),
('HUMAN_INTERVENTION', 'checkMessage', '审核提示消息', 'textarea', 0, '显示给用户的审核提示文本', NULL, NULL, 3),
('HUMAN_INTERVENTION', 'allowModifyOutput', '允许修改输出', 'boolean', 0, '是否允许用户在审核时修改节点输出', 'false', NULL, 4),
('HUMAN_INTERVENTION', 'timeout', '超时时间(毫秒)', 'number', 0, '超时后自动通过,null表示永久等待', NULL, NULL, 5);

-- ============================================================
-- SYSTEM_PROMPT 配置字段定义
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('SYSTEM_PROMPT', 'promptId', '提示词ID', 'text', 0, '系统提示词的唯一标识', NULL, NULL, 1),
('SYSTEM_PROMPT', 'promptContent', '提示词内容', 'textarea', 0, '自定义系统提示词内容', NULL, NULL, 2);

-- ============================================================
-- ROUTING_STRATEGY 配置字段定义（用于 RouterNode）
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('ROUTING_STRATEGY', 'strategyType', '路由策略类型', 'select', 1, '选择路由决策方式', 'AI_EVALUATE', '["AI_EVALUATE","RULE_BASED","MANUAL"]', 1),
('ROUTING_STRATEGY', 'evaluationPrompt', '评估提示词', 'textarea', 0, 'AI评估时使用的提示词', NULL, NULL, 2),
('ROUTING_STRATEGY', 'rules', '路由规则', 'json', 0, '基于规则的路由配置（JSON格式）', NULL, NULL, 3);

-- ============================================================
-- CANDIDATE_NODES 配置字段定义（用于 RouterNode）
-- ============================================================
INSERT INTO `ai_config_field_definition` 
(`config_type`, `field_name`, `field_label`, `field_type`, `required`, `description`, `default_value`, `options`, `sort_order`)
VALUES
('CANDIDATE_NODES', 'nodes', '候选节点列表', 'json', 1, '可选的下游节点ID列表（JSON数组）', NULL, NULL, 1),
('CANDIDATE_NODES', 'allowMultiple', '允许多选', 'boolean', 0, '是否允许选择多个节点', 'false', NULL, 2);

-- ============================================================
-- 更新 ai_node_template 表，为所有节点添加 HUMAN_INTERVENTION 支持
-- ============================================================
-- 注意：这需要根据实际的 config_schema 结构来调整
-- 如果 config_schema 是 JSON 格式，可以使用 JSON 函数更新
-- 如果是逗号分隔的字符串，则需要字符串拼接

-- 示例：假设 config_schema 存储的是逗号分隔的配置类型
UPDATE `ai_node_template`
SET `config_schema` = CONCAT(COALESCE(`config_schema`, ''), ',HUMAN_INTERVENTION')
WHERE `node_type` IN ('PLAN_NODE', 'ACT_NODE', 'REACT_NODE', 'ROUTER_NODE')
  AND (config_schema IS NULL OR config_schema NOT LIKE '%HUMAN_INTERVENTION%');
