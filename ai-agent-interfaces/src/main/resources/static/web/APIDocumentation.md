# API Documentation

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
    场景： 获取某个会话的所有消息
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
            thoughtProcess: 思考过程列表
            citations: 引用列表
            status: 消息状态
            createdAt: 创建时间
            metadata: 元数据
        }
    ]

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