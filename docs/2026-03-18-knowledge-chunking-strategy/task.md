# Task：知识库上传分块策略可配置化任务清单

## 任务说明

本任务围绕“知识库上传支持可选分块策略与参数配置”展开，目标是在保持现有链路可用的前提下，完成前后端和数据库的联合改造。

## Phase 1：模型与协议设计

- [ ] 新增 `ChunkingStrategy` 枚举，至少包含 `FIXED` 和 `SEMANTIC`
- [ ] 扩展 `ChunkingConfig`，支持策略字段和对应参数
- [ ] 为 `ChunkingConfig` 增加默认配置工厂方法
- [ ] 为 `ChunkingConfig` 增加参数合法性校验逻辑
- [ ] 梳理并固定前后端统一字段名

## Phase 2：接口层改造

- [ ] 改造知识库上传接口，支持接收 `chunkStrategy`
- [ ] 改造知识库上传接口，支持接收语义分块参数
- [ ] 保留 `chunkSize` 和 `chunkOverlap` 的兼容处理
- [ ] 更新上传接口参数组装逻辑
- [ ] 更新接口参数校验逻辑

## Phase 3：数据库与仓储改造

- [ ] 为 `knowledge_document` 表新增 `chunk_strategy`
- [ ] 为 `knowledge_document` 表新增 `chunk_config_json`
- [ ] 补充数据库迁移脚本
- [ ] 更新 `KnowledgeDocumentPO`
- [ ] 更新 `MySQLKnowledgeDocumentRepository.toPO`
- [ ] 更新 `MySQLKnowledgeDocumentRepository.toDomain`
- [ ] 实现新字段优先、旧字段回退的兼容读取逻辑

## Phase 4：分块执行能力改造

- [ ] 重构 `TextSplitterPort`，改为接收完整 `ChunkingConfig`
- [ ] 新增策略路由实现
- [ ] 实现 `FIXED` 分块器
- [ ] 实现 `SEMANTIC` 分块器
- [ ] 改造 `AsyncDocumentProcessor` 接入新分块入口
- [ ] 为处理日志增加策略与参数输出
- [ ] 评估是否将 `chunk_strategy` 写入向量 metadata

## Phase 5：前端上传页改造

- [ ] 设计上传区域中的分块配置交互
- [ ] 为上传页增加策略选择器
- [ ] 为 `FIXED` 策略增加参数表单
- [ ] 为 `SEMANTIC` 策略增加参数表单
- [ ] 设置各策略默认值
- [ ] 更新上传接口调用参数
- [ ] 处理参数校验与错误提示

## Phase 6：类型与适配层改造

- [ ] 更新 `knowledgeAdapter.ts` 上传入参类型
- [ ] 更新 `knowledgeService.ts` 上传入参类型
- [ ] 增加前端分块策略类型定义
- [ ] 确认前端字段名与后端字段名一致

## Phase 7：测试补齐

- [ ] 补充 `ChunkingConfig` 单元测试
- [ ] 补充上传接口参数映射测试
- [ ] 补充上传配置持久化测试
- [ ] 补充仓储兼容读写测试
- [ ] 补充固定分块器测试
- [ ] 补充语义分块器测试
- [ ] 扩展知识库前端主流程测试
- [ ] 补充前端策略切换与参数提交测试

## Phase 8：文档与验证

- [ ] 更新 `docs/api/knowledge.md`
- [ ] 补充上传分块策略说明
- [ ] 扩展知识库上传检索冒烟脚本
- [ ] 分别验证 `FIXED` 和 `SEMANTIC` 上传链路
- [ ] 验证旧上传参数兼容场景

## 建议实施顺序

1. 先完成 Domain、DTO、数据库结构设计。
2. 再完成仓储与异步处理器改造。
3. 然后接入 `FIXED` 与 `SEMANTIC` 两种策略实现。
4. 再改前端上传页和 API 适配层。
5. 最后补测试、补文档、跑冒烟。

## 完成定义

满足以下条件即可视为任务完成：

- 上传页可选择分块策略并填写对应参数
- 后端可正确保存并执行对应分块策略
- 旧调用方式兼容
- 关键自动化测试通过
- 冒烟验证通过
