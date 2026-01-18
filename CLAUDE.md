# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an **AI Agent Platform** built with **Spring Boot 3.4.9 + Spring AI 1.0.1** using **Domain-Driven Design (DDD)** architecture. The core business is **workflow orchestration** with DAG-based execution and human-in-the-loop capabilities.

## Architecture

### Module Structure (Maven Multi-module)
1. **ai-agent-shared** - Framework-agnostic utilities, constants, design patterns
2. **ai-agent-domain** - Core business logic, aggregates, domain services (NO dependencies on other modules)
3. **ai-agent-application** - Use case orchestration, DTOs, commands, events
4. **ai-agent-infrastructure** - Persistence implementations (MySQL, Redis), PO objects, migrations
5. **ai-agent-interfaces** - REST controllers, configuration, main application class

### DDD Concepts
- **Aggregate Roots**: `Agent`, `User`, `Workflow`
- **Value Objects**: In `valobj` packages (e.g., `Email`, `Credential`)
- **Repositories**: Interface in domain layer, implementation in infrastructure
- **Domain Services**: Business logic in domain layer
- **Application Services**: Orchestration in application layer
- **Ports & Adapters**: Clear separation between domain ports and infrastructure adapters

### Dependency Direction
```
interfaces → application → domain ← infrastructure
```
Domain module has NO dependencies on other modules.

## Development Commands

### Build & Run
```bash
# Build all modules
mvn clean install

# Run with local profile (requires Docker services)
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local

# Run tests
mvn test
mvn test -pl ai-agent-interfaces  # Run tests for specific module
```

### Database & Services
```bash
# Start required services (MySQL, Redis, Milvus, MinIO)
cd ai-agent-infrastructure/src/main/resources/docker
docker-compose up -d

# View service status
docker-compose ps

# Stop services
docker-compose down
```

### Deployment
```bash
# Build Docker image
docker build -t ai-agent:latest .

# Deploy with script (requires environment variables)
./cd-deploy.sh
```

## Configuration

### Profiles
- **local**: Local development with Docker services
- **dev**: Development environment
- **prod**: Production environment

### Key Configuration Files
- `ai-agent-interfaces/src/main/resources/application.yml` - Base configuration
- `application-local.yml` - Local development settings
- `application-dev.yml` - Development environment
- `application-prod.yml` - Production environment

### Environment Variables Required
- Database: `PRIMARY_DB_HOST`, `PRIMARY_DB_PORT`, `PRIMARY_DB_NAME`, `PRIMARY_DB_USER`, `PRIMARY_DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- AI Models: `EMBEDDING_API_KEY`, `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL`
- MinIO: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
- Milvus: `MILVUS_HOST`, `MILVUS_PORT`

## Database

### Schema Management
- Uses **Flyway** for database migrations
- Schema file: `ai-agent-infrastructure/src/main/resources/db/migration/V1_0__init_schema.sql`
- Logical deletion: Uses `deleted` field (1 = deleted, 0 = active)

### Services (Docker Compose)
- **MySQL 8.0**: Port 13306, root/root123
- **Redis 7**: Port 6379, password redis123
- **Milvus 2.3**: Port 19530 (vector database)
- **MinIO**: Port 9000 (API), 9001 (Console), admin/admin123456
- **etcd**: Required by Milvus

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.4.9 + Spring AI 1.0.1
- **Language**: Java 21
- **Build**: Maven Multi-module
- **Persistence**: MySQL 8.0 + MyBatis Plus
- **Cache**: Redis 7
- **Authentication**: JWT
- **Messaging**: SSE (Server-Sent Events) for streaming

### AI Integration
- **Embeddings**: OpenAI compatible endpoints
- **Vector DB**: Milvus for semantic search
- **Object Storage**: MinIO for knowledge base files
- **Dynamic Model Configuration**: Spring AI auto-configuration disabled for user-controlled setup

### Monitoring
- **Actuator**: `/actuator` endpoints for health, metrics, prometheus
- **Logging**: Logback with structured logging
- **Metrics**: Prometheus integration

## Code Patterns

### Naming Conventions
- **DTOs**: `*DTO.java` in application layer
- **Entities**: `*.java` in domain layer
- **POs**: `*PO.java` in infrastructure layer (Persistence Objects)
- **Value Objects**: In `valobj` packages
- **Repositories**: `*Repository.java` interface in domain, `*RepositoryImpl.java` in infrastructure

### Package Structure
```
com.zj.aiagent
├── domain
│   ├── [bounded-context] (agent, user, workflow, etc.)
│   │   ├── entity
│   │   ├── valobj
│   │   ├── repository
│   │   ├── service
│   │   └── exception
├── application
│   ├── [bounded-context]
│   │   ├── dto
│   │   ├── service
│   │   └── event
├── infrastructure
│   ├── repository
│   ├── mapper
│   └── po
└── interfaces
    ├── controller
    ├── config
    └── interceptor
```

### Testing
- **Unit Tests**: In `src/test/java` of each module
- **Integration Tests**: Test database interactions and external services
- **Test Structure**: Follows same package structure as main code

## Workflow System (Core Domain)

The workflow system is the **core domain** with these key concepts:
1. **DAG-based execution**: Directed Acyclic Graph for workflow definition
2. **Node types**: LLM nodes, condition nodes, human review nodes
3. **Execution strategies**: Different executors for different node types
4. **State management**: Execution snapshots and checkpoints
5. **Human-in-the-loop**: Pause/resume with approval workflows

## Important Notes

1. **Chinese Codebase**: Most documentation and comments are in Chinese, but code follows standard Java/Spring conventions
2. **Current Branch**: `feature/shcema-refactory` - schema refactoring in progress
3. **Database Schema**: Complete schema in Flyway migrations; uses logical deletion
4. **AI Model Configuration**: Dynamic model creation - Spring AI auto-configuration is disabled
5. **Health Check**: `/public/health` endpoint for deployment health checks

## Common Issues & Solutions

1. **Protobuf Version Conflicts**: Milvus SDK requires specific protobuf version (3.25.3) - managed in parent POM
2. **Service Dependencies**: Application requires MySQL, Redis, Milvus, MinIO to be running
3. **Profile Selection**: Use `-Dspring-boot.run.profiles=local` for local development
4. **Database Migrations**: Flyway runs automatically on application startup