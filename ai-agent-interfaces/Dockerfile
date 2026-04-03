# ============================================================
# AI Agent Platform - Spring Boot Backend Dockerfile
# Multi-stage build: Maven builder → JRE runtime
# ============================================================
# 构建: docker build -f ai-agent-interfaces/Dockerfile -t ghcr.io/<owner>/ai-agent-backend:<tag> .
# 或使用 docker-compose.prod.yml 自动构建
# ============================================================

# ── Stage 1: Builder ────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 预下载依赖层，加速 rebuild
COPY pom.xml .
COPY ai-agent-shared/pom.xml ai-agent-shared/
COPY ai-agent-domain/pom.xml ai-agent-domain/
COPY ai-agent-application/pom.xml ai-agent-application/
COPY ai-agent-infrastructure/pom.xml ai-agent-infrastructure/
COPY ai-agent-interfaces/pom.xml ai-agent-interfaces/

RUN mvn dependency:go-offline -B -pl ai-agent-shared,ai-agent-domain,ai-agent-application,ai-agent-infrastructure,ai-agent-interfaces -am

# 复制源码并构建（跳过测试以加快镜像构建）
COPY . .
RUN mvn package -DskipTests -B -pl ai-agent-interfaces -am

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# 安全增强：非 root 运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 创建可写目录供挂载日志，安装 curl 供 healthcheck 使用
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app && \
    apk add --no-cache curl

# 从 builder 复制 fat-jar
COPY --from=builder /app/ai-agent-interfaces/target/*.jar app.jar

USER appuser

# 标准 Spring Boot 入口
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
