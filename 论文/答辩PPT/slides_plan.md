---
title: 基于 Spring Boot 的 AI Agent 智能体编排系统的设计与实现
---

## 1. [cover] 基于 Spring Boot 的 AI Agent 智能体编排系统
副标题：本科毕业设计答辩
学院：西南科技大学 计算机科学与技术学院
答辩人：zj669　　指导教师：（请填写）
2026 年 5 月

## 2. [content] 汇报提纲
01　选题背景与研究意义
02　国内外现状与核心问题
03　需求分析与系统设计
04　核心模块实现
05　主要创新点
06　测试与部署
07　总结与展望

## 3. [content] 选题背景与研究意义
大模型推动 AI 应用从单轮问答走向流程化任务执行。

企业业务场景的新诉求
过程可观察　结果可校验　操作可追溯

研究意义
为 Java 业务系统提供可视化 Agent 编排参考方案
为高风险流程提供 "自动执行 + 人工把关" 的工程范式

## 4. [content] 国内外现状与核心问题
现状梳理
低代码平台　Dify / Coze　配置直观，运行时托管在平台侧
开发框架　LangGraph / AutoGen　图结构编排成熟，但生态在 Python
Java 生态　模型调用齐全，可视化编排实践相对较少

两个核心问题
一　Java 业务系统缺少图结构 Agent 编排与状态恢复的配套实现
二　多数工作流 "一启动跑到底"，关键节点缺少人工把关闸门

## 5. [content] 研究目标与主要工作
目标
在 Spring Boot 体系下构建可视化、可控、可追溯的 Agent 编排原型

主要工作（五项）
一　可视化 Agent 编排（草稿、发布、回滚）
二　DAG 工作流调度（节点状态机、条件分支）
三　人工检查点（执行前后两类暂停 + 审批恢复）
四　知识库与检索增强（异步分块 + 向量化）
五　用户认证与基本安全支撑

## 6. [content] 需求分析
用户角色（两类）
应用构建者　配置流程、维护知识库、发布版本
业务使用者　发起任务、查看进度、参与人工审核

功能模块（五大）
Agent 管理　工作流调度　人工审核　知识库　用户认证

非功能指标（原型级）
核心接口 P95 ≤ 200ms　SSE 推流 P95 ≤ 500ms　并发工作流 ≥ 5 路

## 7. [content] 系统总体架构
四层后端 + 前后端分离

接口层　REST 控制器　SSE 通道　参数校验
应用层　Agent / 调度 / 知识 / 用户 服务编排
领域层　Execution 聚合根　WorkflowGraph　端口接口
基础设施层　MySQL　Redis　Milvus　MinIO　节点执行器

前端
React 19 + TypeScript + React Flow 画布 + SSE 事件接收

设计原则
domain 层零框架依赖　跨域协调放在 application 层

## 8. [content] 工作流执行状态机
六个状态
PENDING　RUNNING　PAUSED_FOR_REVIEW　SUCCEEDED　FAILED　CANCELLED

关键转换
启动　PENDING → RUNNING
人工审核　RUNNING → PAUSED_FOR_REVIEW
审批通过　PAUSED_FOR_REVIEW → RUNNING
审批拒绝　PAUSED_FOR_REVIEW → FAILED
节点全部完成　RUNNING → SUCCEEDED

## 9. [content] 数据库与技术选型
最小核心数据表（8 张）
user_info / agent_info / agent_version
workflow_execution / workflow_node_execution_log
workflow_human_review_record
knowledge_dataset / knowledge_document

技术选型
后端　Java 21 + Spring Boot 3.4.9 + Spring AI 1.0.1
存储　MySQL 8.0　Redis 7　Milvus 2.3.3　MinIO
前端　React 19 + React Flow 12 + Vite 7
部署　Docker Compose 容器化

## 10. [content] 实现一　Agent 可视化编排
画布编排
React Flow 拖拽节点 → 序列化为 graphJson

版本管理（草稿 + 发布快照 + 回滚）
草稿　仅更新编辑态，不影响运行
发布　图校验 → 写入 agent_version 快照 → 更新已发布指针
回滚　按版本号读取快照恢复

衔接运行时
执行优先读取已发布版本，草稿修改不影响在跑实例

