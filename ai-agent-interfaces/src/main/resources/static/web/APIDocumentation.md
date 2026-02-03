# API Documentation

---

## 消息保存机制说明

### 概述

系统采用**双表存储**策略，分离对话消息和执行日志：

1. **messages 表** - 存储用户和 AI 的对话消息（用于聊天历史）
2. **workflow_node_execution_log 表** - 存储节点执行日志（用于思维链展示）

### 数据流程

```
用户发送消息
    ↓
保存 USER 消息到 messages 表
    ↓
初始化 ASSISTANT 消息 (PENDING 状态)
    ↓
启动 Workflow 执行
    ↓
每个节点执行完成 → 保存到 workflow_node_execution_log 表
    ↓
Workflow 执行完成
    ↓
从 workflow_node_execution_log 提取最终响应
    ↓
更新 ASSISTANT 消息 (COMPLETED 状态)
```

### 关键特性

1. **自动保存**
   - Workflow 启动时自动保存用户消息
   - Workflow 完成时自动更新 AI 响应
   - 无需手动调用保存接口

2. **状态管理**
   - PENDING: AI 正在思考中
   - COMPLETED: AI 已完成响应
   - FAILED: Workflow 执行失败

3. **关联查询**
   - 消息的 metadata 包含 executionId
   - 通过 executionId 可查询完整的思维链
   - 支持从对话历史跳转到思维链详情

4. **数据持久化**
   - 所有数据持久化到 MySQL
   - 支持分页查询和历史回溯
   - 消息与执行日志独立存储，互不影响

### API 使用示例

```javascript
// 1. 启动对话
POST /api/workflow/execution/start
{
  "agentId": 1001,
  "userId": 101,
  "conversationId": "conv-uuid-...",
  "inputs": { "query": "你好" }
}

// 2. 查询聊天历史
GET /api/chat/conversations/conv-uuid-.../messages
// 返回: [
//   { role: "USER", content: "你好", metadata: { executionId: "exec-001" } },
//   { role: "ASSISTANT", content: "你好！有什么可以帮助你的吗？", status: "COMPLETED" }
// ]

// 3. 查询思维链（从 metadata 获取 executionId）
GET /api/workflow/execution/exec-001/logs
// 返回: [
//   { nodeId: "node-1", nodeName: "意图识别", outputs: { intent: "greeting" } },
//   { nodeId: "node-2", nodeName: "生成回复", outputs: { response: "你好！..." } }
// ]
```

---

## /client/user （用户管理）

### /email/sendCode
    功能： 发送邮箱验证码
    场景： 用户注册时，通过此接口发送验证码
    请求方式： POST
    入参： {
        email: 用户输入的邮箱
    }
    返回值： 无

### /email/register
    功能： 通过邮箱注册
    场景： 用户注册时，通过邮箱注册
    请求方式： POST
    入参： {
        email: 注册邮箱
        code: 邮箱验证码
        password: 密码
        username: 用户名
    }
    返回值: {
        token: 身份标识
        expireIn: 过期时间（秒）
        user: {
            id: 用户id
            username: 用户名
            email: 用户邮箱
            phone： 手机号
            avatarUrl： 头像地址
            status： 用户状态
            createdAt: 创建时间
        }
    }

### /login
    功能： 用户登录
    场景： 用户通过邮箱和密码登录系统
    请求方式： POST
    入参： {
        email: 邮箱
        password: 密码
    }
    返回值: {
        token: 身份标识
        expireIn: 过期时间（秒）
        user: {
            id: 用户id
            username: 用户名
            email: 用户邮箱
            phone： 手机号
            avatarUrl： 头像地址
            status： 用户状态
            createdAt: 创建时间
        }
    }

### /info
    功能： 获取当前用户信息
    场景： 获取已登录用户的详细信息
    请求方式： GET
    入参： 无
    返回值: {
            id: 用户id
            username: 用户名
            email: 用户邮箱
            phone： 手机号
            avatarUrl： 头像地址
            status： 用户状态
            createdAt: 创建时间
        }

