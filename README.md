# ai-agent

## 简介
ai-agent 是一个基于Spring Boot和Spring AI的智能代理框架，提供了丰富的功能模块，包括但不限于：
- 智能对话代理
- 任务规划与执行
- 知识库检索增强生成(RAG)
- 待办事项管理

## 功能特性

### 待办事项管理
本项目提供了一个完整的待办事项管理系统，包含以下功能：

1. **自动创建待办事项**：通过TodoListPlanAdvisor自动解析AI生成的任务列表并存储到数据库
2. **待办事项执行跟踪**：通过TodoListExecuteAdvisor跟踪任务执行状态
3. **REST API接口**：提供HTTP接口用于查询待办事项

#### 数据库表结构
```sql
CREATE TABLE IF NOT EXISTS `todo_list` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '对话ID',
  `task_content` text NOT NULL COMMENT '任务内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态(0:未完成, 1:进行中, 2:已完成)',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级(1:低, 2:中, 3:高)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';
```

#### API接口
- GET /api/todo/list/{conversationId} - 获取指定对话的待办事项列表
- GET /api/todo/uncompleted/{conversationId} - 获取指定对话的未完成待办事项列表
- GET /api/todo/in-progress/{conversationId} - 获取指定对话的进行中待办事项列表

#### 核心组件
- `TodoListPlanAdvisor` - 在AI对话后自动解析并创建待办事项
- `TodoListExecuteAdvisor` - 在任务执行前后更新待办事项状态
- `TodoListService` - 提供待办事项的业务逻辑处理
- `TodoListController` - 提供REST API接口
- `TodoListMapper` - 数据库访问接口

## 使用说明
（更多使用说明待补充）

https://sub01.fkknet.cn/api/v1/client/subscribe?token=08d3b4bc52311d8d922e919887632e1c


https://drfytjmjhggnrgergergergerg6555.saojc.xyz/api/v1/client/subscribe?token=bea2ab4e717ea290480ea539187bf647

https://s1.byte77.com/api/v1/client/subscribe?token=2f1105aa6e8d1def2cac62e51ebd016d