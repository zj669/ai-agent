# 工作流编排模块

## 概述

工作流编排模块提供了可视化的工作流设计和执行功能,基于 @xyflow/react 实现。

## 功能特性

### 1. 可视化编辑器
- ✅ 拖拽添加节点
- ✅ 节点连线
- ✅ 画布缩放和平移
- ✅ 小地图导航
- ✅ 节点属性编辑

### 2. 节点类型
- **START**: 开始节点 (绿色)
- **END**: 结束节点 (红色)
- **LLM**: 大模型调用节点 (蓝色)
- **HTTP**: HTTP 请求节点 (紫色)
- **CONDITION**: 条件分支节点 (黄色)
- **TOOL**: 工具调用节点 (靛蓝色)

### 3. 实时执行
- ✅ SSE 流式推送执行进度
- ✅ 节点状态实时更新
- ✅ 执行日志查看
- ✅ 执行控制 (启动/停止)

### 4. 工作流管理
- ✅ 保存工作流
- ✅ 导入/导出 JSON
- ✅ 工作流列表
- ✅ 工作流复制

## 文件结构

```
ai-agent-foward/src/
├── types/
│   └── workflow.ts                    # 工作流类型定义
├── services/
│   └── workflowService.ts             # 工作流 API 服务
├── hooks/
│   └── useWorkflowEditor.ts           # 工作流编辑器 Hook
├── components/
│   ├── WorkflowNode.tsx               # 节点组件
│   ├── NodePanel.tsx                  # 节点面板
│   ├── NodePropertiesPanel.tsx        # 属性编辑面板
│   └── ExecutionLogPanel.tsx          # 执行日志面板
└── pages/
    ├── WorkflowListPage.tsx           # 工作流列表页
    └── WorkflowEditorPage.tsx         # 工作流编辑器页
```

## 使用方法

### 1. 创建工作流

1. 访问 `/workflows` 页面
2. 点击"创建工作流"按钮
3. 输入工作流名称和描述
4. 点击"编辑工作流"进入编辑器

### 2. 编辑工作流

1. 从左侧节点面板选择节点类型
2. 点击节点类型按钮,节点会添加到画布中央
3. 拖拽节点到合适位置
4. 点击节点的输出点,拖拽到另一个节点的输入点建立连接
5. 点击节点打开属性面板,配置节点参数
6. 点击"保存"按钮保存工作流

### 3. 执行工作流

1. 在编辑器页面点击"执行"按钮
2. 输入会话 ID 和输入参数
3. 选择执行模式 (STANDARD/DEBUG/DRY_RUN)
4. 点击"开始执行"
5. 查看实时执行日志和节点状态

### 4. 节点配置

#### LLM 节点
- **模型名称**: 例如 gpt-4
- **系统提示词**: 系统级别的提示词
- **温度**: 0-2 之间的浮点数
- **最大 Token**: 最大生成 Token 数

#### HTTP 节点
- **请求方法**: GET/POST/PUT/DELETE
- **URL**: 请求地址
- **请求头**: JSON 格式的请求头
- **请求体**: 请求体内容

#### CONDITION 节点
- **条件模式**: EXPRESSION (SpEL 表达式) 或 LLM (语义理解)
- **条件表达式**: SpEL 表达式或 LLM 提示词

#### TOOL 节点
- **工具名称**: 工具标识
- **工具参数**: JSON 格式的参数

### 5. 高级配置

所有节点都支持以下高级配置:
- **超时时间**: 节点执行超时时间 (毫秒)
- **需要人工审核**: 是否需要人工审核
- **最大重试次数**: 失败后的重试次数

## API 接口

### 启动执行
```typescript
POST /api/workflow/execution/start
Content-Type: application/json

{
  "agentId": 1,
  "userId": 100,
  "conversationId": "conv_123",
  "versionId": 2,
  "inputs": {
    "query": "你好"
  },
  "mode": "STANDARD"
}
```

### 停止执行
```typescript
POST /api/workflow/execution/stop
Content-Type: application/json

{
  "executionId": "exec_123"
}
```

### 获取执行详情
```typescript
GET /api/workflow/execution/{executionId}
```

### 获取执行日志
```typescript
GET /api/workflow/execution/{executionId}/logs
```

## SSE 事件

工作流执行通过 SSE 推送实时事件:

- **connected**: 连接建立成功
- **start**: 节点开始执行
- **update**: 节点执行进度更新
- **finish**: 节点执行完成
- **error**: 执行错误
- **ping**: 心跳事件 (每 15 秒)

## 注意事项

1. **节点连接**:
   - START 节点只能有输出,没有输入
   - END 节点只能有输入,没有输出
   - 其他节点可以有多个输入和输出

2. **条件分支**:
   - CONDITION 节点可以有多个输出边
   - 每个输出边需要配置条件表达式
   - 未选中的分支会被标记为 SKIPPED

3. **执行模式**:
   - **STANDARD**: 标准模式,正常执行
   - **DEBUG**: 调试模式,记录详细日志
   - **DRY_RUN**: 干运行模式,不实际执行

4. **性能建议**:
   - 单个工作流建议不超过 50 个节点
   - SSE 连接超时时间为 30 分钟
   - 建议设置合理的节点超时时间

## 后续优化

- [ ] 支持节点拖拽添加 (从面板拖拽到画布)
- [ ] 支持工作流模板
- [ ] 支持工作流版本管理
- [ ] 支持工作流执行历史查看
- [ ] 支持工作流性能分析
- [ ] 支持工作流调试断点
- [ ] 支持工作流变量管理
- [ ] 支持工作流权限控制

## 相关文档

- [后端接口文档](../../../docs/api/workflow.md)
- [工作流引擎架构](../../../.blueprint/domain/workflow/WorkflowEngine.md)
- [执行上下文设计](../../../.blueprint/domain/workflow/ExecutionContext.md)
- [节点执行器实现](../../../.blueprint/infrastructure/adapters/NodeExecutors.md)
