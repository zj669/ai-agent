# ConversationSummaryMemory重构任务对齐文档

## 项目特性规范

### 现有项目架构分析
- **框架**: Spring Boot 3.4.9 + Spring AI 1.0.1
- **设计模式**: 分层架构、策略模式、责任链模式、Advisor模式
- **AI集成**: 基于Spring AI框架的ChatClient和Advisor机制
- **记忆管理**: 当前使用ChatMemory接口进行对话历史管理

### 现有ConversationSummaryMemoryAdvisor分析
当前实现将记忆管理功能和请求处理功能混合在一个类中：
- 管理对话消息计数 (`conversationMessageCounts`)
- 管理对话摘要缓存 (`conversationSummaries`) 
- 执行摘要生成逻辑 (`generateSummary`)
- 处理请求前后的Advisor逻辑 (`before`/`after`)

## 原始需求

**用户需求**：实现ConversationSummaryMemory，参照ConversationSummaryMemoryAdvisor，将其中记忆管理功能放在ConversationSummaryMemory中，ConversationSummaryMemoryAdvisor主要负责对用户请求进行增加管理，即将历史聊天记录查询出来然后根据提示词组合用户请求和历史记录，具体可以参考PromptChatMemoryAdvisor

## 边界确认

### 任务范围
✅ **包含**：
1. 创建功能完整的ConversationSummaryMemory类，实现ChatMemory接口
2. 将记忆管理功能从ConversationSummaryMemoryAdvisor中分离
3. 重构ConversationSummaryMemoryAdvisor，专注于请求处理和历史记录组合
4. 确保新实现与现有Spring AI框架完全兼容
5. 保持现有的摘要生成和压缩逻辑

❌ **不包含**：
1. 修改数据库结构或持久化逻辑
2. 修改AI客户端配置
3. 修改其他Advisor实现
4. 创建新的配置类或Bean

### 技术约束
- 必须实现Spring AI的ChatMemory接口
- 必须保持与现有AiAgentEnumVO枚举的兼容性
- 必须使用现有的SpringContextUtil和日志框架
- 必须保持线程安全性（使用ConcurrentHashMap）

## 需求理解

### 核心目标
将现有的单一职责违反的ConversationSummaryMemoryAdvisor重构为符合单一职责原则的两个组件：

1. **ConversationSummaryMemory**: 负责记忆管理
   - 存储和检索对话消息
   - 管理摘要生成和压缩逻辑
   - 维护对话状态（消息计数、摘要缓存）

2. **ConversationSummaryMemoryAdvisor**: 负责请求处理
   - 在请求前加载历史记录
   - 组合用户请求和历史上下文
   - 在响应后保存新消息

### 参考模式
- **PromptChatMemoryAdvisor**: 展示了如何在Advisor中使用ChatMemory
- **现有ChatMemory接口**: 定义了标准的记忆管理契约

## 疑问澄清

### 已明确的设计决策
1. **接口实现**: ConversationSummaryMemory实现ChatMemory接口
2. **依赖注入**: ConversationSummaryMemoryAdvisor将依赖ConversationSummaryMemory
3. **摘要逻辑位置**: 摘要生成逻辑应该在ConversationSummaryMemory中
4. **状态管理**: 对话状态（计数、摘要）由ConversationSummaryMemory管理

### 需要确认的设计细节
1. **配置参数传递**: 摘要相关配置如何传递给ConversationSummaryMemory
2. **AI客户端访问**: ConversationSummaryMemory如何访问AI客户端进行摘要生成
3. **错误处理策略**: 摘要失败时的降级策略应该在哪个组件中实现

## 验收标准

### 功能验收
1. ConversationSummaryMemory能够正确存储和检索对话消息
2. 摘要功能正常工作，能够在消息数量达到阈值时自动压缩
3. ConversationSummaryMemoryAdvisor能够正确组合历史记录和用户请求
4. 重构后的功能与原实现功能等价

### 技术验收
1. 代码符合现有项目规范和风格
2. 线程安全性得到保证
3. 日志记录完整且格式一致
4. 异常处理机制完善

### 集成验收
1. 与现有Spring AI框架无缝集成
2. 与现有AI客户端配置兼容
3. 不影响其他Advisor的正常工作
4. 配置参数传递机制正常工作