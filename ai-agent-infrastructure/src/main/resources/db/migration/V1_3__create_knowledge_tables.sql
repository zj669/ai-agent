-- ============================================================
-- 知识库管理模块 DDL
-- 创建 knowledge_dataset 和 knowledge_document 表
-- ============================================================

-- 知识数据集表（知识库）
CREATE TABLE `knowledge_dataset` (
  `dataset_id` varchar(36) NOT NULL COMMENT '知识库ID (UUID)',
  `name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` text COMMENT '知识库描述',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `agent_id` bigint(20) DEFAULT NULL COMMENT '绑定的Agent ID（可选）',
  `document_count` int(11) DEFAULT 0 COMMENT '文档数量',
  `total_chunks` int(11) DEFAULT 0 COMMENT '总分块数量',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dataset_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识数据集表';

-- 知识文档表
CREATE TABLE `knowledge_document` (
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
