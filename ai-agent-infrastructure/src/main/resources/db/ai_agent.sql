/*
 Navicat Premium Dump SQL

 Source Server         : 本地开发docker版
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:13306
 Source Schema         : ai_agent

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 02/02/2026 21:51:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_info
-- ----------------------------
DROP TABLE IF EXISTS `agent_info`;
CREATE TABLE `agent_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '智能体描述',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标URL',
  `graph_json` json NULL COMMENT '工作流图JSON(草稿)',
  `status` int NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线',
  `published_version_id` bigint NULL DEFAULT NULL COMMENT '当前发布的版本ID',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` int NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_version
-- ----------------------------
DROP TABLE IF EXISTS `agent_version`;
CREATE TABLE `agent_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本记录ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `version` int NOT NULL COMMENT '版本号',
  `graph_snapshot` json NOT NULL COMMENT '工作流图快照',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_version`(`agent_id` ASC, `version` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for conversations
-- ----------------------------
DROP TABLE IF EXISTS `conversations`;
CREATE TABLE `conversations`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID (UUID)',
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `agent_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_agent`(`user_id` ASC, `agent_id` ASC) USING BTREE,
  INDEX `idx_updated_at`(`updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_dataset
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_dataset`;
CREATE TABLE `knowledge_dataset`  (
  `dataset_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库ID (UUID)',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识库描述',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '绑定的Agent ID',
  `document_count` int NULL DEFAULT 0 COMMENT '文档数量',
  `total_chunks` int NULL DEFAULT 0 COMMENT '总分块数量',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dataset_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识数据集表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document`;
CREATE TABLE `knowledge_document`  (
  `document_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档ID (UUID)',
  `dataset_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属知识库ID',
  `filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MinIO 文件URL',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `content_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING, PROCESSING, COMPLETED, FAILED',
  `total_chunks` int NULL DEFAULT NULL COMMENT '总分块数',
  `processed_chunks` int NULL DEFAULT 0 COMMENT '已处理分块数',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `chunk_size` int NULL DEFAULT 500 COMMENT '分块大小(tokens)',
  `chunk_overlap` int NULL DEFAULT 50 COMMENT '分块重叠(tokens)',
  `uploaded_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (`document_id`) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_document_dataset` FOREIGN KEY (`dataset_id`) REFERENCES `knowledge_dataset` (`dataset_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_chunk`;
CREATE TABLE `knowledge_chunk`  (
  `chunk_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分块ID (UUID)',
  `document_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属文档ID',
  `dataset_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属知识库ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分块内容',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `token_count` int NULL DEFAULT NULL COMMENT 'Token 数量',
  `metadata` json NULL COMMENT '元数据',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chunk_id`) USING BTREE,
  INDEX `idx_document_id`(`document_id` ASC) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id` ASC) USING BTREE,
  CONSTRAINT `fk_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`document_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识分块表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_vector_index
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_vector_index`;
CREATE TABLE `knowledge_vector_index`  (
  `vector_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '向量ID (UUID)',
  `chunk_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联的分块ID',
  `milvus_id` bigint NOT NULL COMMENT 'Milvus 向量ID',
  `collection_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Milvus Collection 名称',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`vector_id`) USING BTREE,
  UNIQUE INDEX `uk_chunk_id`(`chunk_id` ASC) USING BTREE,
  INDEX `idx_milvus_id`(`milvus_id` ASC) USING BTREE,
  CONSTRAINT `fk_vector_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`chunk_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '向量索引映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息ID (UUID)',
  `conversation_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色: USER, ASSISTANT, SYSTEM',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `thought_process` json NULL COMMENT '思维链过程 (JSON数组)',
  `citations` json NULL COMMENT '引用列表 (JSON数组)',
  `meta_data` json NULL COMMENT '元数据 (JSON对象)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'COMPLETED' COMMENT '消息状态: PENDING, STREAMING, COMPLETED, FAILED',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_created`(`conversation_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for node_template
-- ----------------------------
DROP TABLE IF EXISTS `node_template`;
CREATE TABLE `node_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `type_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点类型代码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模板描述',
  `icon` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标标识',
  `default_schema_policy` json NULL COMMENT '默认Schema策略',
  `initial_schema` json NULL COMMENT '初始化Schema',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` int NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_code`(`type_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '节点模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for node_template_config_mapping
-- ----------------------------
DROP TABLE IF EXISTS `node_template_config_mapping`;
CREATE TABLE `node_template_config_mapping`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `node_template_id` bigint NOT NULL COMMENT '节点模板ID',
  `field_def_id` bigint NOT NULL COMMENT '字段定义ID',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置分组名',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `override_default` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '覆盖默认值',
  `is_required` int NULL DEFAULT 0 COMMENT '是否必填',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_id`(`node_template_id` ASC) USING BTREE,
  INDEX `idx_field_def_id`(`field_def_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '节点模板配置映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config_field_def
-- ----------------------------
DROP TABLE IF EXISTS `sys_config_field_def`;
CREATE TABLE `sys_config_field_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `field_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段键名',
  `field_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段标签',
  `field_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段类型',
  `options` json NULL COMMENT '选项值',
  `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认值',
  `placeholder` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '输入提示',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '字段描述',
  `validation_rules` json NULL COMMENT '校验规则',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_field_key`(`field_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置字段定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(加密)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `status` int NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workflow_human_review_record
-- ----------------------------
DROP TABLE IF EXISTS `workflow_human_review_record`;
CREATE TABLE `workflow_human_review_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `execution_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点ID',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `decision` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核决定',
  `trigger_phase` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发阶段',
  `original_data` json NULL COMMENT '原始数据',
  `modified_data` json NULL COMMENT '修改后数据',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核备注',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_reviewer_id`(`reviewer_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人工审核记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workflow_human_review_task
-- ----------------------------
DROP TABLE IF EXISTS `workflow_human_review_task`;
CREATE TABLE `workflow_human_review_task`  (
  `task_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务ID (UUID)',
  `execution_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING, APPROVED, REJECTED',
  `input_data` json NULL COMMENT '待审核的输入数据',
  `output_data` json NULL COMMENT '待审核的输出数据',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`task_id`) USING BTREE,
  INDEX `idx_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_reviewer_id`(`reviewer_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人工审核任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workflow_execution
-- ----------------------------
DROP TABLE IF EXISTS `workflow_execution`;
CREATE TABLE `workflow_execution`  (
  `execution_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行ID (UUID)',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联的会话ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态: RUNNING, COMPLETED, FAILED, CANCELLED',
  `mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STANDARD' COMMENT '执行模式: STANDARD, DEBUG, DRY_RUN',
  `input_data` json NULL COMMENT '输入数据',
  `output_data` json NULL COMMENT '输出数据',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '执行时长(毫秒)',
  PRIMARY KEY (`execution_id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_started_at`(`started_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流执行主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workflow_node_execution_log
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_execution_log`;
CREATE TABLE `workflow_node_execution_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `execution_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点ID',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点类型',
  `render_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渲染模式',
  `status` int NULL DEFAULT 0 COMMENT '执行状态',
  `inputs` json NULL COMMENT '输入参数',
  `outputs` json NULL COMMENT '输出结果',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_execution_node`(`execution_id` ASC, `node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 125 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流节点执行日志表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
