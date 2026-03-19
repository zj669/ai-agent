# Plan：知识库上传分块策略可配置化实施方案

## 1. 目标

围绕知识库上传链路，完成“分块策略 + 参数配置”的前后端改造，首期支持：

- `FIXED`
- `SEMANTIC`

并确保：

- 兼容旧上传方式
- 兼容旧文档数据
- 不破坏现有异步处理与检索链路

## 2. 当前现状

当前系统实现特点：

1. 上传接口通过 `multipart/form-data` 接收文件。
2. 后端仅支持 `chunkSize`、`chunkOverlap` 两个参数。
3. `ChunkingConfig` 只有两个字段。
4. `TextSplitterPort` 只有单一分块接口和单一实现。
5. 数据库 `knowledge_document` 表只存 `chunk_size`、`chunk_overlap`。
6. 前端上传页没有可配置的分块策略表单。

## 3. 设计原则

### 3.1 兼容优先

优先保证旧调用方式继续可用，避免本次重构影响当前知识库上传能力。

### 3.2 扩展优先

模型设计要能支持后续新增更多分块策略，而不是继续围绕两个固定字段扩张。

### 3.3 实现务实

首版先把协议、存储、前后端交互和策略路由搭起来，语义分块实现可采取规则驱动的近似方案。

## 4. 总体方案

### 4.1 配置模型升级

将当前简单的 `ChunkingConfig` 升级为策略化配置模型。

建议结构：

- `strategy`
- `chunkSize`
- `chunkOverlap`
- `maxChunkSize`
- `minChunkSize`
- `similarityThreshold`
- `mergeSmallChunks`

备注：

- 首版使用扁平字段，降低 DTO、PO、MyBatis 映射成本。
- 后续若策略继续增多，再考虑拆分为更强的多态配置对象。

### 4.2 分块执行升级

将当前单一分块器升级为“策略路由 + 多实现”结构：

- `TextSplitterPort` 改为接收完整 `ChunkingConfig`
- 新增策略路由器
- 新增 `FixedTextSplitter`
- 新增 `SemanticTextSplitter`

### 4.3 数据持久化升级

在 `knowledge_document` 中新增：

- `chunk_strategy`
- `chunk_config_json`

兼容策略：

- 保留 `chunk_size`
- 保留 `chunk_overlap`

### 4.4 前端交互升级

在知识库上传区域增加：

- 分块策略选择
- 按策略显示参数面板
- 参数默认值与基本说明

## 5. 后端实施方案

### 5.1 Domain 层

改造项：

1. 新增 `ChunkingStrategy` 枚举
2. 扩展 `ChunkingConfig`
3. 为 `ChunkingConfig` 增加默认工厂和校验方法

建议默认：

- `FIXED`: `chunkSize=500`, `chunkOverlap=50`
- `SEMANTIC`: `minChunkSize=200`, `maxChunkSize=800`, `similarityThreshold=0.75`, `mergeSmallChunks=true`

### 5.2 接口层

改造上传接口参数：

新增：

- `chunkStrategy`
- `maxChunkSize`
- `minChunkSize`
- `similarityThreshold`
- `mergeSmallChunks`

保留：

- `chunkSize`
- `chunkOverlap`

处理逻辑：

1. 若 `chunkStrategy` 为空，则视为 `FIXED`
2. 若仅有旧参数，则按 `FIXED` 组装配置
3. 若传入新策略，则按策略进行参数校验

### 5.3 Application 层

改造上传编排逻辑：

1. 接收完整分块配置
2. 校验配置
3. 保存到文档实体
4. 异步处理器按配置执行

### 5.4 Infrastructure 层

改造项：

1. 扩展 `KnowledgeDocumentPO`
2. 扩展 `MySQLKnowledgeDocumentRepository`
3. 新增配置 JSON 序列化与反序列化逻辑
4. 实现固定分块器与语义分块器
5. 实现分块策略路由器

### 5.5 AsyncDocumentProcessor

改造为：

1. 读取文档完整 `ChunkingConfig`
2. 调用新的 splitter 接口
3. 记录日志：策略、参数、chunk 数
4. 可选把 `chunk_strategy` 写入向量 metadata

## 6. 前端实施方案

### 6.1 页面改造

改造页面：

- `KnowledgePage.tsx`

新增区域：

1. 分块策略选择器
2. 固定分块参数表单
3. 语义分块参数表单

### 6.2 API 改造

改造：

- `knowledgeAdapter.ts`
- `knowledgeService.ts`

新增上传参数类型定义：

- `FixedChunkUploadConfig`
- `SemanticChunkUploadConfig`
- `UploadKnowledgeDocumentInput`

### 6.3 默认交互

默认行为：

1. 页面默认选中 `FIXED`
2. 默认参数自动填充
3. 用户不改时也可直接上传

## 7. 数据库迁移方案

### 7.1 新增字段

在 `knowledge_document` 表中新增：

- `chunk_strategy varchar(32)`
- `chunk_config_json text`

### 7.2 兼容读取

读取规则：

1. 若存在 `chunk_strategy` 和 `chunk_config_json`，优先使用新字段
2. 若新字段为空，则回退旧字段构造 `FIXED`

### 7.3 兼容写入

写入规则：

1. 新文档写入新字段
2. 固定分块策略同时回写 `chunk_size` 和 `chunk_overlap`
3. 语义分块策略的旧字段可写默认值或可解释值，用于兼容观察

## 8. 测试方案

### 8.1 后端测试

需要新增或修改：

1. `ChunkingConfig` 校验测试
2. 上传接口参数映射测试
3. `KnowledgeApplicationService` 上传配置保存测试
4. `MySQLKnowledgeDocumentRepository` 新旧字段兼容测试
5. `AsyncDocumentProcessor` 策略分发测试
6. 固定分块与语义分块单元测试

### 8.2 前端测试

需要新增或修改：

1. 默认策略展示测试
2. 策略切换表单渲染测试
3. 固定分块上传参数测试
4. 语义分块上传参数测试
5. 上传失败和校验提示测试

### 8.3 冒烟验证

建议保留并扩展现有知识库冒烟脚本，至少验证：

1. `FIXED` 上传成功
2. `SEMANTIC` 上传成功
3. 上传完成后可正常检索

## 9. 风险控制

### 风险 1：语义分块实现复杂度偏高

控制方式：

- 首版采用规则驱动的语义近似实现
- 先满足产品可配置目标，再逐步优化算法

### 风险 2：数据库升级影响旧数据

控制方式：

- 保留旧列
- 以新字段优先、旧字段回退的方式兼容

### 风险 3：前后端参数不一致

控制方式：

- 明确定义统一字段名
- 用测试覆盖上传参数映射

## 10. 里程碑

### 里程碑 1：后端模型与存储升级

完成以下内容：

- `ChunkingConfig` 升级
- 上传接口参数升级
- 数据库迁移
- 仓储兼容

### 里程碑 2：分块策略实现

完成以下内容：

- 固定分块实现
- 语义分块实现
- 策略路由
- 异步处理器接入

### 里程碑 3：前端上传页接入

完成以下内容：

- 上传配置 UI
- 动态参数表单
- 接口参数传递

### 里程碑 4：测试与文档收尾

完成以下内容：

- 前后端测试
- 冒烟验证
- API 文档更新

## 11. 交付物

本次改造交付物包括：

1. 可配置分块策略的前端上传界面
2. 支持多策略参数的后端上传接口
3. 多策略分块执行能力
4. 兼容旧数据的数据库与仓储改造
5. 更新后的测试与文档
