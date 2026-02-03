# 实施计划：移除 JPA 组件

## 概述

本实施计划将系统地移除项目中所有 JPA 相关的组件，确保项目完全使用 MyBatis Plus 作为唯一的 ORM 框架。删除过程遵循从外到内的顺序，每个步骤后都进行验证，确保安全可控。

## 任务列表

- [x] 1. 准备工作和环境验证
  - 创建 Git 分支 `refactor/remove-jpa-components`
  - 验证当前代码可以正常编译：`mvn clean compile`
  - 运行所有测试确保基线正常：`mvn clean test`
  - 记录当前状态作为回滚点
  - _需求：6.1, 6.2_

- [ ] 2. 删除 JPA 测试类
  - [x] 2.1 删除 JpaChatMessageRepositoryImplTest.java
    - 删除文件：`ai-agent-infrastructure/src/test/java/com/zj/aiagent/infrastructure/chat/repository/JpaChatMessageRepositoryImplTest.java`
    - _需求：3.2, 3.4_
  
  - [ ]* 2.2 验证测试套件
    - 运行测试：`mvn clean test`
    - 确认测试套件中不包含已删除的测试类
    - 确认所有其他测试通过
    - _需求：3.4, 6.2_

- [ ] 3. 删除 JPA 实现类
  - [x] 3.1 删除 JpaChatMessageRepositoryImpl.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/repository/JpaChatMessageRepositoryImpl.java`
    - _需求：3.1, 3.3_
  
  - [ ]* 3.2 验证编译
    - 执行编译：`mvn clean compile`
    - 确认编译成功，无错误
    - 搜索代码引用：`grep -r "JpaChatMessageRepositoryImpl" --include="*.java" ai-agent-infrastructure/src/`
    - 确认无代码引用该类
    - _需求：3.3, 6.1_

- [ ] 4. 删除 JPA Repository 接口
  - [x] 4.1 删除 JpaChatMessageRepository.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/repository/JpaChatMessageRepository.java`
    - _需求：2.2, 2.4_
  
  - [x] 4.2 删除 JpaMessageUpdateHistoryRepository.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/repository/JpaMessageUpdateHistoryRepository.java`
    - _需求：2.3, 2.5_
  
  - [ ]* 4.3 验证编译和代码引用
    - 执行编译：`mvn clean compile`
    - 搜索 JpaChatMessageRepository 引用：`grep -r "JpaChatMessageRepository" --include="*.java" ai-agent-infrastructure/src/`
    - 搜索 JpaMessageUpdateHistoryRepository 引用：`grep -r "JpaMessageUpdateHistoryRepository" --include="*.java" ai-agent-infrastructure/src/`
    - 确认无代码引用这些接口
    - _需求：2.4, 2.5, 6.1_

- [ ] 5. 删除 JPA 实体类
  - [x] 5.1 删除 ChatMessageEntity.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/entity/ChatMessageEntity.java`
    - _需求：1.2, 1.4_
  
  - [x] 5.2 删除 MessageUpdateHistoryEntity.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/persistence/entity/MessageUpdateHistoryEntity.java`
    - _需求：1.3, 1.5_
  
  - [ ]* 5.3 验证编译和代码引用
    - 执行编译：`mvn clean compile`
    - 搜索 ChatMessageEntity 引用：`grep -r "ChatMessageEntity" --include="*.java" ai-agent-infrastructure/src/`
    - 搜索 MessageUpdateHistoryEntity 引用：`grep -r "MessageUpdateHistoryEntity" --include="*.java" ai-agent-infrastructure/src/`
    - 确认无代码引用这些实体
    - _需求：1.2, 1.3, 6.1_
  
  - [ ]* 5.4 验证 @Entity 注解清理
    - 搜索 @Entity 注解：`grep -r "@Entity" --include="*.java" ai-agent-infrastructure/src/`
    - 确认 Infrastructure 层不包含任何 @Entity 注解的类
    - _需求：1.1_

- [x] 6. 检查点 - 确保代码清洁
  - 确保所有编译通过
  - 确保所有测试通过
  - 如有问题，询问用户

- [x] 7. 删除 JPA 配置类
  - [x] 7.1 删除 JpaRepositoryConfig.java
    - 删除文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/JpaRepositoryConfig.java`
    - _需求：4.1_
  
  - [x]* 7.2 验证编译
    - 执行编译：`mvn clean compile`
    - 确认编译成功
    - _需求：6.1_

- [ ] 8. 移除 Maven 依赖
  - [x] 8.1 编辑 pom.xml 移除 JPA 依赖
    - 打开文件：`ai-agent-infrastructure/pom.xml`
    - 删除 `spring-boot-starter-data-jpa` 依赖块
    - 删除 `hibernate-core` 依赖块
    - _需求：5.1, 5.2_
  
  - [ ]* 8.2 验证编译和依赖树
    - 执行编译：`mvn clean compile`
    - 检查依赖树：`mvn dependency:tree | grep -i "jpa\|hibernate"`
    - 确认依赖树中不包含 JPA 或 Hibernate 相关依赖
    - _需求：5.3, 5.4, 6.1_
  
  - [ ]* 8.3 验证 MyBatis Plus 依赖保留
    - 检查依赖树：`mvn dependency:tree | grep -i "mybatis"`
    - 确认 mybatis-plus-spring-boot3-starter 依赖存在
    - _需求：5.5_

