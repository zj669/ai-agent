# 知识库管理 API - 任务跟踪

## Phase 0: 依赖准备
- [x] Task 0.1: 添加 MinIO 依赖
- [x] Task 0.2: 添加 Spring AI 依赖 (已存在)
- [x] Task 0.3: 配置 application.yml（MinIO + Async）

## Phase 1: Domain Layer
- [x] Task 1.1: 创建 DocumentStatus 枚举
- [x] Task 1.2: 创建 ChunkingConfig 值对象
- [x] Task 1.3: 创建 KnowledgeDataset 聚合根
- [x] Task 1.4: 创建 KnowledgeDocument 聚合根
- [x] Task 1.5: 创建 KnowledgeDatasetRepository 接口
- [x] Task 1.6: 创建 KnowledgeDocumentRepository 接口
- [x] Task 1.7: 创建 FileStorageService 接口
- [x] Task 1.8: 创建 KnowledgeRetrievalService 接口

## Phase 2: Infrastructure Layer
- [x] Task 2.1: 实现 MinIOFileStorageService
- [x] Task 2.2: 配置 MinioClient Bean
- [x] Task 2.3: 创建 SpringAIDocumentReaderAdapter
- [x] Task 2.4: 创建 SpringAITextSplitterAdapter
- [x] Task 2.5: 扩展 VectorStore 支持 SearchRequest
- [x] Task 2.6: 实现 MilvusVectorStore 的 Filter 支持
- [x] Task 2.7: 创建 KnowledgeDatasetPO
- [x] Task 2.8: 创建 KnowledgeDocumentPO
- [x] Task 2.9: 实现 MySQLKnowledgeDatasetRepository
- [x] Task 2.10: 实现 MySQLKnowledgeDocumentRepository

## Phase 3: Application Layer
- [x] Task 3.1: 创建 KnowledgeApplicationService
- [x] Task 3.2: 创建 AsyncDocumentProcessor
- [x] Task 3.3: 实现 KnowledgeRetrievalServiceImpl
- [x] Task 3.4: 配置 @EnableAsync

## Phase 4: Interface Layer
- [x] Task 4.1: 创建 DTOs
- [x] Task 4.2: 创建 KnowledgeController

## Phase 5: 数据库脚本
- [x] Task 5.1: 创建数据库表 DDL

## Phase 6: 编译与验证
- [x] Task 6.1: 编译验证 (Phase 2)
- [x] Task 6.2: 编译验证 (Phase 3-6) ✅
- [x] Task 6.3: 单元测试 ✅ KnowledgeApplicationServiceTest
