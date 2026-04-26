---
created: 2026-04-24
reason: 基于逐章评估结果形成可直接执行的论文修订清单，供后续 code 模式逐项落地
---

# 论文修订执行清单

## 选中的模块
- Agent 管理与可视化编排
- 工作流调度执行
- 人工审核与恢复机制
- 知识库与检索增强
- 用户认证与系统安全支撑

## 已加载的模块 AGENT 列表
- [`ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/AGENT.md`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/AGENT.md)
- [`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md)
- [`ai-agent-foward/src/modules/agent/AGENT.md`](ai-agent-foward/src/modules/agent/AGENT.md)
- [`ai-agent-foward/src/modules/workflow/AGENT.md`](ai-agent-foward/src/modules/workflow/AGENT.md)

## 修订总原则
1. 全文统一到五模块主线，不再以多智能体、MCP、三层记忆作为主叙事。
2. 所有“核心贡献”都必须能在实现章与测试章中找到对应证据。
3. MCP、Awareness、多智能体协作可保留，但应降级为扩展能力、背景材料或未来工作。
4. 所有性能结论必须与第五章测试口径保持一致。

---

## 第一阶段：统一全文口径

### 任务 1：重写摘要的功能项排序与措辞
- 目标文件：[`论文/chapter/abstract.tex`](论文/chapter/abstract.tex:1)
- 操作内容：
  1. 将摘要中的核心功能重排为五项：
     - Agent 管理与可视化编排
     - 工作流调度执行
     - 人工审核与恢复机制
     - 知识库与检索增强
     - 用户认证与系统安全支撑
  2. 将“统一上下文管理系统”改写为知识增强或上下文增强支撑能力，不再单列为与五模块并行的核心模块。
  3. 将 MCP 从摘要主贡献中降级，必要时改成“系统具备一定工具扩展能力”。
  4. 将 P95 200 ms 这类强结论改成与测试章一致的表达，若测试证据不足则弱化。
- 交付标准：
  - 中英文摘要术语一致
  - 五模块口径与正文一致
  - 不出现无法在正文支撑的硬性结论

### 任务 2：统一绪论中的研究对象与贡献描述
- 目标文件：[`论文/chapter/ch1_intro.tex`](论文/chapter/ch1_intro.tex:1)
- 操作内容：
  1. 将“多智能体系统”主表述收束为“AI Agent 编排系统”或“AI Agent 工作流编排与执行平台”。
  2. 调整论文目标、研究内容、论文贡献，使其与五模块完全对齐。
  3. 将多智能体协作、MCP 生态、增强型记忆机制写入扩展方向或后续章节的次级表述。
- 交付标准：
  - 绪论与摘要、结论的主线一致
  - 创新点均能映射到后文实现和测试

### 任务 3：统一结论中的成果归纳口径
- 目标文件：[`论文/chapter/ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)
- 操作内容：
  1. 按五模块重写“本文完成的工作”。
  2. 将“多智能体协同”“MCP 扩展生态”“更完整记忆体系”调整到未来展望。
  3. 删除或弱化无法被第四章、第五章支撑的贡献表述。
- 交付标准：
  - 结论和摘要完全呼应
  - 未来工作与已完成工作边界清晰

---

## 第二阶段：重构正文目录与模块映射

