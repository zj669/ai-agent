DROP TABLE IF EXISTS `ai_agent_version`;
CREATE TABLE `ai_agent_version` (
                                       `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `agent_id` bigint(20) NOT NULL COMMENT '关联的工作流ID',
                                       `version` varchar(32) NOT NULL COMMENT '版本号 (如: v1.0.1)',
                                       `graph_json` longtext NOT NULL COMMENT '核心编排规则 (节点、连线、DSL配置)',
                                       `status` tinyint(4) DEFAULT '0' COMMENT '状态 (0:草稿, 1:已发布, 2:历史版本)',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_workflow_id` (`agent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工作流版本配置表';

DROP TABLE IF EXISTS `ai_node_template`;
CREATE TABLE `ai_node_template` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `node_type` varchar(50) NOT NULL COMMENT '节点类型 (PLAN, ACT, REFLECT, HUMAN, ROUTER, END)',
                                    `node_name` varchar(64) NOT NULL COMMENT '节点展示名称',
                                    `icon` varchar(255) DEFAULT NULL COMMENT '前端图标标识',
                                    `default_system_prompt` text COMMENT '默认系统提示词 (用于初始化)',
                                    `config_schema` text COMMENT '前端表单配置Schema (JSON格式)',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI节点模版配置表';

DROP TABLE IF EXISTS `ai_workflow_instance`;
CREATE TABLE `ai_workflow_instance` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `agent_id` bigint(20) NOT NULL COMMENT '关联的agentID',
                                        `version_id` bigint(20) NOT NULL COMMENT '关联的工作流版本ID',
                                        `conversation_id` varchar(100) NOT NULL COMMENT '会话/任务ID',
                                        `current_node_id` varchar(64) DEFAULT NULL COMMENT '当前停留的节点ID',
                                        `status` varchar(32) NOT NULL COMMENT '运行状态 (RUNNING, PAUSED, COMPLETED, FAILED)',
                                        `runtime_context_json` longtext COMMENT '运行时上下文快照 (变量、Memory、历史)',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
                                        KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工作流运行实例表';


-- 如果需要存储"1.0"这样的版本号，应该这样修改：
ALTER TABLE `ai_agent`
    CHANGE COLUMN `channel` `active_version_id` varchar(20) DEFAULT NULL COMMENT '当前发布的版本号';

-- 更新数据为"1.0"
UPDATE `ai_agent` SET
                      `active_version_id` = '1.0',
                      `update_time` = CURRENT_TIMESTAMP;