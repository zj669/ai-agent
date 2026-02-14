# 工作流编排界面重设计方案

## 📋 设计目标

基于参考截图,实现类似 Dify/Coze 的现代化工作流编排界面,提升用户体验和视觉吸引力。

## 🎨 核心设计变更

### 1. 节点卡片重设计

#### 当前问题
- 节点过小(188-260px),信息密度不足
- 缺少内部结构化展示
- 配置信息不可见,需要点击才能查看

#### 目标设计
- **尺寸**: 宽度 320-400px,高度自适应内容
- **结构**: 
  - 顶部: 节点类型标签 + 图标
  - 中部: 配置预览区(表格/表单)
  - 底部: 操作按钮区
- **样式**:
  - 白色背景,圆角 12px
  - 边框 1px solid #e5e7eb
  - 悬停时阴影加深
  - 选中时蓝色边框

#### 节点类型配置预览

**LLM 节点**:
```
┌─────────────────────────────┐
│ 🤖 LLM 节点                  │
├─────────────────────────────┤
│ 模型: GPT-4o                │
│ 温度: 0.7                   │
│ 最大令牌: 2000              │
│ 系统提示: [预览前50字...]   │
├─────────────────────────────┤
│ [+ 添加输入] [⚙️ 配置]      │
└─────────────────────────────┘
```

**HTTP 节点**:
```
┌─────────────────────────────┐
│ 🌐 HTTP 请求                │
├─────────────────────────────┤
│ 方法: POST                  │
│ URL: https://api.example... │
│ 请求头: 2 项                │
│ 请求体: JSON                │
├─────────────────────────────┤
│ [测试] [配置]               │
└─────────────────────────────┘
```

**条件节点**:
```
┌─────────────────────────────┐
│ 🔀 条件分支                 │
├─────────────────────────────┤
│ 模式: 表达式                │
│ 分支数: 3                   │
│ ├─ 条件1: score > 80       │
│ ├─ 条件2: score > 60       │
│ └─ 默认分支                 │
├─────────────────────────────┤
│ [+ 添加分支] [配置]         │
└─────────────────────────────┘
```

### 2. 连接线优化

#### 当前问题
- 连接线样式较硬
- 缺少流动感

#### 目标设计
- **线型**: 平滑贝塞尔曲线
- **颜色**: #3b82f6 (蓝色)
- **宽度**: 2px
- **箭头**: 实心三角形
- **动画**: 悬停时加粗到 3px
- **选中**: 添加虚线流动动画

### 3. 画布背景

#### 当前设计
```css
background: 
  linear-gradient(...),
  repeating-linear-gradient(...); /* 网格 */
```

#### 目标设计
```css
background: #f5f7fa; /* 纯色浅灰 */
/* 或者极淡的点阵 */
background: 
  radial-gradient(circle, #e5e7eb 1px, transparent 1px);
background-size: 20px 20px;
```

### 4. 左侧面板优化

#### 当前: 编排配置面板
- 显示工作流名称、描述
- 显示节点/连接统计

#### 目标: 节点库面板
- 分类展示所有节点类型
- 支持搜索过滤
- 拖拽添加到画布
- 显示节点描述和使用提示

### 5. 右侧面板优化

#### 当前: 节点快捷添加栏
- 垂直排列节点图标
- 点击添加到画布中心

#### 目标: 节点属性面板
- 选中节点时显示详细配置
- 表单式编辑
- 实时预览
- 支持折叠分组

### 6. 顶部工具栏增强

#### 新增功能
- 面包屑导航
- 版本历史
- 协作者头像
- 发布/草稿状态切换

## 🛠️ 技术实现方案

### Phase 1: 节点组件重构

#### 1.1 创建新的节点组件结构

```typescript
// WorkflowNodeLarge.tsx
interface NodeCardProps {
  data: {
    nodeType: NodeType;
    label: string;
    config: NodeConfig;
    status?: NodeExecutionStatus;
  };
  selected: boolean;
}

// 节点内部结构
<div className="workflow-node-large">
  {/* 头部 */}
  <div className="node-header">
    <Icon />
    <span>{nodeType}</span>
    <StatusBadge />
  </div>
  
  {/* 配置预览区 */}
  <div className="node-content">
    <ConfigPreview config={config} nodeType={nodeType} />
  </div>
  
  {/* 操作区 */}
  <div className="node-actions">
    <Button>测试</Button>
    <Button>配置</Button>
  </div>
  
  {/* Handles */}
  <Handle type="target" position="left" />
  <Handle type="source" position="right" />
</div>
```

#### 1.2 配置预览组件

```typescript
// NodeConfigPreview.tsx
function LLMConfigPreview({ config }) {
  return (
    <div className="config-preview">
      <div className="config-row">
        <span className="label">模型</span>
        <span className="value">{config.model}</span>
      </div>
      <div className="config-row">
        <span className="label">温度</span>
        <span className="value">{config.temperature}</span>
      </div>
      <div className="config-row">
        <span className="label">系统提示</span>
        <span className="value truncate">{config.systemPrompt}</span>
      </div>
    </div>
  );
}
```

### Phase 2: 连接线样式优化