### 任务 4：重构第二章需求分析为五模块结构
- 目标文件：[`论文/chapter/ch2_requirements.tex`](论文/chapter/ch2_requirements.tex:1)
- 操作内容：
  1. 将需求模块统一整理为五个一级或二级功能模块。
  2. 为 Agent 管理与可视化编排补充需求：
     - Agent 创建、更新、发布、回滚
     - 节点模板配置
     - 画布拖拽连线与 `graphJson` 保存
     - 依据可参考 [`docs/api/agent.md`](docs/api/agent.md:11)
  3. 为知识库补充闭环需求：
     - 数据集创建
     - 文档上传
     - 分块与向量化
     - 检索增强
     - 对齐 [`KnowledgeController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/web/KnowledgeController.java:32)
  4. 为用户认证补充独立需求：
     - 注册
     - 登录
     - 邮箱验证码
     - 找回密码
     - JWT 访问控制
     - 限流与密码安全
     - 对齐 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:30)
- 交付标准：
  - 第二章的模块划分与论文最终模块范围一致
  - 每个模块都能支撑第四章对应实现小节

### 任务 5：重构第三章系统设计的主模块结构
- 目标文件：[`论文/chapter/ch3_design.tex`](论文/chapter/ch3_design.tex:1)
- 操作内容：
  1. 将第三章的功能模块设计按五模块重写。
  2. 补一段 Agent 管理与编排的数据流设计：
     - 前端画布编排
     - 节点元数据获取
     - `graphJson` 持久化
     - 版本发布与回滚
  3. 补一段知识库数据流设计：
     - 文件校验
     - MinIO 存储
     - 文档记录入库
     - 异步处理
     - Milvus 检索
     - 依据可参考 [`KnowledgeApplicationService`](ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java:34) 与 [`MilvusVectorStoreAdapter`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:32)
  4. 补一段认证安全设计：
     - 注册与登录流程
     - 验证码发送
     - BCrypt 加密
     - JWT 鉴权
     - 登录失败限制
     - 依据可参考 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:124)
- 交付标准：
  - 第三章能自然引出第四章五模块实现
  - 设计描述与现有真实代码结构一致

---

## 第三阶段：补齐第四章实现缺口

### 任务 6：新增 Agent 管理与可视化编排实现小节
- 目标文件：[`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
- 操作内容：
  1. 在第四章中新增“Agent 管理与可视化编排实现”。
  2. 小节建议包含：
     - Agent 创建、修改、发布、回滚流程
     - 前端画布编排与节点元数据加载
     - `graphJson` 的保存与后端解析
     - 版本快照与执行时版本选择
  3. 素材来源优先使用：
     - [`docs/api/agent.md`](docs/api/agent.md:11)
     - [`SchedulerService.startExecution`](ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:80)
     - 前端截图可优先使用工作流编辑器界面素材
- 交付标准：
  - 第四章正式覆盖你定义的第一个核心模块
  - 与后续工作流执行实现形成前后衔接

### 任务 7：保留并微调工作流调度实现小节
- 目标文件：[`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
- 操作内容：
  1. 保留现有工作流启动、DAG 推进、策略模式、恢复执行、并发安全内容。
  2. 将“三个核心机制”开头改成“五模块实现总览”。
  3. 将“统一上下文管理系统”从主轴降级为工作流或知识增强的支撑机制。
- 交付标准：
  - 现有优势内容不被破坏
  - 章节结构与新主线兼容

### 任务 8：单列知识库与检索增强实现小节
- 目标文件：[`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
- 操作内容：
  1. 新增“知识库与检索增强实现”小节。
  2. 必写内容：
     - 数据集创建与管理
     - 文档上传与文件校验
     - MinIO 存储
     - 异步文档处理
     - 文档切分与向量化
     - 检索接口与工作流中的知识召回
  3. 关键代码锚点：
     - [`KnowledgeController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/web/KnowledgeController.java:47)
     - [`KnowledgeApplicationService.uploadDocument`](ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java:179)
     - [`MilvusVectorStoreAdapter.searchKnowledge`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:93)
- 交付标准：
  - 知识库不再只是“记忆水合的一部分”
  - 能独立构成论文主模块实现章节

### 任务 9：新增用户认证与系统安全支撑实现小节
- 目标文件：[`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
- 操作内容：
  1. 新增“用户认证与系统安全支撑实现”小节。
  2. 必写内容：
     - 验证码发送
     - 注册流程
     - 登录流程
     - 找回密码
     - 密码加密
     - 邮件异步发送
     - 登录失败与发送频率限制
  3. 关键代码锚点：
     - [`sendVerificationCode`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:50)
     - [`register`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:80)
     - [`login`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:124)
     - [`resetPassword`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:186)
     - [`EmailServiceImpl`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/email/EmailServiceImpl.java:18)
- 交付标准：
  - 第四章补齐第五个论文主模块
  - 能与第二章需求和第五章测试形成闭环

### 任务 10：整理第四章最终推荐结构
- 目标文件：[`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
- 推荐结构：
  1. Agent 管理与可视化编排实现
  2. 工作流调度执行实现
  3. 人工审核与恢复机制实现
  4. 知识库与检索增强实现
  5. 用户认证与系统安全支撑实现
  6. SSE 实时交互与前端联动实现
- 交付标准：
  - 五模块全部落地
  - 章节阅读逻辑顺畅

---

## 第四阶段：补齐第五章测试闭环

### 任务 11：重构测试章节为五模块对应结构
- 目标文件：[`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
- 操作内容：
  1. 按五模块重排测试内容。
  2. 每个模块至少给出一组功能测试或集成测试说明。
  3. 避免测试只偏向工作流和审核。
- 交付标准：
  - 测试覆盖范围与论文主模块匹配

### 任务 12：补充 Agent 管理与编排测试
- 目标文件：[`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
- 操作内容：
  1. 增加 Agent 创建、保存、发布、执行版本读取测试。
  2. 如已有前端测试素材，可简述编排页的基本操作验证。
- 交付标准：
  - 第一模块在测试章有实际验证条目

### 任务 13：补充知识库模块测试
- 目标文件：[`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
- 操作内容：
  1. 增加数据集创建、文档上传、处理成功、检索命中的测试案例。
  2. 若自动化覆盖不足，可使用功能测试表形式补齐。
- 交付标准：
  - 知识库模块形成独立验证闭环

### 任务 14：补充用户认证与安全测试
- 目标文件：[`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
- 操作内容：
  1. 增加注册成功、验证码错误、登录成功、密码错误、登录限流、重置密码测试。
  2. 将安全性验证与功能性验证区分开写。
- 交付标准：
  - 认证模块不再只有实现没有测试

### 任务 15：校正性能测试结论
- 目标文件：[`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
- 操作内容：
  1. 检查性能指标是否能支撑摘要和结论。
  2. 明确写出测试环境、并发方式、样本范围、核心接口。
  3. 若当前数据不够严谨，则弱化摘要和结论中的性能表述。
- 交付标准：
  - 性能结论与摘要、结论保持一致
  - 不留下明显可追问漏洞

---

## 第五阶段：全文一致性收尾检查

### 任务 16：统一术语
- 目标文件：
  - [`论文/chapter/abstract.tex`](论文/chapter/abstract.tex:1)
  - [`论文/chapter/ch1_intro.tex`](论文/chapter/ch1_intro.tex:1)
  - [`论文/chapter/ch2_requirements.tex`](论文/chapter/ch2_requirements.tex:1)
  - [`论文/chapter/ch3_design.tex`](论文/chapter/ch3_design.tex:1)
  - [`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
  - [`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)
  - [`论文/chapter/ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)
- 操作内容：
  1. 统一“Agent 编排系统”“工作流执行”“人工审核”“知识库检索增强”“用户认证与系统安全支撑”等术语。
  2. 统一是否使用“人工检查点”还是“人工审核节点”。
  3. 统一中文术语和英文翻译。
- 交付标准：
  - 全文表述一致，不互相打架

### 任务 17：统一图号、代码编号、引用锚点
- 目标文件：全文相关章节
- 操作内容：
  1. 检查新增小节后图号、表号、代码编号是否连续。
  2. 检查代码标题是否与真实代码含义匹配。
  3. 检查是否有“文中提到但未出现”的图表。
- 交付标准：
  - 论文排版层面无明显结构错误

### 任务 18：统一中英文摘要与结论口径
- 目标文件：[`论文/chapter/abstract.tex`](论文/chapter/abstract.tex:1)、[`论文/chapter/ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)
- 操作内容：
  1. 核查中文摘要、英文摘要、结论中的五模块顺序和术语是否一致。
  2. 检查是否仍残留多智能体、MCP、三层记忆的过强主叙事。
- 交付标准：
  - 开头与结尾形成完整闭环

---

## 推荐实际执行顺序
1. 先改 [`abstract.tex`](论文/chapter/abstract.tex:1)、[`ch1_intro.tex`](论文/chapter/ch1_intro.tex:1)、[`ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)，统一总口径。
2. 再改 [`ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)，补齐三大缺口模块。
3. 再回补 [`ch2_requirements.tex`](论文/chapter/ch2_requirements.tex:1) 和 [`ch3_design.tex`](论文/chapter/ch3_design.tex:1)，让需求与设计匹配实现。
4. 最后补 [`ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)，形成测试闭环。
5. 末尾做全篇术语、图号、结论一致性检查。

---

## 建议切换到 code 模式后的首批修改范围
如果下一步进入 code 模式，建议第一批只改这 4 个文件：
1. [`论文/chapter/abstract.tex`](论文/chapter/abstract.tex:1)
2. [`论文/chapter/ch1_intro.tex`](论文/chapter/ch1_intro.tex:1)
3. [`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)
4. [`论文/chapter/ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)

原因：
- 这四个文件最直接决定全文主线是否统一。
- 第四章是当前最大缺口，优先补它最划算。
- 等第四章补齐后，再回头改第二章、第三章、第五章会更顺。
