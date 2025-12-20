-- 邮件发送历史记录表
CREATE TABLE IF NOT EXISTS `email_send_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `email` VARCHAR(255) NOT NULL COMMENT '收件人邮箱',
  `email_type` TINYINT NOT NULL COMMENT '邮件类型:1-验证码',
  `send_status` TINYINT NOT NULL COMMENT '发送状态:0-失败,1-成功',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备指纹',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_email_time` (`email`, `create_time`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件发送日志';
