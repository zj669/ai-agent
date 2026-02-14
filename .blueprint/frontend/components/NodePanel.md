# NodePanel Blueprint

## 职责契约
- **做什么**: 展示节点库,支持搜索、分类、点击添加节点
- **不做什么**: 不处理节点编辑、不管理节点数据、不处理画布交互

## 接口摘要 (Props)

| 名称 | 类型 | 说明 | 必需 |
|------|------|------|------|
| onAddNode | (type: NodeType) => void | 点击添加节点回调 | 否 |
| collapsed | boolean | 是否折叠 | 否 |
| onCollapsedChange | (collapsed: boolean) => void | 折叠状态变更回调 | 否 |

## 依赖拓扑
- **上游**: WorkflowEditorPage
- **下游**: 
  - Ant Design (Input, Collapse, Tooltip)
  - Lucide Icons
  - NODE_GROUPS (节点配置)

## 组件结构

```tsx
<div className="workflow-nodepanel">
  {/* 头部 */}
  <div className="panel-header">
    <h3>节点库</h3>
    <Button onClick={onCollapsedChange}>折叠</Button>
    <Input.Search placeholder="搜索节点..." />
  </div>
  
  {/* 节点列表 */}
  <div className="node-list">
    <Collapse activeKey={activeKeys}>
      {filteredGroups.map(group => (
        <Collapse.Panel key={group.key} header={group.label}>
          {group.nodes.map(node => (
            <NodeCard
              key={node.type}
              node={node}
              onDragStart={handleDragStart}
              onClick={onAddNode}
            />
          ))}
        </Collapse.Panel>
      ))}
    </Collapse>
  </div>
  
  {/* 底部提示 */}
  <div className="panel-footer">
    <p>点击节点添加到画布</p>
  </div>
</div>
```

## 节点分组配置

### NODE_GROUPS
```typescript
const NODE_GROUPS = [
  {
    key: 'basic',
    label: '基础节点',
    icon: Workflow,
    nodes: [
      { type: NodeType.START, label: '开始', ... },
      { type: NodeType.END, label: '结束', ... }
    ]
  },
  {
    key: 'ai',
    label: 'AI 节点',
    icon: Sparkles,
    nodes: [
      { type: NodeType.LLM, label: 'LLM', ... }
    ]
  },
  // ... 其他分组
];
```

## 交互行为

### 点击添加
```typescript
handleClick(nodeType) {
  onAddNode?.(nodeType);
}
```

### 搜索过滤
```typescript
const filteredGroups = searchText
  ? NODE_GROUPS.map(group => ({
      ...group,
      nodes: group.nodes.filter(node =>
        node.label.includes(searchText) ||
        node.description.includes(searchText)
      )
    })).filter(group => group.nodes.length > 0)
  : NODE_GROUPS;
```

## 折叠模式

### 折叠状态
- 宽度: 56px
- 只显示节点图标
- 悬停显示 Tooltip

### 展开状态
- 宽度: 280px
- 显示完整节点信息
- 支持搜索和分类

## 设计约束

### 样式约束
- 面板宽度: 280px (展开) / 56px (折叠)
- 节点卡片高度: 自适应
- 分组可折叠

### 交互约束
- 点击直接添加到画布中心
- 搜索实时过滤

### 性能约束
- 使用 useMemo 缓存过滤结果
- 避免不必要的重渲染

## 变更日志
- [2026-02-13] 创建 NodePanel 蓝图
- [2026-02-13] 定义节点分组、点击添加交互、折叠模式