### /profile
    功能： 修改用户信息
    场景： 用户修改个人资料
    请求方式： post
    入参： {
        username: 用户名（可选）
        avatarUrl: 头像地址（可选）
        phone: 手机号码（可选）
    }
    返回值: {
        id: 用户id
        username: 用户名
        email: 用户邮箱
        phone: 用户手机号码
        avatarUrl: 头像地址
    }

### /logout
    功能： 用户登出
    场景： 用户退出登录
    请求方式： POST
    请求头： Authorization: Bearer {token}
    入参： 无
    返回值： 无

---

## /api/agent （智能体管理）

### /create
    功能： 创建智能体
    场景： 用户创建一个新的智能体
    请求方式： POST
    入参： {
        name: 智能体名称（必填）
        description: 描述（可选）
        icon: 图标（可选）
    }
    返回值: 智能体ID (Long)
    

### /update
    功能： 更新智能体
    场景： 用户修改已有智能体的配置
    请求方式： Post
    请求头： Authorization: Bearer {token}
    入参： {
        id: 智能体ID（必填）
        name: 智能体名称（必填）
        description: 描述（可选）
        icon: 图标（可选）
        graphJson: 工作流图JSON（可选）
        version: 版本号（必填，用于乐观锁）
    }
    返回值： 无
graphJson结构演示：
    
    ## 顶层结构
    {
        "dagId": "dag-xxx",           // 图唯一标识
        "version": "3.0",             // 版本号
        "description": "Agent描述",   // 描述
        "memory": true                  // 是否启用记忆
        "startNodeId": "start",       // 起始节点ID
        "nodes": [...],               // 节点列表
        "edges": [...]                // 边列表
    }
    
    ## 节点定义（nodes[]）
    {
        "nodeId": "node-001",         // 节点唯一标识
        "nodeName": "分析节点",        // 节点显示名称
        "nodeType": "LLM",            // 节点类型：START/END/LLM/TOOL/CONDITION/HTTP
        "position": {                 // 前端可视化位置
            "x": 300,
            "y": 200
        },
        "policy": {
            "inputSchemaAdd": true;  // 是否允许新增
            "outputSchemaAdd": true; // 是否允许新增
        },
        "inputSchema": [              // 输入字段定义
            {
                "key": "userMessage",       // 字段键名
                "label": "用户消息",         // 显示标签
                "type": "string",           // 数据类型：string/number/boolean/array/object
                "description": "用户输入",   // 字段描述
                "required": true,           // 是否必填
                "defaultValue": "",         // 默认值
                "sourceRef": "state.user_input",  // 数据来源引用
                "system" : true,         // 系统参数，不可修改
            }
        ],
        "outputSchema": [             // 输出字段定义
            {
                "key": "result",
                "label": "分析结果",
                "type": "string",
                "description": "AI生成的结果"
            }
        ],
        "userConfig": {               // 用户可配置项（扁平结构，字段由数据库驱动）
            "model": "gpt-4",         // 模型名称
            "temperature": 0.7,       // 温度（0-1）
            "maxTokens": 2000,        // 最大token数
            "userPromptTemplate": "请分析：{{userMessage}}"  // 用户提示词模板
        }
    }
    
    ## 边定义（edges[]）
    {
        "edgeId": "edge-001",         // 边唯一标识
        "source": "node-001",         // 源节点ID
        "target": "node-002",         // 目标节点ID
        "condition": "#score > 80",   // 条件表达式或决策描述（条件节点后的边需要）
        "edgeType": "CONDITIONAL"     // 边类型
    }
    
    ## 节点类型说明
    | 类型 | 说明 |
    |------|------|
    | START | 开始节点 |
    | END | 结束节点 |
    | LLM | 大模型节点 |
    | TOOL | MCP工具节点 |
    | CONDITION | 条件分支节点（评估出边的 condition 字段） |
    | HTTP | HTTP请求节点 |
    
    ## 边类型说明
    | 类型 | 说明 |
    |------|------|
    | DEPENDENCY | 标准依赖边（默认） |
    | CONDITIONAL | 条件边，condition 字段为 SpEL 表达式或 LLM 决策描述 |

