# AI Agent 平台前端样式与设计规范记录

本文档记录了 `ai-agent-foward` 前端项目的整体 UI 风格、技术栈选型、布局结构以及色彩与排版规范。

## 1. 技术栈与样式方案

项目采用了**混合样式方案**，结合了传统的组件库与现代的实用类 CSS 框架：

- **核心框架**：React 19 + Vite + TypeScript
- **UI 组件库**：Ant Design v6.3.0
- **CSS 框架**：Tailwind CSS v3.4.17
- **图标库**：`@ant-design/icons`
- **复杂交互组件**：`@xyflow/react` (用于 Workflow 工作流画布)

**样式组织策略**：
- **全局布局与特定页面（如登录页、工作流编辑器界面）**：倾向于使用 **Tailwind CSS** 来实现高度定制化的结构与响应式布局。
- **业务数据展示（如表格、表单、仪表盘卡片）**：主要依赖 **Ant Design** 组件，并通过覆盖默认的 Inline Style 或 ConfigProvider 来契合项目的设计语言。

## 2. 色彩规范 (Color Palette)

系统整体采用现代、清新的浅色主题（Light Theme），未默认开启暗黑模式。配色灵感偏向于企业级的 SaaS 应用。

### 2.1 基础色彩
- **主品牌色 (Primary)**：`#2970FF` (亮蓝色) —— 用于主按钮、活跃菜单项、重要图标。
- **主品牌浅色底 (Primary Light)**：`#EFF4FF` —— 用于菜单 Hover 背景、图标背景框等。

### 2.2 文本与灰阶色彩 (Neutral Colors)
- **主标题/强调文本**：`#101828` (深黑色)
- **正文文本**：`#344054` (深灰色)
- **次要/辅助文本**：`#667085` (中灰色)
- **页面主背景色**：`#F9FAFB` (极浅灰)
- **内容区/卡片背景色**：`#FFFFFF` (纯白)
- **边框颜色**：`#EAECF0` (浅灰边框)

### 2.3 状态语义色彩 (Semantic Colors)
- **成功 (Success)**：`#12B76A` (绿色) / 浅色底 `#ECFDF3`
- **警告/待处理 (Warning)**：`#F79009` (橙色) / 浅色底 `#FFFAEB`
- **信息/对话 (Info/Highlight)**：`#7A5AF8` (紫色) / 浅色底 `#F4F3FF`
- **危险/错误 (Danger)**：红色系 (使用 Ant Design 默认红，或自定义类似 `#F04438`)

## 3. 字体与排版 (Typography)

全局字体在 `index.css` 中统一定义，采用现代的无衬线字体栈：

```css
font-family: Inter, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
```
- 优先使用 `Inter` 字体（如果可用）以提升英文字符及数字的现代感与可读性。
- 中文回退到平滑的 `PingFang SC` (苹果设备) 和 `Microsoft YaHei` (Windows 设备)。
- 大部分标题字体加粗程度为 `fontWeight: 600` 或 `700`。

## 4. 整体布局与组件风格

### 4.1 认证/登录页面 (Auth Layout)
- **结构**：左右分栏设计。左侧（45%）展示隐藏的 3D 视觉场景 (`Auth3DScene`)，右侧（55%）为白色的认证表单。
- **视觉风格**：右侧背景使用了柔和的渐变色 `bg-gradient-to-bl from-slate-50 via-white to-blue-50/60`，增加了界面的层次感与呼吸感。整体偏向现代和年轻化。

### 4.2 主应用骨架 (App Shell)
- **导航侧边栏 (Sider)**：白底 (`#fff`)，极简风格。选中状态的菜单项移除传统 AntD 的深色底，改为使用品牌色 `#2970FF` 搭配浅蓝底色 `#EFF4FF`，文字颜色 `#344054`。
- **顶部导航 (Header)**：包含面包屑、通知图标以及用户头像下拉菜单。高度 `56px`。
- **内容区 (Content)**：使用全局的浅灰背景 `#F9FAFB`，与内部的白色卡片形成对比。

### 4.3 卡片与容器风格 (Cards & Containers)
- **圆角 (Border Radius)**：系统内广泛使用了较大圆角。通常面板和 Card 使用 `borderRadius: 12px` 或 `8px`。
- **边框与阴影**：
  - 静态卡片主要使用边框 `border: 1px solid #EAECF0`，取代了厚重的阴影。
  - **悬浮态 (Hover)**：类似 Agent 列表的卡片，悬停时会有明显的阴影效果 `box-shadow: 0 6px 20px rgba(0,0,0,0.12)` 产生浮浮感。

## 5. 样式实现示例

项目中的 Tailwind CSS 基础变量配置 (`tailwind.config.ts` & `index.css`) 采用了基于 CSS 变量的配置方式：

```css
:root {
  --background: 0 0% 100%;
  --foreground: 222.2 84% 4.9%;
  --muted-foreground: 215.4 16.3% 46.9%;
}
```

结合 Ant Design 时，常常直接在组件上写入 `style` 对象覆盖默认表现，例如 Dashboard 中的统计卡片图标：

```tsx
<div style={{
  width: 48, height: 48, borderRadius: 12,
  background: '#EFF4FF', // 浅色底
  color: '#2970FF'       // 主色图标
}}>
  <RobotOutlined />
</div>
```

## 6. 总结
整个 AI Agent 平台的前端设计语言统一且现代，大量采用了**大圆角、浅色背景、轻边框、明确的主题色点缀**。代码层面上，**外层框架和复杂定制页面依赖 Tailwind CSS** 快速构建，而**核心数据展现页面深度结合和复写 Ant Design** 组件，达成了既高效又美观的开发效果。