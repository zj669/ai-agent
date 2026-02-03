# 技术栈与构建指南 (Tech Stack & Build Guide)

## 后端技术栈

### 核心框架
- **Spring Boot**: 3.4.9
- **Spring AI**: 1.0.1 (OpenAI、Milvus、Tika 集成)
- **Java**: 21 (LTS)
- **Maven**: Multi-module 项目结构

### 数据存储
- **MySQL**: 8.0 (主数据库，MyBatis Plus ORM)
- **Redis**: 7.x (缓存，Redisson 客户端)
- **Milvus**: 2.3 (向量数据库，语义检索)
- **MinIO**: 对象存储 (知识库文件)

### 关键依赖
- **MyBatis Plus**: 3.5.5 (持久层框架)
- **JWT**: 0.12.3 (认证授权)
- **Flyway**: 数据库迁移
- **Lombok**: 代码简化
- **Hutool**: 工具库
- **Guava**: Google 工具库

### 监控与日志
- **Spring Actuator**: 健康检查和指标
- **Prometheus**: 指标采集
- **Logback**: 结构化日志

## 前端技术栈

### 核心框架
- **React**: 19.2.0
- **TypeScript**: 5.9.3
- **Vite**: 7.2.4 (构建工具)

### UI 与样式
- **Tailwind CSS**: 3.4.17 (原子化 CSS)
- **Lucide React**: 图标库
- **React Flow**: 11.11.4 (工作流画布)

### 状态与路由
- **React Router DOM**: 7.12.0 (路由管理)
- **STOMP.js**: WebSocket 通信

### 开发工具
- **ESLint**: 代码检查
- **PostCSS**: CSS 处理
- **Autoprefixer**: CSS 兼容性

## 项目结构

### 后端模块 (Maven Multi-module)

```
ai-agent/
├── ai-agent-shared/          # 共享内核 (工具类、常量、设计模式)
├── ai-agent-domain/          # 领域层 (实体、值对象、领域服务)
├── ai-agent-application/     # 应用层 (用例编排、DTO、事件)
├── ai-agent-infrastructure/  # 基础设施层 (数据库、缓存、第三方服务)
└── ai-agent-interfaces/      # 接口层 (REST API、配置、启动类)
```

**依赖方向**: `interfaces → application → domain ← infrastructure`

**关键原则**: Domain 层不依赖任何其他模块，保持纯净的业务逻辑。

### 前端结构 (Feature-Based)

```
app/frontend/src/
├── app/                # 应用层 (路由、全局配置)
├── features/           # 功能模块 (按业务领域划分)
│   ├── auth/          # 认证模块
│   ├── agent/         # Agent 管理
│   ├── orchestration/ # 工作流编排
│   ├── chat/          # 对话界面
│   └── knowledge/     # 知识库管理
└── shared/            # 共享组件和工具
```

## 常用命令

### 后端开发

#### 构建项目
```bash
# 完整构建所有模块
mvn clean install

# 跳过测试快速构建
mvn clean install -DskipTests

# 只构建特定模块
mvn clean install -pl ai-agent-interfaces -am
```

#### 运行应用
```bash
# 使用 local 配置运行
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local

# 使用 dev 配置运行
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=dev
```

#### 测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl ai-agent-domain

# 运行特定测试类
mvn test -Dtest=UserServiceTest
```

### 前端开发

```bash
cd app/frontend

# 安装依赖
npm install

# 启动开发服务器 (http://localhost:5173)
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 预览生产构建
npm run preview
```

### Docker 服务管理

```bash
# 进入 Docker Compose 目录
cd ai-agent-infrastructure/src/main/resources/docker

# 启动所有服务 (MySQL, Redis, Milvus, MinIO, etcd)
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f [service-name]

# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 数据库迁移

```bash
# Flyway 迁移在应用启动时自动执行
# 迁移文件位置: ai-agent-infrastructure/src/main/resources/db/migration/

# 手动执行迁移 (如需要)
mvn flyway:migrate -pl ai-agent-infrastructure
```

## 环境配置

### 配置文件位置
- `ai-agent-interfaces/src/main/resources/application.yml` - 基础配置
- `application-local.yml` - 本地开发配置
- `application-dev.yml` - 开发环境配置

### 必需的环境变量

#### 数据库配置
```bash
PRIMARY_DB_HOST=localhost
PRIMARY_DB_PORT=13306
PRIMARY_DB_NAME=ai_agent
PRIMARY_DB_USER=root
PRIMARY_DB_PASSWORD=root123
PRIMARY_DB_DRIVER=com.mysql.cj.jdbc.Driver
PRIMARY_DB_MIN_IDLE=5
PRIMARY_DB_MAX_POOL_SIZE=20
PRIMARY_DB_IDLE_TIMEOUT=600000
PRIMARY_DB_MAX_LIFETIME=1800000
PRIMARY_DB_CONNECTION_TIMEOUT=30000
```

#### Redis 配置
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123
REDIS_POOL_SIZE=64
REDIS_MIN_IDLE_SIZE=10
REDIS_IDLE_TIMEOUT=10000
REDIS_CONNECT_TIMEOUT=10000
REDIS_RETRY_ATTEMPTS=3
REDIS_RETRY_INTERVAL=1500
REDIS_PING_INTERVAL=30000
REDIS_KEEP_ALIVE=true
```

#### AI 模型配置
```bash
EMBEDDING_BASE_URL=https://api.openai.com
EMBEDDING_ENDPOINT=/v1/embeddings
EMBEDDING_MODEL=text-embedding-ada-002
EMBEDDING_API_KEY=sk-your-api-key
EMBEDDING_NUM_CTX=8192
```

#### Milvus 配置
```bash
MILVUS_ENABLED=true
MILVUS_HOST=localhost
MILVUS_PORT=19530
MILVUS_USERNAME=
MILVUS_PASSWORD=
```

#### MinIO 配置
```bash
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=admin123456
MINIO_BUCKET_NAME=knowledge-files
```

#### 认证配置
```bash
AUTH_DEBUG_ENABLED=true
AUTH_DEBUG_HEADER_NAME=X-User-Id
```

#### 邮件配置 (可选)
```bash
MAIL_HOST=smtp.example.com
MAIL_PORT=465
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-password
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Spring Boot | 8080 | 后端 API |
| Vite Dev Server | 5173 | 前端开发服务器 |
| MySQL | 13306 | 数据库 |
| Redis | 6379 | 缓存 |
| Milvus | 19530 | 向量数据库 |
| MinIO API | 9000 | 对象存储 API |
| MinIO Console | 9001 | MinIO 管理界面 |
| etcd | 2379 | Milvus 依赖 |

## 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# 应用信息
curl http://localhost:8080/actuator/info
```

## 常见问题

### Protobuf 版本冲突
项目使用 Milvus SDK，需要特定的 protobuf 版本 (3.25.3)。已在父 POM 中统一管理，避免版本冲突。

### 数据库连接失败
确保 Docker 服务已启动，并且环境变量配置正确。检查 MySQL 容器日志：
```bash
docker-compose logs mysql
```

### 前端代理配置
开发环境下，前端通过 Vite 代理访问后端 API。配置在 `vite.config.ts` 中。

### Spring AI 自动配置
项目禁用了 Spring AI 的自动配置，模型由用户动态创建。不要依赖自动注入的 ChatClient。
