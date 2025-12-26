-- ========================================
-- NodeTemplate 表结构扩展 SQL
-- ========================================

-- 1. 修改表结构，添加缺失字段
ALTER TABLE `ai_node_template`
    ADD COLUMN `template_id` varchar(64) NOT NULL COMMENT '模板唯一ID' AFTER `id`,
    ADD COLUMN `template_label` varchar(100) NULL COMMENT '模板显示标签(中文)' AFTER `node_name`,
    ADD COLUMN `base_type` varchar(32) NOT NULL DEFAULT 'LLM_NODE' COMMENT '基础节点类型: LLM_NODE, TOOL_NODE, ROUTER_NODE' AFTER `description`,
    ADD COLUMN `output_schema` text NULL COMMENT '输出Schema定义(JSON格式)' AFTER `default_system_prompt`,
    ADD COLUMN `editable_fields` text NULL COMMENT '用户可编辑字段列表(JSON数组)' AFTER `output_schema`,
    ADD COLUMN `is_built_in` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否系统内置模板' AFTER `editable_fields`,
    ADD COLUMN `is_deprecated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已废弃' AFTER `is_built_in`,
    ADD UNIQUE INDEX `uk_template_id`(`template_id` ASC),
    DROP INDEX `uk_node_type`;

-- 2. 重命名字段使其更符合语义
ALTER TABLE `ai_node_template`
    CHANGE COLUMN `default_system_prompt` `system_prompt_template` text NULL COMMENT 'System Prompt模板(支持{state.fieldName}变量占位符)';

-- 3. 删除不再使用的旧字段
ALTER TABLE `ai_node_template`
    DROP COLUMN `config_schema`;


-- ========================================
-- 插入预设节点模板
-- ========================================

-- 3. 清空旧数据（如果需要）
-- TRUNCATE TABLE `ai_node_template`;

-- 4. 插入 PlanNode 模板
INSERT INTO `ai_node_template` (
    `template_id`,
    `node_type`,
    `node_name`,
    `template_label`,
    `description`,
    `base_type`,
    `icon`,
    `system_prompt_template`,
    `output_schema`,
    `editable_fields`,
    `is_built_in`,
    `is_deprecated`
) VALUES (
    'plan_node_v1',
    'PLAN_NODE',
    'PlanNode',
    '规划节点',
    '根据用户问题和可用工具生成执行计划',
    'LLM_NODE',
    'plan-icon',
    '你是一个规划专家。

用户问题: {state.userQuestion}

可用工具:
{state.availableTools}

请输出 JSON 格式的执行计划，格式如下：
{
  "planSteps": [
    {
      "stepId": "1",
      "description": "步骤描述",
      "toolName": "工具名称",
      "toolParams": {...}
    }
  ]
}',
    '[
      {
        "name": "planSteps",
        "type": "list",
        "description": "执行计划步骤列表",
        "required": true,
        "reducerType": "overwrite"
      }
    ]',
    '["model", "mcpTools", "temperature"]',
    1,
    0
);

-- 5. 插入 ActNode 模板
INSERT INTO `ai_node_template` (
    `template_id`,
    `node_type`,
    `node_name`,
    `template_label`,
    `description`,
    `base_type`,
    `icon`,
    `system_prompt_template`,
    `output_schema`,
    `editable_fields`,
    `is_built_in`,
    `is_deprecated`
) VALUES (
    'act_node_v1',
    'ACT_NODE',
    'ActNode',
    '执行节点',
    '执行计划中的工具调用，将结果存入执行历史',
    'TOOL_NODE',
    'tool-icon',
    NULL,  -- 工具节点不需要 Prompt 模板
    '[
      {
        "name": "executionHistory",
        "type": "list",
        "description": "执行历史记录",
        "required": true,
        "reducerType": "append"
      },
      {
        "name": "currentStepIndex",
        "type": "number",
        "description": "当前步骤索引",
        "required": true,
        "reducerType": "increment"
      }
    ]',
    '[]',
    1,
    0
);

-- 6. 插入 SummaryNode 模板
INSERT INTO `ai_node_template` (
    `template_id`,
    `node_type`,
    `node_name`,
    `template_label`,
    `description`,
    `base_type`,
    `icon`,
    `system_prompt_template`,
    `output_schema`,
    `editable_fields`,
    `is_built_in`,
    `is_deprecated`
) VALUES (
    'summary_node_v1',
    'SUMMARY_NODE',
    'SummaryNode',
    '总结节点',
    '评估执行结果是否足够回答用户问题',
    'LLM_NODE',
    'summary-icon',
    '你是一个评估专家。

用户问题: {state.userQuestion}

执行历史:
{state.executionHistory}

请评估是否已收集到足够信息回答用户问题，输出 JSON 格式：
{
  "hasEnoughInfo": true/false,
  "finalAnswer": "如果信息充足，请给出最终答案",
  "reflectionReason": "如果信息不足，说明原因"
}',
    '[
      {
        "name": "hasEnoughInfo",
        "type": "boolean",
        "description": "是否有足够信息",
        "required": true,
        "reducerType": "overwrite"
      },
      {
        "name": "finalAnswer",
        "type": "string",
        "description": "最终答案",
        "required": false,
        "reducerType": "overwrite"
      },
      {
        "name": "reflectionReason",
        "type": "string",
        "description": "反思原因",
        "required": false,
        "reducerType": "overwrite"
      }
    ]',
    '["model", "temperature"]',
    1,
    0
);

-- 7. 插入 RouterNode 模板（如果需要）
INSERT INTO `ai_node_template` (
    `template_id`,
    `node_type`,
    `node_name`,
    `template_label`,
    `description`,
    `base_type`,
    `icon`,
    `system_prompt_template`,
    `output_schema`,
    `editable_fields`,
    `is_built_in`,
    `is_deprecated`
) VALUES (
    'router_node_v1',
    'ROUTER_NODE',
    'RouterNode',
    '路由节点',
    '根据条件选择下一个执行节点',
    'ROUTER_NODE',
    'router-icon',
    '你是一个决策专家。

当前状态:
{state.hasEnoughInfo}

候选节点:
{state.candidateNodes}

请选择下一个要执行的节点。',
    '[
      {
        "name": "nextNodeId",
        "type": "string",
        "description": "下一个节点ID",
        "required": true,
        "reducerType": "overwrite"
      }
    ]',
    '[]',
    1,
    0
);

-- ========================================
-- 验证插入结果
-- ========================================
SELECT 
    template_id,
    node_name,
    template_label,
    base_type,
    is_built_in,
    is_deprecated
FROM ai_node_template
ORDER BY id;
