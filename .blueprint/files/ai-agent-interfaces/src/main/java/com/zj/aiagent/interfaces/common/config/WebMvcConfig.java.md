## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WebMvcConfig.java
- 注册登录拦截器与跨域策略；在生产环境按白名单放行，在非生产环境开放 origin pattern。

## 2) 核心方法
- `addInterceptors(InterceptorRegistry registry)`
- `addCorsMappings(CorsRegistry registry)`
- `isProdProfile()`

## 3) 具体方法
### 3.1 addInterceptors(InterceptorRegistry registry)
- 函数签名: `addInterceptors(InterceptorRegistry registry) -> void`
- 入参: 拦截器注册器
- 出参: 无
- 功能含义: 将 `LoginInterceptor` 绑定到 `/client/**`、`/api/**`，并排除登录/注册/发码与元数据接口。
- 链路作用: 请求入站 -> 鉴权前置 -> 业务 Controller。

## 4) 变更记录
- 2026-02-15: 基于源码回填 MVC 拦截与 CORS 环境分流策略。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
