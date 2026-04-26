---
created: 2026-04-24
reason: 逐章评估论文现状并形成后续修订计划依据
---

# 论文逐章评估与修订建议

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

## 总体判断
当前论文已经具备完整骨架，尤其是工作流调度、人工审核恢复、SSE 推流这三部分写得最扎实，和实现代码对应度最高，例如 [`SchedulerService.startExecution`](ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:80)、[`SchedulerService.resumeExecution`](ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:384)、[`WorkflowController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:36)、[`HumanReviewController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:30)。

主要问题不是“没有内容”，而是“模块重心偏了、部分表述超前、与最终论文范围不完全一致”。当前最需要修的不是工作流主体，而是把全文统一收束到你已经确定的五个模块上，并补齐 Agent 管理、知识库实现、登录注册这三块在实现章和测试章中的篇幅。

---

## 第 0 部分：摘要 [`论文/chapter/abstract.tex`](论文/chapter/abstract.tex:1)

### 已有优点
- 研究背景和项目定位比较清楚。
- 核心技术路线已经点明：Spring Boot、DDD、DAG、人工检查点、SSE。
- 中英文摘要结构对应，后续同步修改相对容易。

### 写得不够的地方
1. 模块表述与当前论文范围不一致。
   - 现在摘要里第 3 点写的是“统一上下文管理系统”，第 4 点写的是 “MCP 协议工具集成”，但你当前已经确定论文重点是五个模块：Agent 管理与可视化编排、工作流调度、人工审核恢复、知识库检索增强、用户认证与安全支撑。
   - 也就是说，摘要没有把 Agent 管理和用户认证提到应有位置。

2. MCP 表述偏重，容易超出当前论文主线。
   - [`abstract.tex`](论文/chapter/abstract.tex:21) 直接把 MCP 作为核心功能列出，但从你当前已核验的实现链路来看，论文最强证据并不在 MCP，而在工作流、审核、知识库、认证。
   - 如果后文没有专章支撑，摘要里突出 MCP 会造成前后失衡。

3. “三层记忆系统”表述略满。
   - [`abstract.tex`](论文/chapter/abstract.tex:19) 到 [`abstract.tex`](论文/chapter/abstract.tex:20) 把 STM、LTM、Awareness 并列为成熟体系，但当前代码强支撑的是 STM 和基于向量检索的长期知识/记忆注入，Awareness 更适合写成“执行态上下文感知”而不是与前两者并列成熟子系统。

4. 性能结论过硬。
   - [`abstract.tex`](论文/chapter/abstract.tex:25) 直接写 “API P95 响应延迟在 200 ms 以内”，这类句子必须严格来自测试章可复现数据。如果你的测试章最终不提供完整实验环境、指标口径和测量方法，这句要么弱化，要么把表述限定为“在测试环境下核心接口满足基本实时性要求”。

### 修订建议
- 摘要的五个核心功能建议重排为：
  1. Agent 管理与可视化编排
  2. DAG 工作流调度执行
  3. 人工审核与恢复机制
  4. 知识库与检索增强
  5. 用户认证与系统安全支撑
- MCP 不删除也可以，但建议降级为“可扩展能力”而不是摘要主贡献。
- “统一上下文管理系统”建议改写为“结合会话历史与向量检索的上下文增强机制”。
- 性能指标建议与第五章严格对齐后再保留。

---

## 第 1 章：绪论 [`论文/chapter/ch1_intro.tex`](论文/chapter/ch1_intro.tex:1)

### 已有优点
- 背景、意义、国内外现状、论文结构这四个部分框架齐全。
- 适合承接“为什么做 Java 原生 Agent 编排平台”这一主题。

### 写得不够的地方
1. “多智能体”主线仍然偏重。
   - 当前绪论如果仍以“多智能体协作系统”为主陈述，就会与项目已验证实现不完全一致。
   - 你的现阶段论文最稳妥定位应是“面向 AI Agent 编排与执行的系统”，而不是把重点放在 Swarm 或多 Agent 协同上。