### /publish
    功能： 发布智能体
    场景： 将智能体草稿发布为正式版本
    请求方式： POST
    请求头： Authorization: Bearer {token}
    入参： {
        id: 智能体ID（必填）
    }
    返回值： 无

### /rollback
    功能： 回滚智能体版本
    场景： 将智能体回滚到指定历史版本
    请求方式： POST
    请求头： Authorization: Bearer {token}
    入参： {
        id: 智能体ID（必填）
        targetVersion: 目标版本号（必填）
    }
    返回值： 无

### /{id}/versions
    功能： 获取智能体版本历史
    场景： 查看智能体的历史版本列表
    请求方式： GET
    请求头： Authorization: Bearer {token}
    入参： id (PathVariable，智能体ID)
    返回值： {
        "versions": [
            {
                "id": 101,                  // 版本记录ID
                "agentId": 1,               // 智能体ID
                "version": 1,               // 版本号
                "description": "初始版本",   // 版本描述
                "createTime": "2023-10-27T10:00:00" // 创建时间
            }
        ]
    }

### /{id}/versions/{version}
    功能： 删除智能体版本
    场景： 用户删除指定智能体版本
    请求方式： DELETE
    请求头： Authorization: Bearer {token}
    入参： id (PathVariable，智能体ID), version (PathVariable，版本号)
    返回值： 无

### /{id}/force
    功能： 删除所有智能体版本
    场景： 用户删除所有智能体版本
    请求方式： DELETE
    请求头： Authorization: Bearer {token}
    入参： id (PathVariable，智能体ID)
    返回值： 无

### /list
    功能： 获取智能体列表
    场景： 获取当前用户的所有智能体
    请求方式： GET
    请求头： Authorization: Bearer {token}
    入参： 无
    返回值: [
        {
            id: 智能体ID
            name: 智能体名称
            description: 描述
            icon: 图标
            publishedVersion: 已发布版本号
            draftVersion: 草稿版本号
        }
    ]

### /{id}
    功能： 获取智能体详情
    场景： 获取指定智能体的完整信息
    请求方式： GET
    入参： id (PathVariable，智能体ID)
    返回值: {
        id: 智能体ID
        name: 智能体名称
        description: 描述
        icon: 图标
        graphJson: 工作流图JSON
        version: 版本号
        publishedVersion: 已发布版本号
    }

---

## /api/chat （聊天会话管理）

### /conversations
    功能： 创建会话
    场景： 用户开启与智能体的新对话
    请求方式： POST
    入参： 
        userId: 用户ID (Query Param)
        agentId: 智能体ID (Query Param)
    返回值: 会话ID (String)

### /conversations
    功能： 获取会话列表
    场景： 获取用户与某智能体的所有历史会话
    请求方式： GET
    入参： 
        userId: 用户ID (Query Param)
        agentId: 智能体ID (Query Param)
        page: 页码，默认1 (Query Param)
        size: 每页数量，默认20 (Query Param)
    返回值: {
        total: 总数
        pages: 总页数
        list: [
            {
                id: 会话ID
                title: 会话标题
                createdAt: 创建时间
                updatedAt: 更新时间
            }
        ]
    }

