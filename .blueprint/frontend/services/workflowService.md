## Metadata
- file: `.blueprint/frontend/services/workflowService.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: workflowService (MVP)
- 对应 `ai-agent-foward/src/services/workflowService.ts`（新建），仅负责 MVP 编辑器所需的：
  1) 加载 Agent 工作流定义（graphJson + metadata）；
  2) 保存工作流（graphJson 提交）；
  3) 版本冲突检测与乐观锁处理。
- 注意：本文件与 `executionService.ts`（运行态执行）职责分离，编辑器不涉及 SSE 执行链路。

## 2) 核心方法 (MVP裁剪)
- `loadWorkflow(agentId)`
- `saveWorkflow(agentId, graphJson, version)`

## 3) 具体方法
### 3.1 loadWorkflow(agentId)
- 函数签名: `async loadWorkflow(agentId: string): Promise<{ graphJson: GraphJson; metadata: NodeMetadata[]; version: number }>`
- 入参: `agentId` Agent 唯一标识
- 出参: 包含 graphJson、metadata 模板列表、当前版本号
- 功能含义: 加载指定 Agent 的完整工作流定义，包括持久化图结构和节点元数据。metadata 默认包含节点三段结构定义：`inputSchema`、`outputSchema`、`userConfig`。
- 链路作用: 编辑器初始化数据源，连接后端 Agent 查询接口。

### 3.2 saveWorkflow(agentId, graphJson, version)
- 函数签名: `async saveWorkflow(agentId: string, graphJson: GraphJson, version: number): Promise<{ success: boolean; newVersion?: number; conflict?: boolean }>`
- 入参: Agent ID、待保存的图结构、当前版本号（乐观锁）
- 出参: 保存结果，若版本冲突返回 `conflict: true`
- 功能含义: 提交工作流变更，携带 version 做乐观锁校验，服务端返回新版本号或冲突标记。
- 链路作用: 编辑器保存接口，与 `WorkflowEditorPage.handleSaveWorkflow` 配合。


## 4) 关键协作契约（MVP裁剪）
- workflowService 仅服务于编辑器的数据持久化，不负责执行态 SSE。
- 初始化单路径：页面必须通过 `loadWorkflow` 一次性获取 `graphJson + metadata + version`，禁止并行拆分加载。
- 保存操作必须携带 version，服务端返回冲突时由页面层处理（提示用户并加载最新快照）。
- metadata 全量缓存于编辑器启动时，不支持运行时动态更新（MVP 简化）。
- 不纳入本阶段：执行启动/停止、SSE 流处理、执行日志查询（这些由 executionService 负责）。

## 5) 变更记录
- 2026-02-14: 初始版本，基于历史执行服务定义。
- 2026-02-16: 按 MVP 目标重写为编辑器专用服务，移除执行态相关方法，仅保留 graphJson 读写与 metadata 加载。
- 2026-02-16: 修复必修问题：初始化改为单路径 `loadWorkflow`，移除并行 metadata 加载歧义。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
