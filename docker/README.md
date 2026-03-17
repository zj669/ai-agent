# AI Agent Platform - Docker 中间件

开发环境所需的全部中间件，一键启动。

## 服务清单

| 服务 | 镜像 | 端口 | 账号 / 密码 |
|------|------|------|-------------|
| MySQL 8.0 | mysql:8.0 | 13306 | root / root123 |
| Redis 7 | redis:7-alpine | 6379 | redis123 |
| MinIO | minio:RELEASE.2023-03-20 | 9000(API) / 9001(Console) | admin / admin123456 |
| Milvus 2.3 | milvusdb/milvus:v2.3.3 | 19530(gRPC) / 9091(Metrics) | 无认证 |
| etcd | quay.io/coreos/etcd:v3.5.5 | 2379(内部) | - |

## 快速启动

```bash
cd docker
cp .env.example .env   # 按需修改密码
docker-compose up -d
```

## 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f redis

# 停止所有服务
docker-compose down

# 停止并清除数据卷（慎用）
docker-compose down -v
```

## 目录结构

```
docker/
├── docker-compose.yml      # 编排文件
├── .env.example            # 环境变量模板
├── init/
│   ├── mysql/
│   │   └── 01_init_schema.sql   # MySQL 建表 + 种子数据
│   └── milvus/
│       └── milvus.yaml          # Milvus 配置（关闭认证）
└── README.md
```

## 说明

- MySQL 初始化脚本在容器首次启动时自动执行，会创建 `ai_agent` 库及全部表结构
- MinIO 被 Milvus 和知识库模块共用，Milvus 用它存储索引段文件
- Redis 绑定 127.0.0.1，仅本机可访问
- 数据通过 Docker named volumes 持久化，`docker-compose down` 不会丢数据，`down -v` 会清除
