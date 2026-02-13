-- AI Agent Platform - 测试数据准备脚本
-- 日期: 2026-02-10
-- 用途: 为集成测试准备测试数据

-- ========================================
-- 清理现有测试数据
-- ========================================

-- 删除测试用户的数据（邮箱以 test_ 开头）
DELETE FROM agent_version WHERE agent_id IN (
    SELECT id FROM agent_info WHERE user_id IN (
        SELECT id FROM user_account WHERE email LIKE 'test_%@%'
    )
);

DELETE FROM agent_info WHERE user_id IN (
    SELECT id FROM user_account WHERE email LIKE 'test_%@%'
);

DELETE FROM user_account WHERE email LIKE 'test_%@%';

-- ========================================
-- 创建测试用户
-- ========================================

-- 测试用户1: test_user1@example.com / Test123456
INSERT INTO user_account (email, username, password_hash, status, deleted, create_time, update_time)
VALUES (
    'test_user1@example.com',
    'TestUser1',
    '$2a$10$rZ8qN5YYZ5YYZ5YYZ5YYZ.YYZ5YYZ5YYZ5YYZ5YYZ5YYZ5YYZ5YYZ',  -- Test123456
    'ACTIVE',
    0,
    NOW(),
    NOW()
);

-- 测试用户2: test_user2@example.com / Test123456
INSERT INTO user_account (email, username, password_hash, status, deleted, create_time, update_time)
VALUES (
    'test_user2@example.com',
    'TestUser2',
    '$2a$10$rZ8qN5YYZ5YYZ5YYZ5YYZ.YYZ5YYZ5YYZ5YYZ5YYZ5YYZ5YYZ5YYZ',  -- Test123456
    'ACTIVE',
    0,
    NOW(),
    NOW()
);

-- ========================================
-- 创建测试 Agent
-- ========================================

-- 获取测试用户1的ID
SET @user1_id = (SELECT id FROM user_account WHERE email = 'test_user1@example.com');

-- Agent 1: 简单的客服 Agent (DRAFT 状态)
INSERT INTO agent_info (user_id, name, description, icon, graph_json, status, version, deleted, create_time, update_time)
VALUES (
    @user1_id,
    'Customer Service Agent',
    'AI-powered customer service assistant',
    'service-icon.png',
    '{"nodes":[{"id":"start","type":"START"},{"id":"llm1","type":"LLM","config":{"model":"gpt-4","prompt":"You are a helpful customer service assistant"}},{"id":"end","type":"END"}],"edges":[{"source":"start","target":"llm1"},{"source":"llm1","target":"end"}]}',
    'DRAFT',
    1,
    0,
    NOW(),
    NOW()
);

-- Agent 2: 带条件分支的 Agent (PUBLISHED 状态)
INSERT INTO agent_info (user_id, name, description, icon, graph_json, status, version, deleted, create_time, update_time)
VALUES (
    @user1_id,
    'Smart Routing Agent',
    'Agent with conditional branching',
    'routing-icon.png',
    '{"nodes":[{"id":"start","type":"START"},{"id":"condition1","type":"CONDITION","config":{"mode":"EXPRESSION","branches":[{"name":"VIP","conditionGroups":[{"logicalOperator":"AND","conditions":[{"variable":"{{userLevel}}","operator":"EQUALS","value":"VIP"}]}]},{"name":"Normal","conditionGroups":[{"logicalOperator":"AND","conditions":[{"variable":"{{userLevel}}","operator":"EQUALS","value":"NORMAL"}]}]}]}},{"id":"vip_service","type":"LLM","config":{"prompt":"VIP service"}},{"id":"normal_service","type":"LLM","config":{"prompt":"Normal service"}},{"id":"end","type":"END"}],"edges":[{"source":"start","target":"condition1"},{"source":"condition1","target":"vip_service","sourceHandle":"VIP"},{"source":"condition1","target":"normal_service","sourceHandle":"Normal"},{"source":"vip_service","target":"end"},{"source":"normal_service","target":"end"}]}',
    'PUBLISHED',
    1,
    0,
    NOW(),
    NOW()
);

-- 为 Agent 2 创建版本快照
SET @agent2_id = LAST_INSERT_ID();

INSERT INTO agent_version (agent_id, version, graph_snapshot, change_description, deleted, create_time)
VALUES (
    @agent2_id,
    1,
    '{"nodes":[{"id":"start","type":"START"},{"id":"condition1","type":"CONDITION","config":{"mode":"EXPRESSION","branches":[{"name":"VIP","conditionGroups":[{"logicalOperator":"AND","conditions":[{"variable":"{{userLevel}}","operator":"EQUALS","value":"VIP"}]}]},{"name":"Normal","conditionGroups":[{"logicalOperator":"AND","conditions":[{"variable":"{{userLevel}}","operator":"EQUALS","value":"NORMAL"}]}]}]}},{"id":"vip_service","type":"LLM","config":{"prompt":"VIP service"}},{"id":"normal_service","type":"LLM","config":{"prompt":"Normal service"}},{"id":"end","type":"END"}],"edges":[{"source":"start","target":"condition1"},{"source":"condition1","target":"vip_service","sourceHandle":"VIP"},{"source":"condition1","target":"normal_service","sourceHandle":"Normal"},{"source":"vip_service","target":"end"},{"source":"normal_service","target":"end"}]}',
    'Initial version',
    0,
    NOW()
);

-- ========================================
-- 创建测试知识库
-- ========================================

-- 知识库 1: 产品 FAQ
INSERT INTO knowledge_dataset (user_id, name, description, deleted, create_time, update_time)
VALUES (
    @user1_id,
    'Product FAQ',
    'Frequently asked questions about our products',
    0,
    NOW(),
    NOW()
);

SET @dataset1_id = LAST_INSERT_ID();

-- 知识文档 1: 退货政策
INSERT INTO knowledge_document (dataset_id, title, content, source_type, source_url, deleted, create_time, update_time)
VALUES (
    @dataset1_id,
    'Return Policy',
    'Our return policy allows customers to return products within 30 days of purchase. Products must be in original condition with all tags attached. Refunds will be processed within 5-7 business days.',
    'MANUAL',
    NULL,
    0,
    NOW(),
    NOW()
);

-- 知识文档 2: 配送信息
INSERT INTO knowledge_document (dataset_id, title, content, source_type, source_url, deleted, create_time, update_time)
VALUES (
    @dataset1_id,
    'Shipping Information',
    'We offer free shipping on orders over $50. Standard shipping takes 3-5 business days. Express shipping is available for an additional fee and takes 1-2 business days.',
    'MANUAL',
    NULL,
    0,
    NOW(),
    NOW()
);

-- ========================================
-- 验证测试数据
-- ========================================

-- 查询测试用户
SELECT
    id,
    email,
    username,
    status,
    create_time
FROM user_account
WHERE email LIKE 'test_%@%';

-- 查询测试 Agent
SELECT
    a.id,
    a.name,
    a.status,
    a.version,
    u.email as owner_email
FROM agent_info a
JOIN user_account u ON a.user_id = u.id
WHERE u.email LIKE 'test_%@%';

-- 查询测试知识库
SELECT
    d.id,
    d.name,
    COUNT(doc.id) as document_count,
    u.email as owner_email
FROM knowledge_dataset d
JOIN user_account u ON d.user_id = u.id
LEFT JOIN knowledge_document doc ON d.id = doc.dataset_id AND doc.deleted = 0
WHERE u.email LIKE 'test_%@%'
GROUP BY d.id, d.name, u.email;

-- ========================================
-- 测试数据准备完成
-- ========================================

SELECT '测试数据准备完成！' as status;
