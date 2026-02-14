## Metadata
- file: `.blueprint/frontend/workflow/HooksAndStoreSlices.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HooksAndStoreSlices
- 该文件用于描述 HooksAndStoreSlices 的职责边界与协作关系。

## 2) 核心方法
- `resolveHookResponsibilities()`
- `resolveSliceWritePolicy()`
- `validateHistoryAction(actionType, payload)`
- `validateHistoryAction()`

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

### 3.3 validateHistoryAction(actionType: string, payload: any)
- 函数签名: `validateHistoryAction(actionType: string, payload: any): ValidationResult`
- 入参: `actionType` - 历史操作类型（如 `'undo'`, `'redo'`, `'snapshot'`），`payload` - 操作携带的数据
- 出参: `ValidationResult` 包含 `{ valid: boolean, errors: string[], canExecute: boolean }`
- 功能含义: 验证历史操作的合法性，检查当前状态是否允许执行该操作（如 undo 栈是否为空）
- 链路作用: 历史管理中间件，在执行 undo/redo 前进行前置校验，防止非法状态转换


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全 Hook 和 Store Slice 管理方法的具体签名，明确职责映射、写入策略和历史操作验证契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
