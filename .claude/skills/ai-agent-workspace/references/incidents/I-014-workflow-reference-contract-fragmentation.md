# I-014 workflow-reference-contract-fragmentation

## 故障签名

- 下游 TOOL/KNOWLEDGE 节点引用上游 START 输出时，传给工具的占位参数为空。
- 图 JSON、前端条件编辑器、后端表达式解析同时存在 `nodes.<nodeId>.<key>`、`<nodeId>.output.<key>`、`inputs.<key>`、`state.<key>`、`#{...}` 等多套格式。
- 日志中可能看到 MCP 工具请求参数 `query=""`，或者真实外部调用失败为 400。

## 根因

前后端没有共用同一套值引用标准。部分代码把前端 `sourceRef` 转成 SpEL，部分代码直接按字符串解析，条件节点又保存另一套 `nodes.*` 格式。格式分裂后，执行器可能无法精确判断引用缺失，进而把空占位参数继续传给下游工具。

## 标准

- `inputs.<key>`：启动输入。
- `<nodeId>.output.<key>`：上游节点输出。
- `sharedState.<key>`：共享状态。

## 修复原则

- 新图和前端 UI 只保存标准格式。
- 历史 `nodes.<nodeId>.<key>`、`state.<key>`、`#{...}` 仅做兼容读取，不作为新契约继续扩散。
- 引用不存在必须抛错；必填参数为空必须失败；不要回退到原始表达式、空串、任意用户输入或上下文中的近似字段。
- TOOL 节点必须在外部 MCP 调用前校验 JSON Schema `required` 字段。
- KNOWLEDGE 节点只接受解析后的 `query`，不允许从 `user_input`、第一个非系统字符串或 `context.inputs.*` 兜底。

## 验证记录

- 正向 Browser Relay：executionId=`56cdbc83-4b5d-43e9-bcd7-df295fdd3bff`，输入 `inputs.inputMessage=周杰是谁？`，TOOL 入参 `query="周杰是谁？"`，同一 execution 只有一条 TOOL 节点日志。
- 反向 Browser Relay：executionId=`7c7fad9b-53ea-436b-88ab-a3bf2a3a5f2c`，只传 `inputs.query`，图中引用 `start.output.inputMessage` 为空，TOOL/KNOWLEDGE 均在节点执行阶段失败。
- 反向日志要求：允许出现 `Preparing MCP tool`，不允许出现真正的 `Executing MCP tool`。
