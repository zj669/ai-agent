# WorkflowNodeLarge Blueprint

## 职责契约
- **做什么**: 渲染大尺寸工作流节点(320-400px),内嵌配置预览,提供快捷操作
- **不做什么**: 不处理节点编辑逻辑、不管理节点数据、不处理拖拽逻辑(由 ReactFlow 负责)

## 接口摘要 (Props)

| 名称 | 类型 | 说明 | 必需 |
|------|------|------|------|
| data | WorkflowNodeData | 节点数据 | 是 |
| data.label | string | 节点名称 | 是 |
| data.nodeType | NodeType | 节点类型 | 是 |
| data.config | NodeConfig | 节点配置 | 否 |
| data.status | NodeExecutionStatus | 执行状态 | 否 |
| selected | boolean | 是否选中 | 是 |

## 依赖拓扑
- **上游**: WorkflowEditorPage (通过 ReactFlow)
- **下游**: 
  - ConfigPreview (配置预览子组件)
  - Ant Design (Button, Tag, Tooltip)
  - Lucide Icons
  - ReactFlow Handle

## 组件结构

```tsx
<div className="workflow-node-large">
  {/* 输入 Handle */}
  <Handle type="target" position="left" />
  
  {/* 节点头部 */}
  <div className="node-header">
    <Icon /> {/* 节点类型图标 */}
    <div>
      <div>{nodeType}</div>
      <div>{label}</div>
    </div>
    <StatusIcon /> {/* 执行状态图标 */}
  </div>
  
  {/* 配置预览区 */}
  <div className="node-content">
    <ConfigPreview nodeType={nodeType} config={config} />
  </div>
  
  {/* 操作按钮区 */}
  <div className="node-actions">
    <Button>测试</Button>
    <Button>配置</Button>
  </div>
  
  {/* 执行状态条 */}
  {status && <div className="status-bar" />}
  
  {/* 输出 Handle */}
  <Handle type="source" position="right" />
</div>
```

## 节点类型配置

### NODE_CONFIG 映射
```typescript
{
  [NodeType.START]: {
    icon: PlayCircle,
    label: '开始',
    color: '#10b981',
    bgColor: '#ecfdf5',
    borderColor: '#a7f3d0'
  },
  [NodeType.LLM]: {
    icon: MessageSquare,
    label: 'LLM',
    color: '#8b5cf6',
    bgColor: '#faf5ff',
    borderColor: '#e9d5ff'
  },
  // ... 其他类型
}
```

### STATUS_CONFIG 映射
```typescript
{
  [NodeExecutionStatus.RUNNING]: {
    icon: Loader2,
    color: '#3b82f6',
    label: '运行中',
    animate: true
  },
  [NodeExecutionStatus.SUCCEEDED]: {
    icon: CheckCircle2,
    color: '#10b981',
    label: '成功',
    animate: false
  },
  // ... 其他状态
}
```

## ConfigPreview 子组件

### 职责
- 根据节点类型展示配置摘要
- 格式化配置数据为可读文本
- 处理长文本截断

### 支持的节点类型

#### LLM 节点
```
模型          GPT-4o
温度          0.7
最大令牌      2000
系统提示      你是一个专业的AI助手...
```

#### HTTP 节点
```
方法          POST
URL           https://api.example.com/data
请求头        2 项
```

#### 条件节点
```
模式          表达式
分支数        3
├─ 条件1: score > 80
├─ 条件2: score > 60
└─ 默认分支
```

#### 工具节点
```
工具名称      calculator
描述          执行数学计算
```

## Handle 布局规则

### 普通节点
- 输入 Handle: 左侧中央
- 输出 Handle: 右侧中央

### 条件节点 (多分支)
- 输入 Handle: 左侧中央
- 输出 Handle: 右侧均匀分布
  - 2 分支: 33%, 66%
  - 3 分支: 25%, 50%, 75%
  - N 分支: 按比例分布

## 状态可视化

### 运行中
- 节点整体脉冲动画
- 状态图标旋转动画
- 底部蓝色状态条

### 成功
- 绿色状态图标
- 底部绿色状态条

### 失败
- 红色状态图标
- 底部红色状态条

## 交互行为

### 悬停 (Hover)
- 节点轻微上浮 (translateY(-1px))
- 阴影加深
- Handle 放大

### 选中 (Selected)
- 蓝色边框 (border-blue-400)
- 外围光晕 (ring-2 ring-blue-200)
- z-index 提升

### 拖拽 (Dragging)
- 由 ReactFlow 处理
- 节点跟随鼠标移动
- 显示连接预览

## 尺寸规范

### 节点尺寸
- 最小宽度: 320px
- 最大宽度: 400px
- 最小高度: 180px
- 高度: 自适应内容

### 内部间距
- 头部: padding 16px
- 内容区: padding 16px
- 操作区: padding 10px 16px

### 圆角
- 节点外框: 12px
- 图标容器: 8px
- 按钮: 6px

## 性能优化

### React.memo
- 使用 memo 包裹组件
- 避免不必要的重渲染
- 依赖 data 和 selected 变化

### useMemo
- nodeConfig 计算结果缓存
- statusConfig 计算结果缓存

### CSS 优化
- 使用 transform 代替 top/left
- 启用 GPU 加速 (translateZ(0))
- 避免复杂的 box-shadow

## 设计约束

### 配置预览约束
- 最大高度: 200px
- 超出滚动显示
- 长文本使用 line-clamp-2 截断

### 操作按钮约束
- 最多显示 2-3 个按钮
- 使用小尺寸 (size="small")
- 图标 + 文字组合

### 样式隔离
- 使用 .workflow-node-large 类名
- 不影响 WorkflowNode (紧凑节点)
- 独立的 CSS 文件

## 错误处理

### 配置缺失
- 显示"未配置"提示
- 不阻塞节点渲染

### 类型未知
- 使用默认配置
- 记录警告日志

## 变更日志
- [2026-02-13] 创建 WorkflowNodeLarge 蓝图
- [2026-02-13] 定义节点结构、配置预览、Handle 布局
- [2026-02-13] 明确尺寸规范和性能优化策略
