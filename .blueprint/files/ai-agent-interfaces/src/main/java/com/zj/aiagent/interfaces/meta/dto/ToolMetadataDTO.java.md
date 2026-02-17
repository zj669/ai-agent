## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/meta/dto/ToolMetadataDTO.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ToolMetadataDTO.java
- 工具元数据响应结构，承载工具基础信息与输入/输出 JSON Schema，用于前端画布节点面板渲染。

## 2) 核心方法
- 无显式方法（字段型 DTO）

## 3) 具体方法
### 3.1 结构契约
- 函数签名: `N/A`
- 入参: 无
- 出参: `toolId/name/description/icon/inputSchema/outputSchema`
- 功能含义: 作为工具与节点元信息传输对象，桥接后端元数据与前端可配置表单。
- 链路作用: 元数据接口返回 -> 前端节点配置 UI 动态生成。

## 4) 变更记录
- 2026-02-15: 基于源码回填工具元数据 DTO 字段语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
