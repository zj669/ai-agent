## Metadata
- file: `.blueprint/frontend/workflow/HooksAndStoreSlices.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HooksAndStoreSlices
- 该文件用于描述 HooksAndStoreSlices 的职责边界与协作关系。
- MVP 说明：本文件用于职责与写入边界校验，不要求引入额外全局 store 架构改造。

## 2) 核心方法
- `resolveHookResponsibilities()`
- `resolveSliceWritePolicy()`
- `validateMvpSliceContract(sliceName, payload)`

## 3) 具体方法
### 3.1 resolveHookResponsibilities()
- 函数签名: `resolveHookResponsibilities(): HookResponsibilityMap`
- 入参: 无（读取当前工作流模块的 Hook 定义）
- 出参: `HookResponsibilityMap` 对象，结构为 `{ [hookName: string]: { purpose: string, storeSlices: string[], externalDeps: string[] } }`
- 功能含义: 返回所有工作流相关 Hook 的职责清单，明确每个 Hook 负责的状态切片和外部依赖
- 链路作用: 架构文档生成器，用于审查 Hook 职责边界，防止单个 Hook 承担过多职责

### 3.2 resolveSliceWritePolicy()
- 函数签名: `resolveSliceWritePolicy(): SliceWritePolicy`
- 入参: 无（分析 Zustand store 定义）
- 出参: `SliceWritePolicy` 对象，结构为 `{ [sliceName: string]: { allowedWriters: string[], writePattern: 'direct' | 'action-only' } }`
- 功能含义: 定义每个 store slice 的写入权限策略，明确哪些组件/Hook 可以修改特定状态
- 链路作用: 状态管理规范检查器，确保状态修改遵循单向数据流，防止跨层级直接修改

### 3.3 validateMvpSliceContract(sliceName: string, payload: any)
- 函数签名: `validateMvpSliceContract(sliceName: string, payload: any): ValidationResult`
- 入参: `sliceName`（如 `'canvas'`, `'selection'`, `'metadata'`, `'propertiesForm'`），`payload` 为写入数据
- 出参: `ValidationResult` 包含 `{ valid: boolean, errors: string[] }`
- 功能含义: 校验 MVP 阶段关键切片的写入合法性，确保拖拽、卡片配置、metadata 回填的数据结构稳定。
- 链路作用: MVP 状态契约守卫，防止跨层写入污染 graphJson 映射链路。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全 Hook 和 Store Slice 管理方法的具体签名，明确职责映射、写入策略和历史操作验证契约。
- 2026-02-16: 按 MVP 目标移除历史功能（undo/redo/snapshot）契约，收敛为拖拽编辑与卡片配置必需切片。
- 2026-02-16: 新增 `validateMvpSliceContract`，约束 metadata 与 graphJson 主链路写入。
- 2026-02-16: 修复必修问题：明确该文档为约束文档，不额外引入复杂 store 机制。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
