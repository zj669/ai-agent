# 大尺寸节点实现指南

## 📦 已创建的文件

1. **WorkflowNodeLarge.tsx** - 大尺寸节点组件
2. **workflow-large-nodes.css** - 大节点专用样式
3. **NodeSizeToggle.tsx** - 节点尺寸切换组件
4. **WORKFLOW_REDESIGN_PLAN.md** - 完整重设计方案

## 🚀 快速集成

### Step 1: 在 WorkflowEditorPage 中添加切换功能

```typescript
// ai-agent-foward/src/pages/WorkflowEditorPage.tsx

import { useState } from 'react';
import { nodeTypes } from '../components/WorkflowNode'; // 原有小节点
import { largeNodeTypes } from '../components/WorkflowNodeLarge'; // 新的大节点
import { NodeSizeToggle } from '../components/NodeSizeToggle';

function WorkflowEditorInner() {
  // 添加状态
  const [useLargeNodes, setUseLargeNodes] = useState(true); // 默认使用大节点
  
  // 选择节点类型
  const currentNodeTypes = useLargeNodes ? largeNodeTypes : nodeTypes;
  
  return (
    <div className="workflow-editor-shell">
      {/* 顶部工具栏 */}
      <div className="workflow-toolbar">
        {/* 现有工具栏内容 */}
        
        {/* 添加节点尺寸切换 */}
        <NodeSizeToggle
          useLargeNodes={useLargeNodes}
          onChange={setUseLargeNodes}
        />
      </div>
      
      {/* ReactFlow 画布 */}
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={currentNodeTypes} // 使用动态节点类型
        // ... 其他配置
      />
    </div>
  );
}
```

### Step 2: 调整画布背景(可选)

如果想要更简洁的背景,可以修改画布样式:

```typescript
// 在 ReactFlow 组件中
<ReactFlow
  // ... 其他配置
  className={useLargeNodes ? "workflow-canvas-simple" : "workflow-canvas"}
>
  <Background 
    variant={useLargeNodes ? BackgroundVariant.Dots : BackgroundVariant.Dots}
    gap={useLargeNodes ? 20 : 22}
    size={useLargeNodes ? 1 : 1.2}
    color={useLargeNodes ? "#e5e7eb" : "#cbd5e1"}
  />
</ReactFlow>
```

### Step 3: 测试

```bash
cd ai-agent-foward
npm run dev
```

访问工作流编辑器页面,应该能看到:
- 顶部工具栏有节点尺寸切换开关
- 默认显示大尺寸节点
- 节点内部显示配置预览
- 可以切换回小节点

## 🎨 大节点特性

### 1. 配置可见性

**LLM 节点示例**:
```
┌─────────────────────────────────┐
│ 🤖 LLM                          │
│ GPT-4 对话节点                  │
├─────────────────────────────────┤
│ 模型          GPT-4o            │
│ 温度          0.7               │
│ 最大令牌      2000              │
│ 系统提示                        │
│ 你是一个专业的AI助手...         │
├─────────────────────────────────┤
│ [测试] [配置]                   │
└─────────────────────────────────┘
```

### 2. 节点尺寸

- 最小宽度: 320px
- 最大宽度: 400px
- 高度: 自适应内容(最小 180px)

### 3. 交互增强

- 悬停时轻微上浮
- 选中时蓝色边框 + 阴影
- Handle 悬停放大
- 连接线悬停加粗

### 4. 状态可视化

- 运行中: 蓝色脉冲动画
- 成功: 绿色状态条
- 失败: 红色状态条

## 📐 样式定制

### 修改节点颜色

编辑 `WorkflowNodeLarge.tsx` 中的 `NODE_CONFIG`:

```typescript
const NODE_CONFIG = {
  [NodeType.LLM]: {
    icon: MessageSquare,
    label: 'LLM',
    color: '#8b5cf6', // 主色
    bgColor: '#faf5ff', // 背景色
    borderColor: '#e9d5ff' // 边框色
  },
  // ... 其他节点类型
};
```

