# 🚀 快速开始 - 大尺寸节点

## 一键启用大节点

只需修改一个文件即可启用大尺寸节点!

### 方式 1: 完全替换(推荐快速测试)

直接在 `WorkflowEditorPage.tsx` 中替换导入:

```typescript
// 找到这一行
import { nodeTypes } from '../components/WorkflowNode';

// 替换为
import { largeNodeTypes as nodeTypes } from '../components/WorkflowNodeLarge';
```

保存后刷新页面,立即看到大节点效果!

### 方式 2: 添加切换开关(推荐生产环境)

在 `WorkflowEditorPage.tsx` 中添加以下代码:

#### 1. 添加导入

```typescript
import { useState } from 'react'; // 如果已有则跳过
import { nodeTypes } from '../components/WorkflowNode';
import { largeNodeTypes } from '../components/WorkflowNodeLarge';
import { NodeSizeToggle } from '../components/NodeSizeToggle';
```

#### 2. 在 WorkflowEditorInner 函数开头添加状态

```typescript
function WorkflowEditorInner() {
  // ... 现有代码
  
  // 添加这一行
  const [useLargeNodes, setUseLargeNodes] = useState(true);
  
  // 添加这一行
  const currentNodeTypes = useLargeNodes ? largeNodeTypes : nodeTypes;
  
  // ... 其他代码
}
```

#### 3. 在顶部工具栏添加切换按钮

找到工具栏的"右侧"部分(通常在"运行"、"保存"按钮附近):

```typescript
{/* 右侧:运行/停止 + 保存 + 更多 */}
<div className="flex items-center gap-2">
  {/* 添加节点尺寸切换 */}
  <NodeSizeToggle
    useLargeNodes={useLargeNodes}
    onChange={setUseLargeNodes}
  />
  
  {/* 现有按钮 */}
  {!isExecuting ? (
    <Button type="primary" icon={<Play />} onClick={handleOpenExecutionModal}>
      运行
    </Button>
  ) : (
    // ...
  )}
  // ...
</div>
```

#### 4. 更新 ReactFlow 组件

找到 `<ReactFlow>` 组件,修改 `nodeTypes` 属性:

```typescript
<ReactFlow
  nodes={nodes}
  edges={edges}
  onNodesChange={onNodesChange}
  onEdgesChange={onEdgesChange}
  onConnect={onConnect}
  onNodeClick={handleNodeClick}
  onPaneClick={handlePaneClick}
  onMoveEnd={handleMoveEnd}
  nodeTypes={currentNodeTypes} // 改为动态节点类型
  edgeTypes={workflowEdgeTypes}
  // ... 其他配置
/>
```

## 🎨 效果预览

启用后你会看到:

### 小节点 vs 大节点对比

**小节点(原有)**:
```
┌──────────────┐
│ 🤖 LLM       │
│ GPT-4 对话   │
│              │
│ GPT-4o       │
└──────────────┘
```

**大节点(新增)**:
```
┌─────────────────────────────────┐
│ 🤖 LLM                          │
│ GPT-4 对话节点                  │
├─────────────────────────────────┤
│ 模型          GPT-4o            │
│ 温度          0.7               │
│ 最大令牌      2000              │
│ 系统提示                        │
│ 你是一个专业的AI助手,擅长...   │
├─────────────────────────────────┤
│ [测试] [配置]                   │
└─────────────────────────────────┘
```

## 🔍 验证效果

1. 启动开发服务器:
```bash
cd ai-agent-foward
npm run dev
```

2. 访问工作流编辑器页面

3. 检查以下特性:
   - ✅ 节点宽度明显增大(320-400px)
   - ✅ 节点内部显示配置信息
   - ✅ 底部有"测试"和"配置"按钮
   - ✅ 悬停时节点轻微上浮
   - ✅ 选中时蓝色边框高亮
   - ✅ 连接线更流畅

## 🎯 快速测试场景

### 测试 1: LLM 节点

1. 添加一个 LLM 节点
2. 点击配置,设置:
   - 模型: GPT-4o
   - 温度: 0.7
   - 系统提示: "你是一个专业的AI助手"
3. 保存后,节点应该直接显示这些配置

### 测试 2: HTTP 节点

1. 添加一个 HTTP 节点
2. 配置:
   - 方法: POST
   - URL: https://api.example.com/data
   - 添加 2 个请求头
3. 节点应该显示方法、URL 和请求头数量

### 测试 3: 条件节点

1. 添加一个条件节点
2. 添加 3 个分支
3. 节点应该显示分支列表

### 测试 4: 节点切换

如果添加了切换开关:
1. 点击工具栏的节点尺寸切换
2. 节点应该在大小尺寸间切换
3. 配置信息应该保持不变

## 🐛 常见问题

### Q: 节点没有变大?

A: 检查以下几点:
1. 确认已正确导入 `largeNodeTypes`
2. 确认 `ReactFlow` 的 `nodeTypes` 属性已更新
3. 清除浏览器缓存并刷新
4. 检查控制台是否有错误

### Q: 配置信息不显示?

A: 确保节点的 `config.properties` 有数据:
```typescript
// 检查节点数据结构
console.log(node.data.config);
```

### Q: 样式不生效?

A: 确认 `index.css` 已导入样式文件:
```css
@import './styles/workflow-large-nodes.css';
```

### Q: 切换开关不工作?

A: 检查:
1. `NodeSizeToggle` 组件是否正确导入
2. `useLargeNodes` 状态是否正确传递
3. `currentNodeTypes` 是否正确计算

## 📞 获取帮助

如果遇到问题:

1. 查看浏览器控制台错误
2. 检查 `IMPLEMENTATION_GUIDE.md` 详细文档
3. 查看 `WORKFLOW_REDESIGN_PLAN.md` 完整方案
4. 联系开发团队

## 🎉 下一步

成功启用大节点后,可以考虑:

1. 调整节点颜色和样式
2. 添加更多配置预览字段
3. 实现节点库面板
4. 实现属性编辑器面板
5. 优化拖拽体验

参考 `WORKFLOW_REDESIGN_PLAN.md` 了解完整路线图。