2. 研究目标和正文主模块不完全对齐。
   - 如果绪论目标中大量提到 MCP、动态工具发现、多智能体自治协同，但正文实现章主要写工作流、审核、知识库、认证，会产生“开题目标大于落地实现”的观感。

3. 创新点写法需要收束。
   - 建议创新点不要写成“提出完整多层智能体体系”，而要写成“在 Java 技术栈下实现可视化编排、可恢复执行、可审核、可检索增强的 AI Agent 平台”。

### 修订建议
- 将研究对象统一改成“AI Agent 智能体编排系统”或“AI Agent 工作流编排与执行平台”。
- 多智能体协同保留在“未来工作”或“拓展方向”中。
- 绪论中的贡献点必须能在第四章和第五章找到一一对应的实现与测试支撑。

---

## 第 2 章：需求分析 [`论文/chapter/ch2_requirements.tex`](论文/chapter/ch2_requirements.tex:1)

### 已有优点
- 用例和功能需求基本完整。
- 工作流执行、人工审核、知识库检索这几块需求描述比较像样。
- 非功能需求也已涉及安全、性能、可维护性。

### 写得不够的地方
1. 五模块范围还没有彻底统一。
   - 当前第二章如果还保留六模块或把“上下文管理”单独列成一级功能，而没有把“用户认证与系统安全支撑”明确提升，会和你现在的论文范围不一致。

2. Agent 管理与可视化编排需求不够展开。
   - 既然它已被列为论文主模块，就应明确写出：
     - Agent 的创建、更新、发布、版本管理
     - 节点模板配置
     - 画布拖拽、连线、保存 graphJson
   - 这部分在 [`docs/api/agent.md`](docs/api/agent.md:11) 到 [`docs/api/agent.md`](docs/api/agent.md:271) 其实已经有较好素材。

3. 用户认证需求深度不足。
   - 你已经决定写登录和注册功能，但如果第二章对注册、邮箱验证码、找回密码、JWT 鉴权、登录限流没有明确需求分解，后文实现会显得突然。
   - 可直接对齐 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:30)。

4. 知识库需求要更贴近真实实现。
   - 目前知识库如果只写“上传文档并用于问答”会偏浅。
   - 应补上文档分块、异步处理、向量化入库、检索调用链等需求描述，对齐 [`KnowledgeController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/knowledge/web/KnowledgeController.java:32) 和 [`KnowledgeApplicationService`](ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java:34)。

### 修订建议
- 第二章建议显式重构为五个功能模块小节。
- 给每个模块补“目标用户 + 核心场景 + 输入输出 + 成功判定”。
- 用户认证模块必须单列，不要只藏在非功能需求里。

---

## 第 3 章：系统设计 [`论文/chapter/ch3_design.tex`](论文/chapter/ch3_design.tex:1)

### 已有优点
- 系统架构、分层设计、数据库设计、关键技术选型框架齐全。
- DDD 分层和前后端分离架构是这篇论文的优势点。

### 写得不够的地方
1. 模块设计仍未完全贴合五模块主线。
   - 如果第三章仍把“统一上下文管理模块”列成主模块，而没有把用户认证与系统安全支撑作为主模块之一，就会与已确定论文范围冲突。

2. Agent 管理与可视化编排设计应更细。
   - 这部分应补设计图或文字说明：
     - 节点元数据获取
     - 画布编排与 `graphJson` 持久化
     - 版本发布与回滚
   - 依据材料可来自 [`docs/api/agent.md`](docs/api/agent.md:209) 到 [`docs/api/agent.md`](docs/api/agent.md:449)。

3. 知识库设计需要更完整的数据链路。
   - 目前设计章若只停留在“接入 Milvus 做向量检索”，还不足以支撑第四章实现。
   - 应明确写：上传文件 → 文件校验 → MinIO 存储 → 文档记录入库 → 异步切分与向量化 → 检索增强。

4. 用户认证与安全设计需要从“配置项”升格为“模块设计”。
   - 建议显式说明：
     - 注册/登录/重置密码流程
     - 邮箱验证码发送机制
     - JWT 令牌认证
     - BCrypt 密码加密
     - 限流与安全校验
   - 对应实现依据在 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:50) 到 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:230) 与 [`EmailServiceImpl`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/email/EmailServiceImpl.java:18)。

5. 数据库设计与模块关注点不完全匹配。
   - 如果当前库表设计重点落在会话、执行、日志，却没有交代用户认证、知识库文档、审批记录等关键表之间关系，论文会显得只展示了部分系统。

### 修订建议
- 第三章最好按照“五模块 + 基础支撑架构”重排。
- 可增加一个“核心数据流”小节，把 Agent 编排、Workflow 执行、Review 审核、Knowledge 检索、Auth 认证五条主链并列描述。

---

## 第 4 章：系统实现 [`论文/chapter/ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:1)