### /conversations/{conversationId}/messages
    功能： 获取会话消息历史
    场景： 获取某个会话的所有消息（包括用户输入和 AI 响应）
    请求方式： GET
    入参： 
        conversationId: 会话ID (PathVariable)
        page: 页码，默认1 (Query Param)
        size: 每页数量，默认50 (Query Param)
    返回值: [
        {
            id: 消息ID
            conversationId: 会话ID
            role: 角色 (USER/ASSISTANT/SYSTEM)
            content: 消息内容
            thoughtProcess: 思考过程列表（已废弃，请使用 /api/workflow/execution/{executionId}/logs 查询）
            citations: 引用列表
            status: 消息状态 (PENDING/COMPLETED/FAILED)
            createdAt: 创建时间
            metadata: 元数据（包含 executionId）
        }
    ]
    
    **消息保存机制说明：**
    
    1. **用户消息 (USER)**
       - 在 workflow 启动时自动保存
       - content 字段存储用户的原始输入
       - metadata 中包含 executionId，用于关联工作流执行
    
    2. **AI 响应消息 (ASSISTANT)**
       - 在 workflow 启动时初始化为 PENDING 状态
       - 在 workflow 执行完成后更新为 COMPLETED 状态
       - content 字段存储 AI 的最终响应（从最后一个 LLM 节点提取）
       - 如果 workflow 执行失败，状态更新为 FAILED
       - metadata 中包含 executionId，用于关联工作流执行
    
    3. **思维链查询**
       - 消息表只存储最终的对话结果（用户输入 + AI 响应）
       - 要查看 AI 的思考过程和中间步骤，请使用：
         `GET /api/workflow/execution/{executionId}/logs`
       - executionId 可从消息的 metadata 字段中获取
    
    4. **数据持久化**
       - 所有消息持久化到 MySQL 的 messages 表
       - 支持分页查询，按创建时间升序排序
       - 消息与 workflow 执行通过 executionId 关联

### /conversations/{conversationId}
    功能： 删除会话
    场景： 删除指定会话及其所有消息
    请求方式： DELETE
    入参： conversationId (PathVariable，会话ID)
    返回值： 无

---

## /api/knowledge （知识库管理）

### /datasets
    功能： 创建知识库
    场景： 用户创建新的知识库
    请求方式： POST
    请求头： Authorization: Bearer {token}
    入参： {
        name: 知识库名称（必填）
        description: 描述（可选）
        agentId: 绑定的智能体ID（可选）
    }
    返回值: {
        datasetId: 知识库ID
        name: 名称
        description: 描述
        userId: 用户ID
        agentId: 智能体ID
        documentCount: 文档数量
        totalChunks: 总分块数
        createdAt: 创建时间
        updatedAt: 更新时间
    }

### /datasets
    功能： 查询知识库列表
    场景： 获取当前用户的所有知识库
    请求方式： GET
    请求头： Authorization: Bearer {token}
    入参： 无
    返回值: [
        {
            datasetId: 知识库ID
            name: 名称
            description: 描述
            documentCount: 文档数量
            totalChunks: 总分块数
        }
    ]

### /datasets/{id}
    功能： 查询知识库详情
    场景： 获取指定知识库的详细信息
    请求方式： GET
    入参： id (PathVariable，知识库ID)
    返回值: {
        datasetId: 知识库ID
        name: 名称
        description: 描述
        userId: 用户ID
        agentId: 智能体ID
        documentCount: 文档数量
        totalChunks: 总分块数
        createdAt: 创建时间
        updatedAt: 更新时间
    }

### /datasets/{id}
    功能： 删除知识库
    场景： 删除指定知识库及其所有文档
    请求方式： DELETE
    入参： id (PathVariable，知识库ID)
    返回值： 无

### /documents
    功能： 上传文档
    场景： 向知识库上传文档进行向量化处理
    请求方式： POST
    入参： 
        file: 文件 (MultipartFile)
        datasetId: 知识库ID (Query Param)
        chunkSize: 分块大小，默认500 (Query Param)
        chunkOverlap: 分块重叠，默认50 (Query Param)
    返回值: {
        documentId: 文档ID
        datasetId: 知识库ID
        filename: 文件名
        fileUrl: 文件URL
        fileSize: 文件大小
        contentType: 文件类型
        status: 状态 (PENDING/PROCESSING/COMPLETED/FAILED)
        totalChunks: 总分块数
        processedChunks: 已处理分块数
        uploadedAt: 上传时间
    }

### /documents
    功能： 查询文档列表
    场景： 获取知识库中的文档列表
    请求方式： GET
    入参： 
        datasetId: 知识库ID (Query Param，必填)
        page: 页码，默认0 (Query Param)
        size: 每页数量，默认20 (Query Param)
    返回值: [
        {
            documentId: 文档ID
            datasetId: 知识库ID
            filename: 文件名
            status: 状态
            totalChunks: 总分块数
            processedChunks: 已处理分块数
            uploadedAt: 上传时间
        }
    ]

