# 常用命令（Windows 环境）

## 基础
- 查看目录：`dir`
- 递归列目录：`Get-ChildItem -Recurse`
- 文本搜索（推荐）：`rg "pattern" -n .`
- Git 状态：`git status`

## 启动依赖服务
```powershell
cd ai-agent-infrastructure/src/main/resources/docker
docker-compose up -d
```

## 启动后端
```powershell
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local
```

## 构建后端
```powershell
mvn clean package -DskipTests
```

## 运行后端测试
```powershell
mvn test
mvn -pl ai-agent-infrastructure test
```

## 启动前端
```powershell
cd ai-agent-foward
npm install
npm run dev
```

## 构建前端
```powershell
cd ai-agent-foward
npm run build
```