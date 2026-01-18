---
name: api-post
description: 专业的HTTP API客户端工具，支持GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS等所有HTTP方法，具备SSE流式响应处理能力。用于API调用、接口测试、HTTP请求调试和响应数据处理。当用户需要执行接口调用、API测试、发送HTTP请求或处理API响应时优先使用此技能。
---

# API Client - HTTP请求执行工具

这是一个功能完整的HTTP API客户端，专为AI执行各种HTTP请求而设计。

## 🚀 核心功能

- **全方法支持**: GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS
- **SSE流式处理**: 支持Server-Sent Events实时响应
- **灵活参数配置**: 请求头、查询参数、请求体自由组合
- **双输出模式**: 终端格式化显示 + 文件保存
- **智能错误处理**: 完整的状态码检查和异常捕获

## 📖 使用指南

### 基本用法

当用户需要执行API调用时，请使用配套的Python脚本：

```bash
python script/api_client.py [选项] <URL>
```

### 常用命令示例

**GET请求**:
```bash
python script/api_client.py --method GET https://api.example.com/users
```

**POST请求带JSON数据**:
```bash
python script/api_client.py --method POST --data '{"name":"张三","email":"zhang@example.com"}' https://api.example.com/users
```

**带认证头的请求**:
```bash
python script/api_client.py --header "Authorization: Bearer token123" --header "Content-Type: application/json" https://api.example.com/protected
```

**保存响应到文件**:
```bash
python script/api_client.py --output response.json https://api.example.com/data
```

**SSE流式响应**:
```bash
python script/api_client.py --method GET --stream https://api.example.com/events
```

## 🛠️ 脚本参数详解

### 主要参数

| 参数 | 简写 | 说明 | 示例 |
|------|------|------|------|
| `--method` | `-X` | HTTP方法 | `--method POST` |
| `--data` | `-d` | 请求体数据 | `--data '{"key":"value"}'` |
| `--header` | `-H` | 请求头(可多次使用) | `--header "Content-Type: application/json"` |
| `--param` | `-p` | 查询参数(可多次使用) | `--param "page=1" --param "size=10"` |
| `--output` | `-o` | 输出文件路径 | `--output result.json` |
| `--stream` | `-s` | 启用流式响应 | `--stream` |
| `--timeout` | `-t` | 超时时间(秒) | `--timeout 30` |
| `--verbose` | `-v` | 详细输出模式 | `--verbose` |

### HTTP方法支持

- `GET`: 获取资源
- `POST`: 创建资源
- `PUT`: 更新完整资源
- `PATCH`: 部分更新资源
- `DELETE`: 删除资源
- `HEAD`: 获取头部信息
- `OPTIONS`: 获取服务器支持的方法

## 📤 响应处理

### 终端输出
- 自动格式化JSON响应
- 彩色状态码显示
- 详细的响应头信息
- 避免长内容截断

### 文件输出
- 支持任意文件路径
- 保留原始响应格式
- 自动创建目录结构

## ⚠️ 注意事项

1. **JSON数据**: 使用单引号包裹，内部双引号不需要转义
2. **URL编码**: 查询参数会自动URL编码
3. **大文件**: 对于大响应建议使用`--output`保存到文件
4. **流式响应**: SSE连接会持续到服务器关闭连接

## 🎯 使用场景

- API接口测试和调试
- 第三方服务集成验证
- 数据获取和同步
- 微服务间通信测试
- RESTful API调用自动化

使用此技能可以让AI轻松执行各种HTTP API调用任务！