### 已有优点
- 这是当前论文写得最扎实的一章。
- 工作流启动、DAG 推进、人工审核恢复、并发控制、SSE 推流，这几节都能直接映射到真实实现。
- 例如：
  - 工作流启动对应 [`SchedulerService.startExecution`](ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:80)
  - 恢复执行对应 [`SchedulerService.resumeExecution`](ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:384)
  - SSE 推流对应 [`WorkflowController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:36)
  - 人工审核接口对应 [`HumanReviewController`](ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:30)

### 写得不够的地方
1. 章节开头主线需要调整。
   - [`ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:5) 到 [`ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:7) 目前写成“三个核心机制”：工作流、人工审核、统一上下文管理。
   - 这已经落后于你当前确定的五模块论文结构。
   - 第四章不应只围绕三个机制，而应覆盖五个模块中的“实现部分”。

2. Agent 管理与可视化编排实现缺失。
   - 这是当前第四章最大缺口。
   - 你已经把它列为论文主模块，但第四章现有内容几乎看不到：
     - Agent 创建与更新
     - 版本发布与回滚
     - 节点模板与字段配置
     - 前端可视化画布保存 graphJson
   - 这会导致论文章节结构“说有这个模块，但实现章没展开”。

3. 知识库与检索增强实现篇幅不足。
   - 目前第四章偏重把知识能力写成“记忆水合”背景中的一部分，例如 [`ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:29) 到 [`ch4_implementation.tex`](论文/chapter/ch4_implementation.tex:31)。
   - 但你论文主模块里已经把“知识库与检索增强”单独列出来，这就必须单列实现小节，至少写清：
     - 数据集创建与文档上传接口
     - 文件落盘或对象存储
     - 文档异步解析与切分
     - 向量化入库
     - 查询时检索增强调用
   - 可以直接锚定 [`KnowledgeApplicationService.uploadDocument`](ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java:179) 与 [`MilvusVectorStoreAdapter.searchKnowledge`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:93)。

4. 用户认证与安全支撑实现缺失。
   - 当前第四章如果没有独立写注册、登录、验证码、找回密码、JWT，这也是明显缺口。
   - 至少要写出：
     - 发送验证码 [`sendVerificationCode`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:50)
     - 注册 [`register`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:80)
     - 登录 [`login`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:124)
     - 重置密码 [`resetPassword`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:186)
     - 异步邮件发送 [`EmailServiceImpl`](ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/email/EmailServiceImpl.java:18)
   - 否则第五章若测试登录注册，第四章没有实现支撑，会前后断裂。

5. “统一上下文管理 / 三层记忆系统”建议降级为支持性机制，而不是第四章主轴。
   - 你可以保留相关内容，但更建议把它放入“工作流执行的上下文增强机制”或“知识增强支撑机制”中。
   - 这样更贴近代码真实强项，也更贴近你的五模块主线。

### 修订建议
第四章建议重构为以下结构：
1. Agent 管理与可视化编排实现
2. 工作流调度执行实现
3. 人工审核与恢复机制实现
4. 知识库与检索增强实现
5. 用户认证与系统安全支撑实现
6. 实时交互与前端联动实现

其中第 2、3、6 节你现有内容可大量复用；第 1、4、5 节需要补写。

---

## 第 5 章：系统测试 [`论文/chapter/ch5_testing.tex`](论文/chapter/ch5_testing.tex:1)

### 已有优点
- 已有单元测试、集成测试、功能测试、性能测试的分层意识。
- 工作流主链路和人工审核链路的测试写得相对完整。

