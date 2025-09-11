# TODO_ConversationSummaryMemory

## 配置相关待办事项

### 🔧 必需配置

#### 1. AI客户端配置
**描述**: 确保AI客户端Bean正确配置和注册
**位置**: Spring配置类或application.yml
**建议操作**:
```java
// 在配置类中确保有AI客户端Bean
@Bean(name = "ai_client_3002") 
public ChatClient aiClient3002() {
    return ChatClient.builder(chatModel).build();
}
```

#### 2. SpringContextUtil Bean注册
**描述**: 确保SpringContextUtil正确注册为Spring Bean
**位置**: `SpringContextUtil.java`
**建议操作**:
```java
@Component
public class SpringContextUtil implements ApplicationContextAware {
    // 确保类上有@Component注解
}
```

#### 3. ChatMemory实现配置
**描述**: 选择合适的ChatMemory实现类
**位置**: 使用ConversationSummaryMemory的地方
**建议操作**:
```java
// 推荐使用MessageWindowChatMemory
ChatMemory chatMemory = new MessageWindowChatMemory(maxMessages);
```

### 🎯 集成配置

#### 1. Advisor链配置
**描述**: 将ConversationSummaryMemory集成到ChatClient的Advisor链中
**位置**: ChatClient构建代码
**建议操作**:
```java
ConversationSummaryMemory summaryMemory = new ConversationSummaryMemory(
    springContextUtil, chatMemory, 20, 15, "3002", 500, Duration.ofSeconds(5), 100);

ChatClient chatClient = ChatClient.builder(chatModel)
    .advisors(summaryMemory)
    .build();
```

#### 2. 执行顺序配置
**描述**: 确定ConversationSummaryMemory在Advisor链中的执行顺序
**建议**: 
- 在PromptChatMemoryAdvisor之前执行（order < PromptChatMemoryAdvisor的order）
- 推荐order值: 50-100

### ⚙️ 可选优化配置

#### 1. 应用配置文件
**描述**: 添加application.yml配置支持
**位置**: `application.yml`
**建议配置**:
```yaml
aiagent:
  conversation-summary:
    max-messages: 20
    summary-trigger-threshold: 15
    ai-client-id: "3002"
    summary-max-length: 500
    summary-timeout: 5000
    enable: true
```

#### 2. 配置类支持
**描述**: 创建专门的配置类读取yml配置
**建议操作**:
```java
@ConfigurationProperties(prefix = "aiagent.conversation-summary")
@Data
public class ConversationSummaryProperties {
    private int maxMessages = 20;
    private int summaryTriggerThreshold = 15;
    private String aiClientId = "3002";
    private int summaryMaxLength = 500;
    private Duration summaryTimeout = Duration.ofSeconds(5);
    private boolean enable = true;
}
```

#### 3. 自动配置类
**描述**: 创建自动配置类简化使用
**建议操作**:
```java
@Configuration
@EnableConfigurationProperties(ConversationSummaryProperties.class)
public class ConversationSummaryAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "aiagent.conversation-summary.enable", havingValue = "true")
    public ConversationSummaryMemory conversationSummaryMemory(
            ConversationSummaryProperties properties,
            SpringContextUtil springContextUtil,
            ChatMemory chatMemory) {
        return new ConversationSummaryMemory(
            springContextUtil, chatMemory,
            properties.getMaxMessages(),
            properties.getSummaryTriggerThreshold(),
            properties.getAiClientId(),
            properties.getSummaryMaxLength(),
            properties.getSummaryTimeout(),
            100
        );
    }
}
```

### 🧪 测试配置

#### 1. 单元测试依赖
**描述**: 确保测试依赖正确配置
**位置**: `pom.xml`
**建议操作**:
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### 2. 测试配置文件
**描述**: 创建测试专用配置
**位置**: `src/test/resources/application-test.yml`
**建议配置**:
```yaml
aiagent:
  conversation-summary:
    ai-client-id: "test-client"
    summary-trigger-threshold: 5
    enable: true
```

### 📝 文档和示例

#### 1. 使用示例文档
**描述**: 创建详细的使用示例
**建议内容**:
- 基本用法示例
- 高级配置示例  
- 常见问题解答
- 性能调优指南

#### 2. API文档更新
**描述**: 更新项目API文档，包含ConversationSummaryMemory相关接口
**建议操作**:
- 在项目README中添加使用说明
- 更新Swagger文档（如果有）
- 添加JavaDoc文档生成

### 🔍 监控和日志

#### 1. 日志配置
**描述**: 配置合适的日志级别
**位置**: `logback-spring.xml`
**建议配置**:
```xml
<logger name="com.zj.aiagemt.service.memory.ConversationSummaryMemory" level="INFO"/>
```

#### 2. 监控指标
**描述**: 添加监控指标收集
**建议指标**:
- 摘要触发频率
- 摘要生成耗时
- AI调用成功率
- 内存使用情况

### 🚀 部署准备

#### 1. 环境变量配置
**描述**: 支持环境变量覆盖配置
**建议环境变量**:
```bash
AIAGENT_CONVERSATION_SUMMARY_ENABLE=true
AIAGENT_CONVERSATION_SUMMARY_AI_CLIENT_ID=3002
AIAGENT_CONVERSATION_SUMMARY_TRIGGER_THRESHOLD=15
```

#### 2. 健康检查
**描述**: 添加健康检查端点
**建议操作**:
```java
@Component
public class ConversationSummaryHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 检查AI客户端连接状态
        // 检查内存使用情况
        return Health.up().build();
    }
}
```

## 操作指引

### 快速开始（5分钟配置）
1. 确保 `SpringContextUtil` 注册为Spring Bean
2. 确保AI客户端Bean（名称：`ai_client_3002`）存在
3. 在需要的地方创建ConversationSummaryMemory实例并加入Advisor链

### 完整配置（推荐）
1. 按照上述配置清单逐项配置
2. 运行单元测试验证功能
3. 在开发环境测试集成效果
4. 添加监控和日志
5. 部署到生产环境

### 问题排查
- **AI调用失败**: 检查AI客户端Bean配置和网络连接
- **摘要不生成**: 检查触发阈值设置和消息计数逻辑
- **内存泄漏**: 定期调用 `cleanupInactiveConversations` 方法
- **性能问题**: 调整触发阈值和摘要长度限制

如有其他问题，请查看日志或联系开发团队。