### 修改节点尺寸

编辑 `workflow-large-nodes.css`:

```css
.workflow-node-large {
  min-width: 320px; /* 调整最小宽度 */
  max-width: 400px; /* 调整最大宽度 */
}
```

### 修改连接线样式

编辑 `workflow-large-nodes.css`:

```css
.react-flow__edge-path {
  stroke: #3b82f6; /* 连接线颜色 */
  stroke-width: 2; /* 连接线宽度 */
}
```

## 🔧 高级定制

### 添加新的配置预览

在 `WorkflowNodeLarge.tsx` 的 `ConfigPreview` 组件中添加新的节点类型:

```typescript
function ConfigPreview({ nodeType, config }) {
  // ... 现有代码
  
  switch (nodeType) {
    // ... 现有 case
    
    case NodeType.YOUR_NEW_TYPE:
      return (
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">自定义字段</span>
            <span className="text-gray-800">{config.customField}</span>
          </div>
        </div>
      );
  }
}
```

### 添加节点操作按钮

在节点底部操作区添加自定义按钮:

```typescript
{/* 操作按钮区 */}
<div className="px-4 py-2.5 border-t border-gray-100 flex items-center gap-2">
  <Button size="small" icon={<Play />}>测试</Button>
  <Button size="small" icon={<Settings />}>配置</Button>
  
  {/* 添加自定义按钮 */}
  <Button 
    size="small" 
    icon={<YourIcon />}
    onClick={handleCustomAction}
  >
    自定义操作
  </Button>
</div>
```

## 🎯 最佳实践

### 1. 性能优化

- 大节点会增加 DOM 复杂度,建议在节点数量 > 50 时提示用户切换到小节点
- 使用 React.memo 避免不必要的重渲染
- 配置预览区域使用虚拟滚动(如果内容很长)

### 2. 用户体验

- 提供节点尺寸切换选项,让用户自主选择
- 在大节点模式下,适当增加画布缩放比例
- 保存用户的节点尺寸偏好到 localStorage

### 3. 响应式设计

- 在小屏幕(<1440px)自动切换到小节点
- 或者调整大节点的最小/最大宽度

```typescript
useEffect(() => {
  const handleResize = () => {
    if (window.innerWidth < 1440 && useLargeNodes) {
      setUseLargeNodes(false);
      message.info('屏幕较小,已自动切换到紧凑节点');
    }
  };
  
  window.addEventListener('resize', handleResize);
  return () => window.removeEventListener('resize', handleResize);
}, [useLargeNodes]);
```

## 📊 对比

| 特性 | 小节点 | 大节点 |
|------|--------|--------|
| 宽度 | 188-260px | 320-400px |
| 配置可见性 | 需要点击查看 | 直接显示 |
| 信息密度 | 低 | 高 |
| 适用场景 | 复杂流程(>50节点) | 中小流程(<30节点) |
| 编辑效率 | 需要多次点击 | 一目了然 |

## 🐛 已知问题

1. **条件节点多分支 Handle 位置**: 当分支数量 > 5 时,Handle 可能重叠
   - 解决方案: 限制最大分支数为 5,或调整 Handle 布局算法

2. **长文本溢出**: 系统提示等长文本可能溢出
   - 解决方案: 使用 `line-clamp-2` 限制行数,悬停显示完整内容

3. **性能**: 大量大节点时可能卡顿
   - 解决方案: 节点数 > 50 时自动切换到小节点,或使用虚拟化

## 🔄 后续优化

1. **节点库面板**: 替换左侧编排配置面板
2. **属性编辑器**: 替换右侧快捷添加栏
3. **拖拽优化**: 改进拖拽预览和放置体验
4. **连接线标签**: 在连接线上显示条件标签
5. **节点分组**: 支持节点分组和折叠
6. **快捷键**: 添加键盘快捷键支持

## 📝 反馈

如有问题或建议,请在项目中创建 Issue 或联系开发团队。
