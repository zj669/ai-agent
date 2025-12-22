-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
    `agent_id` bigint NOT NULL COMMENT 'Agent ID',
    `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
    `instance_id` bigint NULL DEFAULT NULL COMMENT '关联的执行实例ID(仅assistant)',
    
    `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息角色: user, assistant',
    `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容（用户消息原文）',
    
    `final_response` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI最终回复内容',
    `is_error` tinyint(1) NULL DEFAULT 0 COMMENT '是否有错误',
    `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
    
    `timestamp` bigint NOT NULL COMMENT '消息时间戳（毫秒）',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_conversation_id` (`conversation_id`) USING BTREE,
    INDEX `idx_agent_id` (`agent_id`) USING BTREE,
    INDEX `idx_user_id` (`user_id`) USING BTREE,
    INDEX `idx_instance_id` (`instance_id`) USING BTREE,
    INDEX `idx_timestamp` (`timestamp`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI聊天消息记录表' ROW_FORMAT = DYNAMIC;
