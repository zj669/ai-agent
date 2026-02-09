# AuthService Blueprint

## 职责契约
- **做什么**: 管理认证授权——JWT 令牌的生成/验证/刷新、速率限制
- **不做什么**: 不负责用户数据管理（那是 UserService 的职责）；不负责业务级权限（如 Agent 所有权验证）

## 接口摘要

### ITokenService (Domain Port)
| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| generateToken | userId, claims | String(JWT) | 无 | - |
| validateToken | token | Claims | 无 | 过期抛异常 |
| refreshToken | refreshToken | TokenPair | 无 | 验证refresh token有效性 |

### RateLimiter (Domain Service)
| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| isAllowed | key, limit, window | boolean | 更新计数器(Redis) | 滑动窗口算法 |

## 依赖拓扑
- **上游**: AuthFilter(接口层), UserService
- **下游**: Redis(速率限制计数器)

## 设计约束
- JWT 使用 JJWT 0.12.3 库
- 令牌过期时间可配置
- 速率限制基于 Redis 滑动窗口
- 调试模式下可通过 Header 跳过认证

## 变更日志
- [初始] 从现有代码逆向生成蓝图
