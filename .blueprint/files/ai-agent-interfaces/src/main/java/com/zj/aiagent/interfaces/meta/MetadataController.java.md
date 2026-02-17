## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/meta/MetadataController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: MetadataController.java
- 元数据控制器，提供节点模板查询接口及别名接口，服务前端工作流画布初始化。

## 2) 核心方法
- `getNodeTemplates()`
- `getNodeTypes()`

## 3) 具体方法
### 3.1 getNodeTemplates()
- 函数签名: `getNodeTemplates() -> Response<List<NodeTemplateDTO>>`
- 入参: 无
- 出参: 节点模板列表
- 功能含义: 调用 `MetadataApplicationService.getAllNodeTemplates()` 返回节点模板元数据。
- 链路作用: 画布初始化 -> 元数据拉取 -> 前端节点库展示。

## 4) 变更记录
- 2026-02-15: 基于源码回填元数据查询控制器职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