### /documents/{id}
    功能： 查询文档详情
    场景： 获取指定文档的详细信息
    请求方式： GET
    入参： id (PathVariable，文档ID)
    返回值: {
        documentId: 文档ID
        datasetId: 知识库ID
        filename: 文件名
        fileUrl: 文件URL
        fileSize: 文件大小
        contentType: 文件类型
        status: 状态
        totalChunks: 总分块数
        processedChunks: 已处理分块数
        errorMessage: 错误信息
        uploadedAt: 上传时间
        completedAt: 完成时间
    }

### /documents/{id}
    功能： 删除文档
    场景： 删除指定文档及其向量数据
    请求方式： DELETE
    入参： id (PathVariable，文档ID)
    返回值： 无

### /search
    功能： 知识检索
    场景： 在知识库中进行语义相似度检索
    请求方式： POST
    入参： {
        datasetId: 知识库ID（必填）
        query: 查询内容（必填）
        topK: 返回结果数量，默认5
    }
    返回值: [
        {
            content: 匹配内容
            score: 相似度分数
            documentId: 文档ID
            filename: 文件名
            chunkIndex: 分块索引
        }
    ]

---

## /api/meta （元数据查询）

### /node-templates
    功能： 获取节点模板列表
    场景： 前端画布获取可用的节点模板
    请求方式： GET
    入参： 无
    返回值: [
        {
            id: 模板ID
            name: 模板名称
            type: 节点类型
            description: 描述
            icon: 图标
            defaultConfig: 默认配置
        }
    ]

### /{executionId}/logs
    功能： 获取工作流执行日志（思维链）
    场景： 查看 AI 的思考过程和每个节点的执行详情
    请求方式： GET
    入参： executionId (PathVariable，执行ID)
    返回值: [
        {
            id: 日志ID
            executionId: 执行ID
            nodeId: 节点ID
            nodeName: 节点名称
            nodeType: 节点类型 (LLM/HTTP/CONDITION/TOOL等)
            renderMode: 渲染模式 (MESSAGE/HIDDEN/THOUGHT)
            status: 执行状态 (0:Running, 1:Success, 2:Failed)
            statusText: 状态描述 (Running/Success/Failed)
            inputs: 输入参数 (JSON对象)
            outputs: 输出结果 (JSON对象)
            errorMessage: 错误信息（如果失败）
            startTime: 开始时间
            endTime: 结束时间
            durationMs: 执行耗时（毫秒）
        }
    ]
    
    **使用说明：**
    
    1. **获取 executionId**
       - 从消息的 metadata 字段中获取
       - 或从 workflow 执行响应中获取
    
    2. **渲染模式 (renderMode)**
       - MESSAGE: 显示为对话消息（用户可见）
       - THOUGHT: 显示为思考过程（可折叠展示）
       - HIDDEN: 隐藏节点（不显示给用户）
    
    3. **节点类型 (nodeType)**
       - LLM: 大语言模型节点
       - HTTP: HTTP 请求节点
       - CONDITION: 条件分支节点
       - TOOL: MCP 工具节点
       - START: 开始节点
       - END: 结束节点
    
    4. **数据结构**
       - inputs/outputs 为 JSON 对象，包含节点的完整输入输出
       - 按节点执行时间升序排序
       - 包含所有中间步骤，用于可视化展示思维链
    
    5. **前端展示建议**
       - 根据 renderMode 决定是否显示
       - 使用时间线或流程图展示节点执行顺序
       - 支持展开/折叠查看详细的 inputs/outputs
       - 使用不同颜色标识 Success/Failed 状态

---

## /api/workflow/execution （工作流执行）

