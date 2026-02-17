## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/EmbeddingModelConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: EmbeddingModelConfig.java
- 将 `spring.ai.openai.embedding` 配置绑定为运行参数，并创建 `EmbeddingModel` Bean，供知识检索/向量化流程复用。

## 2) 核心方法
- `embeddingModel()`

## 3) 具体方法
### 3.1 embeddingModel()
- 函数签名: `embeddingModel() -> EmbeddingModel`
- 入参: 无（使用配置属性）
- 出参: `EmbeddingModel`
- 功能含义: 基于 `OpenAiApi` 与 `OpenAiEmbeddingOptions` 构造 embedding 模型实例（固定 dimensions=1024）。
- 链路作用: 向量化调用入口 -> 模型实例 -> 知识库切片嵌入生成。

## 4) 变更记录
- 2026-02-15: 基于源码回填 embedding 配置绑定与 Bean 装配语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