- [ ] 9. 清理编译产物
  - [x] 9.1 执行 Maven clean
    - 执行命令：`mvn clean`
    - 确认 target 目录被删除
    - _需求：7.3_
  
  - [ ]* 9.2 重新编译并验证
    - 执行完整编译：`mvn clean compile`
    - 检查 target/classes 目录
    - 确认不存在 JPA 相关的 .class 文件
    - _需求：7.1, 7.4_
  
  - [ ]* 9.3 检查测试编译产物
    - 检查 target/test-classes 目录
    - 确认不存在 JPA 相关的测试 .class 文件
    - _需求：7.2_

- [x] 10. 检查点 - 完整验证
  - 确保所有编译通过
  - 确保所有测试通过
  - 如有问题，询问用户

- [ ] 11. 运行完整测试套件
  - [x] 11.1 执行所有单元测试
    - 运行命令：`mvn clean test`
    - 确认所有测试通过
    - _需求：6.2_
  
  - [ ]* 11.2 验证 MyBatis Plus 实现正常工作
    - 检查 MybatisChatMessageRepositoryImpl 相关测试通过
    - 确认聊天消息 CRUD 操作正常
    - _需求：3.5, 6.4_

- [ ] 12. 应用启动验证（可选）
  - [ ]* 12.1 启动应用并检查日志
    - 启动应用：`mvn spring-boot:run`
    - 收集启动日志
    - 搜索 JPA 关键字：`grep -i "jpa\|hibernate\|entitymanager" app.log`
    - 确认日志中无 JPA 或 Hibernate 初始化信息
    - _需求：4.2, 4.3, 4.4, 6.3, 6.5_
  
  - [ ]* 12.2 验证 Spring 上下文纯净性
    - 使用 Spring Boot Actuator 检查 Bean 列表（如果可用）
    - 确认 Spring 上下文中无 JPA 相关的 Bean
    - _需求：4.2, 4.3, 4.4_

- [ ] 13. 静态代码分析验证
  - [ ]* 13.1 全局搜索 JPA 引用
    - 搜索 JPA 类引用：`grep -r "ChatMessageEntity\|MessageUpdateHistoryEntity\|JpaChatMessageRepository\|JpaMessageUpdateHistoryRepository\|JpaChatMessageRepositoryImpl" --include="*.java" ai-agent-infrastructure/src/`
    - 确认无任何引用
    - _需求：1.2, 1.3, 2.4, 2.5, 3.3_
  
  - [ ]* 13.2 搜索 JPA 注解
    - 搜索 @Entity 注解：`grep -r "@Entity" --include="*.java" ai-agent-infrastructure/src/`
    - 搜索 JpaRepository 继承：`grep -r "extends JpaRepository" --include="*.java" ai-agent-infrastructure/src/`
    - 确认无 JPA 相关注解或继承
    - _需求：1.1, 2.1_
  
  - [ ]* 13.3 验证文件删除完整性
    - 检查所有待删除文件是否已删除
    - 使用 `ls` 命令验证文件不存在
    - _需求：1.4, 1.5, 2.2, 2.3, 3.1, 3.2, 4.1_

- [ ] 14. 提交变更
  - [ ] 14.1 提交所有变更到 Git
    - 添加所有变更：`git add .`
    - 提交变更：`git commit -m "refactor: 移除所有 JPA 组件，统一使用 MyBatis Plus"`
    - 在提交信息中列出所有删除的文件和依赖
    - 引用相关的 issue 编号
    - _需求：所有需求_
  
  - [ ] 14.2 推送分支并创建 Pull Request
    - 推送分支：`git push origin refactor/remove-jpa-components`
    - 创建 Pull Request
    - 在 PR 描述中说明变更内容和验证结果

## 注意事项

### 可选任务说明

- 标记为 `*` 的子任务是验证性任务，可以根据项目需求选择是否执行
- 建议在开发环境中执行所有验证任务，确保删除过程安全
- 在生产环境部署前，必须执行所有验证任务

### 错误处理

如果在任何步骤遇到错误：

1. **不要继续下一步**
2. **分析错误原因**
3. **根据设计文档中的错误处理策略处理**
4. **如果无法解决，使用 Git 回滚：`git reset --hard HEAD`**
5. **重新评估删除计划**

### 回滚策略

如果需要回滚：

```bash
# 回滚所有未提交的变更
git reset --hard HEAD
git clean -fd

# 恢复 pom.xml（如果已修改）
git checkout HEAD -- ai-agent-infrastructure/pom.xml

# 重新安装依赖
mvn clean install
```

### 验证检查清单

在完成所有任务后，确认以下检查项：

- [ ] 所有 JPA 相关的 Java 文件已删除
- [ ] 所有 JPA 相关的 Maven 依赖已移除
- [ ] 项目能够成功编译，无错误和警告
- [ ] 所有测试通过
- [ ] 应用能够正常启动和运行（如果执行了启动验证）
- [ ] 聊天消息功能使用 MyBatis Plus 正常工作
- [ ] 依赖树中不包含任何 JPA 相关依赖
- [ ] 代码中无任何 JPA 类的引用
- [ ] Spring 上下文中无 JPA 相关的 Bean（如果执行了启动验证）
- [ ] 日志中无 JPA 或 Hibernate 相关信息（如果执行了启动验证）

## 预期结果

完成所有任务后，项目将：

1. ✅ 完全移除 JPA 组件
2. ✅ 统一使用 MyBatis Plus 作为唯一 ORM 框架
3. ✅ 减少代码复杂度和依赖冲突风险
4. ✅ 减小应用打包体积
5. ✅ 提高项目可维护性
6. ✅ 保持所有现有功能正常工作