### /start
    功能： 启动工作流执行
    场景： 启动智能体工作流并返回SSE流式响应
    请求方式： POST
    入参： {
        agentId: 智能体ID（必填）
        userId: 用户ID（必填）
        conversationId: 会话ID（可选）
        versionId: 版本ID（可选）
        inputs: 输入参数 (Map)
        mode: 执行模式 (STANDARD/DEBUG)，默认STANDARD
    }
    返回值: SSE流 (text/event-stream)
        事件类型：
        - node_start: 节点开始执行
        - node_output: 节点输出（流式）
        - node_complete: 节点执行完成
        - execution_complete: 工作流执行完成
        - error: 执行错误

### /stop
    功能： 停止/取消执行
    场景： 终止正在运行的工作流
    请求方式： POST
    入参： {
        executionId: 执行ID（必填）
    }
    返回值： 无

### /{executionId}
    功能： 获取执行详情
    场景： 调试时获取执行的完整信息
    请求方式： GET
    入参： executionId (PathVariable，执行ID)
    返回值: {
        executionId: 执行ID
        agentId: 智能体ID
        conversationId: 会话ID
        status: 执行状态
        startTime: 开始时间
        endTime: 结束时间
        nodeStatuses: 节点状态映射 (Map<NodeId, Status>)
    }

### /{executionId}/nodes/{nodeId}/log
    功能： 获取节点执行日志
    场景： 调试时查看特定节点的执行日志
    请求方式： GET
    入参： 
        executionId: 执行ID (PathVariable)
        nodeId: 节点ID (PathVariable)
    返回值: {
        nodeId: 节点ID
        logs: 日志列表
        inputs: 输入参数
        outputs: 输出结果
    }

### /history/{conversationId}
    功能： 获取会话执行历史
    场景： 获取某个会话的所有工作流执行记录
    请求方式： GET
    入参： conversationId (PathVariable，会话ID)
    返回值: [
        {
            executionId: "exec-uuid-...",
            agentId: 1001,
            userId: 101,
            conversationId: "conv-uuid-...",
            status: "SUCCEEDED",
            startTime: "2023-10-27T10:00:00",
            endTime: "2023-10-27T10:00:05",
            nodeStatuses: {
                "node_1": "SUCCEEDED",
                "node_2": "SKIPPED"
            }
        }
    ]

### /{executionId}/context
    功能： 获取执行上下文快照
    场景： 调试时获取LTM、STM、执行日志和全局变量
    请求方式： GET
    入参： executionId (PathVariable，执行ID)
    返回值: {
        ltm: 长期记忆
        stm: 短期记忆
        logs: 执行日志
        globalVariables: 全局变量
    }

---

## /api/workflow/human-review （人工审核）

### /pending
    功能： 获取待审核列表
    场景： 获取所有等待人工审核的任务
    请求方式： GET
    入参： 无
    返回值: [
        {
            executionId: 执行ID
            nodeId: 节点ID
            nodeName: 节点名称
            agentName: 智能体名称
            triggerPhase: 触发阶段 (BEFORE_EXECUTION/AFTER_EXECUTION)
            pausedAt: 暂停时间
        }
    ]

### /{executionId}
    功能： 获取审核详情
    场景： 获取待审核任务的详细信息
    请求方式： GET
    入参： executionId (PathVariable，执行ID)
    返回值: {
        executionId: 执行ID
        nodeId: 节点ID
        nodeName: 节点名称
        triggerPhase: 触发阶段
        contextData: 上下文数据（输入或输出）
        config: {
            prompt: 审核提示
            editableFields: 可编辑字段列表
        }
    }

### /resume
    功能： 提交审核（恢复执行）
    场景： 人工审核通过后恢复工作流执行
    请求方式： POST
    入参： {
        executionId: 执行ID（必填）
        nodeId: 节点ID（必填）
        edits: 修改内容 (Map，可选)
        comment: 审核备注（可选）
    }
    返回值: {
        success: 是否成功
        message: 消息
    }

### /history
    功能： 审核历史
    场景： 查询已完成的审核记录
    请求方式： GET
    入参： 
        userId: 用户ID (Query Param，可选)
        page: 页码，默认0
        size: 每页数量，默认20
    返回值: 分页的审核记录列表