### 写得不够的地方
1. 测试覆盖与论文主模块不匹配。
   - 既然论文最终写五个模块，那么测试章也应尽量对应五模块。
   - 当前如果测试重点只落在工作流、审核、性能，而没有 Agent 管理、知识库、用户认证，就会显得“写了实现，没证明可用”。

2. 用户认证测试明显不足。
   - 建议补：
     - 注册成功/验证码错误
     - 登录成功/密码错误
     - 验证码发送频率限制
     - 连续登录失败限制
     - 重置密码流程
   - 这些都可以直接从 [`UserAuthenticationDomainService`](ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java:30) 中抽测试点。

3. Agent 管理与编排测试不足。
   - 建议补：
     - Agent 创建与发布
     - 画布保存 graphJson
     - 已发布版本执行读取正确版本快照
   - 可结合 [`docs/api/agent.md`](docs/api/agent.md:11) 到 [`docs/api/agent.md`](docs/api/agent.md:271) 的接口说明组织测试。

4. 知识库测试应增加链路闭环。
   - 建议补：
     - 数据集创建
     - 文档上传成功/失败
     - 异步处理状态变化
     - 检索结果命中
   - 如果没有完整自动化测试，也可以用功能测试表格形式体现。

5. MCP 测试如果没有充足实现依据，建议降级或删除。
   - 否则容易让老师追问“这一块实际如何验证的”。

6. 性能测试结论要与摘要完全一致。
   - 如果第五章不能严谨证明 P95 200 ms，就不要在摘要和结论硬写死。

### 修订建议
- 第五章测试案例建议按五模块重排。
- 每个模块至少给出 1 组功能测试案例和必要时的单元/集成测试说明。
- 对性能指标增加“测试环境、并发数、接口范围、采样方式”的交代。

---

## 第 6 章：总结与展望 [`论文/chapter/ch6_conclusion.tex`](论文/chapter/ch6_conclusion.tex:1)

### 已有优点
- 结构完整，具备总结与未来工作两个部分。
- 适合做最后统一收束。

### 写得不够的地方
1. 总结点和当前五模块不完全一致。
   - 如果结论仍以“统一上下文管理”“MCP”“多智能体”作为主要贡献，而忽略 Agent 管理和用户认证，会与正文主线不一致。

2. 贡献总结有过度抽象风险。
   - 结论最好回到可验证结果：
     - 可视化编排能力
     - DAG 调度执行能力
     - 人工审核恢复能力
     - 知识增强能力
     - 用户认证安全能力
   - 这些比抽象谈“智能涌现”“自治协同”更稳。

3. 展望应把“多智能体协同”和“MCP 扩展生态”放在未来工作中。
   - 这样既不浪费现有材料，又不会让已实现范围显得夸大。

### 修订建议
- 结论部分按五模块做归纳。
- 展望部分保留：多智能体协作、更强工具生态、更完善记忆系统、更大规模性能优化。
- 避免把尚未充分论证的能力写成已完成贡献。

---

## 优先级最高的修订点

### 第一优先级
1. 全文统一成五模块主线。
2. 下调多智能体、MCP、三层记忆的中心地位。
3. 第四章补写：
   - Agent 管理与可视化编排实现
   - 知识库与检索增强实现
   - 用户认证与系统安全支撑实现

### 第二优先级
1. 第二章补足 Agent 需求、认证需求、知识库闭环需求。
2. 第三章补足对应模块设计与数据流。
3. 第五章补足 Agent、知识库、认证测试。

### 第三优先级
1. 摘要与结论统一改口径。
2. 中英文摘要同步修订。
3. 性能指标与测试数据统一。

---

## 建议的修订执行清单
1. 先改摘要、绪论、结论的总口径，统一五模块范围。
2. 再重构第三章与第四章目录结构。
3. 补写第四章缺失的三大实现小节：
   - Agent 管理与可视化编排
   - 知识库与检索增强
   - 用户认证与系统安全支撑
4. 回补第二章需求与第五章测试，使其与第四章一一对应。
5. 最后统一检查：术语、图号、代码编号、性能结论、中英文摘要一致性。