## 11. [content] 实现二　DAG 调度执行
核心聚合根　Execution

调度流程
1　hydrateMemory 注入会话历史与检索结果
2　Execution.start 校验图结构与环路
3　调度器异步派发首批就绪节点
4　节点完成 → advance 推进状态
5　条件分支 → 递归剪枝未选中路径

节点执行器（策略模式）
LLM　CONDITION　KNOWLEDGE　TOOL　HTTP　START　END

## 12. [content] 实现三　人工检查点（核心）
两种触发阶段

BEFORE_EXECUTION（节点执行前）
审批人修改节点输入
通过后重新执行节点
场景　高风险操作预审

AFTER_EXECUTION（节点执行后）
审批人修改节点输出
通过后跳过执行，直接推进
场景　AI 输出内容人工校正

触发链路
checkPause → 分布式锁内置 PAUSED_FOR_REVIEW
写入 Redis 待审批队列 → SSE 推送暂停事件

## 13. [content] 实现四　知识库与检索增强
文档接入（异步解耦）
上传 → MinIO 存储 → PENDING 文档
@Async 处理器　解析 → 分块 → 向量化 → 写入 Milvus

文档状态机
PENDING → PROCESSING → COMPLETED / FAILED

运行时检索
工作流知识节点拼接查询
嵌入模型生成查询向量
Milvus 语义召回片段注入 LLM 上下文

## 14. [content] 主要创新点
一　Java 栈 × 可视化编排 × 人工审核三位一体
将 LangGraph 风格的图结构编排在 Spring Boot 中工程化落地

二　双触发阶段的人工检查点
BEFORE_EXECUTION 改输入重执行　AFTER_EXECUTION 改输出直接放行
两种语义覆盖 "预审" 与 "校正" 两类典型业务

三　工作流恢复的双层并发保护
Redis 分布式锁保证临界区独占
Execution.version 乐观锁兜底，并发审批返回 409

四　知识接入异步化
上传接口响应缩至 500ms 内，向量化解析在后台完成

## 15. [content] 系统功能演示
界面与流程截图
Agent 可视化编排画布　工作流执行实时画布
人工审核面板　知识库管理页

演示要点
节点拖拽与参数配置
执行实时状态推送
节点暂停 → 审批 → 恢复
知识检索测试

（答辩现场可切换至系统真机演示 1-2 分钟）

## 16. [content] 开发难点与解决方案
DAG 环路死锁　start 前 DFS 递归栈判环（O(V+E)）
并发审批冲突　Redis 分布式锁 + 乐观锁双保险
重复暂停　reviewedNodes 集合标记已审节点
SSE 长连接中断　服务端 15 秒心跳 ping
向量化阻塞上传　@Async 异步解耦 + 状态回写

## 17. [content] 系统测试方案
单元测试（JUnit 5 + Mockito）
42 个用例　行覆盖 78%　分支覆盖 71%

功能测试（黑盒）
35 个用例覆盖五大模块　全部通过
Agent 5　工作流 7　人工审核 9　知识库 6　认证 8

接口与前端
Postman 验证 REST 与 SSE
浏览器 DevTools 验证画布与事件流

## 18. [data] 关键性能数据
142
SSE 推流平均延迟（毫秒）

核心 API（50 并发）
GET 待审列表　P95 45ms
POST 审批恢复　P95 120ms
POST 登录　P95 96ms
GET 执行详情　P95 52ms

SSE 推流（100 样本）
平均 142ms　P95 298ms　最大 387ms　零中断

并发执行　5 路工作流互不串扰，全部成功

均达到第 2 章设定的性能指标

## 19. [content] 总结、不足与展望
主要工作
打通　可视化编排 → 工作流执行 → 人工审核 → 知识增强 → 安全访问　闭环
42 单测 + 35 功能用例全部通过
Docker Compose 一键部署演示

不足
单工作流为主，缺少子流程复用与跨 Agent 协同
分块策略与召回机制基础，无重排
审批 UI 以 JSON 为主，对非技术用户不友好

未来工作
子工作流与流程模板
自适应分块与召回重排
字段级渲染审批界面
多智能体协同与工具生态扩展

## 20. [cover] 谢谢聆听
敬请各位老师批评指正
zj669　西南科技大学
2026 年 5 月
