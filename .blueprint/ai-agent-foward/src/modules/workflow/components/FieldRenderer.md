# FieldRenderer.tsx 蓝图

## Metadata
- title: FieldRenderer
- type: module
- summary: 数据库驱动的动态节点配置渲染器，根据后端返回的 ConfigFieldDTO.fieldType 分发渲染对应控件（text/textarea/select/number/switch/boolean/llm_config_select/knowledge_select）

## 关键方法单元

### FieldRenderer
- location: FieldRenderer（default export）
- purpose: 根据 field.fieldType 分发渲染对应的表单控件
- input: field（ConfigFieldDTO）、value（unknown）、onChange（回调）
- output: JSX 表单控件
- core_steps:
  1. switch(fieldType) 分发到对应 case
  2. select 类型从 field.options 渲染下拉选项（搜索策略走此路径）
  3. knowledge_select 类型渲染 KnowledgeSelect 组件（异步加载知识库列表）
  4. llm_config_select 类型渲染 LlmConfigSelect 组件（异步加载模型配置列表）

### KnowledgeSelect
- location: KnowledgeSelect（内部组件）
- purpose: 异步加载知识库列表并渲染为下拉选择框，带模块级缓存
- input: value、onChange、fieldKey、placeholder
- output: JSX select 元素
- core_steps:
  1. 检查 knowledgeCache，命中则直接使用
  2. 未命中则调用 getKnowledgeDatasetList() 加载
  3. 渲染 dataset 列表为 option（显示名称 + 文档数）

## 变更记录
- 2026-03-16: 初始蓝图生成
