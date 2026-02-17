## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/dto/KnowledgeDTO.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: KnowledgeDTO.java
- 知识库接口 DTO 容器，定义 dataset/document/search 三组请求与响应结构，并附带参数校验注解。

## 2) 核心方法
- 无显式方法（静态内部 DTO 定义）

## 3) 具体方法
### 3.1 结构契约
- 函数签名: `N/A`
- 入参: `DatasetCreateReq` / `DocumentUploadReq` / `SearchReq` 等
- 出参: `DatasetResp` / `DocumentResp` / `SearchResp`
- 功能含义: 统一知识库模块 API 入参与出参结构，降低控制器签名复杂度。
- 链路作用: `KnowledgeController` 参数绑定与返回序列化。

## 4) 变更记录
- 2026-02-15: 基于源码回填知识库 DTO 分组与校验语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
