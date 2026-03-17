# KnowledgeRetrievalService.java 蓝图

## Metadata
- title: KnowledgeRetrievalService
- type: interface
- summary: 知识检索领域服务端口，提供按 agentId 和 datasetId 两种检索模式，由 application 层实现

## 关键方法单元

### retrieve
- location: KnowledgeRetrievalService.retrieve
- purpose: 按 Agent ID 检索知识（权限隔离），用于 LLM 节点 RAG 和 SchedulerService 内存注入
- input: agentId（Long）、query（String）、topK（int）
- output: List<String>

### retrieveByDataset
- location: KnowledgeRetrievalService.retrieveByDataset
- purpose: 按 Dataset ID 检索知识，用于 KnowledgeNodeExecutorStrategy
- input: datasetId（String）、query（String）、topK（int）
- output: List<String>

## 变更记录
- 2026-03-16: 初始蓝图生成
