## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HumanReviewDTO.java
- 人工审核模块 DTO 容器，定义待审核项、审核详情、恢复执行请求/响应等接口结构。

## 2) 核心方法
- 无显式方法（静态内部 DTO 定义）

## 3) 具体方法
### 3.1 结构契约
- 函数签名: `N/A`
- 入参: `ResumeExecutionRequest`/`RejectExecutionRequest` 等
- 出参: `PendingReviewDTO`/`ReviewDetailDTO`/`ResumeExecutionResponse`
- 功能含义: 统一审核接口的数据边界，解耦 controller 与领域对象。
- 链路作用: `HumanReviewController` 请求绑定与响应序列化。

## 4) 变更记录
- 2026-02-15: 基于源码回填人工审核 DTO 分组语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
