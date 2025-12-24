# 多阶段构建 - 构建阶段
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制POM文件并下载依赖
COPY pom.xml .
COPY ai-agent-shared/pom.xml ai-agent-shared/
COPY ai-agent-domain/pom.xml ai-agent-domain/
COPY ai-agent-application/pom.xml ai-agent-application/
COPY ai-agent-infrastructure/pom.xml ai-agent-infrastructure/
COPY ai-agent-interfaces/pom.xml ai-agent-interfaces/

# 下载依赖（利用Docker缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY ai-agent-shared/src ai-agent-shared/src
COPY ai-agent-domain/src ai-agent-domain/src
COPY ai-agent-application/src ai-agent-application/src
COPY ai-agent-infrastructure/src ai-agent-infrastructure/src
COPY ai-agent-interfaces/src ai-agent-interfaces/src

# 构建项目（跳过测试）
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建非root用户
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser

# 创建日志目录
RUN mkdir -p /app/data/log && chown -R appuser:appgroup /app/data

# 从构建阶段复制JAR
COPY --from=builder /app/ai-agent-interfaces/target/*.jar app.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM调优参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用（使用dev配置）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=dev -jar app.jar"]
