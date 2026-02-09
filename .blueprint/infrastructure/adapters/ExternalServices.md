# External Services (Infrastructure Adapters) Blueprint

## 职责契约
- **做什么**: 实现 Domain 层定义的外部服务端口——文件存储(MinIO)、向量数据库(Milvus)、文档解析(Tika)、文本分块、邮件发送
- **不做什么**: 不包含业务逻辑；不直接被 Controller 调用

## 实现清单

| 端口接口 (Domain) | 实现类 (Infrastructure) | 外部服务 |
|-------------------|------------------------|---------|
| FileStorageService | MinIOFileStorageService | MinIO 对象存储 |
| VectorStore | (Spring AI Milvus 集成) | Milvus 向量数据库 |
| DocumentReaderAdapter | SpringAIDocumentReaderAdapter | Apache Tika |
| TextSplitterAdapter | SpringAITextSplitterAdapter | Spring AI TextSplitter |
| IEmailService | (SMTP 实现) | 邮件服务器 |

## 依赖拓扑
- **上游**: KnowledgeApplicationService, KnowledgeRetrievalService
- **下游**: MinIO SDK, Milvus SDK, Spring AI, JavaMail

## 设计约束
- MinIO 配置通过环境变量注入 (MINIO_ENDPOINT, MINIO_ACCESS_KEY 等)
- Milvus 连接可通过 MILVUS_ENABLED 开关控制
- 文档解析支持多种格式 (PDF, DOCX, TXT 等)，通过 Apache Tika 自动检测
- Embedding 模型通过用户配置动态创建，不使用 Spring AI 自动配置

## 变更日志
- [初始] 从现有代码逆向生成蓝图