```css
/* 平滑贝塞尔曲线 */
.react-flow__edge-path {
  stroke: #3b82f6;
  stroke-width: 2;
  fill: none;
  transition: all 0.2s ease;
}

.react-flow__edge:hover .react-flow__edge-path {
  stroke-width: 3;
  filter: drop-shadow(0 0 4px rgba(59, 130, 246, 0.4));
}

/* 流动动画 */
.react-flow__edge.animated .react-flow__edge-path {
  stroke-dasharray: 5;
  animation: dash 0.5s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}
```

### Phase 3: 布局调整

#### 3.1 左侧面板: 节点库

```typescript
// NodeLibraryPanel.tsx
<div className="node-library-panel">
  <div className="panel-header">
    <h3>节点库</h3>
    <Input.Search placeholder="搜索节点..." />
  </div>
  
  <div className="node-categories">
    {NODE_CATEGORIES.map(category => (
      <Collapse.Panel key={category.key} header={category.label}>
        {category.nodes.map(node => (
          <NodeLibraryCard
            key={node.type}
            node={node}
            onDragStart={handleDragStart}
            onClick={handleAddNode}
          />
        ))}
      </Collapse.Panel>
    ))}
  </div>
</div>
```

#### 3.2 右侧面板: 属性编辑器

```typescript
// NodePropertiesEditor.tsx
<div className="node-properties-editor">
  <div className="editor-header">
    <h3>{selectedNode.data.label}</h3>
    <Button icon={<X />} onClick={onClose} />
  </div>
  
  <Tabs>
    <TabPane tab="基础配置" key="basic">
      <Form>
        <Form.Item label="节点名称">
          <Input value={label} onChange={...} />
        </Form.Item>
        {/* 节点特定配置 */}
        <NodeSpecificConfig nodeType={nodeType} config={config} />
      </Form>
    </TabPane>
    
    <TabPane tab="输入输出" key="io">
      <InputOutputConfig inputs={inputs} outputs={outputs} />
    </TabPane>
    
    <TabPane tab="高级选项" key="advanced">
      <AdvancedConfig config={config} />
    </TabPane>
  </Tabs>
</div>
```

### Phase 4: 画布背景

```css
.workflow-canvas-wrap {
  background: #f5f7fa;
  /* 或者极淡点阵 */
  background-image: 
    radial-gradient(circle, #e5e7eb 1px, transparent 1px);
  background-size: 20px 20px;
}
```

## 📐 样式规范

### 节点尺寸
- 最小宽度: 320px
- 最大宽度: 400px
- 最小高度: 180px
- 内边距: 16px
- 圆角: 12px

### 颜色系统
- 主色: #3b82f6 (蓝色)
- 背景: #f5f7fa (浅灰)
- 边框: #e5e7eb (灰色)
- 文字主色: #1f2937
- 文字次色: #6b7280

### 间距系统
- xs: 4px
- sm: 8px
- md: 16px
- lg: 24px
- xl: 32px

### 阴影系统
- 默认: 0 1px 3px rgba(0,0,0,0.1)
- 悬停: 0 4px 12px rgba(0,0,0,0.15)
- 选中: 0 0 0 2px #3b82f6

## 🚀 实施步骤

### Step 1: 创建新组件 (1-2天)
- [ ] WorkflowNodeLarge.tsx
- [ ] NodeConfigPreview.tsx (各节点类型)
- [ ] NodeLibraryPanel.tsx
- [ ] NodePropertiesEditor.tsx

### Step 2: 样式重构 (1天)
- [ ] workflow-large-nodes.css
- [ ] 更新 workflow-enhanced.css
- [ ] 调整画布背景

### Step 3: 布局调整 (1天)
- [ ] 重构 WorkflowEditorPage 布局
- [ ] 左侧改为节点库
- [ ] 右侧改为属性编辑器

### Step 4: 交互优化 (1天)
- [ ] 拖拽体验优化
- [ ] 节点选中/编辑流程
- [ ] 连接线交互

### Step 5: 测试与优化 (1天)
- [ ] 功能测试
- [ ] 性能优化
- [ ] 响应式适配

## 📊 预期效果

### 视觉提升
- ✅ 节点信息密度提升 3-4 倍
- ✅ 配置可见性提升,减少点击次数
- ✅ 整体视觉更现代、专业

### 交互提升
- ✅ 拖拽体验更流畅
- ✅ 编辑流程更直观
- ✅ 减少面板切换次数

### 开发效率
- ✅ 组件结构更清晰
- ✅ 样式管理更规范
- ✅ 易于扩展新节点类型

## 🎯 成功指标

1. 节点配置可见性: 从 0% → 80%
2. 平均编辑步骤: 从 5 步 → 3 步
3. 用户满意度: 目标 > 4.5/5
4. 页面加载时间: < 2s
5. 交互响应时间: < 100ms

## 📝 注意事项

1. **向后兼容**: 保留旧节点数据结构,仅改变展示层
2. **性能优化**: 大量节点时使用虚拟化渲染
3. **响应式**: 确保在不同屏幕尺寸下可用
4. **无障碍**: 支持键盘导航和屏幕阅读器
5. **国际化**: 预留多语言支持接口

## 🔗 参考资源

- Dify Workflow: https://dify.ai
- Coze Workflow: https://coze.com
- ReactFlow 文档: https://reactflow.dev
- Ant Design: https://ant.design
