## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/MinIOConfig.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/MinIOConfig.java`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: MinIO 客户端装配
- 源文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/MinIOConfig.java`
- 文件类型: `.java`
- 说明:
  - 读取 `minio.endpoint/access-key/secret-key` 配置并初始化 `MinioClient` Bean。
  - 为知识库文件存储服务（如 MinIOFileStorageService）提供统一对象存储客户端。
  - 在启动阶段输出 endpoint 日志，便于环境连通性排查。

## 2) 核心方法
- `minioClient()`：按配置构建并暴露 `MinioClient` 单例。

## 3) 具体方法
### 3.1 `minioClient()`
- 函数签名: `public MinioClient minioClient()`
- 入参:
  - 无（依赖字段注入：`endpoint/accessKey/secretKey`）
- 出参:
  - `MinioClient` - MinIO SDK 客户端实例
- 功能含义:
  - 基于 `MinioClient.builder()` 设置 endpoint 与 credentials，返回可复用客户端。
- 链路作用:
  - 上游: Spring 容器初始化阶段
  - 下游: `knowledge` 基础设施中的文件上传、下载、元数据读取

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 MinIOConfig 真实职责与方法语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
