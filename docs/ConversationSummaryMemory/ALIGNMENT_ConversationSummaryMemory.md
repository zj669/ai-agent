# ALIGNMENT_ConversationSummaryMemory

## 项目特性规范

### 项目上下文分析
- **技术栈**: Spring Boot 3.4.9, Spring AI 1.0.1, MyBatis Plus 3.5.5
- **架构模式**: 分层架构 + Advisor模式 + 策略模式
- **现有记忆管理**: 基于`PromptChatMemoryAdvisor`和`MessageWindowChatMemory`
- **代码风格**: 使用Lombok、遵循Spring Boot编码规范
- **模块位置**: `com.zj.aiagemt.service.memory`包下

### 原始需求
参照`ReReadingAdvisor`和`PromptChatMemoryAdvisor`实现`ConversationSummaryMemory`，该类的主要责任是管理聊天记忆上下文，可以实现当聊天上下文到达一定限度时会自动调用大模型进行浓缩。

### 边界确认
**明确任务范围**:
- ✅ 实现`ConversationSummaryMemory`类
- ✅ 继承`BaseAdvisor`接口
- ✅ 实现上下文长度监控和自动浓缩功能
- ✅ 集成大模型调用进行摘要生成
- ✅ 兼容现有的Advisor链模式
- ❌ 不涉及前端页面修改
- ❌ 不涉及数据库表结构变更
- ❌ 不涉及配置文件大幅修改

### 需求理解
**对现有项目的理解**:
1. **Advisor模式**: 系统使用`BaseAdvisor`作为拦截器，在请求前后处理逻辑
2. **记忆管理**: 当前使用`PromptChatMemoryAdvisor`配合`MessageWindowChatMemory`管理有限窗口记忆
3. **大模型调用**: 通过`ChatClient`和`SpringContextUtil`获取AI客户端进行模型调用
4. **枚举策略**: 使用`AiAgentEnumVO`获取Bean名称进行依赖注入

### 疑问澄清
**存在歧义的地方**:
1. **上下文限度阈值**: 多少条消息或多少token触发浓缩？
   - **决策**: 默认设置为20条消息，可配置
2. **浓缩策略**: 是全量浓缩还是增量浓缩？
   - **决策**: 采用增量浓缩，保留最近的消息，浓缩较老的消息
3. **浓缩模型**: 使用哪个AI客户端进行浓缩？
   - **决策**: 使用默认的AI客户端（ID: 3002）
4. **存储位置**: 浓缩后的摘要存储在哪里？
   - **决策**: 暂时存储在内存中，后续可扩展到数据库
5. **配置方式**: 如何配置浓缩参数？
   - **决策**: 通过构造函数参数配置，保持简单

## 技术实现方案

### 核心功能设计
1. **消息计数监控**: 监控当前会话的消息数量
2. **自动触发浓缩**: 达到阈值时自动调用大模型生成摘要
3. **摘要管理**: 用摘要替换旧消息，保持上下文窗口大小
4. **与现有系统集成**: 兼容`ChatMemory`接口和Advisor链

### 技术约束
- 必须实现`BaseAdvisor`接口
- 必须支持流式和非流式响应
- 必须保持线程安全
- 必须兼容现有的Spring AI架构

### 集成方案
- 与`PromptChatMemoryAdvisor`配合使用
- 通过`SpringContextUtil`获取AI客户端
- 支持配置化的浓缩策略

## 验收标准
1. 类能够正确实现`BaseAdvisor`接口
2. 能够监控消息数量并在达到阈值时触发浓缩
3. 能够调用大模型生成对话摘要
4. 能够维护合理的上下文窗口大小
5. 与现有系统无冲突，能够正常运行
6. 代码符合项目编码规范