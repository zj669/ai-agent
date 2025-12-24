# AI Agent 项目 Git 仓库管理指南

## 📋 当前仓库结构

```
d:\java\ai-agent\              # 后端仓库
├── .git/                      # 后端Git配置
│   └── origin: https://github.com/zj669/ai-agent.git
├── app/                       # 前端仓库（独立）
│   ├── .git/                  # 前端Git配置
│   │   └── origin: https://github.com/zj669/ai-agent-foward.git
│   └── ...
└── ...
```

## ⚠️ 问题说明

目前两个Git仓库嵌套，可能导致：
1. 后端仓库可能会追踪前端文件
2. Git操作时容易混淆
3. 提交时可能误提交到错误的仓库

## ✅ 推荐方案：使用 Git Submodule

### 方案一：将前端作为后端的Submodule（推荐）

#### 1. 在后端仓库中排除app目录
```bash
# 在后端 .gitignore 中添加
echo "/app/" >> .gitignore
```

#### 2. 提交后端的.gitignore更改
```bash
cd d:\java\ai-agent
git add .gitignore
git commit -m "chore: ignore app directory (frontend submodule)"
git push
```

#### 3. 前端独立管理
前端在 `app/` 目录下独立进行Git操作：
```bash
cd d:\java\ai-agent\app
git add .
git commit -m "feat: your changes"
git push
```

### 方案二：完全分离前后端（最清晰）

#### 1. 将前端移到独立目录
```bash
# 创建独立的前端目录
mkdir d:\ai-agent-frontend
# 移动前端代码
move d:\java\ai-agent\app\* d:\ai-agent-frontend\
```

#### 2. 目录结构
```
d:\java\ai-agent\              # 后端项目
d:\ai-agent-frontend\          # 前端项目
```

#### 3. 优势
- ✅ 完全独立，不会混淆
- ✅ 可以独立克隆和部署
- ✅ CI/CD配置更清晰

## 🎯 当前推荐配置（方案一）

### 步骤1: 更新后端.gitignore

在 `d:\java\ai-agent\.gitignore` 末尾添加：
```gitignore
# ===== 前端项目排除 =====
# app目录是独立的前端Git仓库，不纳入后端版本控制
/app/
```

### 步骤2: 清理后端Git缓存

如果app目录已经被后端Git追踪，需要清理：
```bash
cd d:\java\ai-agent
git rm -r --cached app
git commit -m "chore: remove app directory from backend repo"
```

### 步骤3: 验证配置

```bash
# 在后端目录
cd d:\java\ai-agent
git status  # 应该不显示app目录的变更

# 在前端目录
cd d:\java\ai-agent\app
git status  # 只显示前端的变更
```

## 📝 日常工作流程

### 后端开发
```bash
cd d:\java\ai-agent
# 修改后端代码
git add .
git commit -m "feat: backend changes"
git push origin main
```

### 前端开发
```bash
cd d:\java\ai-agent\app
# 修改前端代码
git add .
git commit -m "feat: frontend changes"
git push origin main
```

### 同时修改前后端
```bash
# 1. 提交后端
cd d:\java\ai-agent
git add .
git commit -m "feat: backend changes"
git push

# 2. 提交前端
cd app
git add .
git commit -m "feat: frontend changes"
git push
```

## 🔧 Git配置建议

### 为不同仓库配置不同的用户信息（可选）

```bash
# 后端仓库配置
cd d:\java\ai-agent
git config user.name "zj669"
git config user.email "3218356902@qq.com"

# 前端仓库配置
cd d:\java\ai-agent\app
git config user.name "zj669"
git config user.email "3218356902@qq.com"
```

## 🚀 CI/CD配置

### 后端CI/CD
- **仓库**: `https://github.com/zj669/ai-agent.git`
- **构建目录**: 项目根目录
- **Dockerfile**: `./Dockerfile`

### 前端CI/CD
- **仓库**: `https://github.com/zj669/ai-agent-foward.git`
- **构建目录**: 项目根目录
- **Dockerfile**: `./Dockerfile`

## ⚠️ 注意事项

1. **不要在后端仓库根目录执行 `git add app/`**
2. **前端修改只在 `app/` 目录内操作Git**
3. **后端修改只在项目根目录操作Git**
4. **IDE可能会显示两个Git仓库，注意区分**

## 🔍 故障排查

### 问题：后端仓库显示app目录有变更
```bash
# 检查.gitignore是否生效
cd d:\java\ai-agent
git check-ignore -v app/

# 如果没有输出，说明.gitignore未生效，需要清理缓存
git rm -r --cached app
git commit -m "chore: remove app from tracking"
```

### 问题：不确定当前在哪个仓库
```bash
# 查看当前仓库的远程地址
git remote -v
```

### 问题：误提交到错误的仓库
```bash
# 撤销最后一次提交（保留修改）
git reset --soft HEAD~1
```
