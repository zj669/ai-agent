ALTER TABLE `knowledge_document`
  ADD COLUMN IF NOT EXISTS `chunk_strategy` varchar(32) DEFAULT 'FIXED' COMMENT '分块策略: FIXED, SEMANTIC' AFTER `error_message`,
  ADD COLUMN IF NOT EXISTS `chunk_config_json` text COMMENT '分块配置 JSON' AFTER `chunk_strategy`;

UPDATE `knowledge_document`
SET `chunk_strategy` = 'FIXED'
WHERE `chunk_strategy` IS NULL OR `chunk_strategy` = '';
