# 前端基础设施

## 已实现功能

### 1. 认证系统
- ✅ JWT Token 认证
- ✅ 登录/注册页面（邮箱验证码）
- ✅ 密码重置功能
- ✅ Token 自动管理（localStorage）
- ✅ Token 过期提醒（过期前 5 分钟）
- ✅ 401 错误自动跳转登录并保存 redirect 参数

### 2. 路由系统
- ✅ React Router v6
- ✅ 路由守卫（ProtectedRoute）
- ✅ 公开路由：/login、/reset-password
- ✅ 受保护路由：/dashboard、/agents、/workflows、/chat、/knowledge

### 3. 布局组件
- ✅ MainLayout（Header + Sidebar + Content）
- ✅ 响应式侧边栏（移动端自动折叠）
- ✅ 用户下拉菜单（个人信息、设置、退出登录）
- ✅ 导航菜单（看板、Agent、工作流、对话、知识库）

### 4. 状态管理
- ✅ Zustand authStore
- ✅ 登录/注册/登出方法
- ✅ Token 过期检查
- ✅ 用户信息更新

### 5. HTTP 客户端
- ✅ Axios 配置（apiClient.ts）
- ✅ 请求拦截器（自动添加 Authorization header）
- ✅ 响应拦截器（统一错误处理）
- ✅ 401/403/404/500 错误处理
- ✅ 网络错误提示

### 6. 服务封装
- ✅ authService（认证相关接口）
- ✅ knowledgeService（知识库接口）

### 7. 类型定义
- ✅ auth.ts（用户、登录、注册等类型）
- ✅ knowledge.ts（知识库类型）

## 目录结构

```
src/
├── components/
│   ├── MainLayout.tsx           # 主布局组件
│   ├── ProtectedRoute.tsx       # 路由守卫
│   └── TokenExpirationWarning.tsx # Token 过期提醒
├── pages/
│   ├── LoginPage.tsx            # 登录/注册页面
│   ├── ResetPasswordPage.tsx    # 重置密码页面
│   └── DashboardPage.tsx        # 看板页面（临时）
├── services/
│   ├── apiClient.ts             # Axios 配置
│   ├── authService.ts           # 认证服务
│   └── knowledgeService.ts      # 知识库服务
├── stores/
│   └── authStore.ts             # 认证状态管理
├── types/
│   ├── auth.ts                  # 认证类型定义
│   └── knowledge.ts             # 知识库类型定义
├── App.tsx                      # 路由配置
└── index.tsx                    # 入口文件
```

## 使用说明

### 启动开发服务器

```bash
cd ai-agent-foward
npm install
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
npm run preview
```

## 后续任务

以下页面需要实现：

1. **Agent 管理模块**（任务 #7）
   - Agent 列表页面
   - Agent 创建/编辑表单
   - Agent 详情页面

2. **工作流编排模块**（任务 #10）
   - 工作流画布（@xyflow/react）
   - 节点配置面板
   - 工作流执行监控

3. **聊天对话模块**（任务 #13）
   - 对话列表
   - 聊天界面
   - 消息流式渲染

4. **知识库模块**（任务 #16）
   - 知识库列表
   - 文档上传
   - 文档管理

5. **人工审核模块**（任务 #19）
   - 审核任务列表
   - 审核详情页面

6. **看板页面**（任务 #21）
   - 统计数据展示
   - 快速操作入口

## 技术栈

- React 19.2.1
- TypeScript 5.8.2
- Ant Design 6.1.1
- React Router 7.11.0
- Zustand 5.0.9
- Axios 1.13.2
- Vite 6.2.0

## 注意事项

1. **Token 管理**：Token 存储在 localStorage，过期前 5 分钟会弹出提醒
2. **路由守卫**：所有受保护路由都需要登录，未登录会自动跳转到登录页
3. **错误处理**：所有 API 错误都会通过 Ant Design 的 message 组件显示
4. **CORS**：后端已配置 CORS，前端通过 Vite proxy 转发请求
